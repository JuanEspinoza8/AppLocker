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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.R
import com.jair.applocker.domain.CodeCheck
import com.jair.applocker.domain.LockSettings
import com.jair.applocker.ui.theme.GlassWhite
import com.jair.applocker.ui.viewmodels.AppUiState
import com.jair.applocker.ui.viewmodels.SetupStart

@Composable
fun HomeScreen(
    uiState: AppUiState,
    onSaveRules: (rules: LockSettings, onResult: (saved: Boolean) -> Unit) -> Unit,
    onActivateHoliday: (code: String, onResult: (CodeCheck) -> Unit) -> Unit,
    onBeginAuthenticatorSetup: (currentCode: String?, onResult: (SetupStart) -> Unit) -> Unit,
    onConfirmAuthenticatorSetup: (code: String, onResult: (saved: Boolean) -> Unit) -> Unit,
    onCancelAuthenticatorSetup: () -> Unit,
    onNavigateToAppList: () -> Unit,
) {
    val saved = uiState.settings
    val isEditable = uiState.isEditable
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val protection = rememberProtectionStatus()

    var showHolidayDialog by remember { mutableStateOf(false) }
    var showAuthenticatorSetup by remember { mutableStateOf(false) }

    // Copia editable de las reglas guardadas; se descarta si cambian las guardadas
    var selectedDays by remember(saved.lockedDays) { mutableStateOf(saved.lockedDays) }
    var lockStartTime by remember(saved.lockStart) { mutableStateOf(saved.lockStart) }
    var lockEndTime by remember(saved.lockEnd) { mutableStateOf(saved.lockEnd) }
    var editWindowDay by remember(saved.editDay) { mutableStateOf(saved.editDay) }
    var editStartTime by remember(saved.editStart) { mutableStateOf(saved.editStart) }
    var editEndTime by remember(saved.editEnd) { mutableStateOf(saved.editEnd) }

    if (showHolidayDialog) {
        HolidayCodeDialog(
            isConfigured = uiState.isHolidayConfigured,
            onDismiss = { showHolidayDialog = false },
            onActivateHoliday = onActivateHoliday,
        )
    }

    if (showAuthenticatorSetup) {
        AuthenticatorSetupDialog(
            isConfigured = uiState.isHolidayConfigured,
            onBegin = onBeginAuthenticatorSetup,
            onConfirm = onConfirmAuthenticatorSetup,
            onDismiss = {
                onCancelAuthenticatorSetup()
                showAuthenticatorSetup = false
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp).verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(64.dp), tint = colors.primary)
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = colors.onBackground,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        if (!protection.isAccessibilityEnabled) {
            AccessibilityBanner(onEnableClick = { openAccessibilitySettings(context) })
        }

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

        ProtectionSection(
            status = protection,
            isEditable = isEditable,
            isHolidayConfigured = uiState.isHolidayConfigured,
            onConfigureHoliday = { showAuthenticatorSetup = true },
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { showHolidayDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.horizontalGradient(listOf(colors.primary, colors.primary.copy(alpha = 0.2f))))
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.holiday_button), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
            Text(stringResource(R.string.select_apps_button), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val rules = saved.copy(
                    lockedDays = selectedDays, lockStart = lockStartTime, lockEnd = lockEndTime,
                    editDay = editWindowDay, editStart = editStartTime, editEnd = editEndTime,
                )
                onSaveRules(rules) { ok ->
                    val message = if (ok) R.string.rules_saved else R.string.rules_not_editable
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
            Text(stringResource(R.string.save_rules_button), fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

/** Aviso de que el servicio de accesibilidad está apagado y, por lo tanto, no se bloquea nada. */
@Composable
private fun AccessibilityBanner(onEnableClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(20.dp))
            .background(GlassWhite).border(1.dp, colors.primary, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Accessibility, contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.accessibility_banner_title), color = colors.onSurface, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(stringResource(R.string.accessibility_banner_message), color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.accessibility_banner_hint), color = colors.outline, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onEnableClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.accessibility_banner_button), fontWeight = FontWeight.Black)
            }
        }
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
                Text(stringResource(R.string.locked_banner_title), color = colors.onSurface, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(stringResource(R.string.locked_banner_message), color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
