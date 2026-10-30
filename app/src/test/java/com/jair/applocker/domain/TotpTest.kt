package com.jair.applocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpTest {

    /** Secreto de los vectores de prueba de RFC 6238 (apéndice B) para SHA1. */
    private val rfcSecret = "12345678901234567890".toByteArray(Charsets.US_ASCII)

    private fun stepAt(seconds: Long) = Totp.timeStep(seconds * 1000)

    @Test
    fun `genera los codigos de los vectores de RFC 6238`() {
        val vectors = mapOf(
            59L to "94287082",
            1_111_111_109L to "07081804",
            1_111_111_111L to "14050471",
            1_234_567_890L to "89005924",
            2_000_000_000L to "69279037",
            20_000_000_000L to "65353130",
        )

        vectors.forEach { (seconds, expected) ->
            assertEquals("t=$seconds", expected, Totp.code(rfcSecret, stepAt(seconds), digits = 8))
        }
    }

    @Test
    fun `con 6 digitos son los ultimos 6 del codigo de 8`() {
        assertEquals("287082", Totp.code(rfcSecret, stepAt(59)))
        assertEquals("005924", Totp.code(rfcSecret, stepAt(1_234_567_890)))
    }

    @Test
    fun `acepta el codigo del paso actual y los vecinos por desfasaje de reloj`() {
        val now = 1_234_567_890_000L
        val current = Totp.timeStep(now)

        assertEquals(current, Totp.matchStep(rfcSecret, Totp.code(rfcSecret, current), now, lastUsedStep = -1))
        assertEquals(current - 1, Totp.matchStep(rfcSecret, Totp.code(rfcSecret, current - 1), now, lastUsedStep = -1))
        assertEquals(current + 1, Totp.matchStep(rfcSecret, Totp.code(rfcSecret, current + 1), now, lastUsedStep = -1))
    }

    @Test
    fun `rechaza codigos de hace mas de un paso`() {
        val now = 1_234_567_890_000L
        val current = Totp.timeStep(now)

        assertNull(Totp.matchStep(rfcSecret, Totp.code(rfcSecret, current - 2), now, lastUsedStep = -1))
        assertNull(Totp.matchStep(rfcSecret, Totp.code(rfcSecret, current + 2), now, lastUsedStep = -1))
    }

    @Test
    fun `un codigo ya usado no sirve de nuevo`() {
        val now = 1_234_567_890_000L
        val current = Totp.timeStep(now)
        val code = Totp.code(rfcSecret, current)

        assertNull(Totp.matchStep(rfcSecret, code, now, lastUsedStep = current))
    }

    @Test
    fun `rechaza codigos mal formados`() {
        val now = 1_234_567_890_000L

        assertNull(Totp.matchStep(rfcSecret, "", now, lastUsedStep = -1))
        assertNull(Totp.matchStep(rfcSecret, "12345", now, lastUsedStep = -1))
        assertNull(Totp.matchStep(rfcSecret, "12345a", now, lastUsedStep = -1))
    }

    @Test
    fun `el uri otpauth incluye el secreto en Base32 y los parametros`() {
        val uri = Totp.otpauthUri(rfcSecret, issuer = "AppLocker", account = "Franco")

        assertEquals(
            "otpauth://totp/AppLocker:Franco?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=AppLocker" +
                "&algorithm=SHA1&digits=6&period=30",
            uri
        )
    }

    @Test
    fun `Base32 sigue los vectores de RFC 4648 sin relleno`() {
        assertEquals("", Base32.encode(ByteArray(0)))
        assertEquals("MY", Base32.encode("f".toByteArray()))
        assertEquals("MZXQ", Base32.encode("fo".toByteArray()))
        assertEquals("MZXW6", Base32.encode("foo".toByteArray()))
        assertEquals("MZXW6YQ", Base32.encode("foob".toByteArray()))
        assertEquals("MZXW6YTB", Base32.encode("fooba".toByteArray()))
        assertEquals("MZXW6YTBOI", Base32.encode("foobar".toByteArray()))
        assertTrue(Base32.encode(rfcSecret).length == 32)
    }
}
