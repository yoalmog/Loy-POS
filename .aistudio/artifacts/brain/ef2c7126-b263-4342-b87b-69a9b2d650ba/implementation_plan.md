# Self-Hosted Open-Source OCR Engine (PaddleOCR) Integration

Integrate a high-performance, self-hosted open-source OCR engine (PaddleOCR API) across both the Next.js Web Back Office and the Android Jetpack Compose POS app. This replaces legacy browser-heavy Tesseract with standardized PaddleOCR line-level text and bounding-box detection, paired with a smart heuristic menu/receipt parser and seamless offline manual review fallback.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed based on your requirements and are locked into this plan:

- **Confirmed OCR Engine**: Self-hosted / Open-Source OCR API (PaddleOCR standard format: text line detection, angle rectification, character recognition with confidence scores and polygon bounding coordinates).
- **Confirmed Platforms**: Both Web Back Office (`web/`) and Android POS mobile app (`app/`).
- **Confirmed Fallback Strategy**: Smart regex and linguistic heuristic parser with interactive manual review table when the network or OCR server is unreachable.
- **Endpoint Configuration**: Provides a default API proxy route (`/api/ocr-engine`) and configurable custom endpoint settings (e.g. `http://localhost:8866/predict/ocr_system` or remote GPU instance `http://192.168.1.50:8000/ocr`) in both Web and Android settings.

---

## 1. Overview & Core Concept

### What It Does
Empowers business owners and cashiers to photograph or upload physical menus, vendor invoices, price sheets, and paper receipts, automatically extracting structured line items, dish names, categories, and prices using a dedicated PaddleOCR service.

### Target Audience & Persona
- **Restaurant & Retail Managers**: Onboarding full paper menus (50+ items) into the POS catalog in seconds instead of typing each item manually.
- **POS Cashiers**: Scanning physical delivery slips and supplier receipts for quick inventory adjustments or fast order creation.

### Key Value
- **Higher Accuracy**: PaddleOCR handles bilingual Latin/Spanish/English menus, rotated text, and vertical price columns significantly better than in-browser Tesseract.
- **Privacy & Self-Hosting**: Operates on the user's private server, local network mini-PC, or Docker container without vendor lock-in or recurring cloud per-scan billing.
- **Resilient Fallback**: If the server is offline or experiencing network drops, the built-in smart linguistic heuristic parser guarantees zero operational blockage.

---

## 2. User Experience & Visual Design

### Key User Flows

#### Flow A: Web POS Menu Scanner (`web/components/menu-scanner/MenuScannerModal.tsx`)
1. **Engine Indicator & Settings**: Modal header displays an active engine pill: `[⚡ PaddleOCR Server - Connected]` with a settings gear to configure the OCR server URL and language model (`spa+eng` / `multilingual`).
2. **Image Ingestion & Preprocessing**: Upload menu image or take snapshot; tools for rotation, contrast, and crop.
3. **Extraction & Bounding Overlays**: Dispatches to the PaddleOCR API. The preview displays dynamic bounding boxes highlighting each detected text line in real-time.
4. **Structured Review Table**: Items parsed into Name, Category, Price, and Notes with inline editing, batch category assignment, and one-click catalog import.
5. **Fallback Flow**: If the PaddleOCR endpoint times out or is unreachable, an amber notice appears (`PaddleOCR server offline - switched to Smart Heuristic Parser`) allowing the cashier to continue without interruption.

#### Flow B: Android Mobile POS Scanner (`app/src/main/java/com/example/ui/pos/components/OcrScannerDialog.kt`)
1. **Launch OCR Scanner**: Added to the POS Sales Register action bar / menu alongside Barcode Scanner: `Scan Menu / Invoice (OCR)`.
2. **Camera & Gallery Capture**: Full-screen preview with an interactive rectangular scanning viewfinder and image picker.
3. **PaddleOCR Dispatch & Visual Feedback**: Displays scanning progress with a pulse animation and latency counter (`PaddleOCR (182ms)`).
4. **Item Acceptance Sheet**: Bottom sheet with recognized items, prices, and checkboxes to add directly to the current POS shopping cart or catalog.

### Visual Identity & Theme
- **Color Tokens**: Dark slate base (`#0F172A`), primary electric cyan (`#0EA5E9`), vibrant emerald for recognized prices (`#10B981`), and warning amber (`#F59E0B`) for low-confidence detections.
- **Typography**: Monospace font (`JetBrains Mono` / `FontFamily.Monospace`) for prices, coordinates, and latency badges; bold sans-serif display typography for dish names.
- **Component Polish**: Translucent frosted glass dialogs (`backdrop-blur-md`), rounded corners (`16.dp` / `rounded-2xl`), animated scan lines, and micro-haptic feedback on Android.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct vs. Proxied OCR API Communication**
  - *Chosen Approach*: Support both direct device-to-PaddleOCR communication and a server-side API proxy route (`/api/ocr-engine`).
  - *Why*: In web browsers, calling a private IP (e.g., `http://192.168.1.100:8866`) can trigger CORS or Mixed Content errors if the POS is loaded over HTTPS. The Next.js API route proxies requests securely while the Android native app can connect directly over LAN or through the proxy.
  - *Alternatives Considered*: Enforcing only direct client calls fails modern browser security policies when running mixed HTTP/HTTPS setups.

