package com.jair.applocker.domain

import java.time.LocalTime

/**
 * Reglas configuradas por el usuario.
 *
 * Los días van de 1 (lunes) a 7 (domingo), igual que [java.time.DayOfWeek.getValue].
 */
data class LockSettings(
    val lockedApps: Set<String> = emptySet(),
    val lockedDays: Set<Int> = emptySet(),
    val lockStart: LocalTime = LocalTime.MIDNIGHT,
    val lockEnd: LocalTime = LocalTime.MIDNIGHT,
    /** Día de la ventana de edición semanal, o `null` si no hay ventana. */
    val editDay: Int? = null,
    val editStart: LocalTime = LocalTime.MIDNIGHT,
    val editEnd: LocalTime = LocalTime.MIDNIGHT,
    /** Fin del franco en epoch millis. `0` si nunca se activó. */
    val holidayUntilMillis: Long = 0L,
)
