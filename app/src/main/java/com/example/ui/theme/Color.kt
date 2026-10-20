package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Shared Design System Tokens matching Back Office POS (https://loy-pos.vercel.app/pos)
// Primary brand blue & interactive states
val PosSky600 = Color(0xFF0284C7)         // Primary action blue (sky-600)
val PosSky500 = Color(0xFF0EA5E9)         // Hover / focus blue (sky-500)
val PosSky400 = Color(0xFF38BDF8)         // Light blue accent (sky-400)
val PosSky700 = Color(0xFF0369A1)         // Darker pressed blue (sky-700)
val PosSky50 = Color(0xFFF0F9FF)          // Light blue tint surface (sky-50)
val PosSky100 = Color(0xFFE0F2FE)         // Light blue chip container (sky-100)
val PosSky200 = Color(0xFFBAE6FD)         // Light blue border (sky-200)
val PosIndigo600 = Color(0xFF4F46E5)      // Gradient companion (indigo-600)

// Slate backgrounds, text, and borders
val PosSlate950 = Color(0xFF020617)       // Deepest dark
val PosSlate900 = Color(0xFF0F172A)       // Sidebar & dark navy header
val PosSlate800 = Color(0xFF1E293B)       // Primary text dark
val PosSlate700 = Color(0xFF334155)       // Strong text dark
val PosSlate600 = Color(0xFF475569)       // Muted text
val PosSlate500 = Color(0xFF64748B)       // Secondary text
val PosSlate400 = Color(0xFF94A3B8)       // Tertiary / placeholder text
val PosSlate300 = Color(0xFFCBD5E1)       // Subtle lines
val PosSlate200 = Color(0xFFE2E8F0)       // Standard card & input border
val PosSlate100 = Color(0xFFF1F5F9)       // POS content background
val PosSlate50 = Color(0xFFF8FAFC)        // Cart container & card background
val PosWhite = Color(0xFFFFFFFF)          // Pure white surface

// Status & Accents
val PosEmerald50 = Color(0xFFECFDF5)      // Connected / success background
val PosEmerald200 = Color(0xFFA7F3D0)     // Success border
val PosEmerald400 = Color(0xFF34D399)     // Success pulse dot
val PosEmerald500 = Color(0xFF10B981)     // Success emerald 500
val PosEmerald600 = Color(0xFF059669)     // Success text / icon
val PosEmerald700 = Color(0xFF047857)     // Dark success text

val PosAmber50 = Color(0xFFFFFBEB)        // Favorites / paused background
val PosAmber100 = Color(0xFFFEF3C7)       // Warning container
val PosAmber200 = Color(0xFFFDE68A)       // Warning / favorite border
val PosAmber400 = Color(0xFFFBBF24)       // Warning amber 400
val PosAmber500 = Color(0xFFF59E0B)       // Favorite star / paused order icon
val PosAmber600 = Color(0xFFD97706)       // Points badge
val PosAmber700 = Color(0xFFB45309)       // Warning / chef note text
val PosAmber800 = Color(0xFF92400E)       // Dark warning text

val PosRed50 = Color(0xFFFEF2F2)          // Lock / delete / alert background
val PosRed200 = Color(0xFFFECACA)         // Alert border
val PosRed400 = Color(0xFFF87171)         // Alert icon red 400
val PosRed500 = Color(0xFFEF4444)         // Alert button / trash icon
val PosRed600 = Color(0xFFDC2626)         // Danger / low stock tag / void

// Legacy aliases for backward compatibility with existing components
val LoyverseGreen = PosSky600
val LoyverseGreenDark = PosSky700
val LoyverseGreenLight = PosSky500
val LoyverseMintContainer = PosSky100
val LoyverseTeal = PosSky600
val LoyverseAmber = PosAmber500
val LoyverseRed = PosRed600

val LightBackground = PosSlate100
val LightSurface = PosWhite
val LightSurfaceVariant = PosSlate50
val LightOutline = PosSlate200
val LightOnSurface = PosSlate800
val LightOnSurfaceVariant = PosSlate500

val DarkBackground = PosSlate900
val DarkSurface = PosSlate800
val DarkSurfaceVariant = PosSlate900
val DarkOutline = PosSlate700
val DarkOnSurface = PosSlate100
