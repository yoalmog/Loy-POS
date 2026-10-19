# Phase J: Build, Deployment & Environment Configuration

## 1. Production Architecture Overview
- **Authoritative Web Back Office:** `https://loy-pos.vercel.app`
- **Back Office Admin URL:** `https://loy-pos.vercel.app/admin` (redirects to `/dashboard`)
- **Android POS Release Build:** Native APK built with release signing config using standalone Gradle wrapper.

## 2. Environment Variables Configuration
The application requires the following environment variables. **Never commit actual secrets or credentials to source control.**

| Environment Variable | Target Client | Required | Description |
| :--- | :--- | :--- | :--- |
| `NEXT_PUBLIC_SUPABASE_URL` | Web Back Office | Yes | Supabase Project HTTPS URL (`https://<project-ref>.supabase.co`) |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Web Back Office | Yes | Supabase Public Anonymous API key |
| `SUPABASE_SERVICE_ROLE_KEY` | Web Server (API) | Optional | Elevated key for administrative sync scripts (server-only) |
| `GEMINI_API_KEY` | Web Server (API) | Optional | Google Gemini API key for multimodal menu scanning (`/api/scan-menu`) |
| `KEYSTORE_PATH` | Android Build | Optional | Custom upload keystore path (falls back to repository `debug.keystore`) |
| `STORE_PASSWORD` | Android Build | Optional | Release keystore store password |
| `KEY_ALIAS` | Android Build | Optional | Key alias name |
| `KEY_PASSWORD` | Android Build | Optional | Key alias password |

## 3. Web Production Build Verification
- **Framework:** Next.js 14.2.35 with React 18 and Tailwind CSS
- **Command:** `npm run build`
- **Output Status:** `Compiled successfully`
- **Static Pages Generated:** 19/19 routes
- **Health Check Endpoint:** `GET /api/sync` -> `{ "server_status": "online", "timestamp": ... }`

## 4. Android Release Artifact
- **APK File Path:** `app/build/outputs/apk/release/app-release.apk`
- **File Size:** 16,222,109 bytes (15.47 MB)
- **SHA-256 Hash:** `725121A3E515DC609D45F6D2391A3E0D56797194AB59B9AE3B7DB50275127277`
- **Application ID:** `com.aistudio.loyversepos.kxmpzq`
- **Version Name / Code:** `1.0` (VersionCode `1`)
- **Min SDK / Target SDK:** API 24 (Android 7.0) / API 36 (Android 14+)
- **Installation Command:**
  ```bash
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```

## 5. Deployment Instructions (Vercel)
1. Push all verified commits to `main` on `https://github.com/yoalmog/Loy-POS`.
2. In the Vercel project dashboard for `loy-pos.vercel.app`, ensure the Root Directory is set to `web`.
3. Add the production environment variables (`NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `GEMINI_API_KEY`).
4. Trigger production deploy.
