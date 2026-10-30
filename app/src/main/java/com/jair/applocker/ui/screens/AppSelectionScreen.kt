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
    val apps by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEditable by viewModel.isEditable.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps) { app ->
                AppListItem(
                    app = app,
                    enabled = isEditable,
                    onToggle = { viewModel.toggleAppLock(app.packageName) }
                )
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = "Ícono de ${app.appName}",
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = app.appName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        Switch(
            checked = app.isLocked,
            onCheckedChange = { onToggle() },
            enabled = enabled
        )
    }
}