# Phase A: Implementation Audit — Loy-POS Architecture & Modernization

## 1. Repository Identity & Environment
- **Repository:** `https://github.com/yoalmog/Loy-POS`
- **Current Branch:** `main`
- **Application Architecture:**
  - **Android POS Application:** Native Android app (`app/`) written in Kotlin with Jetpack Compose, Room Database (`AppDatabase`), Retrofit, OkHttp, and Coroutines.
  - **Web Back Office & POS Portal:** Next.js 14 (`web/`) with React 18, TypeScript, Tailwind CSS, Lucide icons, IndexedDB offline caching, and Supabase PostgreSQL client.
  - **Database & Cloud:** Supabase PostgreSQL with migrations (`supabase/migrations/`) providing cloud persistence and multi-device synchronization.
  - **Production Deployed Web App:** `https://loy-pos.vercel.app` (Admin: `/admin`, Dashboard: `/dashboard`, POS: `/pos`).

## 2. Component & Navigation Structure
- **Android App:**
  - Entry point: `app/src/main/java/com/example/MainActivity.kt`
  - Screens:
    - `SalesRegisterScreen.kt`: Primary POS catalog grid, categories selector, cart sidebar, ticket management, and checkout.
    - `BackOfficeScreen.kt`: Settings, sync management, analytics, database backup/restore, and external Back Office web launcher (`https://loy-pos.vercel.app/admin`).
  - Data Layer:
    - `PosRepository.kt`: Unified repository coordinating Room local DB (`ItemDao`, `CategoryDao`, `ReceiptDao`, `ShiftDao`, `ProductDao`) and remote APIs.
    - `SupabaseApiClient.kt` & `BackOfficeApiClient.kt`: Direct Supabase REST API client and Back Office Next.js API client (`/api/sync`, `/api/products`, `/api/categories`).
    - `SyncManager.kt`: Automatic background synchronization upon network recovery and manual sync triggers.
- **Web Back Office:**
  - Entry point: `web/app/page.tsx` (redirects to `/pos` or `/dashboard`)
  - Admin redirect: `web/app/admin/page.tsx` (redirects to `/dashboard`)
  - Dashboard pages:
    - `/dashboard`: High-level metrics, revenue charts, recent transactions.
    - `/dashboard/products`: Catalog management, item and category tables, AI menu scanner modal.
    - `/dashboard/inventory`: Stock levels, restock alerts, stock movement history.
    - `/dashboard/sales`: Receipts log, refund operations, filter by date and cashier.
    - `/dashboard/shifts`: Shift reconciliation, cash in/out, cashier registers.
    - `/dashboard/settings`: Store metadata, currency, tax rates, printer routing, and appearance.
  - State & Context:
    - `PosContext.tsx`: Master state for items, categories, cart, shifts, receipts, and offline sync.
    - `ThemeContext.tsx`: Multilingual localization (Spanish `es` default, English `en`, Hebrew `he` with RTL) and visual theme palettes.

## 3. Existing Defects & Deficiencies Identified
1. **Category Management:**
   - Server endpoint (`web/app/api/categories/route.ts`) only implemented `GET` and `POST`. Lacked `PUT` for updates and `DELETE` for safe deletion.
   - UI (`web/app/dashboard/products/page.tsx`) lacked edit and delete controls on category cards.
   - `PosContext.tsx` lacked `updateCategory` and `deleteCategory` with product reassignment safety policies.
2. **Product Management:**
   - Server endpoint (`web/app/api/products/route.ts`) lacked explicit `PUT` and `DELETE` operations with price/stock validation and sales reference preservation.
3. **Menu Scanner:**
   - `web/components/menu-scanner/MenuScannerModal.tsx` simulated OCR with static hardcoded items instead of parsing uploaded/captured menu images.
   - Needed real image handling, OCR preprocessing (zoom, pan, rotation, contrast), heuristic text/price extraction for Spanish/international menus, candidate review with edit/reject capabilities, and authoritative backend persistence.
4. **Android-to-Back Office Sync Push:**
   - Android sales were only pushed if direct Supabase credentials were set in the client BuildConfig. Sales should also push through `BackOfficeApiClient.api.postSync()` to ensure immediate sync with Back Office even when Supabase credentials are secure on the server.
5. **Safe Category Deletion Policy:**
   - Deleting a category with associated products must safely reassign products to a default "General" category rather than orphaning or deleting items.

## 4. Master Remediation Plan
- **Phase B:** Verify and refine visual styling for both Android POS and Web Back Office to match Loyverse POS design patterns.
- **Phase C & G:** Implement robust synchronization, idempotency, and dual-backend communication between Android and Web API.
- **Phase D:** Complete Category CRUD with safe reassignment policy across API, PosContext, and Back Office UI.
- **Phase E:** Complete Product CRUD with price validation, stock tracking, and category synchronization.
- **Phase F:** Overhaul Menu Scanner with real image input, multi-format price parser, review & correction table, and database import.
- **Phase H & I:** Security verification, input validation, acceptance tests, and build checks.
- **Phase J & Reporting:** Generate complete documentation, APK artifact status, and final acceptance report.
