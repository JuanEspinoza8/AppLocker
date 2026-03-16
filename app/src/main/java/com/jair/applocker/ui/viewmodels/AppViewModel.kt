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

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    // Instanciamos nuestro nuevo gestor de base de datos
    private val dataStoreManager = DataStoreManager(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true

            // 1. Buscamos todas las apps instaladas en el celular de tu amigo
            val baseApps = repository.getInstalledApps()

            // 2. Nos quedamos ESCUCHANDO la base de datos en tiempo real.
            // La función 'collect' es un listener reactivo. Cada vez que el DataStore
            // cambie, este bloque de código se vuelve a ejecutar automáticamente.
            dataStoreManager.lockedAppsFlow.collect { lockedPackageNames ->

                // Mapeamos la lista de apps y les ponemos isLocked = true
                // si su nombre (packageName) está guardado en la base de datos
                val updatedApps = baseApps.map { app ->
                    app.copy(isLocked = lockedPackageNames.contains(app.packageName))
                }

                // Actualizamos la pantalla
                _installedApps.value = updatedApps
                _isLoading.value = false
            }
        }
    }

    // Esta función se ejecuta cuando tu amigo toca el switch (interruptor) de una app
    fun toggleAppLock(packageName: String) {
        viewModelScope.launch {
            // Obtenemos la lista actual de los paquetes que están bloqueados
            val currentLocked = _installedApps.value
                .filter { it.isLocked }
                .map { it.packageName }
                .toMutableSet()

            // Si ya estaba en la lista, lo sacamos (desbloqueamos). Si no estaba, lo agregamos.
            if (currentLocked.contains(packageName)) {
                currentLocked.remove(packageName)
            } else {
                currentLocked.add(packageName)
            }

            // Guardamos la nueva lista en el DataStore.
            // ¡Magia!: Al guardar esto, el 'collect' de arriba detecta el cambio
            // y actualiza la interfaz visual automáticamente sin que hagamos nada más.
            dataStoreManager.saveLockedApps(currentLocked)
        }
    }

    fun saveLockConfig(days: Set<Int>, start: String, end: String) {
        viewModelScope.launch {
            dataStoreManager.saveLockSchedule(days, start, end)
        }
    }
}