- **Decision 2: Parser Architecture (PaddleOCR Raw Text -> Structured Menu Entities)**
  - *Chosen Approach*: Combine spatial geometry (Y-axis proximity, line bounding box alignment) with regex-based currency/price extraction in `menuParser.ts` and `OcrMenuParser.kt`.
  - *Why*: Menus often list items with names on the left and prices on the right. Spatial bounding boxes from PaddleOCR allow pairing names to prices on the same visual horizontal baseline far more accurately than flat text streams.

- **Decision 3: Zero-Blockage Fallback Strategy**
  - *Chosen Approach*: Automatic fallback to the client-side heuristic parser with clear visual notice and quick-entry manual fields.
  - *Why*: A POS station cannot freeze during busy service if the OCR microservice restarts or local Wi-Fi hiccups.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          USER CAPTURE LAYER                            │
│  ┌───────────────────────────────┐  ┌────────────────────────────────┐ │
│  │     Web MenuScannerModal      │  │     Android OcrScannerDialog   │ │
│  │   (Next.js / Lucide / Canvas) │  │  (Jetpack Compose / CameraX)   │ │
│  └──────────────┬────────────────┘  └───────────────┬────────────────┘ │
└─────────────────┼───────────────────────────────────┼──────────────────┘
                  │                                   │
                  ▼                                   ▼
┌──────────────────────────────────┐  ┌──────────────────────────────────┐
│   Web OCR Client / API Proxy     │  │   Android OcrServiceClient       │
│      (/api/ocr-engine)           │  │   (OkHttp3 / Kotlin Coroutines)  │
└─────────────────┬────────────────┘  └───────────────┬──────────────────┘
                  │                                   │
                  ├─────────────────┬─────────────────┘
                  ▼                 ▼
┌──────────────────────────────────┐ ┌───────────────────────────────────┐
│     PaddleOCR Microservice       │ │      Smart Heuristic Parser       │
│  (PP-OCRv4 / FastAPI / HubServing│ │  (Offline Regex + Spatial Layout) │
│   POST /predict or /api/v1/ocr)  │ │     Web + Android Local Logic     │
│   • Text Detection (DBNet)       │ └─────────────────┬─────────────────┘
│   • Text Recognition (SVTR)      │                   │
│   • Confidence & Bounding Boxes  │                   │
└─────────────────┬────────────────┘                   │
                  │                                    │
                  ▼                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   STRUCTURED MENU / POS INGESTION                      │
│      • Item Name, Category, Price, Modifiers, Confidence Scores        │
│      • Review / Edit / Add to Active POS Cart & Product Catalog        │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models & Contracts

#### PaddleOCR Response Contract (`PaddleOcrResponse`):
```typescript
interface PaddleOcrLine {
  text: string
  confidence: number
  box: [[number, number], [number, number], [number, number], [number, number]] // 4 corner points
}

interface PaddleOcrResult {
  success: boolean
  lines: PaddleOcrLine[]
  executionTimeMs: number
  engine: 'paddle-ocr' | 'heuristic-fallback'
}
```

#### Parsed POS Item (`ParsedOcrItem`):
```kotlin
data class ParsedOcrItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val price: Double,
    val description: String = "",
    val confidence: Float,
    val isAccepted: Boolean = true
)
```

### Implementation Checklist
1. **PaddleOCR Client & Engine Configuration**:
   - `web/lib/ocr/paddleOcrClient.ts`: Client for standard PaddleOCR formats with endpoint configuration.
   - `web/app/api/ocr-engine/route.ts`: Proxied REST endpoint with health-check and mock/sample test responses.
   - `web/components/menu-scanner/MenuScannerModal.tsx`: Connects to PaddleOCR client, visualizes bounding boxes on the canvas, and displays engine latency and health.
2. **Android POS Integration**:
   - `app/src/main/java/com/example/util/PaddleOcrClient.kt`: Network service for querying the PaddleOCR engine with timeout handling and heuristic fallback.
   - `app/src/main/java/com/example/util/OcrMenuParser.kt`: Text line analyzer pairing names, categories, and prices into POS items.
   - `app/src/main/java/com/example/ui/pos/components/OcrScannerDialog.kt`: Jetpack Compose OCR dialog with camera capture, photo selection, live detection preview, and item catalog review.
   - Update `app/src/main/java/com/example/ui/pos/SalesRegisterScreen.kt` to offer OCR menu/invoice scanning directly on the POS register.
3. **Verification**:
   - Build verification for both Android (`compile_applet`) and Next.js (`npm run build`).
