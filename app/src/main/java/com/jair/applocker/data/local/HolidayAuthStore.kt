package com.jair.applocker.data.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

private val Context.holidayAuthDataStore by preferencesDataStore(name = "holiday_auth")

/**
 * Datos del autenticador del franco. Van en un archivo aparte de la configuración para poder
 * excluirlo de los backups (ver `backup_rules.xml`): el secreto está cifrado con una clave del
 * Keystore que no viaja con el backup, así que restaurado en otro teléfono no serviría.
 */
class HolidayAuthStore(context: Context) {

    private val dataStore = context.applicationContext.holidayAuthDataStore

    val data: Flow<Preferences> = dataStore.data

    /** Lee y escribe en una sola transacción. */
    suspend fun edit(transform: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(transform)
    }

    companion object {
        /** Secreto TOTP cifrado con [com.jair.applocker.data.security.KeystoreSecretCipher]. */
        val SECRET = stringPreferencesKey("totp_secret")
        /** Último paso de tiempo usado, para que un código no sirva dos veces. */
        val LAST_USED_STEP = longPreferencesKey("totp_last_used_step")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKED_UNTIL = longPreferencesKey("locked_until")
    }
}
