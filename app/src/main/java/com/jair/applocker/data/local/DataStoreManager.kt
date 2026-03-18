package com.jair.applocker.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore by preferencesDataStore(name = "applocker_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val LOCKED_APPS = stringSetPreferencesKey("locked_apps")
        val LOCKED_DAYS = stringSetPreferencesKey("locked_days")
        val LOCK_START_TIME = stringPreferencesKey("lock_start_time")
        val LOCK_END_TIME = stringPreferencesKey("lock_end_time")

        // Mantener la ventana de edición por si acaso, aunque usaremos el PIN
        val EDIT_DAY = intPreferencesKey("edit_day")
        val EDIT_START_TIME = stringPreferencesKey("edit_start_time")
        val EDIT_END_TIME = stringPreferencesKey("edit_end_time")

        // --- NUEVOS: LÓGICA DE VACACIONES ---
        // Guardamos hasta qué hora exacta (en milisegundos) dura el franco
        val HOLIDAY_UNTIL_TIME = longPreferencesKey("holiday_until_time")
        // Guardamos los pines que ya usó para bloquearlos
        val USED_PINS = stringSetPreferencesKey("used_pins")
    }

    // Setters
    suspend fun saveLockedApps(packageNames: Set<String>) { context.dataStore.edit { it[LOCKED_APPS] = packageNames } }
    suspend fun saveLockSchedule(days: Set<Int>, start: String, end: String) {
        context.dataStore.edit {
            it[LOCKED_DAYS] = days.map { it.toString() }.toSet()
            it[LOCK_START_TIME] = start
            it[LOCK_END_TIME] = end
        }
    }
    suspend fun saveEditWindow(day: Int?, start: String, end: String) {
        context.dataStore.edit {
            if (day != null) it[EDIT_DAY] = day else it.remove(EDIT_DAY)
            it[EDIT_START_TIME] = start
            it[EDIT_END_TIME] = end
        }
    }

    // --- NUEVOS SETTERS ---
    suspend fun activateHolidayMode() {
        context.dataStore.edit { prefs ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Sumamos 24 horas exactas a partir de ahora
            prefs[HOLIDAY_UNTIL_TIME] = calendar.timeInMillis
        }
    }

    suspend fun markPinAsUsed(pin: String) {
        context.dataStore.edit { prefs ->
            val currentUsed = prefs[USED_PINS] ?: emptySet()
            prefs[USED_PINS] = currentUsed + pin
        }
    }

    // Getters Reactivos (Flows)
    val lockedAppsFlow: Flow<Set<String>> = context.dataStore.data.map { it[LOCKED_APPS] ?: emptySet() }
    val lockedDaysFlow: Flow<Set<String>> = context.dataStore.data.map { it[LOCKED_DAYS] ?: emptySet() }
    val lockStartTimeFlow: Flow<String> = context.dataStore.data.map { it[LOCK_START_TIME] ?: "00:00" }
    val lockEndTimeFlow: Flow<String> = context.dataStore.data.map { it[LOCK_END_TIME] ?: "00:00" }

    val editDayFlow: Flow<Int?> = context.dataStore.data.map { it[EDIT_DAY] }
    val editStartTimeFlow: Flow<String> = context.dataStore.data.map { it[EDIT_START_TIME] ?: "00:00" }
    val editEndTimeFlow: Flow<String> = context.dataStore.data.map { it[EDIT_END_TIME] ?: "00:00" }

    // --- NUEVOS GETTERS ---
    // Nos devuelve hasta qué hora duran las vacaciones
    val holidayUntilFlow: Flow<Long> = context.dataStore.data.map { it[HOLIDAY_UNTIL_TIME] ?: 0L }
    // Nos devuelve la lista negra de pines usados
    val usedPinsFlow: Flow<Set<String>> = context.dataStore.data.map { it[USED_PINS] ?: emptySet() }
}