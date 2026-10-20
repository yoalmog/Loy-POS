# UI Alignment & System Architecture Report: Loy-POS Android vs. Back Office

## 1. Executive Summary & Root Cause Analysis

### Objective
Align the visual design language, interface structure, component styling, and UX patterns between the **Loy-POS Android application** and the **Back Office POS web application** (deployed at `https://loy-pos.vercel.app/pos`).

### Root Cause of the Original Visual Mismatch
1. **Divergent Design Tokens**: The web application utilized a modern Tailwind / Radix CSS palette dominated by slate/navy navigation (`#0F172A` / `#1E293B`), refined brand blue interactive accents (`#2563EB` / `#1D4ED8`), high-contrast subtle borders (`#E2E8F0`), and clean white/neutral surfaces (`#F8FAFC`). Meanwhile, the Android native app had previously used standard Material 3 default elevation tints and green/slate tones that gave it an unbranded, disconnected appearance.
2. **Disjointed Category & Grid Layouts**: The Back Office UI uses pill-style chips with distinct active badge states, item count tags, and a consistent 8dp radius. In contrast, the Android POS initially used generic tab indicators and squarer card aspect ratios without product images or colored category accents.
3. **Cart & Action Bar Inconsistencies**: The Back Office POS featured a dedicated right-hand cart panel with high-contrast numerical controls, sticky checkout button, and split dining option badges. The Android POS needed responsive adaptation to present the same visual hierarchy whether displayed on a tablet layout or folded into mobile sheets.
4. **BOM & Kitchen Display System (KDS) Integration**: Menu items in food service require ingredients (Bill of Materials) and station-specific prep instructions. These were previously absent from the product editor and POS workflow.

---

## 2. Design System & Token Consolidation

The Android application implements tokens matching the Back Office CSS specification:

| Design Token | Web Reference Value | Android Token (`Theme.kt` / `Color.kt`) | Visual Usage |
| :--- | :--- | :--- | :--- |
| **colorPrimary** | `#2563EB` (Blue 600) | `Color(0xFF2563EB)` | Primary buttons, active tabs, checkout CTA |
| **colorPrimaryHover** | `#1D4ED8` (Blue 700) | `Color(0xFF1D4ED8)` | Pressed states, accent highlights |
| **colorSidebar** | `#0F172A` (Slate 900) | `Color(0xFF0F172A)` | Drawer header, navigation background |
| **colorSurface** | `#FFFFFF` | `Color(0xFFFFFFFF)` | Product cards, dialog surfaces, cart container |
| **colorBackground** | `#F8FAFC` (Slate 50) | `Color(0xFFF8FAFC)` | POS screen background, grid surface |
| **colorBorder** | `#E2E8F0` (Slate 200) | `Color(0xFFE2E8F0)` | Card borders, dividers, chip outlines |
| **colorText** | `#0F172A` (Slate 900) | `Color(0xFF0F172A)` | Primary headlines, item names, totals |
| **colorTextSecondary** | `#64748B` (Slate 500) | `Color(0xFF64748B)` | SKUs, categories, metadata, timestamps |
| **colorSuccess** | `#16A34A` (Green 600) | `Color(0xFF16A34A)` | Paid badges, in-stock indicators, ready KDS |
| **colorDanger** | `#DC2626` (Red 600) | `Color(0xFFDC2626)` | Void, delete, low stock alerts, clear cart |
| **radiusCard** | `12px / 0.75rem` | `RoundedCornerShape(12.dp)` | Product cards, ticket items, summary panels |
| **radiusButton** | `8px / 0.5rem` | `RoundedCornerShape(8.dp)` | Action buttons, numeric keypad, category pills |

---

## 3. Implemented Components & Feature Architecture

