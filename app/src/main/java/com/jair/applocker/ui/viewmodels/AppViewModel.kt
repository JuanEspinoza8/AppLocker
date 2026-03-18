package com.jair.applocker.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jair.applocker.data.local.DataStoreManager
import com.jair.applocker.data.model.AppInfo
import com.jair.applocker.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val dataStoreManager = DataStoreManager(application)

    // Lista maestra de 100 PINes (No tocar)
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
    private var holidayUntil: Long = 0L

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

    private var internalEditDay: Int? = null
    private var internalEditStart: String = "00:00"
    private var internalEditEnd: String = "00:00"

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
            dataStoreManager.lockedAppsFlow.collect { lockedPackageNames ->
                _installedApps.value = baseApps.map { app ->
                    app.copy(isLocked = lockedPackageNames.contains(app.packageName))
                }
                _isLoading.value = false
            }
        }
    }

    private fun loadSavedConfigAndEvaluate() {
        viewModelScope.launch {
            dataStoreManager.lockedDaysFlow.collect { daysStr ->
                _lockedDays.value = daysStr.mapNotNull { it.toIntOrNull() }.toSet()
                evaluateLockStatus()
            }
        }
        viewModelScope.launch { dataStoreManager.lockStartTimeFlow.collect { _lockStartTime.value = it } }
        viewModelScope.launch { dataStoreManager.lockEndTimeFlow.collect { _lockEndTime.value = it } }

        // Recolectar Ventana de Edición
        viewModelScope.launch { dataStoreManager.editDayFlow.collect { _editDay.value = it; internalEditDay = it; evaluateLockStatus() } }
        viewModelScope.launch { dataStoreManager.editStartTimeFlow.collect { _editStartTime.value = it; internalEditStart = it; evaluateLockStatus() } }
        viewModelScope.launch { dataStoreManager.editEndTimeFlow.collect { _editEndTime.value = it; internalEditEnd = it; evaluateLockStatus() } }

        // Recolectar Vacaciones
        viewModelScope.launch { dataStoreManager.usedPinsFlow.collect { usedPins = it } }
        viewModelScope.launch { dataStoreManager.holidayUntilFlow.collect { until -> holidayUntil = until; evaluateLockStatus() } }
    }

    private fun evaluateLockStatus() {
        val currentTime = Calendar.getInstance().timeInMillis

        // REGLA 0: Si está recién instalada o no hay días configurados
        if (_lockedDays.value.isEmpty()) {
            _isEditable.value = true
            return
        }

        // REGLA 1: Franco activo (PIN)
        if (currentTime < holidayUntil) {
            _isEditable.value = true
            return
        }

        // REGLA 2: Ventana de edición semanal
        if (internalEditDay != null) {
            val calendar = Calendar.getInstance()
            val currentDayMapped = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1

            if (currentDayMapped == internalEditDay) {
                val currentTotalMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)
                val startParts = internalEditStart.split(":")
                val startTotalMinutes = (startParts[0].toInt() * 60) + startParts[1].toInt()
                val endParts = internalEditEnd.split(":")
                val endTotalMinutes = (endParts[0].toInt() * 60) + endParts[1].toInt()

                val inEditWindow = if (startTotalMinutes <= endTotalMinutes) {
                    currentTotalMinutes in startTotalMinutes..endTotalMinutes
                } else {
                    currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
                }

                if (inEditWindow) {
                    _isEditable.value = true
                    return
                }
            }
        }

        // Si fallan todas las reglas, se bloquea la app
        _isEditable.value = false
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

            dataStoreManager.markPinAsUsed(pin)
            dataStoreManager.activateHolidayMode()
            onResult(true, "¡PIN Aceptado! Franco activado por 24 horas.")
        }
    }

    fun toggleAppLock(packageName: String) {
        viewModelScope.launch {
            val currentLocked = _installedApps.value.filter { it.isLocked }.map { it.packageName }.toMutableSet()
            if (currentLocked.contains(packageName)) currentLocked.remove(packageName) else currentLocked.add(packageName)
            dataStoreManager.saveLockedApps(currentLocked)
        }
    }

    fun saveLockConfig(days: Set<Int>, start: String, end: String) {
        viewModelScope.launch { dataStoreManager.saveLockSchedule(days, start, end) }
    }

    fun saveEditWindowConfig(day: Int?, start: String, end: String) {
        viewModelScope.launch { dataStoreManager.saveEditWindow(day, start, end) }
    }
}