package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CubeLensColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00262B),
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = NeonOrange,
    onSecondary = Color(0xFF2B1000),
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OnOrangeContainer,
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF002910),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFFA6F7C5),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CubeLensColorScheme,
        typography = Typography,
        content = content
    )
}
