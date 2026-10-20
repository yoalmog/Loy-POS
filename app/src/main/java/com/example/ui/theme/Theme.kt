package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Shared Color Scheme aligned with Back Office POS (loy-pos.vercel.app/pos)
private val DarkColorScheme = darkColorScheme(
    primary = PosSky500,
    onPrimary = PosWhite,
    primaryContainer = PosSky700,
    onPrimaryContainer = PosSky100,
    secondary = PosIndigo600,
    onSecondary = PosWhite,
    secondaryContainer = PosSlate800,
    onSecondaryContainer = PosSlate200,
    tertiary = PosAmber500,
    background = PosSlate900,
    onBackground = PosSlate100,
    surface = PosSlate800,
    onSurface = PosSlate100,
    surfaceVariant = PosSlate900,
    onSurfaceVariant = PosSlate400,
    outline = PosSlate700,
    error = PosRed500
)

private val LightColorScheme = lightColorScheme(
    primary = PosSky600,
    onPrimary = PosWhite,
    primaryContainer = PosSky100,
    onPrimaryContainer = PosSky700,
    secondary = PosIndigo600,
    onSecondary = PosWhite,
    secondaryContainer = PosSlate100,
    onSecondaryContainer = PosSlate800,
    tertiary = PosAmber500,
    background = PosSlate100,
    onBackground = PosSlate900,
    surface = PosWhite,
    onSurface = PosSlate800,
    surfaceVariant = PosSlate50,
    onSurfaceVariant = PosSlate500,
    outline = PosSlate200,
    error = PosRed600
)

@Composable
fun LoyverseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
