package com.jair.applocker.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "applocker_prefs")

/**
 * Lectura y escritura de las preferencias con DataStore. Las claves no se deben renombrar: ya
 * hay instalaciones con datos guardados con estos nombres.
 */
class DataStoreManager(private val context: Context) {

    companion object {
        val LOCKED_APPS = stringSetPreferencesKey("locked_apps")
        val LOCKED_DAYS = stringSetPreferencesKey("locked_days")
        val LOCK_START_TIME = stringPreferencesKey("lock_start_time")
        val LOCK_END_TIME = stringPreferencesKey("lock_end_time")

        // Ventana de edición semanal
        val EDIT_DAY = intPreferencesKey("edit_day")
        val EDIT_START_TIME = stringPreferencesKey("edit_start_time")
        val EDIT_END_TIME = stringPreferencesKey("edit_end_time")

        // Franco: fin en epoch millis y PINs ya usados (cada uno sirve una sola vez)
        val HOLIDAY_UNTIL_TIME = longPreferencesKey("holiday_until_time")
        val USED_PINS = stringSetPreferencesKey("used_pins")
    }

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

    suspend fun saveHolidayUntil(untilMillis: Long) {
        context.dataStore.edit { it[HOLIDAY_UNTIL_TIME] = untilMillis }
    }

    suspend fun markPinAsUsed(pin: String) {
        context.dataStore.edit { prefs ->
            val currentUsed = prefs[USED_PINS] ?: emptySet()
            prefs[USED_PINS] = currentUsed + pin
        }
    }

    /** Todas las preferencias; cada cambio emite una instantánea completa. */
    val preferences: Flow<Preferences> = context.dataStore.data

    val usedPinsFlow: Flow<Set<String>> = context.dataStore.data.map { it[USED_PINS] ?: emptySet() }
}