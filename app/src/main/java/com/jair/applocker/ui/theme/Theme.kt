package com.jair.applocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = PureBlack,
    background = PureBlack,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    error = NeonRed,
    onError = Color.White,
    errorContainer = LockRed,
    onErrorContainer = Color.White,
)

/**
 * Tema de la app. Es siempre oscuro y no usa colores dinámicos (Material You) para mantener la
 * misma identidad visual en cualquier teléfono.
 */
@Composable
fun AppLockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
