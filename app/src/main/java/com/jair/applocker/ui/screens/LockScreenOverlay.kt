package com.jair.applocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockScreenOverlay(onGoHomeClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.errorContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alerta",
            tint = colors.onErrorContainer,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡APLICACIÓN BLOQUEADA!",
            color = colors.onErrorContainer,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Estás en horario estricto.\nNo hay vuelta atrás, volvé a tus tareas.",
            color = colors.onErrorContainer,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoHomeClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.onErrorContainer,
                contentColor = colors.errorContainer
            ),
            modifier = Modifier.height(50.dp)
        ) {
            Text("Ir a la pantalla de Inicio", fontWeight = FontWeight.Bold)
        }
    }
}