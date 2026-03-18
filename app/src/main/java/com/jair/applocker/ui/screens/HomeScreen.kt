package com.jair.applocker.ui.screens

import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jair.applocker.receivers.AdminReceiver
import com.jair.applocker.ui.viewmodels.AppViewModel
import java.util.Calendar

// --- COLORES PREMIER AMOLED / NEÓN ---
val PureBlack = Color(0xFF000000)
val NeonBlue = Color(0xFF00E5FF) // Un azul eléctrico, moderno y vibrante
val GlassWhite = Color(0x1AFFFFFF) // Blanco al 10%, crea efecto cristal
val GlassWhitePressed = Color(0x33FFFFFF) // Blanco al 20% para clicks
val NeonRed = Color(0xFFFF1744) // Rojo vibrante para alertas

// --- FUNCIONES DE PERMISOS ---
fun requestBatteryExemption(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:${context.packageName}") }
        context.startActivity(intent)
    }
}

fun requestDeviceAdmin(context: Context) {
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

// --- INTERFAZ VISUAL AMOLED PREMIUM ---

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAppList: () -> Unit,
    isEditable: Boolean = true
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBatteryExempt by remember { mutableStateOf(false) }
    var isAdminActive by remember { mutableStateOf(false) }

    // Estado para mostrar el diálogo del PIN
    var showPinDialog by remember { mutableStateOf(false) }

    val savedLockedDays by viewModel.lockedDays.collectAsState()
    val savedLockStart by viewModel.lockStartTime.collectAsState()
    val savedLockEnd by viewModel.lockEndTime.collectAsState()

    var selectedDays by remember(savedLockedDays) { mutableStateOf(savedLockedDays) }
    var lockStartTime by remember(savedLockStart) { mutableStateOf(savedLockStart) }
    var lockEndTime by remember(savedLockEnd) { mutableStateOf(savedLockEnd) }

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

    // --- DIÁLOGO FLOTANTE PARA EL PIN DE VACACIONES ---
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = Color(0xFF121212), // Negro azulado muy oscuro para el diálogo
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, NeonBlue, RoundedCornerShape(24.dp)), // Borde neón
            icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = NeonBlue) },
            title = { Text("Activar Modo Vacaciones", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pedile un PIN de 4 dígitos a tu amigo y justificale por qué lo necesitás.", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN de 4 dígitos", color = NeonBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = GlassWhite,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput.length == 4) {
                        viewModel.attemptActivateHoliday(pinInput) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) showPinDialog = false // Cerramos si tuvo éxito
                        }
                    } else {
                        Toast.makeText(context, "El PIN debe tener 4 números.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("ACTIVAR", color = NeonBlue, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }

    // --- CONTENIDO PRINCIPAL SOBRE FONDO NEGRO PURO ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack) // FONDO NEGRO PURO (AMOLED)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Cabecera Neón Moderna
        Spacer(modifier = Modifier.height(24.dp))
        Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(64.dp), tint = NeonBlue)
        Text(
            text = "APPLOCKER PREMIER",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White, // Título en blanco puro
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        // Cartel de Bloqueo Estilo "Glassmorphism"
        if (!isEditable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassWhite) // Fondo transparente blanco
                    .border(1.dp, NeonRed, RoundedCornerShape(20.dp)) // Borde rojo neón
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassDisabled, contentDescription = null, tint = NeonRed, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("SISTEMA BLINDADO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            text = "No podés modificar reglas. Si necesitás franco, usá el botón de vacaciones.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // --- SECCIÓN UNIFICADA DE CONFIGURACIÓN ---
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SettingsPower, contentDescription = null, tint = NeonBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Horario Estricto de Bloqueo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = GlassWhite)

            Text("Días activos de castigo:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            DaysSelector(selectedDays, isEditable) { day ->
                selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TimePickerButton("Inicio", lockStartTime, isEditable, Modifier.weight(1f)) { lockStartTime = it }
                TimePickerButton("Fin", lockEndTime, isEditable, Modifier.weight(1f)) { lockEndTime = it }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN DE PERMISOS CON FEEDBACK VISUAL PREMIUM ---
        Text("ESTADO DE PROTECCIÓN", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 12.dp))

        PermissionGlassButton(
            title = "Evitar Cierre por Batería",
            isGranted = isBatteryExempt,
            isEnabled = isEditable && !isBatteryExempt,
            onClick = { requestBatteryExemption(context) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionGlassButton(
            title = "Escudo Anti-Desinstalación",
            isGranted = isAdminActive,
            isEnabled = isEditable && !isAdminActive,
            onClick = { requestDeviceAdmin(context) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOTONES DE ACCIÓN PRINCIPALES ---

        // El Botón de Vacaciones (Siempre habilitado, su "salida")
        OutlinedButton(
            onClick = { showPinDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonBlue, Color(0x3300E5FF)))) // Borde degradado neón
        ) {
            Icon(Icons.Default.BeachAccess, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("PEDIR FRANCO (VACACIONES)", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToAppList,
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth().height(56.dp).background(GlassWhite, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, disabledContentColor = Color.Gray),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 0.dp) // Sin borde, efecto botón de cristal puro
        ) {
            Icon(Icons.Default.AppBlocking, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("SELECCIONAR APPS", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Guardar - Neón Sólido
        Button(
            onClick = {
                viewModel.saveLockConfig(selectedDays, lockStartTime, lockEndTime)
                Toast.makeText(context, "¡Reglas guardadas y activadas! 🛡️", Toast.LENGTH_SHORT).show()
            },
            enabled = isEditable,
            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = PureBlack, disabledContainerColor = Color(0x1A00E5FF), disabledContentColor = Color.Gray),
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

// --- COMPONENTES DE CRISTAL (Glassmorphism) ---

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp)) // Borde casi invisible blanco
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

@Composable
fun PermissionGlassButton(title: String, isGranted: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val contentColor = if (isGranted) Color(0xFF00C853) else if (isEnabled) NeonBlue else Color.Gray
    val bgColor = if (isGranted) Color(0x1A00C853) else GlassWhite
    val icon = if (isGranted) Icons.Default.VerifiedUser else Icons.Default.NewReleases

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(enabled = isEnabled) { onClick() }
            .border(1.dp, if (isGranted) Color(0x3300C853) else Color.Transparent, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = if (isGranted) Color.White else contentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        }
    }
}

@Composable
fun DaysSelector(selectedDays: Set<Int>, isEditable: Boolean, onDayClick: (Int) -> Unit) {
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index + 1)
            // Degradado neón si está seleccionado, cristal si no
            val modifier = if (isSelected) {
                Modifier.background(Brush.radialGradient(listOf(NeonBlue, Color(0x6600E5FF))), CircleShape)
            } else {
                Modifier.background(GlassWhite, CircleShape)
            }

            val textColor = if (isSelected) PureBlack else if (isEditable) Color.White else Color.Gray

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(modifier)
                    .clickable(enabled = isEditable) { onDayClick(index + 1) }
                    .border(1.dp, if (isSelected) NeonBlue else Color.Transparent, CircleShape)
            ) {
                Text(text = day, color = textColor, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun TimePickerButton(label: String, timeText: String, isEditable: Boolean, modifier: Modifier = Modifier, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassWhite)
            .clickable(enabled = isEditable) {
                val calendar = Calendar.getInstance()
                TimePickerDialog(context, { _, hourOfDay, minute ->
                    onTimeSelected(String.format("%02d:%02d", hourOfDay, minute))
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }
            .border(1.dp, if (isEditable) Color(0x1AFFFFFF) else Color.Transparent, RoundedCornerShape(14.dp))
    ) {
        Text("$label: $timeText", color = if (isEditable) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}