package com.aitrainer.practice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = AccentHover,
    secondary = InkSecondary,
    onSecondary = Color.White,
    background = Paper,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    outline = Hairline,
    error = Danger,
    onError = Color.White,
)

@Composable
fun AiTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
