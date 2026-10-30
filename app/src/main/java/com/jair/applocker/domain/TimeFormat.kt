package com.jair.applocker.domain

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Formato `HH:mm` con el que se guardan y muestran los horarios. */
object TimeFormat {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Devuelve medianoche si el texto falta o no tiene el formato esperado. */
    fun parse(text: String?): LocalTime =
        try {
            if (text == null) LocalTime.MIDNIGHT else LocalTime.parse(text, formatter)
        } catch (e: DateTimeParseException) {
            LocalTime.MIDNIGHT
        }

    fun format(time: LocalTime): String = time.format(formatter)
}
