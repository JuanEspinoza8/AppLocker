package com.jair.applocker.ui.screens.home

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jair.applocker.R
import com.jair.applocker.receivers.AdminReceiver
import com.jair.applocker.services.LockerAccessibilityService
import com.jair.applocker.ui.components.PermissionGlassButton

/** Permisos que necesita la app para bloquear y para que sea difícil saltear el bloqueo. */
data class ProtectionStatus(
    val isAccessibilityEnabled: Boolean,
    val isBatteryExempt: Boolean,
    val isAdminActive: Boolean,
)

/**
 * Lee el [ProtectionStatus] y lo vuelve a leer cada vez que la pantalla vuelve a primer plano,
 * por ejemplo al volver de Ajustes.
 */
@Composable
fun rememberProtectionStatus(): ProtectionStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf(readProtectionStatus(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = readProtectionStatus(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return status
}

/**
 * Sección 3: estado de cada permiso, con acceso directo a la pantalla de Android que lo otorga, y
 * del autenticador del franco.
 */
@Composable
fun ProtectionSection(
    status: ProtectionStatus,
    isEditable: Boolean,
    isHolidayConfigured: Boolean,
    onConfigureHoliday: () -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.protection_title), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
        // Sin el servicio no se bloquea nada (tampoco Ajustes), así que siempre se puede activar
        PermissionGlassButton(stringResource(R.string.protection_accessibility), status.isAccessibilityEnabled, !status.isAccessibilityEnabled) { openAccessibilitySettings(context) }
        Spacer(modifier = Modifier.height(12.dp))
        PermissionGlassButton(stringResource(R.string.protection_battery), status.isBatteryExempt, isEditable && !status.isBatteryExempt) { requestBatteryExemption(context) }
        Spacer(modifier = Modifier.height(12.dp))
        PermissionGlassButton(stringResource(R.string.protection_admin), status.isAdminActive, isEditable && !status.isAdminActive) { requestDeviceAdmin(context) }
        Spacer(modifier = Modifier.height(12.dp))
        // Reemplazar un autenticador existente pide un código suyo, así que se puede tocar siempre
        PermissionGlassButton(stringResource(R.string.protection_holiday_authenticator), isHolidayConfigured, isHolidayConfigured || isEditable, onConfigureHoliday)
    }
}

private fun readProtectionStatus(context: Context): ProtectionStatus {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return ProtectionStatus(
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context),
        isBatteryExempt = pm.isIgnoringBatteryOptimizations(context.packageName),
        isAdminActive = dpm.isAdminActive(ComponentName(context, AdminReceiver::class.java)),
    )
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
        val service = it.resolveInfo.serviceInfo
        service.packageName == context.packageName && service.name == LockerAccessibilityService::class.java.name
    }
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

private fun requestBatteryExemption(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }
        context.startActivity(intent)
    }
}

private fun requestDeviceAdmin(context: Context) {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val compName = ComponentName(context, AdminReceiver::class.java)
    if (!dpm.isAdminActive(compName)) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.device_admin_explanation))
        }
        context.startActivity(intent)
    }
}
