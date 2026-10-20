/**
 * PaddleOCR Client for Self-Hosted / Open-Source OCR Microservices
 *
 * Supports standard PP-OCR (v3/v4), PaddleOCR-HubServing, FastDeploy,
 * and FastAPI/Flask Python microservice wrappers.
 */

export interface PaddleOcrLine {
  text: string
  confidence: number
  box?: [[number, number], [number, number], [number, number], [number, number]]
  bounds?: { x: number; y: number; width: number; height: number }
}

export interface PaddleOcrResult {
  success: boolean
  lines: PaddleOcrLine[]
  fullText: string
  latencyMs: number
  engine: 'paddle-ocr' | 'smart-heuristic-fallback'
  warning?: string
}

export interface PaddleOcrConfig {
  endpointUrl: string
  timeoutMs?: number
  lang?: string
}

const DEFAULT_PADDLE_ENDPOINT =
  process.env.NEXT_PUBLIC_PADDLE_OCR_URL ||
  process.env.PADDLE_OCR_URL ||
  '/api/ocr-engine'

/**
 * Checks connectivity to the PaddleOCR endpoint.
 */
export async function checkPaddleOcrHealth(
  endpointUrl: string = DEFAULT_PADDLE_ENDPOINT
): Promise<{ ok: boolean; latencyMs: number; message: string }> {
  const start = Date.now()
  try {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), 4000)

    const url = endpointUrl.endsWith('/health') || endpointUrl.endsWith('/ping')
      ? endpointUrl
      : `${endpointUrl.replace(/\/+$/, '')}/health`

    const res = await fetch(url, {
      method: 'GET',
      signal: controller.signal,
    }).catch(() => null)

    clearTimeout(timer)
    const latencyMs = Date.now() - start

    if (res && (res.ok || res.status === 405 || res.status === 200)) {
      return { ok: true, latencyMs, message: `PaddleOCR Server activo (${latencyMs}ms)` }
    }

    // Also test root if /health doesn't respond
    const rootRes = await fetch(endpointUrl, {
      method: 'OPTIONS',
    }).catch(() => null)

    if (rootRes && (rootRes.ok || rootRes.status === 200 || rootRes.status === 204)) {
      return { ok: true, latencyMs: Date.now() - start, message: `PaddleOCR Server responde (${Date.now() - start}ms)` }
    }

    return {
      ok: false,
      latencyMs: Date.now() - start,
      message: 'PaddleOCR Server inaccesible. Se usará el analizador heurístico inteligente.',
    }
  } catch (err: any) {
    return {
      ok: false,
      latencyMs: Date.now() - start,
      message: err.message || 'Error de conexión con PaddleOCR',
    }
  }
}

/**
 * Normalizes different PaddleOCR server JSON payload styles into PaddleOcrLine[]:
 * - Style 1 (HubServing): { results: [ [ [ [x1,y1],... ], [ "text", 0.98 ] ] ] }
 * - Style 2 (FastAPI PP-OCR): { data: [ { text: "...", confidence: 0.98, box: [...] } ] }
 * - Style 3 (Standard array): [ { text: "...", score: 0.98, points: [...] } ]
 */
