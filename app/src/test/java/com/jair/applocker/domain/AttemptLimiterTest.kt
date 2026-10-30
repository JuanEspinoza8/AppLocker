package com.jair.applocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttemptLimiterTest {

    private val now = 1_000_000L

    private fun failTimes(times: Int): AttemptState =
        (1..times).fold(AttemptState()) { state, _ -> AttemptLimiter.onFailure(state, now) }

    @Test
    fun `antes del maximo de intentos no hay bloqueo`() {
        val state = failTimes(AttemptLimiter.MAX_ATTEMPTS - 1)

        assertEquals(AttemptLimiter.MAX_ATTEMPTS - 1, state.failedAttempts)
        assertFalse(AttemptLimiter.isLockedOut(state, now))
    }

    @Test
    fun `al llegar al maximo se bloquea por un rato`() {
        val state = failTimes(AttemptLimiter.MAX_ATTEMPTS)

        assertTrue(AttemptLimiter.isLockedOut(state, now))
        assertTrue(AttemptLimiter.isLockedOut(state, now + AttemptLimiter.LOCKOUT_MILLIS - 1))
        assertFalse(AttemptLimiter.isLockedOut(state, now + AttemptLimiter.LOCKOUT_MILLIS))
    }

    @Test
    fun `despues del bloqueo el contador arranca de cero`() {
        val state = failTimes(AttemptLimiter.MAX_ATTEMPTS)

        assertEquals(0, state.failedAttempts)
    }

    @Test
    fun `un acierto reinicia el contador`() {
        assertEquals(AttemptState(), AttemptLimiter.onSuccess())
    }
}
