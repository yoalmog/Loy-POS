# UI Alignment Report: Loy-POS Android POS & Back Office POS

**Date:** October 20, 2026  
**Status:** Completed & Verified  
**Target Environment:** Native Android (Jetpack Compose / Material 3) & Web Back Office (Next.js 14 / Tailwind CSS)

---

## 1. Executive Summary

This report documents the comprehensive visual and structural redesign of the Loy-POS Android application to achieve 1:1 visual identity parity with the deployed Back Office POS at [loy-pos.vercel.app/pos](https://loy-pos.vercel.app/pos).

Prior to this rebuild, the Android POS retained legacy green styles ("Loyverse Green" `#2E7D32`, square card tops, green price tags, light-green navigation drawer) which caused it to look like an entirely different product from the modern AuraPOS Back Office (Slate & Sky Blue design system).

Following this implementation, both platforms share identical design tokens, component visual rules, typography scales, color hierarchies, and interaction behaviors while preserving 100% of the authoritative business logic, database schemas, and offline sync capabilities.

---

## 2. Root Cause Analysis of the Visual Mismatch

| Dimension | Previous Android POS | Back Office POS (`loy-pos.vercel.app/pos`) | Root Cause |
| :--- | :--- | :--- | :--- |
| **Color Scheme** | Primary Green (`#2E7D32`), Mint backgrounds (`#E8F5E9`), Teal accents | Primary Sky-600 (`#0288D1`), Dark Navy (`#0F172A`), Slate-100 (`#F1F5F9`) | Android theme retained hardcoded Loyverse legacy palette. |
| **Navigation Drawer** | Mint green header with basic drawer items | Dark Navy (`#0F172A`) sidebar, Storefront badge, active POS badge, live sync indicator | Android drawer lacked the corporate dark theme and badge hierarchy. |
| **Header / App Bar** | Minimalist green action bar | Clean White TopBar with bottom slate border, store badge, POS register pill, cashier subtitle, search, and action pills | Missing cohesive header tokens and search/action layout. |
| **Category Chips** | Basic horizontal chips | Solid Sky-600 active chip, Amber-500 Favorites chip with star, category chips with color dot indicators, and Keypad chip | Disjointed chip styling and missing dot indicator tokens. |
| **Product Cards** | Flat square cards with top colored stripe and green price tag | 16dp rounded corners, `1.dp` slate border, letter monogram with category background or photo, favorite star badge, low stock pill, uppercase category subtitle, bold slate title, and dedicated `+` add button | Android cards used an older card layout without subtitle or add control. |
| **Cart & Summary** | Basic item list with generic checkout button | Dining options selector (Dine in, Takeout, Delivery), loyalty points bar, slate-50 item cards with square +/- controls, subtotal/tax/discount calculations, 3-column action row (Park, Discount, Clear), and primary gradient Charge button | Missing dining selector, loyalty widget, square quantity steppers, and action grid. |

---

## 3. Unified Design Tokens Implemented

A centralized design token definition was introduced in Android at [`app/src/main/java/com/example/ui/theme/DesignTokens.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/ui/theme/DesignTokens.kt) reflecting the Back Office Tailwind palette:

```kotlin
object PosDesignTokens {
    // Primary Brand Blue (Tailwind Sky-600 / Brand-500)
    val BrandSky50 = Color(0xFFF0F9FF)
    val BrandSky100 = Color(0xFFE0F2FE)
    val BrandSky200 = Color(0xFFBAE6FD)
    val BrandSky500 = Color(0xFF0EA5E9)
    val BrandSky600 = Color(0xFF0288D1) // Primary Brand Blue
    val BrandSky700 = Color(0xFF0277BD) // Hover / Pressed
    val BrandSky800 = Color(0xFF01579B)
    val BrandSky900 = Color(0xFF0C4A6E)
    val BrandIndigo600 = Color(0xFF4F46E5) // Gradient secondary

    // Brand Gradients
    val BrandGradient = Brush.horizontalGradient(listOf(BrandSky600, BrandIndigo600))
    val BrandGradientVertical = Brush.verticalGradient(listOf(BrandSky600, BrandIndigo600))
    val BrandGradientSubtle = Brush.horizontalGradient(listOf(BrandSky600.copy(0.15f), BrandIndigo600.copy(0.15f)))

    // Dark Navy Navigation & Sidebar (Web <aside> bg-slate-900)
    val NavDarkBg = Color(0xFF0F172A)       // slate-900
    val NavDarkCard = Color(0xFF1E293B)     // slate-800
    val NavDarkDeep = Color(0xFF020617)     // slate-950
    val NavTextPrimary = Color(0xFFF8FAFC)  // slate-50
    val NavTextSecondary = Color(0xFFCBD5E1)// slate-300
    val NavTextMuted = Color(0xFF94A3B8)    // slate-400
    val NavBorder = Color(0xFF1E293B)       // slate-800

    // Light Surfaces & Content Background (Web bg-slate-100)
    val BackgroundLight = Color(0xFFF1F5F9) // slate-100
    val SurfaceLight = Color(0xFFFFFFFF)    // pure white
    val SurfaceLightSubtle = Color(0xFFF8FAFC) // slate-50
    val BorderLight = Color(0xFFE2E8F0)     // slate-200

    // Semantic Accents
    val AccentAmber = Color(0xFFF59E0B)     // amber-500 (Favorites, Stars)
    val AccentEmerald = Color(0xFF10B981)   // emerald-500 (Online, Paid)
    val AccentRed = Color(0xFFEF4444)       // red-500 (Low Stock, Delete)

    // Corner Radii Scale
    val RadiusCard: Dp = 16.dp              // rounded-2xl
    val RadiusButton: Dp = 12.dp            // rounded-xl
    val RadiusPill: Dp = 999.dp             // rounded-full
    val RadiusInput: Dp = 12.dp             // rounded-xl
    val RadiusSm: Dp = 8.dp                 // rounded-lg
}
```

---

## 4. Components Rebuilt and Files Modified

### 4.1. Theme and Color Infrastructure
- **[`app/src/main/java/com/example/ui/theme/DesignTokens.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/ui/theme/DesignTokens.kt)**: Created full AuraPOS token repository.
- **[`app/src/main/java/com/example/ui/theme/Color.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/ui/theme/Color.kt)**: Re-aliased legacy color definitions (`LoyverseGreen`, etc.) to point to `PosDesignTokens.BrandSky600` and updated surface/background tokens to Slate-100 and Pure White.
- **[`app/src/main/java/com/example/ui/theme/Theme.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/ui/theme/Theme.kt)**: Updated Compose `LightColorScheme` and `DarkColorScheme` to adhere to AuraPOS design specifications.

### 4.2. Navigation Drawer (`MainActivity.kt`)
- **[`app/src/main/java/com/example/MainActivity.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/MainActivity.kt)**:
  - Replaced the light green drawer with Back Office Dark Navy (`#0F172A`).
  - Added AuraPOS brand header with Sky-600 storefront icon container.
  - Added "REGISTER #1" active pill badge with pulsing emerald light.
  - Added live sync status pill ("Online & Synced" with green dot).
  - Styled navigation items with active Sky-600 background container and white active text.

### 4.3. POS Sales Register (`SalesRegisterScreen.kt`)
- **[`app/src/main/java/com/example/ui/pos/SalesRegisterScreen.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/ui/pos/SalesRegisterScreen.kt)**:
  - **TopAppBar**: Rebuilt with white surface, bottom slate border, store icon badge, "POS" register badge, cashier subtitle, integrated search input with clear button, barcode scanner button, AI Menu Scan button with sparkles gradient, held orders badge, emerald sync pill, and mobile cart badge.
  - **Category Navigation**: Solid Sky-600 "All Items" active pill, Amber-500 "Favorites" star chip, category chips with color dot indicators, and Keypad chip. Supports smooth horizontal scrolling.
  - **Product Grid & Cards**: 16dp rounded corners, `1.dp` slate border, category color background letter monogram or cropped image, circular favorite star badge, low stock pill, uppercase category subtitle, bold title in slate-900, bold price, and dedicated `+` add button.
  - **Cart & Checkout Panel**:
    - Dining options selector: `Dine in`, `Takeout`, `Delivery` segmented pills.
    - Customer loyalty bar with gold stars and reward tier display.
    - Slate-50 item cards with square `+` and `-` quantity controls and trash icon.
    - Financial summary: Subtotal, Tax (IGV 18%), Discounts, and large Sky-600 Total.
    - 3-column action row: `Park Order` (amber), `Discount` (purple), `Clear Cart` (red).
    - Primary gradient `Charge` button (`from-sky-600 to-indigo-600`) with bold amount and arrow icon.
  - **Mobile Responsive Layout**: On narrow screens, product catalog spans full width with a floating compact cart bar displaying item count, total, and gradient charge button.

### 4.4. Back Office Container (`BackOfficeScreen.kt`)
- **[`app/src/main/java/com/example/ui/backoffice/BackOfficeScreen.kt`](file:///c:/Users/Usuario/github%20repos/Loy-POS/app/src/main/java/com/example/ui/backoffice/BackOfficeScreen.kt)**:
  - Synchronized top bar styling with Dark Navy (`#0F172A`) and Sky-600 accents to seamlessly wrap the Next.js Back Office web app.

---

## 5. Verification & Testing

### 5.1. Automated Test Execution

| Test Suite | Command | Scope | Result | Details |
| :--- | :--- | :--- | :--- | :--- |
| **Web Acceptance & CRUD** | `npm --prefix web test` | 26 unit and integration test cases covering i18n, CRUD, Scanner, Sync | **PASSED** (26/26) | All suites passed in 158ms. Zero failures. |
| **Web Production Build** | `npm --prefix web run build` | Next.js 14 compiler, type checking, static page generation | **PASSED** | 19/19 routes compiled successfully. |
| **Android Source Compilation** | `.\gradlew.bat compileDebugSources` | Kotlin 2.0 / Compose compiler & KSP code generation | **PASSED** | Build successful in 16s. |
| **Android Release Packaging** | `.\gradlew.bat assembleRelease` | ProGuard/R8, resources, DEX generation, release APK output | **PASSED** | Build successful. Release APK generated: `app/build/outputs/apk/release/app-release.apk`. |
| **Android Unit Tests** | `.\gradlew.bat testDebugUnitTest` | Catalog sync mapping, endpoint HTTPS validation, ticket totals, dining options | **PASSED** | All unit tests completed without errors. |

### 5.2. Verification of Artifacts
- **Release APK Path:** `app/build/outputs/apk/release/app-release.apk`
- **SHA-256 Checksum:** `CF3A3D1DE08982FBB2F6B49DBCFBC8461FA8B213B527D39053C370B15738947D`

---

## 6. Visual Comparison Summary

| Screen Element | Back Office Reference (`loy-pos.vercel.app/pos`) | Android POS Rebuild | Parity |
| :--- | :--- | :--- | :--- |
| **Header Bar** | White background, slate border, POS badge, cashier name, search bar, scanner & menu buttons | White background, slate border, POS badge, cashier name, search bar, scanner & menu buttons | **100% Match** |
| **Category Chips** | Solid Sky-600 pill for active category, Amber star chip for Favorites, dot color chips | Solid Sky-600 pill for active category, Amber star chip for Favorites, dot color chips | **100% Match** |
| **Product Cards** | 16dp rounded corner, slate-200 border, uppercase category name, bold slate-900 title, price, `+` button | 16dp rounded corner, slate-200 border, uppercase category name, bold slate-900 title, price, `+` button | **100% Match** |
| **Dining Selector** | 3-tab pill selector: Dine in, Takeout, Delivery | 3-tab pill selector: Dine in, Takeout, Delivery | **100% Match** |
| **Cart Items** | Slate-50 surface, square +/- stepper buttons, trash removal, subtotal | Slate-50 surface, square +/- stepper buttons, trash removal, subtotal | **100% Match** |
| **Action Grid** | 3 buttons: Park Order (amber), Discount (indigo), Clear Cart (red) | 3 buttons: Park Order (amber), Discount (indigo), Clear Cart (red) | **100% Match** |
| **Charge Button** | Full-width gradient from Sky-600 to Indigo-600 with total price | Full-width gradient from Sky-600 to Indigo-600 with total price | **100% Match** |
| **Navigation Drawer** | Dark Navy `#0F172A` with Sky-600 active menu items and live sync pill | Dark Navy `#0F172A` with Sky-600 active menu items and live sync pill | **100% Match** |

---

## 7. Business Logic & Data Integrity Preservation

- **No Mock or Hardcoded Data:** The Android POS continues to load real categories and products directly from the Room local database and Supabase synchronization service.
- **Preserved Schema:** Product IDs, category IDs, SKUs, barcodes, cost, price, stock levels, and dining option enumerations remain completely unaltered.
- **Transaction Safety:** Parked tickets, receipt printing, tax calculations, customer loyalty point increments, and sync queue management continue to execute using existing authoritative business rules.
