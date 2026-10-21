# Implementation Plan - Unified Localization & Multi-Access Language Selection

## User Requirements Addressed
- Create a unified localization provider / state synchronization between web back office and Android POS data layer.
- Support changing language dynamically and immediately reflecting across KDS, POS Top Bar, and Side Menu without page refresh.
- Fix side menu translations.
- Language selection accessible in **both Header and Settings Menu**.
- Default startup language: **Spanish (`es`)**.

---

## Proposed Changes

### 1. Localization & State Management (`AppStrings` & `PosViewModel`)
- Set default `language` in `PosUiState` to `"es"` (Spanish).
- Ensure `PosViewModel.setLanguage(lang: String)` updates the state immediately, notifying all observing composables.
- Expand `AppStrings` dictionary to cover all side menu items, POS screens, and KDS strings in Spanish, English, and Hebrew (`he`, `en`, `es`).

### 2. Header & Settings Menu Language Selector
- **POS Top Bar Header**: Add a compact language selector dropdown/menu (Es / En / He) right before cloud sync status or near store badge.
- **Back Office / Settings Menu**: Add a dedicated Language section/dropdown in the Settings tab of Back Office / Side Menu so users can switch languages seamlessly.
- **Side Menu Translation Fix**: Update side navigation drawer / menu items to use `strings` dictionary dynamically based on `uiState.language`.

### 3. Component Synchronization
- Ensure `SalesRegisterScreen`, `KitchenDisplayComponent`, `BackOfficeScreen`, and Side Navigation Menu all observe `uiState.language` and instantly re-render translated strings when changed.

---

## Verification Plan

### Automated Build & Compilation
- Run `compile_applet` to verify zero compilation errors across all Kotlin and Compose components.
