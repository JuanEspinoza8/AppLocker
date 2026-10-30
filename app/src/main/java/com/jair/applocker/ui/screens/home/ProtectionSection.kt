package com.jair.applocker.ui.screens.home

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jair.applocker.receivers.AdminReceiver
import com.jair.applocker.ui.components.PermissionGlassButton

/**
 * Sección 3: permisos que hacen más difícil saltear el bloqueo. El estado se vuelve a leer cada
 * vez que la pantalla vuelve a primer plano, por ejemplo al volver de Ajustes.
 */
@Composable
fun ProtectionSection(isEditable: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBatteryExempt by remember { mutableStateOf(false) }
    var isAdminActive by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                isBatteryExempt = pm.isIgnoringBatteryOptimizations(context.packageName)
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                isAdminActive = dpm.isAdminActive(ComponentName(context, AdminReceiver::class.java))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("ESTADO DE PROTECCIÓN", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
        PermissionGlassButton("Evitar Cierre por Batería", isBatteryExempt, isEditable && !isBatteryExempt) { requestBatteryExemption(context) }
        Spacer(modifier = Modifier.height(12.dp))
        PermissionGlassButton("Escudo Anti-Desinstalación", isAdminActive, isEditable && !isAdminActive) { requestDeviceAdmin(context) }
    }
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
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protección esencial.")
        }
        context.startActivity(intent)
    }
}
