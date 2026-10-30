package com.jair.applocker.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.domain.TimeFormat
import com.jair.applocker.ui.theme.GlassBorder
import com.jair.applocker.ui.theme.GlassWhite
import com.jair.applocker.ui.theme.SuccessGreen
import java.time.LocalTime

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(GlassWhite).border(1.dp, GlassBorder, RoundedCornerShape(24.dp))) {
        Column(modifier = Modifier.padding(24.dp)) { content() }
    }
}

/** Ícono y título de una [GlassCard], con un divisor abajo. */
@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = GlassWhite)
}

@Composable
fun PermissionGlassButton(title: String, isGranted: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (isGranted) SuccessGreen else if (isEnabled) colors.primary else colors.outline
    val bgColor = if (isGranted) SuccessGreen.copy(alpha = 0.1f) else GlassWhite
    val icon = if (isGranted) Icons.Default.VerifiedUser else Icons.Default.NewReleases

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor).clickable(enabled = isEnabled) { onClick() }.border(1.dp, if (isGranted) SuccessGreen.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(16.dp))) {
        Row(modifier = Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = title, color = if (isGranted) colors.onSurface else contentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        }
    }
}

/** Fila de días de lunes (1) a domingo (7). */
@Composable
fun DaysSelector(selectedDays: Set<Int>, isEditable: Boolean, onDayClick: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index + 1)
            val modifier = if (isSelected) Modifier.background(Brush.radialGradient(listOf(colors.primary, colors.primary.copy(alpha = 0.4f))), CircleShape) else Modifier.background(GlassWhite, CircleShape)
            val textColor = if (isSelected) colors.onPrimary else if (isEditable) colors.onSurface else colors.outline
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).clip(CircleShape).then(modifier).clickable(enabled = isEditable) { onDayClick(index + 1) }.border(1.dp, if (isSelected) colors.primary else Color.Transparent, CircleShape)) {
                Text(text = day, color = textColor, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

/** Botón que muestra una hora y abre un [TimePickerDialog] con esa hora ya elegida. */
@Composable
fun TimePickerButton(label: String, time: LocalTime, isEditable: Boolean, modifier: Modifier = Modifier, onTimeSelected: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center, modifier = modifier.height(50.dp).clip(RoundedCornerShape(14.dp)).background(GlassWhite).clickable(enabled = isEditable) {
        TimePickerDialog(context, { _, h, m -> onTimeSelected(LocalTime.of(h, m)) }, time.hour, time.minute, true).show()
    }.border(1.dp, if (isEditable) GlassWhite else Color.Transparent, RoundedCornerShape(14.dp))) {
        Text("$label: ${TimeFormat.format(time)}", color = if (isEditable) colors.onSurface else colors.outline, fontWeight = FontWeight.Bold)
    }
}
