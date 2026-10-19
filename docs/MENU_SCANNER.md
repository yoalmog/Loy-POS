# Phase F: Menu Scanner Architecture & Verification

## 1. Overview
The Menu Scanner enables restaurant and retail managers to digitize printed, photographed, or digital menus into catalog products with automated candidate extraction, category tagging, price normalization, and user verification.

## 2. Scanning Pipeline
```
[User Image: Upload or Camera]
              │
              ▼
    File Validation (< 10MB, JPEG/PNG/WebP)
              │
              ▼
    Image Preprocessing (Zoom, Pan, Rotate, Contrast)
              │
              ▼
    Server-Side Vision / OCR API (`POST /api/scan-menu`)
              ├── Cloud Vision / Gemini Vision API (if configured)
              └── Heuristic OCR & RegEx Price Parser (offline/fallback)
              │
              ▼
    Candidate Parsing & Field Normalization
              ├── Decimal Normalization ("S/ 38,50" -> 38.50)
              ├── Currency Extraction (PEN, USD, EUR)
              ├── Duplicate & Noise Detection (Header text, zero prices)
              └── Existing Catalog Match (NEW vs UPDATE tagging)
              │
              ▼
    Review & Correction Step
              ├── Inline Editing: Name, Category, Price
              ├── Status Toggles: NEW / UPDATE / IGNORE
              └── Reject Reason Inspection
              │
              ▼
    Batch Catalog Persistence
              └── Authoritative Database Upsert (`addItemToCatalog`)
```

## 3. Test Cases & Benchmark Results
| Scenario | Input Sample | Expected Outcome | Actual Result |
| :--- | :--- | :--- | :--- |
| **Spanish Dish with Currency Symbol** | `Lomo Saltado Criollo S/ 38.50` | Name: "Lomo Saltado Criollo", Price: 38.50, Status: NEW | **PASS** (Parsed accurately, decimal preserved) |
| **Comma Decimal Separator** | `Ceviche Clásico de Pescado $34,00` | Name: "Ceviche Clásico de Pescado", Price: 34.00, Status: NEW | **PASS** (Normalized `,` to `.`) |
| **Missing Price / Header Line** | `Menú Ejecutivo del Día` | Rejected with explicit reason: "No price detected" | **PASS** (Flagged as REJECTED, status IGNORE) |
| **Price without Product Name** | `12.50` | Rejected with reason: "Empty dish name" | **PASS** (Prevented empty product creation) |
| **Duplicate Candidate** | Same menu submitted twice | Second pass detects existing item name; flags as UPDATE | **PASS** (Prevents duplicate row creation) |
| **User Correction Before Import** | User corrects OCR price from 38.5 to 40.0 in review table | Database receives corrected 40.0 price | **PASS** (User edit overrides OCR reading) |
| **Image File Size Validation** | > 10 MB payload | Rejection with HTTP 413 Payload Too Large | **PASS** (Validated on client and server) |

## 4. Multi-Format Price Regular Expression
```javascript
/(?:S\/|\$|€)?\s*(\d+[.,]\d{2})/i
```
- Matches Peruvian Soles (`S/`), Dollars (`$`), Euros (`€`), or unadorned numeric values.
- Accommodates both European/Latin American comma decimals (`38,50`) and standard period decimals (`38.50`).
