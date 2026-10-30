package com.jair.applocker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import com.jair.applocker.data.local.HolidayAuthStore
import com.jair.applocker.data.local.HolidayAuthStore.Companion.FAILED_ATTEMPTS
import com.jair.applocker.data.local.HolidayAuthStore.Companion.LAST_USED_STEP
import com.jair.applocker.data.local.HolidayAuthStore.Companion.LOCKED_UNTIL
import com.jair.applocker.data.local.HolidayAuthStore.Companion.SECRET
import com.jair.applocker.data.security.KeystoreSecretCipher
import com.jair.applocker.data.security.SecretCipher
import com.jair.applocker.domain.AttemptLimiter
import com.jair.applocker.domain.AttemptState
import com.jair.applocker.domain.CodeCheck
import com.jair.applocker.domain.Totp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.security.GeneralSecurityException
import java.time.Clock

/**
 * Autenticador del franco: guarda el secreto TOTP cifrado y verifica los códigos que dicta quien
 * tiene la app de autenticación.
 */
class HolidayCodeRepository(
    private val store: HolidayAuthStore,
    private val cipher: SecretCipher = KeystoreSecretCipher(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    constructor(context: Context) : this(HolidayAuthStore(context))

    val isConfigured: Flow<Boolean> = store.data.map { it[SECRET] != null }.distinctUntilChanged()

    /**
     * Verifica [code] y, si es válido, lo marca como usado. Todo ocurre en una sola transacción,
     * así que el mismo código no se puede aceptar dos veces aunque lleguen dos intentos juntos.
     */
    suspend fun verifyAndConsume(code: String): CodeCheck {
        var result: CodeCheck = CodeCheck.NotConfigured
        store.edit { result = it.consumeCode(code, clock.millis(), cipher) }
        return result
    }

    /**
     * Guarda un secreto nuevo en lugar del anterior. [usedStep] es el paso del código con el que se
     * confirmó la configuración, que ya no se puede usar para un franco.
     */
    suspend fun saveSecret(secret: ByteArray, usedStep: Long) {
        val encrypted = cipher.encrypt(secret)
        store.edit {
            it[SECRET] = encrypted
            it[LAST_USED_STEP] = usedStep
            it[FAILED_ATTEMPTS] = 0
            it[LOCKED_UNTIL] = 0L
        }
    }
}

/** Verifica [code] contra el secreto guardado y actualiza el último paso usado y los intentos. */
internal fun MutablePreferences.consumeCode(code: String, nowMillis: Long, cipher: SecretCipher): CodeCheck {
    val stored = this[SECRET] ?: return CodeCheck.NotConfigured

    val attempts = AttemptState(this[FAILED_ATTEMPTS] ?: 0, this[LOCKED_UNTIL] ?: 0L)
    if (AttemptLimiter.isLockedOut(attempts, nowMillis)) return CodeCheck.LockedOut(attempts.lockedUntilMillis)

    val secret = try {
        cipher.decrypt(stored)
    } catch (e: GeneralSecurityException) {
        // La clave del Keystore ya no existe: el secreto no sirve más y hay que configurarlo de nuevo
        remove(SECRET)
        remove(LAST_USED_STEP)
        return CodeCheck.NotConfigured
    }
    val step = Totp.matchStep(secret, code, nowMillis, this[LAST_USED_STEP] ?: -1L)
    secret.fill(0)

    val result: CodeCheck
    val updated: AttemptState
    if (step != null) {
        this[LAST_USED_STEP] = step
        updated = AttemptLimiter.onSuccess()
        result = CodeCheck.Valid
    } else {
        updated = AttemptLimiter.onFailure(attempts, nowMillis)
        result = if (AttemptLimiter.isLockedOut(updated, nowMillis)) {
            CodeCheck.LockedOut(updated.lockedUntilMillis)
        } else {
            CodeCheck.Invalid(attemptsLeft = AttemptLimiter.MAX_ATTEMPTS - updated.failedAttempts)
        }
    }
    this[FAILED_ATTEMPTS] = updated.failedAttempts
    this[LOCKED_UNTIL] = updated.lockedUntilMillis
    return result
}
