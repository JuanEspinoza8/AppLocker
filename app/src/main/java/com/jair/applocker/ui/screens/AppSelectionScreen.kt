package com.jair.applocker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.jair.applocker.data.model.AppInfo
import com.jair.applocker.ui.viewmodels.AppViewModel

@Composable
fun AppSelectionScreen(viewModel: AppViewModel) {
    // Observamos el estado reactivo del ViewModel (tu gestor de estado)
    val apps by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Si está buscando las apps, mostramos un "spinner" de carga
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // LazyColumn es el equivalente a hacer un .map() en un div con scroll en web
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps) { app ->
                AppListItem(
                    app = app,
                    // Cuando tocan el switch, le avisamos al ViewModel
                    onToggle = { viewModel.toggleAppLock(app.packageName) }
                )
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, onToggle: () -> Unit) {
    // Row es un contenedor que alinea elementos en horizontal (flex-direction: row)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Convertimos el ícono nativo de Android a un formato que la UI entienda
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = "Ícono de ${app.appName}",
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // El nombre de la aplicación ocupa el espacio sobrante
        Text(
            text = app.appName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        // El interruptor para bloquear/desbloquear
        Switch(
            checked = app.isLocked,
            onCheckedChange = { onToggle() }
        )
    }
}