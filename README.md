# AuraPOS — Loyverse POS Clone

[![Vercel](https://img.shields.io/badge/Back%20Office-Live%20on%20Vercel-black?logo=vercel)](https://loy-pos.vercel.app)
[![Next.js](https://img.shields.io/badge/Next.js-14-black?logo=next.js)](https://nextjs.org)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-green?logo=android)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Database-Supabase%20PostgreSQL-3ECF8E?logo=supabase)](https://supabase.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A full-stack, offline-first Point-of-Sale system inspired by **Loyverse POS**, consisting of a **Next.js Back Office** web application and a native **Android POS** application — both powered by a shared **Supabase PostgreSQL** database.

---

## 📺 Live Demo

| Component | URL |
|---|---|
| Back Office (Web) | **https://loy-pos.vercel.app** |
| Admin Panel | **https://loy-pos.vercel.app/admin** |
| Products API | `GET https://loy-pos.vercel.app/api/products` |
| Categories API | `GET https://loy-pos.vercel.app/api/categories` |
| Sync Snapshot API | `GET https://loy-pos.vercel.app/api/sync` |

---

## ✨ Features

### Back Office (Web — Next.js)
- 📦 **Product Management** — Create, edit, and delete products with images, prices, categories, modifiers, and variants
- 🗂 **Category Management** — Colour-coded categories synced to the Android POS in real time
- 🛒 **Orders & Receipts** — Full checkout flow with itemised receipts and QR codes
- 📊 **Reports & Analytics** — Sales summaries with `recharts`-powered charts
- 💳 **Inventory Tracking** — Stock levels updated on every sale
- ↩️ **Refunds** — Per-item or full-order refund support
- 🔐 **Authentication** — Supabase Auth with role-based access (Admin / Cashier)
- 🌐 **Internationalisation** — Multi-language UI with persistent language & theme settings
- 📱 **Responsive UI** — TailwindCSS, dark mode, glassmorphism

### Android POS (Jetpack Compose)
- 🟢 **Loyverse-style interface** — Green header, category chips, product grid, numeric keypad
- 🔄 **Real-time synchronisation** — Pulls live catalog snapshots from the Back Office REST API
- 💾 **Offline-first** — Room database caches all products and categories for offline sales
- 🖼 **Async image loading** — Coil Compose for remote product images
- 🧾 **Ticket management** — Add items, apply discounts, process payment
- 🔗 **Deep-link to Back Office** — Settings screen opens `https://loy-pos.vercel.app/admin` in the browser

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Supabase PostgreSQL                       │
│         (products · categories · orders · users)            │
└────────────────────────────┬────────────────────────────────┘
                             │ SQL / REST
           ┌─────────────────▼─────────────────┐
           │       Next.js 14 Back Office       │
           │       (hosted on Vercel)           │
           │                                    │
           │  /api/products   → product list    │
           │  /api/categories → category list   │
           │  /api/sync       → full snapshot   │
           │  /admin          → Back Office UI  │
           └────────────────────┬───────────────┘
                                │ HTTPS / Retrofit
              ┌─────────────────▼──────────────────┐
              │      Android POS Application        │
              │      (Jetpack Compose + Room)        │
              │                                     │
              │  SyncManager → BackOfficeApiClient  │
              │  Room DB     → PosRepository        │
              │  SalesRegisterScreen (Loyverse UI)  │
              └─────────────────────────────────────┘
```

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Web framework** | Next.js 14 (App Router, TypeScript) |
| **Styling** | TailwindCSS 3, `lucide-react`, `recharts` |
| **Database** | Supabase PostgreSQL |
| **Auth** | Supabase Auth |
| **Android UI** | Jetpack Compose (Kotlin) |
| **Android networking** | Retrofit 2 + Moshi |
| **Android persistence** | Room (SQLite) |
| **Android images** | Coil Compose |
| **Deployment** | Vercel (web) · GitHub Actions (Android APK) |

---

## 📁 Project Structure

```
Loy-POS/
├── web/                          # Next.js Back Office
│   ├── app/
│   │   ├── api/
│   │   │   ├── products/route.ts # GET /api/products
│   │   │   ├── categories/route.ts
│   │   │   └── sync/route.ts     # GET /api/sync (full snapshot)
│   │   ├── admin/                # Admin dashboard pages
│   │   └── ...
│   ├── components/               # Shared React components
│   ├── lib/                      # Supabase client, helpers
│   ├── styles/
│   ├── .env.example
│   └── package.json
│
├── app/                          # Android POS (Gradle module)
│   └── src/main/java/com/example/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── api/
│       │   │   └── SupabaseApiClient.kt   # Retrofit + BackOfficeRestApi
│       │   ├── db/
│       │   │   └── AppDatabase.kt         # Room database + seed data
│       │   ├── model/
│       │   │   └── Models.kt              # ItemEntity, CategoryEntity
│       │   └── repository/
│       │       └── PosRepository.kt
│       ├── ui/
│       │   └── pos/
│       │       ├── SalesRegisterScreen.kt # Loyverse-style POS UI
│       │       └── PosViewModel.kt
│       └── util/
│           ├── AppConfig.kt               # Production URL constants
│           └── SyncManager.kt            # Background sync scheduler
│
├── supabase/                     # DB migrations & seed SQL
├── .github/workflows/            # CI — Android APK build
├── .env.example
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Node.js | ≥ 18 |
| npm | ≥ 9 |
| Java (JDK) | ≥ 17 |
| Android Studio | Hedgehog+ |
| Android SDK | API 34 |

---

### 1 — Clone the repository

```bash
git clone https://github.com/yoalmog/Loy-POS.git
cd Loy-POS
```

---

### 2 — Configure environment variables

Copy the example file and fill in your Supabase credentials:

```bash
cp .env.example web/.env.local
```

**`web/.env.local`**
```env
SUPABASE_URL=https://<your-project-id>.supabase.co
SUPABASE_ANON_KEY=<your-anon-public-key>
```

> Get these values from **Supabase → Project Settings → API**.

---

### 3 — Run the Back Office locally

```bash
cd web
npm install
npm run dev
```

Open http://localhost:3000.

#### Run acceptance tests

```bash
npm test
```

---

### 4 — Build the Android POS

Open the root project (`Loy-POS/`) in **Android Studio**, wait for Gradle sync, then:

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

The output APK is written to `app/build/outputs/apk/`.

---

### 5 — Deploy the Back Office to Vercel

```bash
npm install -g vercel
cd web
vercel --prod
```

Set the same `SUPABASE_URL` and `SUPABASE_ANON_KEY` environment variables in the Vercel project dashboard.

---

## 🔌 REST API Reference

All endpoints are served by the Next.js Back Office and require no authentication for read access.

### `GET /api/products`

Returns the full product catalog.

```json
[
  {
    "id": "uuid",
    "name": "Espresso",
    "price": 3.50,
    "category_id": "uuid",
    "image_url": "https://...",
    "in_stock": true
  }
]
```

### `GET /api/categories`

Returns all product categories.

```json
[
  { "id": "uuid", "name": "Coffee", "color": "#4CAF50" }
]
```

### `GET /api/sync`

Returns a single atomic snapshot consumed by the Android `SyncManager`.

```json
{
  "products": [...],
  "categories": [...],
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 📱 Android Sync Flow

```
App launch
    │
    ▼
SyncManager.schedulePeriodic()
    │
    ▼
BackOfficeApiClient.fetchSnapshot()  ──► GET /api/sync
    │
    ▼
AppDatabase (Room) ← upsert items & categories
    │
    ▼
PosRepository.getItems()  ──► SalesRegisterScreen
```

When the device is **offline**, the app falls back to the last cached Room snapshot. On first install, canonical seed products are inserted automatically.

---

## 🗄 Database Schema (Supabase)

Key tables managed by Supabase PostgreSQL:

| Table | Description |
|---|---|
| `products` | Items for sale (name, price, image, stock) |
| `categories` | Product groupings with colour coding |
| `orders` | Completed sales tickets |
| `order_items` | Line items per order |
| `modifiers` | Add-ons / extras per product |
| `variants` | Size / colour variants per product |
| `profiles` | User accounts (admin / cashier roles) |

SQL migrations are in the `supabase/` directory.

---

## 🔐 Environment Variables

| Variable | Where | Description |
|---|---|---|
| `SUPABASE_URL` | `web/.env.local` | Your Supabase project REST URL |
| `SUPABASE_ANON_KEY` | `web/.env.local` | Supabase anon/public API key |
| `BACK_OFFICE_URL` | `AppConfig.kt` | Android production base URL (default: `https://loy-pos.vercel.app/`) |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "feat: add my feature"`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

Please run the web acceptance tests (`npm test`) and Android unit tests (`./gradlew test`) before submitting.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- [Loyverse POS](https://loyverse.com) — design inspiration
- [Supabase](https://supabase.com) — backend-as-a-service
- [Vercel](https://vercel.com) — web hosting
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Android UI toolkit
