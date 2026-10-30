package com.jair.applocker.data.repository

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.jair.applocker.data.local.DataStoreManager
import com.jair.applocker.domain.LockSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** El formato guardado no cambió: estos tests protegen las instalaciones existentes. */
class LockSettingsMappingTest {

    @Test
    fun `sin datos guardados se usan los valores por defecto`() {
        assertEquals(LockSettings(), emptyPreferences().toLockSettings())
    }

    @Test
    fun `lee la configuracion con el formato guardado`() {
        val prefs = preferencesOf(
            DataStoreManager.LOCKED_APPS to setOf("com.instagram.android"),
            DataStoreManager.LOCKED_DAYS to setOf("6", "7"),
            DataStoreManager.LOCK_START_TIME to "00:00",
            DataStoreManager.LOCK_END_TIME to "23:59",
            DataStoreManager.EDIT_DAY to 7,
            DataStoreManager.EDIT_START_TIME to "00:00",
            DataStoreManager.EDIT_END_TIME to "23:45",
            DataStoreManager.HOLIDAY_UNTIL_TIME to 1_700_000_000_000L,
        )

        val expected = LockSettings(
            lockedApps = setOf("com.instagram.android"),
            lockedDays = setOf(6, 7),
            lockStart = LocalTime.MIDNIGHT,
            lockEnd = LocalTime.of(23, 59),
            editDay = 7,
            editStart = LocalTime.MIDNIGHT,
            editEnd = LocalTime.of(23, 45),
            holidayUntilMillis = 1_700_000_000_000L,
        )
        assertEquals(expected, prefs.toLockSettings())
    }

    @Test
    fun `ignora dias que no son numeros`() {
        val prefs = preferencesOf(DataStoreManager.LOCKED_DAYS to setOf("1", "x"))

        assertEquals(setOf(1), prefs.toLockSettings().lockedDays)
    }
}
