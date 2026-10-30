package com.jair.applocker.domain

import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Decide qué se bloquea y cuándo se pueden editar las reglas.
 *
 * No depende de Android: la hora sale del [Clock] recibido, así que se puede testear con
 * [Clock.fixed]. La usan tanto el servicio de accesibilidad como el ViewModel.
 *
 * Las franjas horarias tienen precisión de minuto e incluyen ambos extremos.
 */
class LockPolicy(private val clock: Clock = Clock.systemDefaultZone()) {

    /**
     * Si en este momento se pueden modificar las reglas. Mientras sea `true`, Ajustes no se
     * bloquea.
     */
    fun isEditable(settings: LockSettings): Boolean {
        // Regla 0: sin días de bloqueo (por ejemplo, recién instalada) no hay nada que proteger.
        if (settings.lockedDays.isEmpty()) return true

        // Regla 1: franco activo.
        if (clock.millis() < settings.holidayUntilMillis) return true

        // Regla 2: ventana de edición semanal.
        val editDay = settings.editDay ?: return false
        return isWithin(setOf(editDay), settings.editStart, settings.editEnd)
    }

    /** Si el horario de bloqueo de apps está activo en este momento. */
    fun isScheduleActive(settings: LockSettings): Boolean =
        isWithin(settings.lockedDays, settings.lockStart, settings.lockEnd)

    /** Si hay que tapar la app [packageName] con la pantalla de bloqueo. */
    fun shouldBlock(packageName: String, settings: LockSettings): Boolean {
        if (packageName in SETTINGS_PACKAGES && !isEditable(settings)) return true
        return packageName in settings.lockedApps && isScheduleActive(settings)
    }

    private fun isWithin(days: Set<Int>, start: LocalTime, end: LocalTime): Boolean {
        val now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
        if (now.dayOfWeek.value !in days) return false

        val time = now.toLocalTime()
        return if (start <= end) {
            time in start..end
        } else {
            time >= start || time <= end
        }
    }

    companion object {
        /** Ajustes de Android y de Samsung: se bloquean fuera de la ventana de edición. */
        val SETTINGS_PACKAGES = setOf("com.android.settings", "com.samsung.android.settings")
    }
}
