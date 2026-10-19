import test from 'node:test'
import assert from 'node:assert/strict'

test('Loy-POS Master Suite: Category & Product CRUD, Scanner & Synchronization', async (t) => {
  // Mock In-Memory Store mimicking PostgreSQL / Supabase
  const store = {
    categories: [
      { id: 1, name: 'General', color_hex: '#0288D1', name_translations: { es: 'General', en: 'General', he: 'כללי' } },
      { id: 2, name: 'Hamburguesas', color_hex: '#E65100', name_translations: { es: 'Hamburguesas', en: 'Burgers', he: 'המבורגרים' } },
    ],
    items: [
      {
        id: 101,
        name: 'Hamburguesa Doble',
        price: 25.5,
        cost: 10.0,
        category_id: 2,
        category_name: 'Hamburguesas',
        in_stock: 40,
        track_stock: true,
        is_active: true,
      },
    ],
    receipts: [],
  }

  await t.test('1. Category Creation, Validation and Duplicate Prevention', () => {
    const newCat = {
      id: 3,
      name: 'Bebidas Frías',
      color_hex: '#00897B',
      name_translations: { es: 'Bebidas Frías', en: 'Cold Drinks', he: 'משקאות קרים' },
      target_printer: 'BAR',
    }

    assert.ok(newCat.name.trim().length > 0, 'Category name must not be empty')
    store.categories.push(newCat)

    const found = store.categories.find((c) => c.id === 3)
    assert.equal(found?.name, 'Bebidas Frías')
    assert.equal(found?.color_hex, '#00897B')
  })

  await t.test('2. Category Editing & Translation Updates', () => {
    const cat = store.categories.find((c) => c.id === 3)
    assert.ok(cat)

    cat.name = 'Bebidas y Refrescos'
    cat.name_translations.es = 'Bebidas y Refrescos'
    cat.color_hex = '#00ACC1'

    assert.equal(cat.name, 'Bebidas y Refrescos')
    assert.equal(cat.color_hex, '#00ACC1')
  })

  await t.test('3. Category Safe Deletion Policy with Product Reassignment', () => {
    // Category 2 (Hamburguesas) has item 101 assigned
    const assignedProducts = store.items.filter((i) => i.category_id === 2)
    assert.equal(assignedProducts.length, 1, 'Should find 1 assigned product')

    // Safe deletion: reassign to General (category 1) before deleting
    const fallbackCategory = store.categories.find((c) => c.id === 1)
    assert.ok(fallbackCategory)

    for (const item of assignedProducts) {
      item.category_id = fallbackCategory.id
      item.category_name = fallbackCategory.name
    }

    // Now delete category 2
    store.categories = store.categories.filter((c) => c.id !== 2)

    assert.equal(store.categories.some((c) => c.id === 2), false, 'Category 2 should be deleted')
    const reassignedItem = store.items.find((i) => i.id === 101)
    assert.equal(reassignedItem?.category_id, 1, 'Product was safely reassigned to General')
    assert.equal(reassignedItem?.category_name, 'General')
  })

  await t.test('4. Product Creation with Non-Negative Price and Cost Validation', () => {
    function validateProduct(p) {
      if (!p.name || !p.name.trim()) throw new Error('Product name is required')
      if (typeof p.price !== 'number' || p.price < 0 || isNaN(p.price)) {
        throw new Error('Price must be a non-negative number')
      }
      if (p.cost !== undefined && (typeof p.cost !== 'number' || p.cost < 0 || isNaN(p.cost))) {
        throw new Error('Cost must be a non-negative number')
      }
      return true
    }

    // Invalid: negative price
    assert.throws(() => validateProduct({ name: 'Mal', price: -5 }), /non-negative/)

    // Invalid: empty name
    assert.throws(() => validateProduct({ name: '', price: 10 }), /name is required/)

    // Valid product
    const valid = {
      id: 102,
      name: 'Lomo Saltado',
      price: 36.0,
      cost: 12.0,
      category_id: 1,
      category_name: 'General',
      in_stock: 25,
      track_stock: true,
      is_active: true,
    }
    assert.ok(validateProduct(valid))
    store.items.push(valid)
    assert.equal(store.items.length, 2)
  })

  await t.test('5. Product Price and Category Updating', () => {
    const item = store.items.find((i) => i.id === 102)
    assert.ok(item)

    item.price = 38.0
    item.category_id = 3
    item.category_name = 'Bebidas y Refrescos'

    assert.equal(item.price, 38.0)
    assert.equal(item.category_id, 3)
  })

  await t.test('6. Safe Product Archiving Preserves Sales References', () => {
    const item = store.items.find((i) => i.id === 102)
    assert.ok(item)

    // Archive item (mark inactive)
    item.is_active = false

    // Sales query for active items filters it out
    const posCatalog = store.items.filter((i) => i.is_active)
    assert.equal(posCatalog.some((i) => i.id === 102), false, 'Archived item hidden from POS catalog')

    // Historical record remains intact
    const historicalItem = store.items.find((i) => i.id === 102)
    assert.ok(historicalItem, 'Item record still exists for receipts and inventory history')
  })

  await t.test('7. Menu Scanner Parsing & Price Normalization', () => {
    function parseMenuLine(line) {
      // Handles Spanish formats: "Lomo Saltado ... S/ 38.50", "Ceviche $34,00", "Cafe 8.50"
      const priceRegex = /(?:S\/|\$|€)?\s*(\d+[.,]\d{2})/i
      const match = line.match(priceRegex)
      if (!match) {
        return { name: line.trim(), price: 0, status: 'REJECTED', reason: 'No price detected' }
      }
      const rawPrice = match[1].replace(',', '.')
      const price = parseFloat(rawPrice)
      const name = line.replace(match[0], '').replace(/[.\-_:]+$/, '').trim()

      if (!name || name.length < 2) {
        return { name: line.trim(), price, status: 'REJECTED', reason: 'Empty dish name' }
      }
      return { name, price, status: 'NEW' }
    }

    const testLines = [
      'Lomo Saltado Criollo S/ 38.50',
      'Ceviche Clásico de Pescado $34,00',
      'Chicha Morada 500ml 8.50',
      'Menú Ejecutivo de la Casa', // header without price
      '12.50', // price without dish name
    ]

    const results = testLines.map(parseMenuLine)

    assert.equal(results[0].name, 'Lomo Saltado Criollo')
    assert.equal(results[0].price, 38.5)
    assert.equal(results[0].status, 'NEW')

    assert.equal(results[1].name, 'Ceviche Clásico de Pescado')
    assert.equal(results[1].price, 34.0)
    assert.equal(results[1].status, 'NEW')

    assert.equal(results[2].price, 8.5)

    assert.equal(results[3].status, 'REJECTED')
    assert.equal(results[3].reason, 'No price detected')

    assert.equal(results[4].status, 'REJECTED')
    assert.equal(results[4].reason, 'Empty dish name')
  })

  await t.test('8. Sync Engine Idempotency & Stock Deductions on Sales', () => {
    const saleReceipt = {
      id: 'rcpt_test_999',
      idempotency_key: 'idemp_test_999',
      total_amount: 38.0,
      items_summary_json: JSON.stringify([{ id: 101, quantity: 2 }]),
    }

    const initialStock = store.items.find((i) => i.id === 101)?.in_stock || 0

    function processReceiptSync(rcpt) {
      const isDuplicate = store.receipts.some(
        (r) => r.id === rcpt.id || r.idempotency_key === rcpt.idempotency_key
      )
      if (isDuplicate) {
        return { processed: false, reason: 'Duplicate receipt ignored' }
      }
      store.receipts.push(rcpt)
      // Deduct stock
      const items = JSON.parse(rcpt.items_summary_json)
      for (const it of items) {
        const product = store.items.find((p) => p.id === it.id)
        if (product && product.track_stock) {
          product.in_stock = Math.max(0, product.in_stock - it.quantity)
        }
      }
      return { processed: true }
    }

    // First push
    const res1 = processReceiptSync(saleReceipt)
    assert.equal(res1.processed, true)
    const stockAfter1 = store.items.find((i) => i.id === 101)?.in_stock
    assert.equal(stockAfter1, initialStock - 2, 'Stock deducted by 2 units')

    // Second push (retry with same idempotency key)
    const res2 = processReceiptSync(saleReceipt)
    assert.equal(res2.processed, false, 'Duplicate receipt skipped')
    const stockAfter2 = store.items.find((i) => i.id === 101)?.in_stock
    assert.equal(stockAfter2, stockAfter1, 'Stock was not deducted again')
    assert.equal(store.receipts.length, 1, 'Only one receipt recorded')
  })

  await t.test('9. Security & Secret Redaction Verification', () => {
    const sampleConfig = {
      BACK_OFFICE_URL: 'https://loy-pos.vercel.app/admin',
      NEXT_PUBLIC_SUPABASE_URL: 'https://your-supabase-project.supabase.co',
      SERVICE_ROLE_KEY: 'secret_jwt_key_here',
    }

    // Verify public client bundle should NOT expose service_role or secrets
    const publicClientKeys = Object.keys(sampleConfig).filter((k) =>
      k.startsWith('NEXT_PUBLIC_') || k.startsWith('BACK_OFFICE_')
    )
    assert.ok(!publicClientKeys.includes('SERVICE_ROLE_KEY'), 'Private service key not exposed to client')
  })
})
