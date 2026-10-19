export interface ParsedMenuItem {
  id: string
  name: string
  category: string
  price: number
  description: string
  confidence: number
  status: 'NEW' | 'UPDATE' | 'IGNORE'
  rejection_reason?: string
}

const COMMON_CATEGORY_KEYWORDS: Record<string, string> = {
  'ENTRADA': 'Entradas y Piqueos',
  'ENTRADAS': 'Entradas y Piqueos',
  'PIQUEO': 'Entradas y Piqueos',
  'PIQUEOS': 'Entradas y Piqueos',
  'ENSALADA': 'Ensaladas',
  'ENSALADAS': 'Ensaladas',
  'SEGUNDO': 'Platos de Fondo',
  'SEGUNDOS': 'Platos de Fondo',
  'FONDO': 'Platos de Fondo',
  'FONDOS': 'Platos de Fondo',
  'PLATO': 'Platos de Fondo',
  'PLATOS': 'Platos de Fondo',
  'CARNE': 'Hamburguesas y Carnes',
  'CARNES': 'Hamburguesas y Carnes',
  'PARRILLA': 'Hamburguesas y Carnes',
  'PARRILLAS': 'Hamburguesas y Carnes',
  'HAMBURGUESA': 'Hamburguesas y Carnes',
  'HAMBURGUESAS': 'Hamburguesas y Carnes',
  'BURGER': 'Hamburguesas y Carnes',
  'BURGERS': 'Hamburguesas y Carnes',
  'POLLO': 'Pollos y Brasas',
  'POLLOS': 'Pollos y Brasas',
  'BRASA': 'Pollos y Brasas',
  'PIZZA': 'Pizzas y Empanadas',
  'PIZZAS': 'Pizzas y Empanadas',
  'PASTA': 'Pastas e Italianos',
  'PASTAS': 'Pastas e Italianos',
  'MARISCO': 'Pescados y Mariscos',
  'MARISCOS': 'Pescados y Mariscos',
  'PESCADO': 'Pescados y Mariscos',
  'PESCADOS': 'Pescados y Mariscos',
  'CEVICHE': 'Pescados y Mariscos',
  'CEVICHES': 'Pescados y Mariscos',
  'BEBIDA': 'Bebidas y Refrescos',
  'BEBIDAS': 'Bebidas y Refrescos',
  'REFRESCO': 'Bebidas y Refrescos',
  'REFRESCOS': 'Bebidas y Refrescos',
  'TRAGO': 'Bebidas y Licores',
  'TRAGOS': 'Bebidas y Licores',
  'COCTEL': 'Bebidas y Licores',
  'COCTELES': 'Bebidas y Licores',
  'CERVEZA': 'Bebidas y Licores',
  'CERVEZAS': 'Bebidas y Licores',
  'POSTRE': 'Postres y Dulces',
  'POSTRES': 'Postres y Dulces',
  'CAFE': 'Cafetería',
  'CAFES': 'Cafetería',
}

/**
 * Parses raw text extracted by OCR into structured menu items.
 * Robust against dot-leaders (....), dashes, currency symbols (S/, $, €),
 * European comma decimals, OCR noise, and missing prices.
 */
export function parseRawMenuText(
  rawText: string,
  existingItems: { name: string }[] = []
): { items: ParsedMenuItem[]; rawLinesCount: number } {
  if (!rawText || !rawText.trim()) {
    return { items: [], rawLinesCount: 0 }
  }

  const lines = rawText
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 0)

  const parsed: ParsedMenuItem[] = []
  let currentCategory = 'General'

  // Regex to detect price at end or anywhere in line
  // Matches: S/ 38.50, S/.38,50, $25.00, 25.00, 25,50, S/ 45, etc.
  const priceRegex = /(?:(?:s\/?\.?|\$|€|usd|pen)\s*)?(\d{1,4}(?:[.,]\d{1,2})?)(?:\s*(?:s\/?\.?|\$|€|usd|pen))?(?:\s*$|\s*[|/)]|\s*$)/i

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i]

    // 1. Check if line is a category header (e.g. "=== ENTRADAS ===" or "PLATOS DE FONDO")
    const cleanHeader = rawLine.replace(/[=*\-_#:~]/g, '').trim().toUpperCase()
    if (cleanHeader.length >= 3 && cleanHeader.length <= 35) {
      for (const [kw, catName] of Object.entries(COMMON_CATEGORY_KEYWORDS)) {
        if (cleanHeader === kw || cleanHeader.startsWith(kw + ' ') || cleanHeader.endsWith(' ' + kw)) {
          currentCategory = catName
          break
        }
      }
    }

    // 2. Clean common OCR noise like dots (....), dashes (----), pipes
    const sanitizedLine = rawLine
      .replace(/[.]{2,}/g, ' ')
      .replace(/[-]{2,}/g, ' ')
      .replace(/[_]{2,}/g, ' ')
      .replace(/[|~]/g, ' ')
      .trim()

    // 3. Match price
    const match = sanitizedLine.match(priceRegex)

    if (match && match[1]) {
      const rawPriceStr = match[1].replace(',', '.')
      const numPrice = parseFloat(rawPriceStr)

      // Get item name by removing the price portion
      const namePortion = sanitizedLine
        .replace(match[0], '')
        .replace(/[.,:;\\/]+$/, '')
        .trim()

      if (namePortion.length >= 2 && numPrice > 0 && numPrice < 10000) {
        // Look ahead for an optional description line
        let description = ''
        if (i + 1 < lines.length) {
          const nextLine = lines[i + 1].trim()
          // If next line has no price and is descriptive text (< 120 chars)
          if (!nextLine.match(priceRegex) && nextLine.length > 5 && nextLine.length < 150) {
            description = nextLine
            i++ // consume next line as description
          }
        }

        const isDuplicate = existingItems.some(
          (ex) => ex.name.toLowerCase().trim() === namePortion.toLowerCase().trim()
        )

        parsed.push({
          id: `item_${Date.now()}_${parsed.length + 1}`,
          name: namePortion,
          category: currentCategory,
          price: Math.round(numPrice * 100) / 100,
          description: description,
          confidence: match[0].includes('S/') || match[0].includes('$') ? 0.95 : 0.88,
          status: isDuplicate ? 'UPDATE' : 'NEW',
        })
      }
    }
  }

  // If no items were parsed by line-by-line, try pattern matching across blocks
  if (parsed.length === 0 && rawText.length > 10) {
    const fallbackRegex = /([A-Za-zÀ-ÿ\s]{3,35})\s*(?:[:.\-~]|\.{2,})*\s*(?:S\/?\.?|\$)?\s*(\d+[.,]?\d{0,2})/gi
    let matchGroup: RegExpExecArray | null = null
    while ((matchGroup = fallbackRegex.exec(rawText)) !== null) {
      const name = matchGroup[1].replace(/[.,:;\-_]/g, '').trim()
      const price = parseFloat(matchGroup[2].replace(',', '.'))
      if (name.length > 2 && price > 0 && price < 5000) {
        parsed.push({
          id: `item_fb_${Date.now()}_${parsed.length + 1}`,
          name,
          category: currentCategory,
          price,
          description: '',
          confidence: 0.8,
          status: 'NEW',
        })
      }
    }
  }

  return { items: parsed, rawLinesCount: lines.length }
}
