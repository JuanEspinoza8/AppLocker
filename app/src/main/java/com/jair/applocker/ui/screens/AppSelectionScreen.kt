package com.jair.applocker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.jair.applocker.R
import com.jair.applocker.data.model.AppInfo
import com.jair.applocker.ui.viewmodels.AppUiState

@Composable
fun AppSelectionScreen(uiState: AppUiState, onToggleApp: (packageName: String) -> Unit) {
    val apps = uiState.installedApps

    if (apps == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Desde Android 15 la app se dibuja detrás de las barras del sistema
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = WindowInsets.systemBars.asPaddingValues()) {
            items(apps, key = { it.packageName }) { app ->
                AppListItem(
                    app = app,
                    isLocked = app.packageName in uiState.settings.lockedApps,
                    enabled = uiState.isEditable,
                    onToggle = { onToggleApp(app.packageName) }
                )
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, isLocked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = stringResource(R.string.app_icon_description, app.appName),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = app.appName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        Switch(
            checked = isLocked,
            onCheckedChange = { onToggle() },
            enabled = enabled
        )
    }
}