### A. Sales Register & POS Screen (`SalesRegisterScreen.kt`)
- **Header & Action Bar**: Incorporates active cashier info, fast item search, barcode scanner trigger, OCR menu scanner, ticket selector, and kitchen display screen shortcut.
- **Category Filter**: Horizontally scrolling pills with color-coded dot badges, item count indicators, and "All Items" / "Favorites" filters.
- **Product Card Grid**: Responsive grid featuring high-quality images, accent stripe, price tag, stock level pill, and instant "Quick Recipe & BOM" sheet launcher.
- **Cart & Order Panel**: Shows order lines with modifier details, discounts, tax calculations, quantity steppers, dining option selector (Dine in, Takeout, Delivery), and sticky "Charge" button.

### B. Bill of Materials (BOM) & Recipe Editor (`RecipeEditorDialog.kt` & `ItemEditorDialog`)
- **Three-Tab Editor**:
  1. *General Item Info*: Name, Category, Price, Cost, SKU, Barcode, Stock Tracking.
  2. *BOM & Ingredients*: Direct assignment of raw ingredients with unit quantity (grams, ml, pcs), unit cost, supplier, stock deduction flag, and automatic Gross Margin % / Food Cost calculation.
  3. *Kitchen Prep & Cooking Instructions*: Target preparation time, kitchen station routing (Grill, Fryer, Salad, Bar, Oven), step-by-step cooking procedure, allergen tags (Gluten, Dairy, Nuts, Eggs, Soy, Seafood), chef tips, and plating instructions.
- **Quick Recipe Sheet (`DishRecipeSheet.kt`)**: Allows POS operators and servers to tap the recipe book icon on any product card during live order entry to answer customer questions about ingredients, allergens, or preparation instructions.

### C. Kitchen Display System (KDS) (`KitchenDisplayScreen.kt` & `KitchenDisplayComponent.kt`)
- **Card-Based Ticket Layout**: incoming orders are displayed as modular cards categorized by status (`RECEIVED`, `PREPARING`, `READY`, `SERVED`).
- **Real-Time Elapsed Timer**: Visual color coding (Green < 5 min, Amber 5-10 min, Red > 10 min) to track ticket age.
- **Ingredient & Preparation Drill-Down**: Expandable steps showing exact cooking instructions, allergen warnings, and ingredient lists per dish.
- **Station Filtering**: Operators can filter tickets by kitchen station (All, Grill, Fry, Oven, Cold/Salad, Beverage).

---

## 4. Modified Files & Verification Summary

### Key Files Changed / Created
1. `app/src/main/java/com/example/ui/theme/Color.kt` & `Theme.kt`: Added brand design tokens aligned with Back Office.
2. `app/src/main/java/com/example/ui/pos/SalesRegisterScreen.kt`: Redesigned POS grid, category pills, cart layout, and recipe launchers.
3. `app/src/main/java/com/example/ui/pos/components/DishRecipeSheet.kt`: POS quick sheet for dish BOM and prep instructions.
4. `app/src/main/java/com/example/ui/backoffice/BackOfficeScreen.kt`: Product list and unified 3-tab `ItemEditorDialog`.
5. `app/src/main/java/com/example/ui/backoffice/components/RecipeEditorDialog.kt`: Dedicated recipe and BOM management modal.
6. `app/src/main/java/com/example/ui/kitchen/KitchenDisplayScreen.kt`: Full-screen Kitchen Display System.
7. `app/src/main/java/com/example/ui/kitchen/KitchenDisplayComponent.kt`: KDS cards, timer, station filters, and step checkoffs.
8. `app/src/main/java/com/example/util/RecipeManager.kt`: Shared repository for BOM formulas, kitchen prep guides, and tickets.
9. `app/src/main/java/com/example/data/db/Daos.kt`: Added `getAllProductsSync` and `getAllItemsSync` Room queries.
10. `app/src/main/java/com/example/MainActivity.kt`: Integrated Drawer navigation with KDS destination.

### Build and Test Verification
- **Compilation Check**: `compile_applet` passed successfully without errors.
- **Unit & Robolectric Tests**: Executed `gradle :app:testDebugUnitTest`. Result: **BUILD SUCCESSFUL (33 actionable tasks executed/up-to-date, 0 failures)**.
- **Data Integrity**: Preserved Room database schema, SQLite DAOs, and Supabase / Vercel cloud synchronization compatibility.