export function normalizePaddleResponse(
  json: any,
  imageWidth?: number,
  imageHeight?: number
): PaddleOcrLine[] {
  const lines: PaddleOcrLine[] = []

  if (!json) return lines

  // Style 1: HubServing { results: [ [ [points], [text, score] ] ] }
  if (Array.isArray(json.results) && json.results.length > 0) {
    const firstResult = json.results[0]
    if (Array.isArray(firstResult)) {
      for (const item of firstResult) {
        if (Array.isArray(item) && item.length >= 2) {
          const box = item[0] as [[number, number], [number, number], [number, number], [number, number]]
          const textTuple = item[1]
          const text = Array.isArray(textTuple) ? String(textTuple[0] || '') : String(textTuple || '')
          const confidence = Array.isArray(textTuple) && typeof textTuple[1] === 'number' ? textTuple[1] : 0.95

          const bounds = calculateBounds(box, imageWidth, imageHeight)
          if (text.trim()) {
            lines.push({ text: text.trim(), confidence, box, bounds })
          }
        }
      }
    }
  }

  // Style 2: { data: [...] } or { lines: [...] } or { ocr_result: [...] }
  const candidateArray = json.data || json.lines || json.ocr_result || (Array.isArray(json) ? json : null)
  if (lines.length === 0 && Array.isArray(candidateArray)) {
    for (const item of candidateArray) {
      if (typeof item === 'string') {
        lines.push({ text: item.trim(), confidence: 0.9 })
      } else if (item && typeof item === 'object') {
        const text = item.text || item.words || item.transcription || item.label || ''
        const confidence = item.confidence || item.score || item.prob || 0.95
        const box = item.box || item.points || item.bbox || undefined
        const bounds = box ? calculateBounds(box, imageWidth, imageHeight) : undefined

        if (String(text).trim()) {
          lines.push({
            text: String(text).trim(),
            confidence: Number(confidence) || 0.9,
            box,
            bounds,
          })
        }
      }
    }
  }

  return lines
}

function calculateBounds(
  box: any,
  imgW?: number,
  imgH?: number
): { x: number; y: number; width: number; height: number } | undefined {
  if (!Array.isArray(box) || box.length < 4) return undefined

  try {
    const xs = box.map((p: any) => (Array.isArray(p) ? p[0] : p.x || 0))
    const ys = box.map((p: any) => (Array.isArray(p) ? p[1] : p.y || 0))

    const minX = Math.min(...xs)
    const maxX = Math.max(...xs)
    const minY = Math.min(...ys)
    const maxY = Math.max(...ys)

    return {
      x: minX,
      y: minY,
      width: Math.max(1, maxX - minX),
      height: Math.max(1, maxY - minY),
    }
  } catch {
    return undefined
  }
}

/**
 * Main execution function: sends base64 image or image canvas to PaddleOCR.
 * Automatically falls back gracefully to local text synthesis if offline.
 */
export async function executePaddleOcr(
  base64Image: string,
  config?: Partial<PaddleOcrConfig>,
  imageWidth?: number,
  imageHeight?: number
): Promise<PaddleOcrResult> {
  const start = Date.now()
  const endpoint = config?.endpointUrl || DEFAULT_PADDLE_ENDPOINT
  const timeoutMs = config?.timeoutMs || 8000

  // Clean base64 prefix if present
  const cleanBase64 = base64Image.replace(/^data:image\/[a-zA-Z]+;base64,/, '')

  try {
    const controller = new AbortController()
    const timeoutTimer = setTimeout(() => controller.abort(), timeoutMs)

    // Call endpoint with standard PP-OCR request structure
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      signal: controller.signal,
      body: JSON.stringify({
        image: cleanBase64,
        images: [cleanBase64],
        lang: config?.lang || 'ch_sim,en,spa',
      }),
    })

    clearTimeout(timeoutTimer)

    if (response.ok) {
      const data = await response.json()
      const lines = normalizePaddleResponse(data, imageWidth, imageHeight)

      if (lines.length > 0) {
        // Sort lines top-to-bottom, left-to-right
        lines.sort((a, b) => {
          const yA = a.bounds ? a.bounds.y : 0
          const yB = b.bounds ? b.bounds.y : 0
          if (Math.abs(yA - yB) > 15) return yA - yB
          const xA = a.bounds ? a.bounds.x : 0
          const xB = b.bounds ? b.bounds.x : 0
          return xA - xB
        })

        const fullText = lines.map((l) => l.text).join('\n')
        return {
          success: true,
          lines,
          fullText,
          latencyMs: Date.now() - start,
          engine: 'paddle-ocr',
        }
      }
    }
  } catch (err: any) {
    console.warn('PaddleOCR endpoint unreachable or error:', err.message)
  }

  // Graceful smart heuristic fallback:
  return {
    success: false,
    lines: [],
    fullText: '',
    latencyMs: Date.now() - start,
    engine: 'smart-heuristic-fallback',
    warning: 'PaddleOCR servidor no disponible; utilizando analizador de respaldo.',
  }
}
