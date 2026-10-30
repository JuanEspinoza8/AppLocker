package com.jair.applocker.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jair.applocker.data.model.AppInfo
import com.jair.applocker.data.repository.AppRepository
import com.jair.applocker.data.repository.HolidayCodeRepository
import com.jair.applocker.data.repository.LockSettingsRepository
import com.jair.applocker.domain.Base32
import com.jair.applocker.domain.CodeCheck
import com.jair.applocker.domain.LockPolicy
import com.jair.applocker.domain.LockSettings
import com.jair.applocker.domain.Totp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.SecureRandom

/** Estado de las pantallas de configuración. */
data class AppUiState(
    /** `false` hasta que se lee la configuración guardada. */
    val isLoaded: Boolean = false,
    val settings: LockSettings = LockSettings(),
    val isEditable: Boolean = false,
    /** Apps instaladas, o `null` mientras se cargan. */
    val installedApps: List<AppInfo>? = null,
    /** Si ya hay un autenticador configurado para dar francos. */
    val isHolidayConfigured: Boolean = false,
)

/** Datos para que quien da los francos agregue la cuenta a su app de autenticación. */
data class AuthenticatorSetup(val secretBase32: String, val otpauthUri: String)

sealed interface SetupStart {
    data class Ready(val setup: AuthenticatorSetup) : SetupStart
    /** El código del autenticador actual no fue válido. */
    data class Rejected(val check: CodeCheck) : SetupStart
    /** Sin autenticador previo, solo se puede configurar cuando las reglas son editables. */
    data object NotAllowed : SetupStart
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsRepository = LockSettingsRepository(application)
    private val holidayCodes = HolidayCodeRepository(application)
    private val lockPolicy = LockPolicy()

    /** Secreto generado durante la configuración; se guarda recién cuando se confirma con un código. */
    private var pendingSecret: ByteArray? = null

    private val installedApps = MutableStateFlow<List<AppInfo>?>(null)

    /**
     * Se recalcula con cada cambio de configuración y al comienzo de cada minuto, para que la
     * edición se cierre sola al terminar la ventana o el franco.
     */
    val uiState: StateFlow<AppUiState> = combine(
        settingsRepository.settings,
        installedApps,
        holidayCodes.isConfigured,
        minuteTicker(),
    ) { settings, apps, holidayConfigured, _ ->
        AppUiState(
            isLoaded = true,
            settings = settings,
            isEditable = lockPolicy.isEditable(settings),
            installedApps = apps,
            isHolidayConfigured = holidayConfigured,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        viewModelScope.launch { installedApps.value = appRepository.getInstalledApps() }
    }

    /** Emite al suscribirse y después al comienzo de cada minuto. */
    private fun minuteTicker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(MINUTE_MILLIS - System.currentTimeMillis() % MINUTE_MILLIS)
        }
    }

    /** Activa el franco si [code] es un código válido y sin usar del autenticador. */
    fun attemptActivateHoliday(code: String, onResult: (CodeCheck) -> Unit) {
        viewModelScope.launch {
            val check = holidayCodes.verifyAndConsume(code)
            if (check == CodeCheck.Valid) settingsRepository.activateHoliday()
            onResult(check)
        }
    }

    /**
     * Empieza a configurar el autenticador del franco. La primera vez solo se permite mientras las
     * reglas sean editables; para reemplazar uno existente hace falta un código válido de ese
     * autenticador ([currentCode]), así quien usa el bloqueo no puede quedarse con los francos.
     */
    fun beginAuthenticatorSetup(currentCode: String?, onResult: (SetupStart) -> Unit) {
        viewModelScope.launch {
            if (holidayCodes.isConfigured.first()) {
                val check = holidayCodes.verifyAndConsume(currentCode.orEmpty())
                // NotConfigured acá significa que el secreto guardado no se pudo leer y se borró
                val canStartOver = check == CodeCheck.NotConfigured && currentSettingsIfEditable() != null
                if (check != CodeCheck.Valid && !canStartOver) {
                    onResult(SetupStart.Rejected(check))
                    return@launch
                }
            } else if (currentSettingsIfEditable() == null) {
                onResult(SetupStart.NotAllowed)
                return@launch
            }

            val secret = ByteArray(Totp.SECRET_BYTES).also { SecureRandom().nextBytes(it) }
            pendingSecret?.fill(0)
            pendingSecret = secret
            val setup = AuthenticatorSetup(
                secretBase32 = Base32.encode(secret),
                otpauthUri = Totp.otpauthUri(secret, issuer = "AppLocker", account = "Franco"),
            )
            onResult(SetupStart.Ready(setup))
        }
    }

    /** Guarda el autenticador si [code] coincide con el secreto mostrado. */
    fun confirmAuthenticatorSetup(code: String, onResult: (saved: Boolean) -> Unit) {
        val secret = pendingSecret ?: return onResult(false)
        viewModelScope.launch {
            val step = Totp.matchStep(secret, code, System.currentTimeMillis(), lastUsedStep = -1)
            if (step == null) {
                onResult(false)
                return@launch
            }
            holidayCodes.saveSecret(secret, usedStep = step)
            cancelAuthenticatorSetup()
            onResult(true)
        }
    }

    fun cancelAuthenticatorSetup() {
        pendingSecret?.fill(0)
        pendingSecret = null
    }

    override fun onCleared() {
        cancelAuthenticatorSetup()
    }

    /** La UI deshabilita la edición, pero la regla se valida acá contra la configuración guardada. */
    private suspend fun currentSettingsIfEditable(): LockSettings? =
        settingsRepository.settings.first().takeIf { lockPolicy.isEditable(it) }

    fun toggleAppLock(packageName: String) {
        viewModelScope.launch {
            val lockedApps = currentSettingsIfEditable()?.lockedApps ?: return@launch
            val updated = if (packageName in lockedApps) lockedApps - packageName else lockedApps + packageName
            settingsRepository.saveLockedApps(updated)
        }
    }

    /**
     * Guarda el horario de bloqueo y la ventana de edición de [rules]. [onResult] recibe `false`
     * si en este momento no se permite editar.
     */
    fun saveRules(rules: LockSettings, onResult: (saved: Boolean) -> Unit) {
        viewModelScope.launch {
            if (currentSettingsIfEditable() == null) {
                onResult(false)
                return@launch
            }
            settingsRepository.saveLockSchedule(rules.lockedDays, rules.lockStart, rules.lockEnd)
            settingsRepository.saveEditWindow(rules.editDay, rules.editStart, rules.editEnd)
            onResult(true)
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
