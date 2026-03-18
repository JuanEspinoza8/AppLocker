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

    // --- DATOS MAESTROS (NO TOCAR) ---
    // Esta es la lista que generamos. La app necesita saber cuáles son válidos.
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

    // Variables internas de control
    private var usedPins: Set<String> = emptySet()
    private var holidayUntil: Long = 0L

    // Estados para la UI
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lockedDays = MutableStateFlow<Set<Int>>(emptySet())
    val lockedDays: StateFlow<Set<Int>> = _lockedDays.asStateFlow()

    private val _lockStartTime = MutableStateFlow("00:00")
    val lockStartTime: StateFlow<String> = _lockStartTime.asStateFlow()

    private val _lockEndTime = MutableStateFlow("00:00")
    val lockEndTime: StateFlow<String> = _lockEndTime.asStateFlow()

    // El estado definitivo que bloquea la pantalla
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
        // Cargar Horarios de Bloqueo (UI estática)
        viewModelScope.launch { dataStoreManager.lockedDaysFlow.collect { daysStr -> _lockedDays.value = daysStr.mapNotNull { it.toIntOrNull() }.toSet() } }
        viewModelScope.launch { dataStoreManager.lockStartTimeFlow.collect { _lockStartTime.value = it } }
        viewModelScope.launch { dataStoreManager.lockEndTimeFlow.collect { _lockEndTime.value = it } }

        // --- LÓGICA DE EVALUACIÓN DEL CANDADO (NUEVA) ---
        // Escuchamos los pines usados y el tiempo de vacaciones al mismo tiempo
        viewModelScope.launch {
            dataStoreManager.usedPinsFlow.collect { usedPins = it }
        }

        viewModelScope.launch {
            dataStoreManager.holidayUntilFlow.collect { until ->
                holidayUntil = until
                evaluateLockStatus()
            }
        }
    }

    // El núcleo de la decisión: ¿Bloqueamos la app AppLocker?
    private fun evaluateLockStatus() {
        val currentTime = Calendar.getInstance().timeInMillis

        // REGLA 1: Si estamos dentro de las 24hs de franco, la app SIEMPRE es editable.
        if (currentTime < holidayUntil) {
            _isEditable.value = true
            return
        }

        // REGLA 2: Si expiró el franco, la app se vuelve INMODIFICABLE por defecto.
        // (Borramos la lógica de la ventana de edición anterior por simplicidad y rigor)
        _isEditable.value = false
    }

    // --- NUEVA FUNCIÓN: INTENTAR ACTIVAR FRANCO CON PIN ---
    // Devuelve 'true' si el PIN fue válido y activó el franco, 'false' si falló.
    fun attemptActivateHoliday(pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // 1. Verificar si el PIN existe en la lista maestra
            if (!masterPins.contains(pin)) {
                onResult(false, "PIN inválido. Pedile uno a tu amigo.")
                return@launch
            }

            // 2. Verificar si el PIN ya fue usado
            if (usedPins.contains(pin)) {
                onResult(false, "Este PIN ya fue utilizado. Pedí otro.")
                return@launch
            }

            // 3. PIN Válido y Nuevo: Activar magía
            dataStoreManager.markPinAsUsed(pin) // Quemamos el PIN
            dataStoreManager.activateHolidayMode() // Activamos 24hs de franco
            onResult(true, "¡PIN Aceptado! Franco activado por 24 horas.")
        }
    }

    // Funciones de guardado estándar
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
}