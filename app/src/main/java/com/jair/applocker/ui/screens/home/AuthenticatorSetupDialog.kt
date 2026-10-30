package com.jair.applocker.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jair.applocker.R
import com.jair.applocker.domain.Totp
import com.jair.applocker.ui.components.QrCode
import com.jair.applocker.ui.viewmodels.AuthenticatorSetup
import com.jair.applocker.ui.viewmodels.SetupStart

/**
 * Configura el autenticador del franco en dos pasos: si ya había uno, primero pide un código de
 * ese autenticador; después muestra el QR y pide un código de la app nueva para confirmar.
 */
@Composable
fun AuthenticatorSetupDialog(
    isConfigured: Boolean,
    onBegin: (currentCode: String?, onResult: (SetupStart) -> Unit) -> Unit,
    onConfirm: (code: String, onResult: (saved: Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var setup by remember { mutableStateOf<AuthenticatorSetup?>(null) }
    var code by remember { mutableStateOf("") }

    val handleStart: (SetupStart) -> Unit = { start ->
        when (start) {
            is SetupStart.Ready -> {
                setup = start.setup
                code = ""
            }
            is SetupStart.Rejected -> {
                Toast.makeText(context, start.check.message(context), Toast.LENGTH_SHORT).show()
                code = ""
            }
            SetupStart.NotAllowed -> {
                Toast.makeText(context, R.string.authenticator_not_allowed, Toast.LENGTH_LONG).show()
                onDismiss()
            }
        }
    }

    // Sin autenticador previo no hay nada que validar: se genera el secreto al abrir
    LaunchedEffect(Unit) {
        if (!isConfigured) onBegin(null, handleStart)
    }

    val currentSetup = setup
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, colors.primary, RoundedCornerShape(24.dp)),
        icon = { Icon(Icons.Default.QrCode2, contentDescription = null, tint = colors.primary) },
        title = { Text(stringResource(R.string.authenticator_title), color = colors.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                if (currentSetup == null) {
                    if (isConfigured) {
                        Text(stringResource(R.string.authenticator_current_code_message), color = colors.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        CodeField(code, onValueChange = { code = it })
                    } else {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(stringResource(R.string.authenticator_scan_message), color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    QrCode(
                        content = currentSetup.otpauthUri,
                        contentDescription = stringResource(R.string.authenticator_qr_description),
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.authenticator_manual_key), color = colors.outline, fontSize = 12.sp)
                    SelectionContainer {
                        Text(currentSetup.secretBase32.chunked(4).joinToString(" "), color = colors.onSurface, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.authenticator_confirm_message), color = colors.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeField(code, onValueChange = { code = it })
                }
            }
        },
        confirmButton = {
            val label = if (currentSetup == null) R.string.dialog_continue else R.string.authenticator_confirm
            TextButton(
                enabled = currentSetup != null || isConfigured,
                onClick = {
                    if (code.length != Totp.DIGITS) {
                        Toast.makeText(context, R.string.code_wrong_length, Toast.LENGTH_SHORT).show()
                    } else if (currentSetup == null) {
                        onBegin(code, handleStart)
                    } else {
                        onConfirm(code) { saved ->
                            if (saved) {
                                Toast.makeText(context, R.string.authenticator_saved, Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, R.string.authenticator_code_mismatch, Toast.LENGTH_LONG).show()
                                code = ""
                            }
                        }
                    }
                }
            ) { Text(stringResource(label), color = colors.primary, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel), color = colors.outline) }
        }
    )
}
