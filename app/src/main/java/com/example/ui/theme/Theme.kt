package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PosDesignTokens.BrandSky500,
    onPrimary = Color.Black,
    primaryContainer = PosDesignTokens.BrandSky800,
    onPrimaryContainer = PosDesignTokens.BrandSky100,
    secondary = PosDesignTokens.BrandIndigo600,
    onSecondary = Color.White,
    secondaryContainer = PosDesignTokens.NavDarkCard,
    onSecondaryContainer = PosDesignTokens.NavTextSecondary,
    tertiary = PosDesignTokens.AccentAmber,
    background = PosDesignTokens.NavDarkDeep,
    onBackground = PosDesignTokens.NavTextPrimary,
    surface = PosDesignTokens.NavDarkBg,
    onSurface = PosDesignTokens.NavTextPrimary,
    surfaceVariant = PosDesignTokens.NavDarkCard,
    onSurfaceVariant = PosDesignTokens.NavTextSecondary,
    outline = PosDesignTokens.NavBorder,
    error = PosDesignTokens.AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = PosDesignTokens.BrandSky600,
    onPrimary = Color.White,
    primaryContainer = PosDesignTokens.BrandSky100,
    onPrimaryContainer = PosDesignTokens.BrandSky800,
    secondary = PosDesignTokens.BrandIndigo600,
    onSecondary = Color.White,
    secondaryContainer = PosDesignTokens.BrandSky50,
    onSecondaryContainer = PosDesignTokens.BrandSky700,
    tertiary = PosDesignTokens.AccentAmber,
    background = PosDesignTokens.BackgroundLight,
    onBackground = PosDesignTokens.TextPrimary,
    surface = PosDesignTokens.SurfaceLight,
    onSurface = PosDesignTokens.TextPrimary,
    surfaceVariant = PosDesignTokens.SurfaceLightSubtle,
    onSurfaceVariant = PosDesignTokens.TextSecondary,
    outline = PosDesignTokens.BorderLight,
    error = PosDesignTokens.AccentRed
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
