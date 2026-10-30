package com.jair.applocker.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jair.applocker.data.model.AppInfo
import com.jair.applocker.data.repository.AppRepository
import com.jair.applocker.data.repository.LockSettingsRepository
import com.jair.applocker.domain.LockPolicy
import com.jair.applocker.domain.LockSettings
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

/** Estado de las pantallas de configuración. */
data class AppUiState(
    /** `false` hasta que se lee la configuración guardada. */
    val isLoaded: Boolean = false,
    val settings: LockSettings = LockSettings(),
    val isEditable: Boolean = false,
    /** Apps instaladas, o `null` mientras se cargan. */
    val installedApps: List<AppInfo>? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsRepository = LockSettingsRepository(application)
    private val lockPolicy = LockPolicy()

    // PINs de un solo uso que habilitan el franco
    private val masterPins = setOf(
        "1408", "2731", "3954", "4170", "5092", "6315", "7538", "8761", "9984", "1207",
        "2430", "3653", "4876", "5190", "6413", "7636", "8859", "9082", "1305", "2528",
        "3751", "4974", "5297", "6520", "7743", "8966", "9189", "1403", "2626", "3849",
        "4072", "5395", "6618", "7841", "9064", "9287", "1501", "2724", "3947", "4170",
        "5493", "6716", "7939", "9162", "9385", "1699", "2822", "3045", "4268", "5591",
        "6814", "8037", "9260", "9483", "1797", "2920", "3143", "4366", "5689", "6912",
        "8135", "9358", "9581", "1895", "2018", "3241", "4464", "5787", "6010", "7233",
        "8456", "9679", "1993", "2116", "3339", "4562", "5885", "6108", "7331", "8554",
        "9777", "1091", "2214", "3437", "4660", "5983", "6206", "7429", "8652", "9875",
        "1189", "2312", "3535", "4758", "5081", "6304", "7527", "8750", "9973", "1296"
    )

    private val installedApps = MutableStateFlow<List<AppInfo>?>(null)

    /**
     * Se recalcula con cada cambio de configuración y al comienzo de cada minuto, para que la
     * edición se cierre sola al terminar la ventana o el franco.
     */
    val uiState: StateFlow<AppUiState> = combine(
        settingsRepository.settings,
        installedApps,
        minuteTicker(),
    ) { settings, apps, _ ->
        AppUiState(
            isLoaded = true,
            settings = settings,
            isEditable = lockPolicy.isEditable(settings),
            installedApps = apps,
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

    fun attemptActivateHoliday(pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!masterPins.contains(pin)) {
                onResult(false, "PIN inválido. Pedile uno a tu amigo.")
                return@launch
            }
            if (settingsRepository.usedPins.first().contains(pin)) {
                onResult(false, "Este PIN ya fue utilizado. Pedí otro.")
                return@launch
            }

            settingsRepository.activateHoliday(pin)
            onResult(true, "¡PIN Aceptado! Franco activado por 24 horas.")
        }
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
