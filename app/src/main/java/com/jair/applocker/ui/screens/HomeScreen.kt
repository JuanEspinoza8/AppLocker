package com.jair.applocker.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import com.jair.applocker.ui.viewmodels.AppViewModel

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAppList: () -> Unit,
    // isEditable vendrá del ViewModel en el futuro (evaluando si es el día y hora correctos)
    isEditable: Boolean = true
) {
    // Scroll state para que la pantalla se pueda deslizar si es muy chica
    val scrollState = rememberScrollState()

    val context = LocalContext.current // Poné esto arriba, donde definís tus variables 'var'

    // Estados locales temporales (luego esto vivirá en tu ViewModel / Base de datos)
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var lockStartTime by remember { mutableStateOf("00:00") }
    var lockEndTime by remember { mutableStateOf("00:00") }

    var editWindowDay by remember { mutableStateOf<Int?>(null) }
    var editStartTime by remember { mutableStateOf("00:00") }
    var editEndTime by remember { mutableStateOf("00:00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Modo Kiosco Estricto",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Cartel de advertencia si no está en horario de edición
        if (!isEditable) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Modificación Bloqueada.\nSolo podés editar esto en tu Ventana de Edición.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                )
            }
        }

        // --- SECCIÓN 1: HORARIO DE BLOQUEO ---
        SectionCard(title = "1. Horario de Bloqueo de Apps") {
            Text("Días activos:", style = MaterialTheme.typography.bodyMedium)
            DaysSelector(selectedDays, isEditable) { day ->
                selectedDays = if (selectedDays.contains(day)) {
                    selectedDays - day
                } else {
                    selectedDays + day
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TimePickerButton("Inicio", lockStartTime, isEditable) { lockStartTime = it }
                TimePickerButton("Fin", lockEndTime, isEditable) { lockEndTime = it }
            }
        }

        // --- SECCIÓN 2: VENTANA DE EDICIÓN ---
        SectionCard(title = "2. Ventana de Edición (Tu salida)") {
            Text("Día permitido para modificar/apagar:", style = MaterialTheme.typography.bodyMedium)
            // Para simplificar, usamos el mismo selector pero que solo elija un día
            DaysSelector(editWindowDay?.let { setOf(it) } ?: emptySet(), isEditable) { day ->
                editWindowDay = if (editWindowDay == day) null else day
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TimePickerButton("Inicio", editStartTime, isEditable) { editStartTime = it }
                TimePickerButton("Fin", editEndTime, isEditable) { editEndTime = it }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para ir a elegir las aplicaciones
        OutlinedButton(
            onClick = onNavigateToAppList,
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Seleccionar Apps a Bloquear")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de guardado
        Button(
            onClick = {
                viewModel.saveLockConfig(selectedDays, lockStartTime, lockEndTime)
                android.widget.Toast.makeText(context, "¡Horarios guardados!", android.widget.Toast.LENGTH_SHORT).show()
            },
            enabled = isEditable,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("GUARDAR Y ACTIVAR")
        }
    }
}

// --- COMPONENTES REUTILIZABLES DE LA UI ---

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

@Composable
fun DaysSelector(selectedDays: Set<Int>, isEditable: Boolean, onDayClick: (Int) -> Unit) {
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index + 1)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray)
                    .clickable(enabled = isEditable) { onDayClick(index + 1) }
            ) {
                Text(
                    text = day,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TimePickerButton(label: String, timeText: String, isEditable: Boolean, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current

    OutlinedButton(
        enabled = isEditable,
        onClick = {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    // Formateamos para que siempre tenga dos dígitos (ej: 09:05)
                    val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                    onTimeSelected(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true // true para formato 24hs
            ).show()
        }
    ) {
        Text("$label: $timeText")
    }
}