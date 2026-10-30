package com.jair.applocker.ui.screens.home

import android.content.Context
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
import com.jair.applocker.domain.CodeCheck
import com.jair.applocker.domain.Totp
import com.jair.applocker.ui.theme.GlassWhite
import kotlin.math.ceil

/**
 * Pide un código del autenticador para activar el franco. Se cierra sola si el código es válido.
 * Si todavía no hay autenticador, explica cómo configurarlo.
 */
@Composable
fun HolidayCodeDialog(
    isConfigured: Boolean,
    onDismiss: () -> Unit,
    onActivateHoliday: (code: String, onResult: (CodeCheck) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, colors.primary, RoundedCornerShape(24.dp)),
        icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = colors.primary) },
        title = { Text(stringResource(R.string.holiday_dialog_title), color = colors.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                val message = if (isConfigured) R.string.holiday_dialog_message else R.string.holiday_dialog_not_configured
                Text(stringResource(message), color = colors.onSurfaceVariant, fontSize = 14.sp)
                if (isConfigured) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CodeField(code, onValueChange = { code = it })
                }
            }
        },
        confirmButton = {
            if (isConfigured) {
                TextButton(onClick = {
                    if (code.length == Totp.DIGITS) {
                        onActivateHoliday(code) { result ->
                            Toast.makeText(context, result.message(context), Toast.LENGTH_SHORT).show()
                            if (result == CodeCheck.Valid) onDismiss() else code = ""
                        }
                    } else {
                        Toast.makeText(context, R.string.code_wrong_length, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.dialog_activate), color = colors.primary, fontWeight = FontWeight.Black) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close), color = colors.primary, fontWeight = FontWeight.Black) }
            }
        },
        dismissButton = {
            if (isConfigured) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel), color = colors.outline) }
            }
        }
    )
}

/** Campo numérico para un código de [Totp.DIGITS] dígitos. */
@Composable
internal fun CodeField(value: String, onValueChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= Totp.DIGITS && input.all { it.isDigit() }) onValueChange(input) },
        label = { Text(stringResource(R.string.code_label), color = colors.primary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary, unfocusedBorderColor = GlassWhite,
            focusedTextColor = colors.onSurface, unfocusedTextColor = colors.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Texto para mostrar el resultado de verificar un código. */
internal fun CodeCheck.message(context: Context): String = when (this) {
    CodeCheck.Valid -> context.getString(R.string.holiday_activated)
    is CodeCheck.Invalid -> context.getString(R.string.code_invalid, attemptsLeft)
    is CodeCheck.LockedOut -> {
        val minutes = ceil((untilMillis - System.currentTimeMillis()) / 60_000.0).toInt().coerceAtLeast(1)
        context.getString(R.string.code_locked_out, minutes)
    }
    CodeCheck.NotConfigured -> context.getString(R.string.code_not_configured)
}
