# Implementation Plan: Online Back Office Website & Hosted Supabase Backend for AuraPOS

This plan details the full architecture and execution steps to transform the existing Android POS Back Office into a real, production-grade online web application connected to a hosted **Supabase PostgreSQL** backend, while integrating the Android POS app for direct online synchronization with offline fallback.

---

## 1. Audit of Existing Android POS Architecture

### Working Features & Local Storage
- **Local Persistence**: SQLite database managed via Room (`AppDatabase.kt`, version 6) containing tables for `categories`, `products`, `items`, `modifiers`, `modifier_groups`, `open_tickets`, `ticket_items`, `shifts`, `cash_actions`, `customers`, `receipts`, `timecards`, and `loyalty_points`.
- **UI Components**: Jetpack Compose frontend (`SalesRegisterScreen.kt`, `BackOfficeScreen.kt`) with ViewModel state flows (`PosViewModel.kt`, `BackOfficeViewModel.kt`).
- **Offline Capabilities**: Full local order checkout, stock decrementing, receipt generation, and thermal printer formatting (`ThermalPrinterManager.kt`).

### Hardcoded & Simulated Production Data Identified
- `populateInitialBistroData()` in `AppDatabase.kt`: Automatically seeds Bistro menu items, categories, and stock into Room upon first run or destructive migration.
- `PosRepository.kt`: Automatically invokes `populateInitialBistroData()` if `countItems() == 0`.
- Offline/Sync Simulation in `SyncManager.kt`: Simulates cloud sync passes with a 1.2s delay without connecting to an actual API endpoint.
- Hardcoded defaults in UI: Cashier names ("Alex (Cashier)"), store profile ("Bistro & Grill #1"), and fixed sales tax rates (8.25%).

---

## 2. Target Architecture & User Review Required

### Key User Choices Confirmed
1. **Web Framework**: **Next.js 14** (App Router, React, TypeScript, Tailwind CSS, Recharts).
2. **Backend Engine**: **Supabase** (Hosted PostgreSQL database, Row-Level Security, Auth, Postgrest REST API).
3. **Android POS Integration**: **Direct Online-First Supabase Calls** with local Room fallback and an offline retry queue to prevent duplicate transactions.

### User Setup Requirements
- The user will create a free Supabase project at `https://supabase.com` and obtain the **Supabase Project URL** and **Anon Public Key**.
- Environment variables will be supplied via `.env.local` for the Web app and `BuildConfig` / `.env` for the Android app.
- Database tables and security policies will be initialized by executing the provided SQL migration script in the Supabase SQL Editor.

---

## 3. Proposed Changes & Implementation Stages

### Stage 1: Hosted Supabase Backend & PostgreSQL Schema
- **Database Migration (`supabase/migrations/20261016_initial_schema.sql`)**:
  - `profiles` (id, email, full_name, role ['admin', 'cashier'], created_at)
  - `categories` (id, name, color_hex)
  - `products` (id, name, price, category_name, stock_level, image_url)
  - `items` (id, name, category_id, category_name, price, cost, sku, barcode, sold_by, in_stock, low_stock_threshold, track_stock, color_hex, modifier_ids)
  - `modifier_groups` (id, name, options_json)
  - `modifiers` (id, name, price, category_id)
  - `shifts` (id, opened_at, closed_at, cashier_name, starting_cash, cash_payments, cash_refunds, cash_paid_in, cash_paid_out, card_payments, actual_cash_counted, is_open)
  - `cash_actions` (id, shift_id, is_pay_in, amount, comment, created_at)
  - `customers` (id, name, phone, email, customer_code, points_balance, total_visits, total_spent, note)
  - `loyalty_points` (id, customer_id, points, description, created_at)
  - `receipts` (id, receipt_number, created_at, shift_id, cashier_name, ticket_name, dining_option, subtotal, tax_amount, discount_amount, tip_amount, total_amount, payment_type, payment_details_json, customer_id, customer_name, is_refunded, refund_timestamp, items_summary_json, idempotency_key)
  - `receipt_items` (id, receipt_id, item_id, item_name, unit_price, quantity, selected_modifiers_json, modifiers_cost, comment, discount_percent, line_total)
  - `timecards` (id, employee_name, clock_in_time, clock_out_time, total_hours, is_clocked_in)
