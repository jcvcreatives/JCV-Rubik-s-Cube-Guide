package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalistLightColorScheme = lightColorScheme(
    primary = Color(0xFF141413),
    onPrimary = Color(0xFFF7F6F2),
    primaryContainer = Color(0xFFEBEAE4),
    onPrimaryContainer = Color(0xFF141413),
    secondary = Color(0xFF3D3C38),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE8E1),
    onSecondaryContainer = Color(0xFF141413),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFF0E3D24),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF141413),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141413),
    surfaceVariant = Color(0xFFF0EFEA),
    onSurfaceVariant = Color(0xFF6E6D68),
    outline = Color(0xFFE4E2DC),
    error = CrimsonError,
    onError = Color.White
)

private val MinimalistDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF2F1EC),
    onPrimary = Color(0xFF141413),
    primaryContainer = Color(0xFF262623),
    onPrimaryContainer = Color(0xFFF2F1EC),
    secondary = Color(0xFFB8B6AE),
    onSecondary = Color(0xFF141413),
    secondaryContainer = Color(0xFF262623),
    onSecondaryContainer = Color(0xFFF2F1EC),
    tertiary = Color(0xFF34A86C),
    onTertiary = Color(0xFF0A2918),
    tertiaryContainer = Color(0xFF143824),
    onTertiaryContainer = Color(0xFFA7E8C3),
    background = Color(0xFF111110),
    onBackground = Color(0xFFF2F1EC),
    surface = Color(0xFF1A1A18),
    onSurface = Color(0xFFF2F1EC),
    surfaceVariant = Color(0xFF242421),
    onSurfaceVariant = Color(0xFF9A9890),
    outline = Color(0xFF2E2E2A),
    error = Color(0xFFE05243),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) MinimalistDarkColorScheme else MinimalistLightColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
