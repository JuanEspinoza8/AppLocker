package com.jair.applocker.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inicializamos DataStore (es como crear el archivo de base de datos)
private val Context.dataStore by preferencesDataStore(name = "applocker_prefs")

class DataStoreManager(private val context: Context) {

    // Definimos las "claves" (keys) para guardar cada dato, como si fuera un JSON
    companion object {
        val LOCKED_APPS = stringSetPreferencesKey("locked_apps")
        val LOCKED_DAYS = stringSetPreferencesKey("locked_days")
        val LOCK_START_TIME = stringPreferencesKey("lock_start_time")
        val LOCK_END_TIME = stringPreferencesKey("lock_end_time")

        val EDIT_DAY = intPreferencesKey("edit_day")
        val EDIT_START_TIME = stringPreferencesKey("edit_start_time")
        val EDIT_END_TIME = stringPreferencesKey("edit_end_time")

        val IS_SYSTEM_ACTIVE = booleanPreferencesKey("is_system_active")
    }

    // --- FUNCIONES PARA GUARDAR DATOS (Setters) ---
    // Usamos 'suspend' porque escribir en disco toma tiempo y no debe congelar la UI

    suspend fun saveLockedApps(packageNames: Set<String>) {
        context.dataStore.edit { prefs -> prefs[LOCKED_APPS] = packageNames }
    }

    suspend fun saveLockSchedule(days: Set<Int>, start: String, end: String) {
        context.dataStore.edit { prefs ->
            // Convertimos el Set<Int> a Set<String> porque DataStore guarda Strings
            prefs[LOCKED_DAYS] = days.map { it.toString() }.toSet()
            prefs[LOCK_START_TIME] = start
            prefs[LOCK_END_TIME] = end
        }
    }

    suspend fun saveEditWindow(day: Int?, start: String, end: String) {
        context.dataStore.edit { prefs ->
            if (day != null) {
                prefs[EDIT_DAY] = day
            } else {
                prefs.remove(EDIT_DAY)
            }
            prefs[EDIT_START_TIME] = start
            prefs[EDIT_END_TIME] = end
        }
    }

    suspend fun setSystemActive(isActive: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_SYSTEM_ACTIVE] = isActive }
    }

    // --- FUNCIONES PARA LEER DATOS (Getters reactivos) ---
    // Flow es un flujo de datos constante: si la base de datos cambia,
    // la interfaz o el servicio se actualizan automáticamente al instante.

    val lockedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { it[LOCKED_APPS] ?: emptySet() }

    val isSystemActiveFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_SYSTEM_ACTIVE] ?: false }

    val lockedDaysFlow: Flow<Set<String>> = context.dataStore.data.map { it[LOCKED_DAYS] ?: emptySet() }
    val lockStartTimeFlow: Flow<String> = context.dataStore.data.map { it[LOCK_START_TIME] ?: "00:00" }
    val lockEndTimeFlow: Flow<String> = context.dataStore.data.map { it[LOCK_END_TIME] ?: "00:00" }

    // (Luego agregaremos los flujos para leer los horarios exactos cuando armemos la lógica del bloqueo)
}