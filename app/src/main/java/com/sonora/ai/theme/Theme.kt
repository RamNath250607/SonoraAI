package com.sonora.ai.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SonoraColorScheme = darkColorScheme(
    primary = SonoraPurple,
    onPrimary = SonoraBackground,
    primaryContainer = SonoraPurpleDark,
    onPrimaryContainer = SonoraPurpleLight,
    secondary = SonoraCyan,
    onSecondary = SonoraBackground,
    secondaryContainer = SonoraCyanDark,
    onSecondaryContainer = SonoraCyan,
    tertiary = SonoraPurpleLight,
    background = SonoraBackground,
    onBackground = SonoraTextPrimary,
    surface = SonoraSurface,
    onSurface = SonoraTextPrimary,
    surfaceVariant = SonoraCard,
    onSurfaceVariant = SonoraTextSecondary,
    outline = SonoraCardBorder,
    error = SonoraError
)

@Composable
fun SonoraAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SonoraColorScheme,
        typography = Typography,
        content = content
    )
}
