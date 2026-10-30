package com.jair.applocker.data.repository

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.jair.applocker.data.local.HolidayAuthStore
import com.jair.applocker.data.security.SecretCipher
import com.jair.applocker.domain.AttemptLimiter
import com.jair.applocker.domain.CodeCheck
import com.jair.applocker.domain.Totp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.GeneralSecurityException

class ConsumeCodeTest {

    private val secret = "12345678901234567890".toByteArray(Charsets.US_ASCII)
    private val now = 1_234_567_890_000L

    /** Sin Keystore en los tests: "cifra" pasando a hex. */
    private val fakeCipher = object : SecretCipher {
        override fun encrypt(plain: ByteArray) = plain.joinToString("") { "%02x".format(it) }
        override fun decrypt(stored: String) = stored.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private val brokenCipher = object : SecretCipher {
        override fun encrypt(plain: ByteArray) = ""
        override fun decrypt(stored: String): ByteArray = throw GeneralSecurityException("sin clave")
    }

    private fun configuredPrefs() =
        mutablePreferencesOf(HolidayAuthStore.SECRET to fakeCipher.encrypt(secret))

    private fun currentCode() = Totp.code(secret, Totp.timeStep(now))

    @Test
    fun `sin autenticador configurado no acepta codigos`() {
        assertEquals(CodeCheck.NotConfigured, mutablePreferencesOf().consumeCode("123456", now, fakeCipher))
    }

    @Test
    fun `acepta un codigo valido una sola vez`() {
        val prefs = configuredPrefs()

        assertEquals(CodeCheck.Valid, prefs.consumeCode(currentCode(), now, fakeCipher))
        assertEquals(Totp.timeStep(now), prefs[HolidayAuthStore.LAST_USED_STEP])
        assertEquals(CodeCheck.Invalid(AttemptLimiter.MAX_ATTEMPTS - 1), prefs.consumeCode(currentCode(), now, fakeCipher))
    }

    @Test
    fun `cinco codigos incorrectos bloquean los intentos aunque despues llegue uno valido`() {
        val prefs = configuredPrefs()
        repeat(AttemptLimiter.MAX_ATTEMPTS - 1) {
            assertEquals(CodeCheck.Invalid(AttemptLimiter.MAX_ATTEMPTS - 1 - it), prefs.consumeCode("000000", now, fakeCipher))
        }

        val lockedUntil = now + AttemptLimiter.LOCKOUT_MILLIS
        assertEquals(CodeCheck.LockedOut(lockedUntil), prefs.consumeCode("000000", now, fakeCipher))
        assertEquals(CodeCheck.LockedOut(lockedUntil), prefs.consumeCode(currentCode(), now, fakeCipher))
    }

    @Test
    fun `un acierto reinicia los intentos fallidos`() {
        val prefs = configuredPrefs()
        prefs.consumeCode("000000", now, fakeCipher)

        prefs.consumeCode(currentCode(), now, fakeCipher)

        assertEquals(0, prefs[HolidayAuthStore.FAILED_ATTEMPTS])
    }

    @Test
    fun `si el secreto no se puede descifrar se borra y hay que configurarlo de nuevo`() {
        val prefs = configuredPrefs()

        assertEquals(CodeCheck.NotConfigured, prefs.consumeCode(currentCode(), now, brokenCipher))
        assertNull(prefs[HolidayAuthStore.SECRET])
    }
}
