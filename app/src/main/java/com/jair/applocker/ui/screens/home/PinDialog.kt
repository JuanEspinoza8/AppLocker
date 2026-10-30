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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.R
import com.jair.applocker.ui.theme.GlassWhite
import com.jair.applocker.ui.viewmodels.HolidayResult

/** Pide un PIN de un solo uso para activar el franco. Se cierra sola si el PIN es válido. */
@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onActivateHoliday: (pin: String, onResult: (HolidayResult) -> Unit) -> Unit,
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
        title = { Text(stringResource(R.string.pin_dialog_title), color = colors.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.pin_dialog_message), color = colors.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    label = { Text(stringResource(R.string.pin_dialog_label), color = colors.primary) },
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
                    onActivateHoliday(pinInput) { result ->
                        val message = when (result) {
                            HolidayResult.ACTIVATED -> R.string.holiday_activated
                            HolidayResult.INVALID_PIN -> R.string.pin_invalid
                            HolidayResult.PIN_ALREADY_USED -> R.string.pin_already_used
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (result == HolidayResult.ACTIVATED) onDismiss()
                    }
                } else { Toast.makeText(context, R.string.pin_wrong_length, Toast.LENGTH_SHORT).show() }
            }) { Text(stringResource(R.string.pin_dialog_confirm), color = colors.primary, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pin_dialog_cancel), color = colors.outline) }
        }
    )
}
