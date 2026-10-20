# Dish Recipe (BOM) & Kitchen Preparation System

A complete recipe specification and kitchen preparation engine for every menu item in the POS system. This allows defining raw ingredients (Bill of Materials), step-by-step culinary preparation instructions, cooking times, kitchen stations, and allergens, with automatic raw ingredient inventory deduction, a dedicated interactive Kitchen Display System (KDS), and in-POS recipe reference.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions are aligned with your operational workflow:

- **Recipe & Prep Data Scope**: Every dish will feature structured raw ingredients with precise quantities and units (g, kg, ml, units, pcs), step-by-step cooking directions, preparation time in minutes, kitchen workstation (Grill, Fryer, Oven, Salad, Bar), and allergen tags.
- **Dedicated Kitchen Display System (KDS)**: In addition to BackOffice product management, a full-screen interactive Kitchen Display Screen allows cooks to view active order tickets, expand recipe cards, check prep steps, and track elapsed cooking timers.
- **POS Recipe Inspection**: Waiters and cashiers can view dish recipes and preparation details directly from the POS Sales Register to answer diner questions regarding ingredients and allergens.
- **Automatic Inventory Deduction**: Completing an order automatically deducts the required ingredient quantities from raw inventory stock based on the dish's recipe.

---

## 1. Overview & Core Concept

### What It Does
Transforms standard menu items into complete culinary specifications with:
1. **Product Recipe / Bill of Materials (BOM)**: List of raw ingredients, required quantities, units of measure, and unit costs.
2. **Kitchen Preparation Guide**: Step-by-step execution guide, cooking station assignment, prep time benchmark, plating notes, and allergens.
3. **Kitchen Display System (KDS)**: Live kitchen ticket dashboard where line cooks view incoming orders, inspect exact preparation methods, and mark orders as preparing or ready.
4. **Automated Stock Depletion**: Seamless inventory deduction of raw materials whenever finished dishes are rung up at checkout.

### Target Audience & Persona
- **Head Chefs & Kitchen Staff**: Ensuring recipe consistency, training new line cooks, and tracking prep times across kitchen stations.
- **Restaurant Managers**: Controlling food cost margins through exact ingredient portioning and automated stock tracking.
- **Waiters & Cashiers**: Answering customer inquiries about ingredients, dietary restrictions, and cooking preparation times.

---

## 2. User Experience & Visual Design

### Key User Flows

#### Flow A: Recipe & Kitchen Prep Management (BackOffice)
1. In the **BackOffice Screen** under the Products/Items tab, each item displays a new `[מתכון והכנה (Recipe & Prep)]` action button.
2. An interactive dialog allows chefs and managers to:
   - Add/edit raw ingredients, quantities, and units (e.g. 200g Lomo Fino, 150g Papas, 50g Cebolla).
   - Toggle automatic inventory deduction per ingredient.
   - Enter estimated preparation time (e.g. 12 minutes).
   - Assign kitchen station (Wok / Grill / Fryer / Salad / Bar / Oven).
   - Define step-by-step cooking steps with easy reordering.
   - Check off allergens (Gluten, Dairy, Peanuts, Eggs, Seafood, etc.).

#### Flow B: Dedicated Kitchen Display Screen (KDS)
1. Accessible from the POS navigation menu or top bar via the `Chef / Kitchen` icon.
2. Live grid of active kitchen tickets sorted by order time with visual color indicators:
   - **Green (< 10 min)**: On schedule.
   - **Amber (10–18 min)**: Priority cooking.
   - **Red (> 18 min)**: Urgent ticket.
3. Each item card expands with a single tap to reveal the full recipe: ingredients, quantities, and step-by-step prep instructions.
4. Line cooks can filter tickets by workstation (e.g., show only Grill items or only Bar drinks) and advance ticket statuses (`התחל הכנה` -> `מוכן להגשה`).

#### Flow C: Quick POS Dish Recipe Inspection
1. Long-pressing any item card or clicking the information icon on the POS register opens a bottom sheet with:
   - Dish overview, allergens, and preparation time.
   - Full ingredient list and customer-facing culinary description.

### Visual Identity & Theme
- **Color Tokens**: Restaurant culinary palette with deep slate background (`#0F172A`), warm amber for preparation in progress (`#D97706`), vibrant emerald for completed orders (`#059669`), and crisp white for readability under kitchen lighting.
- **Typography**: High-contrast bold numerals for quantities and cooking times (`14.sp` to `22.sp`), clean typography with RTL Hebrew and English support.
- **Component Polish**: Large touch targets (`56.dp+`) designed for busy kitchen tablets, timer animations, and tactile feedback.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Relational Entities vs. Embedded Recipe Payload**
  - *Chosen Approach*: Hybrid approach using structured data classes (`DishRecipe`, `RecipeIngredient`, `KitchenPrepGuide`) with JSON database persistence in Room and reactive StateFlows in ViewModels.
  - *Why*: Allows instant loading of complete recipe cards on tablet screens without complex multiple-table joins, while maintaining full query capability and zero migration disruption to existing transactions.

