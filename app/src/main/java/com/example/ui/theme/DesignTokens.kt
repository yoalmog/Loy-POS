package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AuraPOS Design System Tokens
 * 1:1 Parity with Web Back Office (loy-pos.vercel.app/pos)
 * Palette: Modern Slate & Sky Blue with Navy Dark Navigation
 */
object PosDesignTokens {
    // -------------------------------------------------------------------------
    // Primary Brand Blue (matching web tailwind sky-600 / brand-500)
    // -------------------------------------------------------------------------
    val BrandSky50 = Color(0xFFF0F9FF)
    val BrandSky100 = Color(0xFFE0F2FE)
    val BrandSky200 = Color(0xFFBAE6FD)
    val BrandSky500 = Color(0xFF0EA5E9)
    val BrandSky600 = Color(0xFF0288D1) // Primary Brand Blue (#0288D1)
    val BrandSky700 = Color(0xFF0277BD) // Hover / Pressed
    val BrandSky800 = Color(0xFF01579B)
    val BrandSky900 = Color(0xFF0C4A6E)
    val BrandIndigo600 = Color(0xFF4F46E5) // Accent Gradient Secondary

    // Gradients
    val BrandGradient = Brush.horizontalGradient(
        colors = listOf(BrandSky600, BrandIndigo600)
    )
    val BrandGradientVertical = Brush.verticalGradient(
        colors = listOf(BrandSky600, BrandIndigo600)
    )
    val BrandGradientSubtle = Brush.horizontalGradient(
        colors = listOf(BrandSky600.copy(alpha = 0.15f), BrandIndigo600.copy(alpha = 0.15f))
    )

    // -------------------------------------------------------------------------
    // Dark Navy Navigation & Sidebar (matching web <aside> bg-slate-900)
    // -------------------------------------------------------------------------
    val NavDarkBg = Color(0xFF0F172A)       // slate-900
    val NavDarkCard = Color(0xFF1E293B)     // slate-800
    val NavDarkDeep = Color(0xFF020617)     // slate-950
    val NavTextPrimary = Color(0xFFF8FAFC)  // slate-50
    val NavTextSecondary = Color(0xFFCBD5E1)// slate-300
    val NavTextMuted = Color(0xFF94A3B8)    // slate-400
    val NavBorder = Color(0xFF1E293B)       // slate-800

    // -------------------------------------------------------------------------
    // Light Surfaces & Workspace Background (matching web bg-slate-100)
    // -------------------------------------------------------------------------
    val BackgroundLight = Color(0xFFF1F5F9) // slate-100
    val SurfaceLight = Color(0xFFFFFFFF)    // pure white
    val SurfaceLightSubtle = Color(0xFFF8FAFC) // slate-50
    val BorderLight = Color(0xFFE2E8F0)     // slate-200
    val BorderLightSubtle = Color(0xFFF1F5F9) // slate-100

    // -------------------------------------------------------------------------
    // Text Hierarchy
    // -------------------------------------------------------------------------
    val TextPrimary = Color(0xFF0F172A)     // slate-900
    val TextSecondary = Color(0xFF475569)   // slate-600
    val TextMuted = Color(0xFF94A3B8)       // slate-400

    // -------------------------------------------------------------------------
    // Semantic Accents
    // -------------------------------------------------------------------------
    val AccentAmber = Color(0xFFF59E0B)     // amber-500 (Favorites, Stars, Alerts)
    val AccentAmberBg = Color(0xFFFFFBEB)   // amber-50
    val AccentAmberBorder = Color(0xFFFDE68A)// amber-200
    val AccentAmberText = Color(0xFF92400E) // amber-800

    val AccentEmerald = Color(0xFF10B981)   // emerald-500 (Online, Paid, Success)
    val AccentEmeraldBg = Color(0xFFECFDF5) // emerald-50
    val AccentEmeraldBorder = Color(0xFFA7F3D0) // emerald-200
    val AccentEmeraldText = Color(0xFF065F46) // emerald-800

    val AccentRed = Color(0xFFEF4444)       // red-500 (Delete, Low Stock, Warning)
    val AccentRedBg = Color(0xFFFEF2F2)     // red-50
    val AccentRedBorder = Color(0xFFFECACA) // red-200
    val AccentRedText = Color(0xFF991B1B)   // red-800

    // -------------------------------------------------------------------------
    // Geometry & Radius Scale
    // -------------------------------------------------------------------------
    val RadiusCard: Dp = 16.dp              // rounded-2xl
    val RadiusButton: Dp = 12.dp            // rounded-xl
    val RadiusPill: Dp = 999.dp             // rounded-full
    val RadiusInput: Dp = 12.dp             // rounded-xl
    val RadiusSm: Dp = 8.dp                 // rounded-lg
}
