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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jair.applocker.R
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
        title = stringResource(R.string.lock_schedule_title),
        daysLabel = stringResource(R.string.lock_schedule_days_label),
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
        title = stringResource(R.string.edit_window_title),
        daysLabel = stringResource(R.string.edit_window_days_label),
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
            TimePickerButton(stringResource(R.string.time_start), start, enabled, Modifier.weight(1f), onStartChange)
            TimePickerButton(stringResource(R.string.time_end), end, enabled, Modifier.weight(1f), onEndChange)
        }
    }
}
