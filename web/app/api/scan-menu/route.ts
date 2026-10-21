import { NextRequest, NextResponse } from 'next/server'
import { parseRawMenuText, ExtractedPriceVariant } from '@/lib/ocr/menuParser'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey',
}

export async function OPTIONS() {
  return NextResponse.json({}, { headers: corsHeaders })
}

export interface ScannedMenuCandidate {
  id: string
  name: string
  name_es: string
  name_en?: string
  name_he?: string
  category: string
  categoryName: string
  price: number
  currency: string
  description?: string
  prices: ExtractedPriceVariant[]
  variants: {
    id: string
    name: string
    price: number
    cost?: number
    in_stock?: number
  }[]
  confidence: number | {
    name: number
    description: number
    category: number
    prices: number
    overall: number
  }
  warnings: string[]
  reviewRequired: boolean
  status: 'NEW' | 'UPDATE' | 'REJECTED'
  rejection_reason?: string
}

export async function POST(request: NextRequest) {
  try {
    const contentType = request.headers.get('content-type') || ''
    let base64Data = ''
    let mimeType = 'image/jpeg'
    let rawText = ''

    if (contentType.includes('application/json')) {
      const body = await request.json()

      // If client sent raw extracted OCR text or pasted text
      if (body.raw_text && typeof body.raw_text === 'string' && body.raw_text.trim()) {
        rawText = body.raw_text
      } else if (body.image) {
        const dataStr = body.image as string
        if (dataStr.startsWith('data:')) {
          const matches = dataStr.match(/^data:([A-Za-z-+/]+);base64,(.+)$/)
          if (matches && matches.length === 3) {
            mimeType = matches[1]
            base64Data = matches[2]
          } else {
            base64Data = dataStr.split(',')[1] || dataStr
          }
        } else {
          base64Data = dataStr
        }
      } else {
        return NextResponse.json(
          { error: 'Image data or raw_text is required' },
          { status: 400, headers: corsHeaders }
        )
      }
    } else if (contentType.includes('multipart/form-data')) {
      const formData = await request.formData()
      const textParam = formData.get('raw_text')
      if (textParam && typeof textParam === 'string' && textParam.trim()) {
        rawText = textParam
      } else {
        const file = formData.get('file') as File | null
        if (!file) {
          return NextResponse.json(
            { error: 'File upload or raw_text is required' },
            { status: 400, headers: corsHeaders }
          )
        }
        mimeType = file.type || 'image/jpeg'
        const buffer = Buffer.from(await file.arrayBuffer())
        base64Data = buffer.toString('base64')
      }
    }

    // Direct text parsing path if rawText is provided
    if (rawText && rawText.trim()) {
      const { products } = parseRawMenuText(rawText)
      const candidates: ScannedMenuCandidate[] = products.map((it, idx) => ({
        id: it.id || `scan_txt_${Date.now()}_${idx}`,
        name: it.name,
        name_es: it.name,
        name_en: '',
        category: it.categoryName || it.category,
        categoryName: it.categoryName || it.category,
        price: it.price,
        currency: 'PEN',
        description: it.description,
        prices: it.prices || [{ label: 'Precio', price: it.price }],
        variants: it.variants || [],
        confidence: it.confidence,
        warnings: it.warnings || [],
        reviewRequired: it.reviewRequired || false,
        status: it.status,
        rejection_reason: it.rejection_reason,
      }))

      return NextResponse.json(
        {
          success: true,
          source: 'structured-ocr-parser',
          candidates,
          count: candidates.length,
        },
        { headers: corsHeaders }
      )
    }

    // Size validation: max 10MB
    const approxSizeBytes = (base64Data.length * 3) / 4
    if (approxSizeBytes > 10 * 1024 * 1024) {
      return NextResponse.json(
        { error: 'Image file exceeds maximum allowable size (10 MB)' },
        { status: 413, headers: corsHeaders }
      )
    }

    // 1. Try PaddleOCR server if PADDLE_OCR_URL is configured
    const paddleOcrUrl = process.env.PADDLE_OCR_URL
    if (paddleOcrUrl) {
      try {
        const paddleRes = await fetch(paddleOcrUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ images: [base64Data] }),
        })

        if (paddleRes.ok) {
          const paddleData = await paddleRes.json()
          const detectedLines: string[] = []
          if (Array.isArray(paddleData.results)) {
            for (const r of paddleData.results) {
              if (Array.isArray(r)) {
                for (const lineObj of r) {
                  if (lineObj.text) detectedLines.push(lineObj.text)
                }
              }
            }
          }

          if (detectedLines.length > 0) {
            const joinedText = detectedLines.join('\n')
            const { products } = parseRawMenuText(joinedText)
            const candidates: ScannedMenuCandidate[] = products.map((it, idx) => ({
              id: `scan_paddle_${Date.now()}_${idx}`,
              name: it.name,
              name_es: it.name,
              name_en: '',
              category: it.categoryName || it.category,
              categoryName: it.categoryName || it.category,
              price: it.price,
              currency: 'PEN',
              description: it.description,
              prices: it.prices,
              variants: it.variants,
              confidence: it.confidence,
              warnings: it.warnings,
              reviewRequired: it.reviewRequired,
              status: it.status,
              rejection_reason: it.rejection_reason,
            }))

            return NextResponse.json(
              {
                success: true,
                source: 'paddle-ocr',
                candidates,
                count: candidates.length,
              },
              { headers: corsHeaders }
            )
          }
        }
      } catch (paddleErr) {
        console.warn('PaddleOCR server connection error:', paddleErr)
      }
    }

    // 2. Try Gemini Vision if GEMINI_API_KEY is configured
    const geminiApiKey = process.env.GEMINI_API_KEY
    if (geminiApiKey) {
      const candidateModels = [
        'gemini-1.5-flash-latest',
        'gemini-1.5-flash',
        'gemini-2.0-flash',
        'gemini-1.5-pro',
      ]

      for (const model of candidateModels) {
        try {
          const response = await fetch(
            `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiApiKey}`,
            {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                contents: [
                  {
                    parts: [
                      {
                        text: `You are an expert OCR and menu structure extraction assistant.
Extract all dishes/products from this menu photograph with high precision.
Rules:
1. "name": The exact dish name in the original language and spelling (preserve accents, punctuation, and parenthetical names like "(tropical)" or "(margherita)"). Do NOT translate dish names. Never use ingredients as the name.
2. "description": The ingredients and description of the dish listed below or beside its name.
3. "categoryName": The exact section or category heading under which the item appears (e.g. "PIZZAS ARDIENTES (picantes)"). Do not make section headings into products!
4. "prices": An array of objects for each price/size on the dish line: [{ "label": "Personal", "price": 17.00 }, { "label": "Mediana (para 2)", "price": 30.00 }, { "label": "Familiar (para + de 2)", "price": 48.00 }]. Associate column size headers with prices.
5. "price": The base price (numeric float).
6. "reviewRequired": true if any field was illegible or ambiguous, false otherwise.

Return ONLY a valid JSON array of objects without markdown formatting.`,
                      },
                      {
                        inline_data: {
                          mime_type: mimeType,
                          data: base64Data,
                        },
                      },
                    ],
                  },
                ],
                generationConfig: {
                  temperature: 0.1,
                  response_mime_type: 'application/json',
                },
              }),
            }
          )

          if (response.ok) {
            const result = await response.json()
            const rawResultText = result.candidates?.[0]?.content?.parts?.[0]?.text
            if (rawResultText) {
              const parsedArray = JSON.parse(rawResultText)
              if (Array.isArray(parsedArray)) {
                const candidates: ScannedMenuCandidate[] = parsedArray.map((it: any, idx: number) => {
                  const prices: ExtractedPriceVariant[] = Array.isArray(it.prices) && it.prices.length > 0
                    ? it.prices.map((p: any) => ({
                        label: p.label || 'Personal',
                        price: Number(p.price) || 0,
                      }))
                    : [{ label: 'Personal', price: Number(it.price) || 0 }]

                  const basePrice = Number(it.price) || prices[0]?.price || 0
                  const variants = prices.map((pv, vIdx) => ({
                    id: `var_${Date.now()}_${idx}_${vIdx}`,
                    name: pv.label,
                    price: pv.price,
                    cost: Math.round(pv.price * 0.3 * 100) / 100,
                    in_stock: 25,
                  }))

                  return {
                    id: `scan_${Date.now()}_${idx}`,
                    name: it.name || 'Producto sin nombre',
                    name_es: it.name || 'Producto sin nombre',
                    name_en: it.name_en || '',
                    category: it.categoryName || it.category || 'General',
                    categoryName: it.categoryName || it.category || 'General',
                    price: basePrice,
                    currency: it.currency || 'PEN',
                    description: it.description || '',
                    prices,
                    variants,
                    confidence: typeof it.confidence === 'number' ? it.confidence : 0.95,
                    warnings: it.reviewRequired ? ['Marcado para revisión manual por ambigüedad'] : [],
                    reviewRequired: Boolean(it.reviewRequired),
                    status: (basePrice > 0 && it.name) ? 'NEW' : 'REJECTED',
                    rejection_reason: basePrice <= 0 ? 'Precio inválido o faltante' : undefined,
                  }
                })

                return NextResponse.json(
                  {
                    success: true,
                    source: `gemini-vision (${model})`,
                    candidates,
                    count: candidates.length,
                  },
                  { headers: corsHeaders }
                )
              }
            }
          }
        } catch (geminiErr) {
          console.warn(`Gemini model ${model} extraction failed:`, geminiErr)
        }
      }
    }

    // 3. Fallback: Server-side Tesseract OCR on base64 image
    try {
      const buffer = Buffer.from(base64Data, 'base64')
      const Tesseract = (await import('tesseract.js')).default
      const ocrResult = await Tesseract.recognize(buffer, 'spa+eng')
      const extractedText = ocrResult?.data?.text || ''

      if (extractedText.trim()) {
        const { products } = parseRawMenuText(extractedText)
        if (products.length > 0) {
          const candidates: ScannedMenuCandidate[] = products.map((it, idx) => ({
            id: it.id || `scan_tess_${Date.now()}_${idx}`,
            name: it.name,
            name_es: it.name,
            name_en: '',
            category: it.categoryName || it.category,
            categoryName: it.categoryName || it.category,
            price: it.price,
            currency: 'PEN',
            description: it.description,
            prices: it.prices,
            variants: it.variants,
            confidence: it.confidence,
            warnings: it.warnings,
            reviewRequired: it.reviewRequired,
            status: it.status,
            rejection_reason: it.rejection_reason,
          }))

          return NextResponse.json(
            {
              success: true,
              source: 'server-tesseract-ocr',
              candidates,
              count: candidates.length,
            },
            { headers: corsHeaders }
          )
        }
      }
    } catch (tessErr) {
      console.warn('Server-side Tesseract OCR error:', tessErr)
    }

    return NextResponse.json(
      {
        success: false,
        error: 'No se pudieron extraer productos del menú. Revise la nitidez de la imagen o ingrese el texto manualmente.',
        candidates: [],
        count: 0,
      },
      { status: 422, headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Error processing menu scan' },
      { status: 500, headers: corsHeaders }
    )
  }
}
