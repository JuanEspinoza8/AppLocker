package com.jair.applocker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.jair.applocker.data.local.DataStoreManager
import com.jair.applocker.domain.LockSettings
import com.jair.applocker.domain.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Punto único de acceso a la configuración: traduce las preferencias de [DataStoreManager] a
 * [LockSettings] y viceversa. Lo usan el ViewModel y el servicio de accesibilidad.
 */
class LockSettingsRepository(
    private val dataStore: DataStoreManager,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    constructor(context: Context) : this(DataStoreManager(context))

    val settings: Flow<LockSettings> = dataStore.preferences
        .map { it.toLockSettings() }
        .distinctUntilChanged()

    val usedPins: Flow<Set<String>> = dataStore.usedPinsFlow

    suspend fun saveLockedApps(packageNames: Set<String>) = dataStore.saveLockedApps(packageNames)

    suspend fun saveLockSchedule(days: Set<Int>, start: LocalTime, end: LocalTime) =
        dataStore.saveLockSchedule(days, TimeFormat.format(start), TimeFormat.format(end))

    suspend fun saveEditWindow(day: Int?, start: LocalTime, end: LocalTime) =
        dataStore.saveEditWindow(day, TimeFormat.format(start), TimeFormat.format(end))

    /** Marca el PIN como usado y habilita la edición durante las próximas 24 horas. */
    suspend fun activateHoliday(pin: String) {
        dataStore.markPinAsUsed(pin)
        dataStore.saveHolidayUntil(ZonedDateTime.now(clock).plusDays(1).toInstant().toEpochMilli())
    }

    private fun Preferences.toLockSettings() = LockSettings(
        lockedApps = this[DataStoreManager.LOCKED_APPS] ?: emptySet(),
        lockedDays = this[DataStoreManager.LOCKED_DAYS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
        lockStart = TimeFormat.parse(this[DataStoreManager.LOCK_START_TIME]),
        lockEnd = TimeFormat.parse(this[DataStoreManager.LOCK_END_TIME]),
        editDay = this[DataStoreManager.EDIT_DAY],
        editStart = TimeFormat.parse(this[DataStoreManager.EDIT_START_TIME]),
        editEnd = TimeFormat.parse(this[DataStoreManager.EDIT_END_TIME]),
        holidayUntilMillis = this[DataStoreManager.HOLIDAY_UNTIL_TIME] ?: 0L,
    )
}
