/**
 * Stage A & Stage B Menu OCR & Structured Interpretation Pipeline
 * 
 * Stage A: Document structure, line segmentation, column/price alignment,
 * section header detection, and ingredient block clustering.
 * 
 * Stage B: Menu semantic interpretation, field mapping, variant association,
 * validation, ambiguity flagging, and database-ready normalization.
 */

export interface ExtractedPriceVariant {
  label: string
  price: number
}

export interface ParsedMenuItem {
  id: string
  name: string
  category: string
  categoryName: string
  price: number
  description: string
  prices: ExtractedPriceVariant[]
  variants: {
    id: string
    name: string
    price: number
    cost?: number
    in_stock?: number
  }[]
  confidence: {
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

export interface MenuParseResult {
  items: ParsedMenuItem[]
  products: ParsedMenuItem[]
  categories: string[]
  warnings: string[]
  rawLinesCount: number
}

// Common ingredient keywords to distinguish ingredient descriptions from product names
const INGREDIENT_KEYWORDS = new Set([
  'tomate', 'salsa', 'muzzarella', 'mozzarella', 'queso', 'rocoto', 'aceituna', 'aceitunas',
  'oregano', 'orégano', 'salame', 'chorizo', 'ahumado', 'tocino', 'cabanossi',
  'piña', 'durazno', 'leche', 'condensada', 'canela', 'plátano', 'platano', 'fresas',
  'pasas', 'chocolate', 'albahaca', 'pimiento', 'espinaca', 'champiñones', 'champinones',
  'jamon', 'jamón', 'cebolla', 'ajo', 'pimienta', 'parmesano', 'prosciutto', 'carne',
  'pollo', 'camarones', 'mariscos', 'pescado', 'limon', 'limón', 'palta', 'guacamole',
  'papas', 'arroz', 'choclo', 'canchita', 'camote'
])

// Section header keywords that signify categories
const SECTION_KEYWORDS = [
  'PIZZA', 'PIZZAS', 'ENTRADA', 'ENTRADAS', 'PIQUEO', 'PIQUEOS',
  'PLATO', 'PLATOS', 'SEGUNDO', 'SEGUNDOS', 'FONDO', 'FONDOS',
  'CARNE', 'CARNES', 'HAMBURGUESA', 'HAMBURGUESAS', 'POLLO', 'POLLOS',
  'PASTA', 'PASTAS', 'ENSALADA', 'ENSALADAS', 'BEBIDA', 'BEBIDAS',
  'POSTRE', 'POSTRES', 'REFRESCO', 'REFRESCOS', 'TRAGO', 'TRAGOS',
  'COCTEL', 'COCTELES', 'CERVEZA', 'CERVEZAS', 'CAFE', 'CAFES', 'CAFETERIA'
]

// Default sizes if 3 prices are detected without an explicit size header
const DEFAULT_THREE_SIZES = ['Personal', 'Mediana (para 2)', 'Familiar (para + de 2)']
const DEFAULT_TWO_SIZES = ['Mediana', 'Familiar']

/**
 * Extracts all numeric prices from a string while supporting:
 * - Currency symbols: S/, $, €, USD, PEN
 * - Dot and comma decimals: 17.00, 17,00, 17
 * - Multiple space-separated or delimiter-separated prices
 */
export function extractPricesFromText(text: string): number[] {
  // Matches prices like 17.00, 30.00, 48.00 or S/ 17,00 or 17
  // We avoid matching single lone small digits that could be counts unless formatted as price
  const priceTokenRegex = /(?:(?:s\/?\.?|\$|€|usd|pen)\s*)?(\d{1,4}(?:[.,]\d{1,2})?)(?:\s*(?:s\/?\.?|\$|€|usd|pen))?/gi
  
  const prices: number[] = []
  let match: RegExpExecArray | null = null

  // Clean out obvious dot-leader separators and extra spaces
  const cleaned = text.replace(/[.]{2,}/g, ' ').replace(/[-]{2,}/g, ' ')
  
  // Specific regex to capture clean numbers that look like prices
  const regex = /\b(\d{1,4}(?:[.,]\d{2})?)\b/g
  while ((match = regex.exec(cleaned)) !== null) {
    const rawNum = match[1].replace(',', '.')
    const val = parseFloat(rawNum)
    // Filter out suspicious numbers (like year 2026, or 0)
    if (!isNaN(val) && val > 0 && val < 5000) {
      prices.push(Math.round(val * 100) / 100)
    }
  }

  return prices
}

/**
 * Detects if a line is a size column header (e.g. "Personal Mediana (para 2) Familiar (para + de 2)")
 */
export function parseSizeHeaders(line: string): string[] | null {
  const lower = line.toLowerCase().trim()
  const hasPersonal = lower.includes('personal') || lower.includes('chica') || lower.includes('individual') || lower.includes('small')
  const hasMediana = lower.includes('mediana') || lower.includes('medium')
  const hasFamiliar = lower.includes('familiar') || lower.includes('grande') || lower.includes('large')

  if ((hasPersonal && hasMediana) || (hasMediana && hasFamiliar) || (hasPersonal && hasFamiliar)) {
    // Check for standard 3-tier patterns using word indices
    const pIdx = line.search(/personal|chica|individual|small/i)
    const mIdx = line.search(/mediana|medium/i)
    const fIdx = line.search(/familiar|grande|large/i)

    if (pIdx !== -1 && mIdx !== -1 && fIdx !== -1 && pIdx < mIdx && mIdx < fIdx) {
      return [
        line.slice(pIdx, mIdx).trim(),
        line.slice(mIdx, fIdx).trim(),
        line.slice(fIdx).trim()
      ]
    }

    if (pIdx !== -1 && mIdx !== -1 && pIdx < mIdx) {
      return [line.slice(pIdx, mIdx).trim(), line.slice(mIdx).trim()]
    }

    if (mIdx !== -1 && fIdx !== -1 && mIdx < fIdx) {
      return [line.slice(mIdx, fIdx).trim(), line.slice(fIdx).trim()]
    }

    // Generic split on multiple spaces or tabs
    const parts = line.split(/\s{2,}|\t|\s*\|\s*/).map(p => p.trim()).filter(Boolean)
    if (parts.length >= 2) {
      return parts
    }

    return DEFAULT_THREE_SIZES
  }

  return null
}

/**
 * Detects if a line is a section/category header
 */
export function isSectionHeader(line: string): boolean {
  const trimmed = line.trim()
  if (!trimmed || trimmed.length < 3 || trimmed.length > 60) return false

  // Does it contain prices? A category header never has prices
  const prices = extractPricesFromText(trimmed)
  if (prices.length > 0) return false

  // Strip punctuation and check uppercase or section keywords
  const upper = trimmed.toUpperCase()
  const cleanUpper = upper.replace(/[=*\-_#:~]/g, '').trim()

  for (const kw of SECTION_KEYWORDS) {
    if (cleanUpper === kw || cleanUpper.startsWith(kw + ' ') || cleanUpper.includes(kw)) {
      return true
    }
  }

  // Header format like "=== CATEGORY ===" or all caps without comma or lowercase
  if (/^[A-ZÁÉÍÓÚÑ\s()]+$/.test(trimmed) && trimmed.length > 5 && !trimmed.includes(',')) {
    return true
  }

  return false
}

/**
 * Checks if text is predominantly an ingredient list
 */
export function isIngredientDescription(line: string): boolean {
  const trimmed = line.trim()
  if (!trimmed) return false

  // Ingredients usually have commas or "y", or lowercase words
  const lower = trimmed.toLowerCase()
  const tokens = lower.split(/[\s,.;]+/).filter(Boolean)

  let ingredientMatches = 0
  for (const token of tokens) {
    if (INGREDIENT_KEYWORDS.has(token)) {
      ingredientMatches++
    }
  }

  // If line contains multiple recognized ingredients or has commas and ingredients
  if (ingredientMatches >= 2) return true
  if (ingredientMatches >= 1 && (lower.includes(',') || lower.includes(' y '))) return true

  return false
}

/**
 * Main parser: Converts raw OCR text or line arrays into validated, structured products.
 */
export function parseRawMenuText(
  rawText: string,
  existingItems: { name: string }[] = []
): MenuParseResult {
  if (!rawText || !rawText.trim()) {
    return { items: [], products: [], categories: [], warnings: [], rawLinesCount: 0 }
  }

  const rawLines = rawText
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 0)

  const products: ParsedMenuItem[] = []
  const foundCategories = new Set<string>()
  const generalWarnings: string[] = []

  let currentCategory = 'General'
  let currentSizes: string[] = DEFAULT_THREE_SIZES

  let i = 0
  while (i < rawLines.length) {
    const line = rawLines[i]

    // 1. Check for Size Column Headers
    const sizeHeader = parseSizeHeaders(line)
    if (sizeHeader) {
      currentSizes = sizeHeader
      i++
      continue
    }

    // 2. Check for Section/Category Headers
    if (isSectionHeader(line)) {
      // Clean leading/trailing decorators like "=== " or "***" but PRESERVE parentheses and accents
      let cleanedCategory = line
        .replace(/^[=*\-_#:~]+\s*/, '')
        .replace(/\s*[=*\-_#:~]+$/, '')
        .trim()

      if (cleanedCategory) {
        currentCategory = cleanedCategory
        foundCategories.add(currentCategory)
      }
      i++
      continue
    }

    // 3. Process Product Candidate
    // Check if line contains prices
    const linePrices = extractPricesFromText(line)

    if (linePrices.length > 0) {
      // We found a product line with prices!
      // Extract the product name portion:
      // If line contains dot leaders (....) or dashes (----), the dish name is strictly before them
      let namePortion = ''
      const dotLeaderMatch = line.match(/[.]{2,}|[-]{2,}|[_]{2,}/)
      if (dotLeaderMatch && dotLeaderMatch.index !== undefined && dotLeaderMatch.index >= 2) {
        namePortion = line.slice(0, dotLeaderMatch.index).trim()
      } else {
        // Otherwise, strip the trailing prices block from the line
        const sanitizedLine = line
          .replace(/[.]{2,}/g, ' ')
          .replace(/[-]{2,}/g, ' ')
          .replace(/[_]{2,}/g, ' ')
          .trim()
        namePortion = sanitizedLine.replace(/(?:\s*(?:S\/?\.?|\$|€)?\s*\d{1,4}(?:[.,]\d{1,2})?\s*)+$/i, '').trim()
      }

      // Remove any trailing separators or noise from the name while keeping parentheses & accents
      namePortion = namePortion
        .replace(/[.,:;\\/|\s]+$/, '')
        .replace(/^[.,:;\\/|\s]+/, '')
        .replace(/\s{2,}/g, ' ')
        .trim()

      // Lookahead: Check next line for ingredients/description
      let description = ''
      let nextLineIndex = i + 1

      if (nextLineIndex < rawLines.length) {
        const nextLine = rawLines[nextLineIndex].trim()
        const nextLinePrices = extractPricesFromText(nextLine)

        // If next line has no prices and looks like ingredients or description
        if (nextLinePrices.length === 0 && !isSectionHeader(nextLine) && !parseSizeHeaders(nextLine)) {
          description = nextLine
          i++ // consume description line
        }
      }

      // Edge case: Sometimes the name was on the previous line and the current line only had prices
      if ((!namePortion || namePortion.length < 2) && i > 0) {
        const prevLine = rawLines[i - 1].trim()
        if (!isSectionHeader(prevLine) && !parseSizeHeaders(prevLine) && extractPricesFromText(prevLine).length === 0) {
          namePortion = prevLine
        }
      }

      // Validate name and prices
      const warnings: string[] = []
      let reviewRequired = false

      if (!namePortion || namePortion.length < 2) {
        warnings.push('Nombre de producto no detectado o ilegible')
        reviewRequired = true
      }

      if (linePrices.length === 0 || linePrices.some(p => p <= 0)) {
        warnings.push('Precio inválido o faltante')
        reviewRequired = true
      }

      if (!description) {
        // Not necessarily invalid, but notable
      }

      // Check if name is indistinguishable from description (e.g. ingredient list misdetected as name)
      if (isIngredientDescription(namePortion)) {
        warnings.push('El nombre parece ser una lista de ingredientes; verifique el plato')
        reviewRequired = true
      }

      // Map prices to size labels
      const priceVariants: ExtractedPriceVariant[] = []
      const activeSizes = linePrices.length === 3 
        ? currentSizes 
        : linePrices.length === 2 
        ? DEFAULT_TWO_SIZES 
        : ['Precio Único']

      linePrices.forEach((val, idx) => {
        const label = activeSizes[idx] || `Tamaño ${idx + 1}`
        priceVariants.push({ label, price: val })
      })

      const basePrice = linePrices[0] || 0
      const variants = priceVariants.map((pv, vIdx) => ({
        id: `var_${Date.now()}_${products.length}_${vIdx}`,
        name: pv.label,
        price: pv.price,
        cost: Math.round(pv.price * 0.3 * 100) / 100,
        in_stock: 25
      }))

      // Check existing duplicate in catalog
      const isDuplicate = existingItems.some(
        (ex) => ex.name.toLowerCase().trim() === (namePortion || '').toLowerCase().trim()
      )

      if (isDuplicate) {
        warnings.push('Producto ya existente en el catálogo (marcado como ACTUALIZACIÓN)')
      }

      // Confidence scoring
      const nameConf = namePortion && namePortion.length >= 3 ? 0.96 : 0.4
      const descConf = description ? 0.95 : 0.7
      const catConf = currentCategory !== 'General' ? 0.98 : 0.8
      const priceConf = linePrices.length > 0 ? 0.98 : 0.3
      const overallConf = Math.round(((nameConf + descConf + catConf + priceConf) / 4) * 100) / 100

      const parsedProduct: ParsedMenuItem = {
        id: `scan_item_${Date.now()}_${products.length + 1}`,
        name: namePortion || 'Plato sin nombre',
        category: currentCategory,
        categoryName: currentCategory,
        price: basePrice,
        description: description,
        prices: priceVariants,
        variants,
        confidence: {
          name: nameConf,
          description: descConf,
          category: catConf,
          prices: priceConf,
          overall: overallConf
        },
        warnings,
        reviewRequired,
        status: isDuplicate ? 'UPDATE' : (!namePortion || linePrices.length === 0) ? 'REJECTED' : 'NEW',
        rejection_reason: warnings.length > 0 ? warnings.join(', ') : undefined
      }

      products.push(parsedProduct)
      i++
      continue
    }

    // 4. Line had no prices: could be a stand-alone product name followed by ingredients then prices
    // Check if next lines contain description and prices
    if (!isSectionHeader(line) && !parseSizeHeaders(line) && line.length >= 3) {
      // Lookahead up to 2 lines
      if (i + 1 < rawLines.length) {
        const nextLine = rawLines[i + 1].trim()
        const nextPrices = extractPricesFromText(nextLine)

        if (nextPrices.length > 0) {
          // Current line was the dish name, next line has prices
          // Handled in next iteration when i advances or handle here
        }
      }
    }

    i++
  }

  return {
    items: products,
    products,
    categories: Array.from(foundCategories),
    warnings: generalWarnings,
    rawLinesCount: rawLines.length
  }
}
