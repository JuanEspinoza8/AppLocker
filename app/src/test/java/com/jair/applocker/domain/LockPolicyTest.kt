package com.jair.applocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class LockPolicyTest {

    private val zone = ZoneOffset.UTC

    /** Semana de referencia: el 16/03/2026 es lunes. */
    private val referenceMonday = LocalDate.of(2026, 3, 16)

    private fun dateTime(day: DayOfWeek, time: String): LocalDateTime =
        referenceMonday.plusDays(day.value - 1L).atTime(LocalTime.parse(time))

    private fun policyAt(day: DayOfWeek, time: String): LockPolicy =
        LockPolicy(Clock.fixed(dateTime(day, time).atZone(zone).toInstant(), zone))

    private fun millisAt(day: DayOfWeek, time: String): Long =
        dateTime(day, time).atZone(zone).toInstant().toEpochMilli()

    private fun schedule(days: Set<Int>, start: String, end: String) = LockSettings(
        lockedApps = setOf(BLOCKED_APP),
        lockedDays = days,
        lockStart = LocalTime.parse(start),
        lockEnd = LocalTime.parse(end),
    )

    @Test
    fun `la semana de referencia empieza en lunes`() {
        assertEquals(MONDAY, referenceMonday.dayOfWeek)
    }

    // --- Regla 0 ---

    @Test
    fun `sin dias de bloqueo todo es editable`() {
        val settings = LockSettings(lockedApps = setOf(BLOCKED_APP))

        val policy = policyAt(MONDAY, "12:00")

        assertTrue(policy.isEditable(settings))
        assertFalse(policy.shouldBlock(SETTINGS_APP, settings))
        assertFalse(policy.shouldBlock(BLOCKED_APP, settings))
    }

    @Test
    fun `con dias de bloqueo y sin ventana ni franco no es editable`() {
        val settings = schedule(setOf(1), "09:00", "17:00")

        val policy = policyAt(SATURDAY, "12:00")

        assertFalse(policy.isEditable(settings))
        assertTrue(policy.shouldBlock(SETTINGS_APP, settings))
    }

    // --- Días ---

    @Test
    fun `domingo es el dia 7`() {
        val settings = schedule(setOf(7), "00:00", "23:59")

        assertTrue(policyAt(SUNDAY, "12:00").isScheduleActive(settings))
        assertFalse(policyAt(MONDAY, "12:00").isScheduleActive(settings))
        assertFalse(policyAt(SATURDAY, "12:00").isScheduleActive(settings))
    }

    @Test
    fun `lunes es el dia 1`() {
        val settings = schedule(setOf(1), "00:00", "23:59")

        assertTrue(policyAt(MONDAY, "12:00").isScheduleActive(settings))
        assertFalse(policyAt(TUESDAY, "12:00").isScheduleActive(settings))
    }

    // --- Límites de la franja ---

    @Test
    fun `la franja incluye el minuto de inicio y el de fin`() {
        val settings = schedule(setOf(1), "09:00", "17:00")

        assertFalse(policyAt(MONDAY, "08:59").isScheduleActive(settings))
        assertTrue(policyAt(MONDAY, "09:00").isScheduleActive(settings))
        assertTrue(policyAt(MONDAY, "17:00").isScheduleActive(settings))
        assertTrue(policyAt(MONDAY, "17:00:59").isScheduleActive(settings))
        assertFalse(policyAt(MONDAY, "17:01").isScheduleActive(settings))
    }

    @Test
    fun `una franja que cruza la medianoche bloquea la noche del dia elegido`() {
        val settings = schedule(setOf(1), "22:00", "07:00")

        assertFalse(policyAt(MONDAY, "21:59").isScheduleActive(settings))
        assertTrue(policyAt(MONDAY, "22:00").isScheduleActive(settings))
        assertTrue(policyAt(MONDAY, "23:59").isScheduleActive(settings))
        assertFalse(policyAt(MONDAY, "12:00").isScheduleActive(settings))
    }

    @Test
    fun `una franja que cruza la medianoche termina al dia siguiente`() {
        val settings = schedule(setOf(1), "22:00", "07:00")

        assertTrue(policyAt(TUESDAY, "00:00").isScheduleActive(settings))
        assertTrue(policyAt(TUESDAY, "07:00").isScheduleActive(settings))
        assertFalse(policyAt(TUESDAY, "07:01").isScheduleActive(settings))
        // La madrugada del lunes pertenece a la noche del domingo, que no está elegido
        assertFalse(policyAt(MONDAY, "06:00").isScheduleActive(settings))
    }

    @Test
    fun `el cruce de medianoche funciona de domingo a lunes`() {
        val settings = schedule(setOf(7), "23:00", "01:00")

        assertTrue(policyAt(SUNDAY, "23:30").isScheduleActive(settings))
        assertTrue(policyAt(MONDAY, "00:30").isScheduleActive(settings))
        assertFalse(policyAt(SUNDAY, "00:30").isScheduleActive(settings))
    }

    // --- Bloqueo de apps ---

    @Test
    fun `una app elegida se bloquea solo dentro del horario`() {
        val settings = schedule(setOf(1), "09:00", "17:00")

        assertTrue(policyAt(MONDAY, "10:00").shouldBlock(BLOCKED_APP, settings))
        assertFalse(policyAt(MONDAY, "18:00").shouldBlock(BLOCKED_APP, settings))
        assertFalse(policyAt(MONDAY, "10:00").shouldBlock(OTHER_APP, settings))
    }

    // --- Ventana de edición ---

    @Test
    fun `la ventana de edicion habilita la edicion solo ese dia y en ese horario`() {
        val settings = schedule(setOf(1, 2, 3, 4, 5), "09:00", "17:00").copy(
            editDay = 7,
            editStart = LocalTime.parse("10:00"),
            editEnd = LocalTime.parse("12:00"),
        )

        assertTrue(policyAt(SUNDAY, "10:00").isEditable(settings))
        assertTrue(policyAt(SUNDAY, "12:00").isEditable(settings))
        assertFalse(policyAt(SUNDAY, "12:01").isEditable(settings))
        assertFalse(policyAt(SATURDAY, "11:00").isEditable(settings))
        assertFalse(policyAt(SUNDAY, "11:00").shouldBlock(SETTINGS_APP, settings))
        assertTrue(policyAt(SUNDAY, "13:00").shouldBlock(SETTINGS_APP, settings))
    }

    @Test
    fun `una ventana de edicion que cruza la medianoche sigue abierta al dia siguiente`() {
        val settings = schedule(setOf(1, 2, 3, 4, 5), "09:00", "17:00").copy(
            editDay = 7,
            editStart = LocalTime.parse("23:00"),
            editEnd = LocalTime.parse("01:00"),
        )

        assertTrue(policyAt(SUNDAY, "23:30").isEditable(settings))
        assertTrue(policyAt(MONDAY, "00:30").isEditable(settings))
        assertFalse(policyAt(SUNDAY, "00:30").isEditable(settings))
    }

    @Test
    fun `sin dia de edicion la ventana no aplica`() {
        val settings = schedule(setOf(1), "09:00", "17:00").copy(
            editDay = null,
            editStart = LocalTime.parse("00:00"),
            editEnd = LocalTime.parse("23:59"),
        )

        assertFalse(policyAt(MONDAY, "12:00").isEditable(settings))
    }

    // --- Franco ---

    @Test
    fun `durante el franco se puede editar`() {
        val settings = schedule(setOf(1), "09:00", "17:00")
            .copy(holidayUntilMillis = millisAt(MONDAY, "13:00"))

        assertTrue(policyAt(MONDAY, "12:59").isEditable(settings))
    }

    @Test
    fun `un franco vencido no habilita la edicion`() {
        val settings = schedule(setOf(1), "09:00", "17:00")
            .copy(holidayUntilMillis = millisAt(MONDAY, "13:00"))

        assertFalse(policyAt(MONDAY, "13:00").isEditable(settings))
        assertFalse(policyAt(MONDAY, "15:00").isEditable(settings))
    }

    @Test
    fun `el franco no levanta el bloqueo de las apps elegidas`() {
        val settings = schedule(setOf(1), "09:00", "17:00")
            .copy(holidayUntilMillis = millisAt(MONDAY, "13:00"))

        assertTrue(policyAt(MONDAY, "12:00").shouldBlock(BLOCKED_APP, settings))
    }

    private companion object {
        const val BLOCKED_APP = "com.example.blocked"
        const val OTHER_APP = "com.example.other"
        const val SETTINGS_APP = "com.android.settings"
    }
}
