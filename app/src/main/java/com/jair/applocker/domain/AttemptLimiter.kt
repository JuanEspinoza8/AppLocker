package com.jair.applocker.domain

/** Intentos fallidos de código de franco y, si se superó el máximo, hasta cuándo dura el bloqueo. */
data class AttemptState(
    val failedAttempts: Int = 0,
    /** Epoch millis; `0` si no hay bloqueo. */
    val lockedUntilMillis: Long = 0L,
)

/** Limita los intentos para que no se pueda adivinar un código probando. */
object AttemptLimiter {
    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_MILLIS = 30 * 60_000L

    fun isLockedOut(state: AttemptState, nowMillis: Long): Boolean = nowMillis < state.lockedUntilMillis

    fun onFailure(state: AttemptState, nowMillis: Long): AttemptState {
        val failed = state.failedAttempts + 1
        return if (failed >= MAX_ATTEMPTS) {
            AttemptState(failedAttempts = 0, lockedUntilMillis = nowMillis + LOCKOUT_MILLIS)
        } else {
            state.copy(failedAttempts = failed)
        }
    }

    fun onSuccess(): AttemptState = AttemptState()
}
