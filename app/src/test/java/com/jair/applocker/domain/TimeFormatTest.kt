package com.jair.applocker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeFormatTest {

    @Test
    fun `lee y escribe el formato HH mm`() {
        assertEquals(LocalTime.of(7, 5), TimeFormat.parse("07:05"))
        assertEquals("07:05", TimeFormat.format(LocalTime.of(7, 5)))
        assertEquals("23:59", TimeFormat.format(LocalTime.of(23, 59)))
    }

    @Test
    fun `un valor faltante o invalido se toma como medianoche`() {
        assertEquals(LocalTime.MIDNIGHT, TimeFormat.parse(null))
        assertEquals(LocalTime.MIDNIGHT, TimeFormat.parse(""))
        assertEquals(LocalTime.MIDNIGHT, TimeFormat.parse("25:00"))
        assertEquals(LocalTime.MIDNIGHT, TimeFormat.parse("7:5"))
    }
}
