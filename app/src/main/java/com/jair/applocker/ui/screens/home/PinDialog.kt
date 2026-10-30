package com.jair.applocker.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.ui.theme.GlassWhite

/** Pide un PIN de un solo uso para activar el franco. Se cierra sola si el PIN es válido. */
@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onActivateHoliday: (pin: String, onResult: (Boolean, String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var pinInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, colors.primary, RoundedCornerShape(24.dp)),
        icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = colors.primary) },
        title = { Text("Activar Modo Vacaciones", color = colors.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Pedile un PIN de 4 dígitos a tu amigo y justificale por qué lo necesitás.", color = colors.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    label = { Text("PIN de 4 dígitos", color = colors.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary, unfocusedBorderColor = GlassWhite,
                        focusedTextColor = colors.onSurface, unfocusedTextColor = colors.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pinInput.length == 4) {
                    onActivateHoliday(pinInput) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) onDismiss()
                    }
                } else { Toast.makeText(context, "El PIN debe tener 4 números.", Toast.LENGTH_SHORT).show() }
            }) { Text("ACTIVAR", color = colors.primary, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = colors.outline) }
        }
    )
}
