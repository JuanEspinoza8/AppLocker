package com.jair.applocker.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jair.applocker.data.model.AppInfo
import com.jair.applocker.data.repository.AppRepository
import com.jair.applocker.data.repository.LockSettingsRepository
import com.jair.applocker.domain.LockPolicy
import com.jair.applocker.domain.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
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

    private var usedPins: Set<String> = emptySet()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- ESTADOS DE LA UI ---
    private val _lockedDays = MutableStateFlow<Set<Int>>(emptySet())
    val lockedDays: StateFlow<Set<Int>> = _lockedDays.asStateFlow()

    private val _lockStartTime = MutableStateFlow("00:00")
    val lockStartTime: StateFlow<String> = _lockStartTime.asStateFlow()

    private val _lockEndTime = MutableStateFlow("00:00")
    val lockEndTime: StateFlow<String> = _lockEndTime.asStateFlow()

    // Ventana de Edición
    private val _editDay = MutableStateFlow<Int?>(null)
    val editDay: StateFlow<Int?> = _editDay.asStateFlow()

    private val _editStartTime = MutableStateFlow("00:00")
    val editStartTime: StateFlow<String> = _editStartTime.asStateFlow()

    private val _editEndTime = MutableStateFlow("00:00")
    val editEndTime: StateFlow<String> = _editEndTime.asStateFlow()

    private val _isEditable = MutableStateFlow(true)
    val isEditable: StateFlow<Boolean> = _isEditable.asStateFlow()

    init {
        loadApps()
        loadSavedConfigAndEvaluate()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val baseApps = repository.getInstalledApps()
            settingsRepository.settings.map { it.lockedApps }.distinctUntilChanged().collect { lockedPackageNames ->
                _installedApps.value = baseApps.map { app ->
                    app.copy(isLocked = lockedPackageNames.contains(app.packageName))
                }
                _isLoading.value = false
            }
        }
    }

    private fun loadSavedConfigAndEvaluate() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _lockedDays.value = settings.lockedDays
                _lockStartTime.value = TimeFormat.format(settings.lockStart)
                _lockEndTime.value = TimeFormat.format(settings.lockEnd)
                _editDay.value = settings.editDay
                _editStartTime.value = TimeFormat.format(settings.editStart)
                _editEndTime.value = TimeFormat.format(settings.editEnd)
            }
        }
        // Se recalcula con cada cambio de configuración y con el paso del tiempo, para que
        // la edición se cierre sola al terminar la ventana o el franco
        viewModelScope.launch {
            combine(settingsRepository.settings, minuteTicker()) { settings, _ -> lockPolicy.isEditable(settings) }
                .distinctUntilChanged()
                .collect { _isEditable.value = it }
        }
        viewModelScope.launch { settingsRepository.usedPins.collect { usedPins = it } }
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
            if (usedPins.contains(pin)) {
                onResult(false, "Este PIN ya fue utilizado. Pedí otro.")
                return@launch
            }

            settingsRepository.activateHoliday(pin)
            onResult(true, "¡PIN Aceptado! Franco activado por 24 horas.")
        }
    }

    /** La UI deshabilita la edición, pero la regla se valida acá contra la configuración guardada. */
    private suspend fun canEditNow(): Boolean =
        lockPolicy.isEditable(settingsRepository.settings.first())

    fun toggleAppLock(packageName: String) {
        viewModelScope.launch {
            if (!canEditNow()) return@launch
            val currentLocked = _installedApps.value.filter { it.isLocked }.map { it.packageName }.toMutableSet()
            if (currentLocked.contains(packageName)) currentLocked.remove(packageName) else currentLocked.add(packageName)
            settingsRepository.saveLockedApps(currentLocked)
        }
    }

    /** Guarda el horario de bloqueo y la ventana de edición. [onResult] recibe `false` si no estaba permitido. */
    fun saveRules(
        lockDays: Set<Int>, lockStart: String, lockEnd: String,
        editDay: Int?, editStart: String, editEnd: String,
        onResult: (saved: Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            if (!canEditNow()) {
                onResult(false)
                return@launch
            }
            settingsRepository.saveLockSchedule(lockDays, TimeFormat.parse(lockStart), TimeFormat.parse(lockEnd))
            settingsRepository.saveEditWindow(editDay, TimeFormat.parse(editStart), TimeFormat.parse(editEnd))
            onResult(true)
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