- **Security & Authorization**:
  - Enable Row Level Security (RLS) on all tables.
  - Create policies allowing authenticated users to read/write records and limiting store admin settings to `role = 'admin'`.

### Stage 2: Responsive Next.js Web Back Office Application
- **Directory Structure (`web/` or `/` subproject)**:
  - Next.js 14 App Router with Tailwind CSS & Lucide icons.
  - `@supabase/supabase-js` client setup (`lib/supabase/client.ts`, `lib/supabase/server.ts`).
  - **Auth System**: Login page, Signup/Invite, Role protection middleware, and Logout handler.
  - **Dashboard Page (`/dashboard`)**: Live metrics (Total Revenue, Transaction Count, Average Order Value, Net Sales), Recharts daily sales trend line chart, and top-selling products pie chart generated directly from PostgreSQL queries.
  - **Sales & Receipts Page (`/sales`)**: Filterable transaction log by date range, payment type, dining option; detailed receipt drill-down drawer; refund processing button.
  - **Products & Inventory Page (`/products`, `/inventory`)**: Product catalog management with CRUD modals, real-time stock quantity editing, stock adjustment log, and low-stock alerts.
  - **Customers & Loyalty Page (`/customers`)**: Customer table, loyalty point balances, and purchase history breakdown.
  - **Shifts & Employees Page (`/shifts`, `/timecards`)**: Drawer register shift histories, cash variance reporting, employee list, and timecard logs.
  - **Reports Page (`/reports`)**: Sales by category, hourly sales distribution, and inventory valuation reports.
  - **Settings Page (`/settings`)**: Store information, tax configuration, currency, user access management.
  - **Honest Empty States**: Clean, informative empty state illustrations when database tables contain zero records.

### Stage 3: Android POS Integration & Online-First Synchronization
- **Supabase Client Integration**:
  - Add Supabase Kotlin / Ktor / Postgrest dependencies or Retrofit Supabase REST API service client (`SupabaseApiService.kt`).
  - Configure Supabase URL and Anon Key via `BuildConfig` / `.env`.
- **Online-First Synchronization Logic**:
  - `OnlinePosRepository.kt`: Directly executes Supabase REST API requests when internet is available.
  - On network availability:
    - Checkout order -> Posts receipt and receipt items to Supabase `receipts` table.
    - Decrements stock in Supabase `items` / `products` tables.
    - Saves shift actions and timecards to Supabase.
  - On offline / network error:
    - Automatically falls back to local Room database.
    - Records pending sync items in an offline queue table (`pending_sync_queue`).
    - When network reconnects, background `SyncWorker` retries pending items using `idempotency_key` (UUID) to ensure zero duplicate sales.
- **Production Data Cleaning**:
  - Isolate `populateInitialBistroData()` behind an explicit `SEED_DEMO_DATA` debug flag.
  - Ensure zero mock data or random chart generators run in production builds.

### Stage 4: Testing & Deployment Instructions
- **Web Deployment**:
  - Complete instructions for deploying the Next.js Web Back Office to **Vercel** or **Netlify**.
  - Environment variable setup (`NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`).
- **Backend Deployment**:
  - Step-by-step SQL migration execution guide in Supabase Dashboard.
  - `.env.example` template files for both Web and Android.

---

## 4. Verification & Mandatory Testing Checklist

1. [ ] **Backend Schema & Auth**: Verify Supabase PostgreSQL tables, indexes, and RLS policies compile and enforce user authentication.
2. [ ] **Web Dashboard**: Verify Next.js web application renders responsive tables, real sales trend charts, and product management forms.
3. [ ] **Database CRUD Operations**: Create a product on the Web app, verify it persists in PostgreSQL, and update its price.
4. [ ] **POS Online Sync**: Complete a transaction on the Android POS and confirm the receipt appears in real-time on the Web Back Office dashboard.
5. [ ] **Inventory Sync**: Verify stock updates made on the Web app immediately reflect on the Android POS menu.
6. [ ] **Offline Queue & Idempotency**: Turn off network connection on Android POS, complete an order, restore network, and verify the queue syncs without creating duplicates.
7. [ ] **Empty States**: Verify that empty tables display genuine empty states instead of fabricated sample numbers.
