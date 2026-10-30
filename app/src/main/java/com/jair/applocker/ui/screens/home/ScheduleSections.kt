package com.jair.applocker.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SettingsPower
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jair.applocker.ui.components.DaysSelector
import com.jair.applocker.ui.components.GlassCard
import com.jair.applocker.ui.components.SectionHeader
import com.jair.applocker.ui.components.TimePickerButton
import java.time.LocalTime

/** Sección 1: días y franja horaria en que se bloquean las apps elegidas. */
@Composable
fun LockScheduleSection(
    days: Set<Int>,
    start: LocalTime,
    end: LocalTime,
    enabled: Boolean,
    onDaysChange: (Set<Int>) -> Unit,
    onStartChange: (LocalTime) -> Unit,
    onEndChange: (LocalTime) -> Unit,
) {
    ScheduleCard(
        icon = Icons.Default.SettingsPower,
        title = "1. Horario Estricto de Bloqueo",
        daysLabel = "Días activos de castigo:",
        selectedDays = days,
        start = start,
        end = end,
        enabled = enabled,
        onDayClick = { day -> onDaysChange(if (day in days) days - day else days + day) },
        onStartChange = onStartChange,
        onEndChange = onEndChange,
    )
}

/** Sección 2: único día y franja de la semana en que se pueden cambiar las reglas. */
@Composable
fun EditWindowSection(
    day: Int?,
    start: LocalTime,
    end: LocalTime,
    enabled: Boolean,
    onDayChange: (Int?) -> Unit,
    onStartChange: (LocalTime) -> Unit,
    onEndChange: (LocalTime) -> Unit,
) {
    ScheduleCard(
        icon = Icons.Default.Edit,
        title = "2. Ventana de Edición Semanal",
        daysLabel = "Único día permitido para modificar reglas:",
        selectedDays = day?.let { setOf(it) } ?: emptySet(),
        start = start,
        end = end,
        enabled = enabled,
        onDayClick = { clicked -> onDayChange(if (day == clicked) null else clicked) },
        onStartChange = onStartChange,
        onEndChange = onEndChange,
    )
}

@Composable
private fun ScheduleCard(
    icon: ImageVector,
    title: String,
    daysLabel: String,
    selectedDays: Set<Int>,
    start: LocalTime,
    end: LocalTime,
    enabled: Boolean,
    onDayClick: (Int) -> Unit,
    onStartChange: (LocalTime) -> Unit,
    onEndChange: (LocalTime) -> Unit,
) {
    GlassCard {
        SectionHeader(icon, title)

        Text(daysLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(12.dp))
        DaysSelector(selectedDays, enabled, onDayClick)

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TimePickerButton("Inicio", start, enabled, Modifier.weight(1f), onStartChange)
            TimePickerButton("Fin", end, enabled, Modifier.weight(1f), onEndChange)
        }
    }
}
