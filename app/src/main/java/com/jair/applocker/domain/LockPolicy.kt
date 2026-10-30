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

    /**
     * Si el fin es anterior al inicio, la franja cruza la medianoche y pertenece al día en que
     * empieza (como el "No molestar" de Android): lunes 22:00–07:00 va del lunes a las 22:00 al
     * martes a las 07:00.
     */
    private fun isWithin(days: Set<Int>, start: LocalTime, end: LocalTime): Boolean {
        val now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
        val today = now.dayOfWeek.value
        val time = now.toLocalTime()

        return if (start <= end) {
            today in days && time in start..end
        } else {
            val yesterday = now.minusDays(1).dayOfWeek.value
            (today in days && time >= start) || (yesterday in days && time <= end)
        }
    }

    companion object {
        /** Ajustes de Android y de Samsung: se bloquean fuera de la ventana de edición. */
        val SETTINGS_PACKAGES = setOf("com.android.settings", "com.samsung.android.settings")
    }
}
