package com.jair.applocker.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.domain.LockSettings
import com.jair.applocker.ui.theme.GlassWhite
import com.jair.applocker.ui.viewmodels.AppUiState

@Composable
fun HomeScreen(
    uiState: AppUiState,
    onSaveRules: (rules: LockSettings, onResult: (saved: Boolean) -> Unit) -> Unit,
    onActivateHoliday: (pin: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onNavigateToAppList: () -> Unit,
) {
    val saved = uiState.settings
    val isEditable = uiState.isEditable
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    var showPinDialog by remember { mutableStateOf(false) }

    // Copia editable de las reglas guardadas; se descarta si cambian las guardadas
    var selectedDays by remember(saved.lockedDays) { mutableStateOf(saved.lockedDays) }
    var lockStartTime by remember(saved.lockStart) { mutableStateOf(saved.lockStart) }
    var lockEndTime by remember(saved.lockEnd) { mutableStateOf(saved.lockEnd) }
    var editWindowDay by remember(saved.editDay) { mutableStateOf(saved.editDay) }
    var editStartTime by remember(saved.editStart) { mutableStateOf(saved.editStart) }
    var editEndTime by remember(saved.editEnd) { mutableStateOf(saved.editEnd) }

    if (showPinDialog) {
        PinDialog(onDismiss = { showPinDialog = false }, onActivateHoliday = onActivateHoliday)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(64.dp), tint = colors.primary)
        Text(
            text = "APPLOCKER",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = colors.onBackground,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        if (uiState.isLoaded && !isEditable) {
            LockedBanner()
        }

        LockScheduleSection(
            days = selectedDays,
            start = lockStartTime,
            end = lockEndTime,
            enabled = isEditable,
            onDaysChange = { selectedDays = it },
            onStartChange = { lockStartTime = it },
            onEndChange = { lockEndTime = it },
        )

        Spacer(modifier = Modifier.height(16.dp))

        EditWindowSection(
            day = editWindowDay,
            start = editStartTime,
            end = editEndTime,
            enabled = isEditable,
            onDayChange = { editWindowDay = it },
            onStartChange = { editStartTime = it },
            onEndChange = { editEndTime = it },
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProtectionSection(isEditable)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { showPinDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.horizontalGradient(listOf(colors.primary, colors.primary.copy(alpha = 0.2f))))
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("PEDIR FRANCO (VACACIONES)", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToAppList,
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth().height(56.dp).background(GlassWhite, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onBackground, disabledContentColor = colors.outline),
            border = ButtonDefaults.outlinedButtonBorder(enabled = isEditable).copy(width = 0.dp)
        ) {
            Icon(Icons.Default.AppBlocking, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("SELECCIONAR APPS", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val rules = saved.copy(
                    lockedDays = selectedDays, lockStart = lockStartTime, lockEnd = lockEndTime,
                    editDay = editWindowDay, editStart = editStartTime, editEnd = editEndTime,
                )
                onSaveRules(rules) { ok ->
                    val message = if (ok) "¡Reglas guardadas y activadas! 🛡️" else "Fuera de la ventana de edición: no se guardaron los cambios."
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = isEditable,
            colors = ButtonDefaults.buttonColors(disabledContainerColor = colors.primary.copy(alpha = 0.1f), disabledContentColor = colors.outline),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("GUARDAR REGLAS", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

/** Aviso de que las reglas no se pueden cambiar en este momento. */
@Composable
private fun LockedBanner() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(20.dp))
            .background(GlassWhite).border(1.dp, colors.error, RoundedCornerShape(20.dp))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HourglassDisabled, contentDescription = null, tint = colors.error, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("SISTEMA BLINDADO", color = colors.onSurface, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Esperá a tu ventana de edición semanal o usá el botón de vacaciones.", color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