- **Decision 2: Automated Raw Material Stock Deduction**
  - *Chosen Approach*: When a sale is completed in `PosViewModel.completePayment`, the system iterates over each purchased item's recipe and deducts ingredient quantities from the database inventory.
  - *Why*: Guarantees that selling 10 burgers automatically deducts 2.0 kg of meat and 10 buns, avoiding inventory discrepancies.

- **Decision 3: Real-Time Cooking Status Synchronization**
  - *Chosen Approach*: Shared ticket state between POS Register and Kitchen Screen backed by the Room database and reactive Flows.
  - *Why*: Both cashiers and line cooks always see identical order statuses without network delay.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        USER INTERFACE LAYER                            │
│  ┌───────────────────────┐ ┌───────────────────┐ ┌───────────────────┐ │
│  │ BackOffice Screen     │ │ Kitchen Screen    │ │ POS Sales Register│ │
│  │ (Recipe & Prep Editor)│ │ (KDS Ticket Cards)│ │ (Recipe Inspector)│ │
│  └───────────┬───────────┘ └─────────┬─────────┘ └─────────┬─────────┘ │
└──────────────┼───────────────────────┼─────────────────────┼───────────┘
               │                       │                     │
               ▼                       ▼                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         VIEWMODEL / LOGIC LAYER                        │
│  ┌───────────────────────────────┐   ┌───────────────────────────────┐ │
│  │     BackOfficeViewModel       │   │         PosViewModel          │ │
│  │ • saveDishRecipe()            │   │ • activeTickets & orders      │ │
│  │ • updateIngredients()         │   │ • deductRecipeStockOnSale()   │ │
│  └───────────────┬───────────────┘   └───────────────┬───────────────┘ │
└──────────────────┼───────────────────────────────────┼─────────────────┘
                   │                                   │
                   ▼                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         DATA & STORAGE LAYER                           │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                      AppDatabase (Room SQLite)                    │ │
│  │ • items (ItemEntity + recipeJson + prepGuideJson)                 │ │
│  │ • products (raw inventory stock levels)                           │ │
│  │ • tickets (kitchen ticket preparation status & timestamps)        │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Contracts

#### Recipe & Preparation Models (`RecipeModels.kt`):
```kotlin
enum class KitchenStation(val labelHebrew: String, val labelEnglish: String) {
    GRILL("גריל וצלייה", "Grill & BBQ"),
    WOK("ווק וטיגון", "Wok & Stir Fry"),
    OVEN("טאבון ותנור", "Oven & Pizza"),
    SALAD("פס קר וסלטים", "Cold & Salads"),
    BAR("בר ומשקאות", "Bar & Drinks"),
    PASTRY("קינוחים ומתוקים", "Pastry & Desserts")
}

data class RecipeIngredient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double,
    val unit: String, // g, kg, ml, L, יח'
    val costPerUnit: Double = 0.0,
    val deductStock: Boolean = true
)

data class KitchenPrepGuide(
    val prepTimeMinutes: Int = 10,
    val station: KitchenStation = KitchenStation.GRILL,
    val steps: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val chefNotes: String = "",
    val platingGuide: String = ""
)

data class DishRecipe(
    val itemId: Long,
    val itemName: String,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val prepGuide: KitchenPrepGuide = KitchenPrepGuide()
)
```

### Implementation Checklist
1. **Core Domain Models (`RecipeModels.kt`)**: Define `RecipeIngredient`, `KitchenPrepGuide`, `KitchenStation`, and `DishRecipe` with default pre-configured restaurant recipes.
2. **Recipe Persistence & Repository (`RecipeManager.kt`)**: Singleton manager and Room entity helper storing recipes and managing automated stock depletion upon checkout.
3. **BackOffice Recipe & Preparation Editor Dialog (`RecipeEditorDialog.kt`)**: Interactive modal in BackOffice for adding ingredients, quantities, cooking steps, prep times, stations, and allergens.
4. **Kitchen Display Screen (`KitchenDisplayScreen.kt`)**: Live ticket screen with timers, expandable recipe guides, station filtering, and ticket status updates.
5. **POS Register Integration (`SalesRegisterScreen.kt`)**:
   - Navigation button to open the Kitchen Display Screen.
   - Quick recipe sheet for cashiers/waiters to inspect ingredients and prep time.
   - Automatic deduction of raw ingredients upon completing payment.
6. **Verification**: Full compilation check with `compile_applet`.
