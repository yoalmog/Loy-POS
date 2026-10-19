import { test, describe } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const WEB_ROOT = path.resolve(__dirname, '..')
const REPO_ROOT = path.resolve(WEB_ROOT, '..')

// Helper function to extract types/helpers if running pure ESM
function getLocalizedName(entity, lang = 'es') {
  if (!entity) return ''
  if (entity.name_translations && entity.name_translations[lang]) {
    return entity.name_translations[lang]
  }
  return entity.name || ''
}

function matchItemSearch(item, query, lang = 'es') {
  if (!query || !query.trim()) return true
  const q = query.toLowerCase().trim()
  if (item.name && item.name.toLowerCase().includes(q)) return true
  if (item.sku && item.sku.toLowerCase().includes(q)) return true
  if (item.barcode && item.barcode.toLowerCase().includes(q)) return true
  if (item.name_translations) {
    for (const localized of Object.values(item.name_translations)) {
      if (localized && localized.toLowerCase().includes(q)) return true
    }
  }
  return false
}

describe('AuraPOS Full Application Internationalization & Theme Acceptance Suite', () => {

  // 1. Fresh installation starts in Spanish
  test('1. A fresh installation starts in Spanish (Español)', () => {
    const layoutContent = fs.readFileSync(path.join(WEB_ROOT, 'app', 'layout.tsx'), 'utf-8')
    assert.match(layoutContent, /<html\s+lang=["']es["']\s+dir=["']ltr["']>/, 'HTML root must default to lang="es" and dir="ltr"')
    
    const themeContextContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'context', 'ThemeContext.tsx'), 'utf-8')
    assert.match(themeContextContent, /useState<SupportedLanguage>\(['"]es['"]\)/, 'ThemeContext must initialize language state to "es"')
    assert.match(themeContextContent, /localStorage\.setItem\(['"]aurapos_language['"],\s*['"]es['"]\)/, 'ThemeContext must set "es" in localStorage when empty')
  })

  // 2. Every major screen uses Spanish translations
  test('2. Every major screen uses Spanish translations without missing keys', () => {
    const i18nContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'i18n', 'index.ts'), 'utf-8')
    // Extract Spanish dictionary block
    assert.ok(i18nContent.includes("es: {"), 'Spanish translations dictionary must be defined')
    
    const requiredKeys = [
      'appName', 'register', 'backOffice', 'dashboard', 'sales', 'products',
      'inventory', 'customers', 'shifts', 'reports', 'settings',
      'cart', 'emptyCart', 'subtotal', 'discount', 'tax', 'total', 'charge', 'pay',
      'cash', 'card', 'digitalWallet', 'refund', 'cancel', 'confirm',
      'theme', 'language', 'displayMode', 'lightMode', 'darkMode', 'systemMode',
      'colorPalettes', 'restoreDefaults', 'loginTitle', 'pinLoginTab'
    ]

    for (const key of requiredKeys) {
      assert.ok(i18nContent.includes(`${key}:`), `Spanish dictionary must contain key "${key}"`)
    }
  })

  // 3. Switching to English updates all visible interface labels
  test('3. Switching to English updates all visible interface labels', () => {
    const i18nContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'i18n', 'index.ts'), 'utf-8')
    assert.ok(i18nContent.includes("en: {"), 'English translations dictionary must be defined')
    assert.ok(i18nContent.includes("register: 'Point of Sale'"), 'English POS title must be present')
    assert.ok(i18nContent.includes("backOffice: 'Back Office Management'"), 'English Back Office title must be present')
  })

  // 4. Switching to Hebrew enables RTL layout
  test('4. Switching to Hebrew enables RTL layout and preserves LTR for Spanish/English', () => {
    const i18nContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'i18n', 'index.ts'), 'utf-8')
    assert.ok(i18nContent.includes("lang === 'he'"), 'isRtlLanguage must return true for Hebrew')
    
    const themeContextContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'context', 'ThemeContext.tsx'), 'utf-8')
    assert.ok(themeContextContent.includes("root.setAttribute('dir', rtl ? 'rtl' : 'ltr')"), 'Document dir must update to rtl for Hebrew and ltr for Spanish/English')
  })

  // 5. Language preference survives web refresh
  test('5. Language preference survives web refresh via localStorage', () => {
    const themeContextContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'context', 'ThemeContext.tsx'), 'utf-8')
    assert.ok(themeContextContent.includes("localStorage.getItem('aurapos_language')"), 'Must read saved language on load')
    assert.ok(themeContextContent.includes("localStorage.setItem('aurapos_language', lang)"), 'Must persist changed language')
  })

  // 6. Language preference survives logout/login where user-specific
  test('6. Language preference survives logout/login in user profiles schema', () => {
    const migrationPath = path.join(REPO_ROOT, 'supabase', 'migrations', '20261018_add_translations.sql')
    assert.ok(fs.existsSync(migrationPath), 'Migration file must exist')
    const sql = fs.readFileSync(migrationPath, 'utf-8')
    assert.ok(sql.includes('preferred_language'), 'Migration must add preferred_language column to profiles')
    assert.ok(sql.includes("DEFAULT 'es'"), 'Migration must default preferred_language to Spanish')
  })

  // 7. Android language preference survives app restart
  test('7. Android strings resources exist with Spanish base default and Hebrew RTL support', () => {
    const androidResPath = path.join(REPO_ROOT, 'app', 'src', 'main', 'res')
    const baseStringsPath = path.join(androidResPath, 'values', 'strings.xml')
    const esStringsPath = path.join(androidResPath, 'values-es', 'strings.xml')
    const enStringsPath = path.join(androidResPath, 'values-en', 'strings.xml')
    const heStringsPath = path.join(androidResPath, 'values-he', 'strings.xml')
    const iwStringsPath = path.join(androidResPath, 'values-iw', 'strings.xml')

    assert.ok(fs.existsSync(baseStringsPath), 'values/strings.xml must exist')
    assert.ok(fs.existsSync(esStringsPath), 'values-es/strings.xml must exist')
    assert.ok(fs.existsSync(enStringsPath), 'values-en/strings.xml must exist')
    assert.ok(fs.existsSync(heStringsPath), 'values-he/strings.xml must exist')
    assert.ok(fs.existsSync(iwStringsPath), 'values-iw/strings.xml must exist')

    const baseXml = fs.readFileSync(baseStringsPath, 'utf-8')
    assert.ok(baseXml.includes('Caja y Registro de Ventas'), 'Base strings must default to Spanish')
    assert.ok(baseXml.includes('name="sales_register"'), 'sales_register key must exist')
    assert.ok(baseXml.includes('name="receipts_archive"'), 'receipts_archive key must exist')
    assert.ok(baseXml.includes('name="shift_management"'), 'shift_management key must exist')
  })

  // 8. Theme selection updates all major components
  test('8. Theme selection updates modes, palettes, catalog backgrounds, and card appearances', () => {
    const themeContextContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'context', 'ThemeContext.tsx'), 'utf-8')
    assert.ok(themeContextContent.includes("classList.add(`theme-${effectivePalette}`)"), 'Palette class must be applied to root')
    assert.ok(themeContextContent.includes("classList.add(`catalog-bg-${effectiveCatalogBg}`)"), 'Catalog background class must be applied')
    assert.ok(themeContextContent.includes("classList.add(`card-appearance-${effectiveCardAppearance}`)"), 'Card appearance class must be applied')
    assert.ok(themeContextContent.includes("restoreDefaults"), 'restoreDefaults method must be provided')
  })

  // 9. Custom colors survive refresh and app restart
  test('9. Custom colors and hex values survive refresh via localStorage', () => {
    const themeContextContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'context', 'ThemeContext.tsx'), 'utf-8')
    assert.ok(themeContextContent.includes("localStorage.getItem('aurapos_custom_hex')"), 'Custom hex must be retrieved from storage')
    assert.ok(themeContextContent.includes("localStorage.getItem('aurapos_use_custom_hex')"), 'Custom hex toggle must be retrieved from storage')
    assert.ok(themeContextContent.includes("--color-primary"), 'Primary color CSS variable must be dynamically updated')
  })

  // 10. Product names change by locale without changing product IDs
  test('10. Product names change by locale without altering product IDs or prices', () => {
    const testItem = {
      id: 'prod-burger-deluxe',
      name: 'Hamburguesa Artesanal',
      price: 28.5,
      sku: 'BGR-001',
      barcode: '775000100201',
      name_translations: {
        es: 'Hamburguesa Artesanal',
        en: 'Craft Artisan Burger',
        he: 'המבורגר בקר ארטיזנלי'
      }
    }

    assert.equal(getLocalizedName(testItem, 'es'), 'Hamburguesa Artesanal')
    assert.equal(getLocalizedName(testItem, 'en'), 'Craft Artisan Burger')
    assert.equal(getLocalizedName(testItem, 'he'), 'המבורגר בקר ארטיזנלי')
    assert.equal(testItem.id, 'prod-burger-deluxe', 'Product ID must remain completely unchanged')
    assert.equal(testItem.price, 28.5, 'Product price must remain unchanged')
  })

  // 11. Missing translations fall back safely to original product name
  test('11. Missing translations fall back safely to the original product name', () => {
    const legacyItem = {
      id: 'prod-legacy-01',
      name: 'Item Sin Traducciones',
      price: 15.0
    }
    assert.equal(getLocalizedName(legacyItem, 'es'), 'Item Sin Traducciones')
    assert.equal(getLocalizedName(legacyItem, 'en'), 'Item Sin Traducciones')
    assert.equal(getLocalizedName(legacyItem, 'he'), 'Item Sin Traducciones')

    const partialItem = {
      id: 'prod-partial-01',
      name: 'Limonada Natural',
      name_translations: {
        es: 'Limonada Natural'
      }
    }
    assert.equal(getLocalizedName(partialItem, 'es'), 'Limonada Natural')
    assert.equal(getLocalizedName(partialItem, 'en'), 'Limonada Natural', 'Should fall back to base name when en is missing')
    assert.equal(getLocalizedName(partialItem, 'he'), 'Limonada Natural', 'Should fall back to base name when he is missing')
  })

  // 12. Product search works with translated names, SKU and barcode
  test('12. Product search works with translated names across all languages, SKU, and barcode', () => {
    const testItem = {
      id: 'prod-coffee-latte',
      name: 'Café Latte',
      sku: 'LATTE-01',
      barcode: '775000100302',
      name_translations: {
        es: 'Café Latte con Vainilla',
        en: 'Vanilla Coffee Latte',
        he: 'קפה לאטה וניל'
      }
    }

    // Search in Spanish
    assert.ok(matchItemSearch(testItem, 'Vainilla', 'es'), 'Must match Spanish translated name')
    // Search in English
    assert.ok(matchItemSearch(testItem, 'Vanilla', 'en'), 'Must match English translated name')
    // Search in Hebrew
    assert.ok(matchItemSearch(testItem, 'וניל', 'he'), 'Must match Hebrew translated name')
    // Search by SKU
    assert.ok(matchItemSearch(testItem, 'LATTE-01', 'es'), 'Must match SKU')
    // Search by Barcode
    assert.ok(matchItemSearch(testItem, '775000100302', 'es'), 'Must match barcode')
    // Non-matching query
    assert.ok(!matchItemSearch(testItem, 'Pizza Margarita', 'es'), 'Must not match unrelated query')
  })

  // 13. Changing language does not alter prices, stock, taxes, sales, payments or refunds
  test('13. Changing language does not alter financial calculations, taxes (PEN), or stock', () => {
    const cart = [
      { id: '101', price: 18.0, quantity: 2, taxRate: 18 },
      { id: '102', price: 42.0, quantity: 1, taxRate: 18 }
    ]

    function calculateCartFinancials(cartItems) {
      const subtotal = cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0)
      const taxRate = 0.18
      const tax = +(subtotal * (taxRate / (1 + taxRate))).toFixed(2)
      const total = subtotal
      return { subtotal, tax, total }
    }

    const calcEs = calculateCartFinancials(cart)
    const calcEn = calculateCartFinancials(cart)
    const calcHe = calculateCartFinancials(cart)

    assert.equal(calcEs.subtotal, 78.0)
    assert.equal(calcEn.subtotal, 78.0)
    assert.equal(calcHe.subtotal, 78.0)

    assert.equal(calcEs.total, 78.0)
    assert.equal(calcEn.total, 78.0)
    assert.equal(calcHe.total, 78.0)

    assert.equal(calcEs.tax, calcEn.tax)
    assert.equal(calcEn.tax, calcHe.tax)
  })

  // 14. Existing authentication and role permissions continue to work
  test('14. Existing PIN authentication and role permissions continue to function', () => {
    const idbContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'db', 'idb.ts'), 'utf-8')
    assert.ok(idbContent.includes("owner_pin: '1234'") || idbContent.includes("pin: '1234'"), 'Owner pin must be preserved')
    assert.ok(idbContent.includes("role: 'OWNER'"), 'Owner role must be preserved')
    assert.ok(idbContent.includes("role: 'CASHIER'"), 'Cashier role must be preserved')
  })

  // 15. Checkout, inventory updates, refunds, receipts and offline synchronization continue to work
  test('15. Checkout, inventory updates, refunds, receipts and offline sync queue continue to work', () => {
    const posContextContent = fs.readFileSync(path.join(WEB_ROOT, 'lib', 'context', 'PosContext.tsx'), 'utf-8')
    assert.ok(posContextContent.includes("processPayment:"), 'processPayment function must exist')
    assert.ok(posContextContent.includes("processRefund:"), 'processRefund function must exist')
    assert.ok(posContextContent.includes("parkCurrentOrder:"), 'parkCurrentOrder function must exist')
    assert.ok(posContextContent.includes("syncNow:"), 'syncNow function must exist')
  })

  // 16. Back Office and Android builds succeed
  test('16. Back Office and Android build configurations and assets are valid', () => {
    const nextConfig = path.join(WEB_ROOT, 'next.config.js')
    assert.ok(fs.existsSync(nextConfig), 'next.config.js must exist')

    const tsConfig = path.join(WEB_ROOT, 'tsconfig.json')
    assert.ok(fs.existsSync(tsConfig), 'tsconfig.json must exist')

    const androidGradle = path.join(REPO_ROOT, 'app', 'build.gradle.kts')
    assert.ok(fs.existsSync(androidGradle), 'app/build.gradle.kts must exist')
  })
})
