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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.ui.theme.GlassWhite
import com.jair.applocker.ui.theme.NeonBlue

/** Pide un PIN de un solo uso para activar el franco. Se cierra sola si el PIN es válido. */
@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onActivateHoliday: (pin: String, onResult: (Boolean, String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, NeonBlue, RoundedCornerShape(24.dp)),
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
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = GlassWhite,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
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
            }) { Text("ACTIVAR", color = NeonBlue, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) }
        }
    )
}
