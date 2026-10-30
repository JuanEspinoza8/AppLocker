package com.jair.applocker.domain

/** Resultado de verificar un código del autenticador del franco. */
sealed interface CodeCheck {
    data object Valid : CodeCheck
    data class Invalid(val attemptsLeft: Int) : CodeCheck
    data class LockedOut(val untilMillis: Long) : CodeCheck
    /** Todavía no se configuró un autenticador. */
    data object NotConfigured : CodeCheck
}
