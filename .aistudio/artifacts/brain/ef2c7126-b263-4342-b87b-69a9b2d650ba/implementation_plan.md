# Loyverse POS & Back Office System

A comprehensive, production-ready Point of Sale (POS) and Back Office inventory management system inspired by Loyverse POS. Designed for restaurants, bistros, and retail with real-time ticket management, open table orders, item modifiers, dining options, multi-tender split payments, employee shift tracking, customer loyalty points, and back-office analytics.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned based on your preferences:

- **Confirmed Scope**: **Full POS and Back Office Combo** — seamlessly toggle between the Front-of-House POS register (fast order entry, open tickets, cart, split tender, shift management) and the Back Office (item & category catalog, inventory tracking, taxes & discounts, customer loyalty base, and sales summary reports).
- **Confirmed Business Preset**: **Fast Food & Bistro with Table Tickets & Dining Options** — comes pre-populated with burgers, combos, sides, beverages, and desserts, complete with modifiers (extra cheese, sauces, sizes), dining options (*Dine In*, *Takeout*, *Delivery*), and predefined open tickets (Table 1, Table 2, Table 3, Bar, Patio).
- **Confirmed Theme & Design**: **Signature Loyverse Green with Clean Material 3** — vibrant Loyverse emerald green (`#2E7D32` / `#4CAF50`), high-contrast dark slate headers, elevation cards, clear touch targets (>= 48dp), and an adaptive layout supporting both phone vertical and tablet split-screen workflows.

---

## 1. Overview & Core Concept

The app faithfully models the full workflow described in the official Loyverse user manual:

1. **Front-of-House Sales Register**:
   - Visual category tabs & item grid with item avatars, prices, and stock indicators.
   - Live ticket/cart on the side or bottom sheet displaying itemized order lines, quantities, modifiers, dining option badge, tax, discount, and total.
   - Quick action bar: **Open Tickets** (create, view, merge, split, move to table), **Save Ticket**, **Discounts** (percentage/amount), and **Charge**.
   - Custom item editing dialog with quantity stepper, order comments, item-level modifiers, and item-level discounts.
2. **Checkout & Tender Processing**:
   - Multi-payment support: **Cash** with fast preset denominations, **Card** (simulated terminal workflow with signature and authorization), and **Split Payment** (divide equally or enter custom amounts across tenders).
   - Instant digital receipt modal with email receipt entry, itemized calculation (pre-tax, included/added tax, discounts, tips), and "New Sale" cycle.
3. **Open Tickets & Table Management**:
   - Table naming (Predefined: Table 1–8, Custom naming).
   - Ticket merging (combining table tabs into a single bill) and splitting (transferring items between tickets).
   - Ticket status, elapsed time, assigned server, and order notes.
4. **Shift & Cash Management**:
   - Open shift with starting cash float.
   - Cash Management: **Pay In** (add cash) and **Pay Out** (remove cash/expenses) with timestamps and comments.
   - Close shift with expected cash calculations (starting + cash sales - refunds + pay in - pay out) vs actual counted cash, variance calculation, and X/Z shift report.
5. **Customer Loyalty & Profiles**:
   - Customer lookup by phone number, name, or customer code / barcode.
   - Loyalty points balance with 1% reward accumulation and instant point redemption during checkout.
   - Customer purchase history with past receipts.
6. **Back Office Management**:
   - **Item & Category Catalog**: Create, edit, set "Sold by Each" or "Sold by Weight", manage variants and modifiers, track stock levels, and configure low-stock alerts.
   - **Sales Summary & Reports**: Gross sales, refunds, discounts, net sales, cost of goods, gross profit, and category breakdowns.
   - **Taxes & Discounts Setup**: Configure "Added to price" vs "Included in price" taxes and dining option tax exemptions.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   POS Register  │ ───►  │ Item Modifiers  │ ───►  │   Order Ticket  │
│  (Grid / List)  │       │ & Weight Picker │       │  (Cart Review)  │
└────────┬────────┘       └─────────────────┘       └────────┬────────┘
         │                                                   │
         ▼                                                   ▼
