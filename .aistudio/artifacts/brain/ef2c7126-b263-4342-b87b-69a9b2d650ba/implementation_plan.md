# Implementation Plan - Loyverse POS Production System

This plan outlines the steps to build and finalize the production-ready Loyverse POS system, connecting the native Android POS application and the Next.js online Back Office website to a shared Supabase PostgreSQL backend.

## User Preferences & Architecture
- **Backend**: Supabase PostgreSQL with RLS policies, migrations, and atomic checkout transactions.
- **Android POS**: Kotlin, Jetpack Compose, Room local database for offline resilience, and Supabase REST/Realtime sync.
- **Web Back Office**: Next.js 14, React, TypeScript, Tailwind CSS, and Recharts (optimized for Vercel deployment).
- **Credentials**: Managed securely via AI Studio Secrets and environment variables.

---

## Proposed Changes

### 1. Database & Migrations (`supabase/migrations/`)
- Ensure complete migration script `20261016_initial_schema.sql` covers all tables (`store_settings`, `categories`, `items`, `products`, `customers`, `employees`, `receipts`, `receipt_items`, `shifts`, `timecards`, `profiles`, `loyalty_points`, `cash_actions`), indexes, RLS policies, and idempotency constraints.

### 2. Next.js Back Office Website (`web/`)
- Verify all pages (Dashboard, Sales/Receipts, Products, Inventory, Customers, Shifts, Reports, Settings) are connected to Supabase client (`web/lib/supabase/client.ts`).
- Ensure SSR/SSG compatibility and clean production builds for Vercel deployment.

### 3. Android POS Application (`app/`)
- Verify Retrofit/OkHttp `SupabaseApiClient` and `SyncManager` bidirectional synchronization.
- Ensure offline Room storage acts as a reliable local queue with automatic retry upon reconnection.
- Protect production data by gating demo seeding behind configuration checks.

### 4. Build & Verification
- Compile Android applet (`compile_applet`).
- Build web application (`npm --prefix web run build`).
