# Phase C, G, H: Database, Synchronization & Security Architecture

## 1. Single Authoritative Backend Architecture
Loy-POS synchronizes both the Android POS application and the Web Back Office against the same persistent database and REST API architecture.

```
                    ┌─────────────────────────────────┐
                    │     Production Web Back Office  │
                    │   (https://loy-pos.vercel.app)  │
                    └───────────────┬─────────────────┘
                                    │
                                    │ HTTPS REST / API
                                    ▼
┌──────────────────┐        ┌────────────────────────┐        ┌────────────────────────┐
│  Android POS App │◄──────►│ Next.js Sync / CRUD API│◄──────►│  Supabase PostgreSQL   │
│   (Room SQLite)  │        │   (/api/sync, products)│        │   (Cloud Persistence)  │
└──────────────────┘        └────────────────────────┘        └────────────────────────┘
```

## 2. Synchronization Strategy
1. **Catalog Pull:**
   - Both Android and Web fetch the catalog from the authoritative API (`GET /api/sync`).
   - Android updates its local Room database (`items`, `categories`, `products`), guaranteeing 100% offline usability.
   - Web caches the snapshot in browser IndexedDB (`idb.ts`) for offline resilience.
2. **Sales Push & Idempotency:**
   - Every sale in Android generates a unique client UUID and idempotency key (`idemp_${receiptId}`).
   - Receipts are saved locally in Room, then posted to `POST /api/sync`.
   - The server verifies that no prior receipt exists with the same idempotency key or ID before processing.
   - If a duplicate push occurs (e.g. following network timeout retry), the duplicate is safely acknowledged without duplicate revenue or inventory deductions.
3. **Inventory Tracking:**
   - On receipt synchronization, the server inspects `items_summary_json` and decrements `in_stock` for each product where `track_stock = true`.
   - Inventory decrements are bounded by zero (`Math.max(0, currentStock - quantity)`), preventing negative stock corruption.

## 3. Safe Deletion & Referential Integrity
- **Category Safe Deletion:**
  - Before a category is deleted, the backend checks for assigned products.
  - If products exist, the deletion requires an explicit target reassignment (`reassign_to_id`) or fallback to category 1 ("General").
  - Products are never orphaned or deleted when their category is removed.
- **Product Safe Archiving:**
  - Deleting a product with past sales transactions performs a soft delete / archival (`is_active = false`).
  - Archived products are hidden from POS sales catalogs but preserved for historical receipts, tax audits, and inventory logs.

## 4. Security & Data Integrity
- **No Client Secrets:**
  - Database service role keys and server credentials reside exclusively on the server in environment variables.
  - Android APKs and public JavaScript bundles only communicate with HTTPS endpoints using sanitized public parameters.
- **Input Validation:**
  - All write endpoints (`POST /api/categories`, `POST /api/products`, `PUT /api/products`) strictly enforce required fields, non-negative prices, and non-negative costs.
- **Secret Redaction:**
  - Log outputs and telemetry never output raw JWT tokens, database passwords, or connection strings.