┌─────────────────┐                                 ┌─────────────────┐
│  Open Tickets   │                                 │ Charge & Tender │
│ (Tables / Merge)│                                 │ (Cash/Card/Split│
└─────────────────┘                                 └────────┬────────┘
                                                             │
                                                             ▼
                                                    ┌─────────────────┐
                                                    │ Digital Receipt │
                                                    │ & Loyalty Points│
                                                    └─────────────────┘
```

- **POS Mode**: Top app bar provides store name, shift status badge, fast search, barcode scanner trigger, customer assignment button, and a quick drawer to access Shift, Receipts, Open Tickets, Items, and Back Office.
- **Back Office Mode**: Modern navigation rail/tabs for **Sales Reports**, **Items & Stock**, **Customer Base**, **Discounts & Taxes**, and **Settings**, featuring analytical cards and interactive data tables.
- **Adaptive Layout**: On compact screens (phones), the cart is accessible via a sticky bottom bar and slide-up sheet; on expanded displays or tablets, the screen splits into a 2/3 item catalog and 1/3 persistent ticket panel.

### Visual Identity & Theme Tokens

- **Primary Brand Color**: Loyverse Green `#2E7D32` (Dark emerald container `#1B5E20`)
- **Accent Color**: Bright Pistachio / Lime `#4CAF50`
- **Secondary / Surface**: Warm Slate White `#F8F9FA` with M3 elevation tones `#FFFFFF` and crisp borders `#E0E0E0`
- **Status Accents**:
  - Open / Safe: `#2E7D32`
  - Low Stock Warning / Pending Table: `#F57C00`
  - Void / Out of Stock / Negative Alert: `#D32F2F`
  - Info / Customer Points: `#1976D2`

---

## 3. Key Product Decisions & Trade-Offs

1. **Room SQLite Database for Offline-First Durability**:
   - *Decision*: Full offline-first local architecture using Android Jetpack Room with Kotlin Coroutines Flow.
   - *Why*: POS systems in hospitality and retail require sub-millisecond response times, zero network latency, and complete operational independence without network failure dropouts.
2. **Unified Single-App Experience**:
   - *Decision*: Build both the POS register and Back Office inside one fluid Compose application with an easy role/section switcher in the navigation drawer.
   - *Why*: Enables testing, demonstration, and single-device operations without needing multiple installations.
3. **Rich Interactive Features**:
   - Include complete implementations for Modifier selection dialogs, Dining Option selectors, Open Ticket merge/split sheets, Shift float reconciliation, and Loyalty point redemption.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│ ┌──────────────────────┐ ┌───────────────────┐ ┌─────────────────────┐ │
│ │  SalesRegisterScreen │ │ OpenTicketsScreen │ │   BackOfficeScreen  │ │
│ │  (Item Grid & Cart)  │ │ (Merge/Split/Tabs)│ │ (Reports & Catalog) │ │
│ └──────────┬───────────┘ └─────────┬─────────┘ └──────────┬──────────┘ │
│            │                       │                      │            │
│            ▼                       ▼                      ▼            │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │                       PosViewModel / BackOfficeViewModel           │ │
│ │                (StateFlow: Cart, Shift, Catalog, Tickets)          │ │
│ └──────────────────────────────────┬─────────────────────────────────┘ │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Data Layer & Repository                         │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │                           PosRepository                            │ │
│ └──────────────────────────────────┬─────────────────────────────────┘ │
│                                    │                                   │
│            ┌───────────────────────┴───────────────────────┐           │
│            ▼                                               ▼           │
│ ┌───────────────────────────────┐             ┌──────────────────────┐ │
│ │     Room Database (SQLite)    │             │   In-Memory Engine   │ │
│ │ • ItemEntity & CategoryEntity │             │ • Active Tender Calc │ │
│ │ • ModifierEntity & Option     │             │ • Weight Barcode EAN │ │
│ │ • TicketEntity & TicketItem   │             │ • Tax Calculation    │ │
│ │ • ShiftEntity & CashAction    │             │   (Inclusive/Add-on) │ │
│ │ • CustomerEntity & Receipt    │             └──────────────────────┘ │
│ └───────────────────────────────┘                                      │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Data Entities

- **`ItemEntity`**: `id`, `name`, `category`, `price`, `cost`, `sku`, `barcode`, `soldBy` (Each / Weight), `inStock`, `lowStockThreshold`, `trackStock`, `colorHex`, `modifierGroupIdsJson`
- **`CategoryEntity`**: `id`, `name`, `colorHex`
- **`ModifierEntity` & Options**: `id`, `name`, `optionsJson` (e.g. Extra Cheese +$1.00, Double Patty +$2.50)
- **`TicketEntity`**: `id`, `ticketName`, `tableName`, `diningOption` (DineIn, Takeout, Delivery), `createdAt`, `serverName`, `comment`, `isClosed`
- **`TicketItemEntity`**: `ticketId`, `itemId`, `itemName`, `unitPrice`, `quantity`, `selectedModifiersJson`, `comment`, `discountAmount`
- **`ShiftEntity`**: `id`, `openedAt`, `closedAt`, `startingCash`, `cashPayments`, `cashRefunds`, `cashPaidIn`, `cashPaidOut`, `expectedCash`, `actualCash`, `isOpen`
- **`CustomerEntity`**: `id`, `name`, `phone`, `email`, `customerCode`, `pointsBalance`, `totalVisits`, `totalSpent`
- **`ReceiptEntity`**: `id`, `receiptNumber`, `timestamp`, `ticketId`, `totalAmount`, `taxAmount`, `discountAmount`, `paymentMethodsJson`, `customerName`, `diningOption`

---

## 5. Verification Plan

1. **Compilation Verification**:
   - Execute `compile_applet` to ensure Kotlin 2.x and Compose Material 3 build cleanly with no syntax or type errors.
2. **Functional CUJ Verification**:
   - **CUJ 1: Add items to ticket**: Tap categories -> tap Fast Food burger items -> select modifier (e.g., Bacon/Cheese) -> verify line item updates.
   - **CUJ 2: Open ticket save & restore**: Save ticket to "Table 3" -> clear register -> open ticket list -> re-open Table 3 -> confirm items intact.
   - **CUJ 3: Charge & Multi-tender Split**: Checkout -> Split between Cash ($10) and Card (remainder) -> review receipt -> verify stock decrements.
   - **CUJ 4: Shift Flow**: Open shift with $100 -> record Cash Pay In $20 -> make cash sale -> close shift -> verify expected cash matches $100 + $20 + cash sale.
   - **CUJ 5: Back Office**: Switch to Back Office -> verify Sales Summary KPI cards, Inventory low stock alerts, and Customer loyalty ledger.
