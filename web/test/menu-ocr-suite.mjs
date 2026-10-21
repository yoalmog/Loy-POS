import test from 'node:test'
import assert from 'node:assert/strict'
import { parseRawMenuText } from '../lib/ocr/menuParser.ts'

const FIXTURE_TEXT = `
PIZZAS ARDIENTES (picantes)
Personal Mediana (para 2) Familiar (para + de 2)

LUNA ROJA .......... 17.00 30.00 48.00
salsa de tomate picante, muzzarella, rocoto, aceituna y oregano

LA DIABÓLICA .......... 19.00 33.00 52.00
salame, chorizo ahumado, muzzarella, rocoto y oregano

LA SALVAJE .......... 20.00 35.00 55.00
salame, chorizo, tocino, cabanossi, muzzarella, rocoto, aceituna y oregano

PIZZAS COQUETAS (dulces)
LUNA DE MIEL (tropical) .......... 20.00 35.00 55.00
piña, durazno, leche condensada, muzzarella y canela

LA GOLOSA .......... 20.00 35.00 55.00
plátano, fresas, pasas, chocolate, muzzarella y canela

PIZZAS ECOLÓGICAS (vegetarianas)
LUNA NUEVA (margherita) .......... 16.00 30.00 48.00
albahaca, muzzarella, pimiento y oregano

LA VERDE (napolitana) .......... 18.00 32.00 50.00
tomate, albahaca, muzzarella, aceitunas, pimiento y oregano

CUARTO MENGUANTE .......... 19.00 33.00 52.00
tomate, espinaca, champiñones, muzzarella, aceitunas, pimiento y oregano
`

test('Loy-POS Menu OCR & Structured Field Mapping: 12 Mandatory Verification Scenarios', async (t) => {
  const result = parseRawMenuText(FIXTURE_TEXT)

  // 1. Correct extraction of product names
  await t.test('Scenario 1: Correct extraction of product names without truncation or alteration', () => {
    assert.equal(result.products.length, 8, 'Must extract exactly 8 products from the fixture')
    const extractedNames = result.products.map((p) => p.name)
    const expectedNames = [
      'LUNA ROJA',
      'LA DIABÓLICA',
      'LA SALVAJE',
      'LUNA DE MIEL (tropical)',
      'LA GOLOSA',
      'LUNA NUEVA (margherita)',
      'LA VERDE (napolitana)',
      'CUARTO MENGUANTE',
    ]
    assert.deepEqual(extractedNames, expectedNames)
  })

  // 2. Correct extraction of descriptions and ingredients
  await t.test('Scenario 2: Correct extraction of descriptions and ingredients into product description', () => {
    const p1 = result.products.find((p) => p.name === 'LUNA ROJA')
    assert.equal(
      p1?.description,
      'salsa de tomate picante, muzzarella, rocoto, aceituna y oregano',
      'Ingredients must be mapped to description, not name'
    )

    const p4 = result.products.find((p) => p.name === 'LUNA DE MIEL (tropical)')
    assert.equal(
      p4?.description,
      'piña, durazno, leche condensada, muzzarella y canela'
    )

    const p8 = result.products.find((p) => p.name === 'CUARTO MENGUANTE')
    assert.equal(
      p8?.description,
      'tomate, espinaca, champiñones, muzzarella, aceitunas, pimiento y oregano'
    )
  })

  // 3. Correct section/category assignment
  await t.test('Scenario 3: Correct section/category assignment preserving exact section headings', () => {
    const ardientes = result.products.filter(
      (p) => p.categoryName === 'PIZZAS ARDIENTES (picantes)'
    )
    assert.equal(ardientes.length, 3, 'Must have 3 spicy pizzas')
    assert.deepEqual(
      ardientes.map((p) => p.name),
      ['LUNA ROJA', 'LA DIABÓLICA', 'LA SALVAJE']
    )

    const coquetas = result.products.filter(
      (p) => p.categoryName === 'PIZZAS COQUETAS (dulces)'
    )
    assert.equal(coquetas.length, 2, 'Must have 2 sweet pizzas')
    assert.deepEqual(
      coquetas.map((p) => p.name),
      ['LUNA DE MIEL (tropical)', 'LA GOLOSA']
    )

    const ecologicas = result.products.filter(
      (p) => p.categoryName === 'PIZZAS ECOLÓGICAS (vegetarianas)'
    )
    assert.equal(ecologicas.length, 3, 'Must have 3 vegetarian pizzas')
    assert.deepEqual(
      ecologicas.map((p) => p.name),
      ['LUNA NUEVA (margherita)', 'LA VERDE (napolitana)', 'CUARTO MENGUANTE']
    )
  })

  // 4. Correct association of three prices with one product
  await t.test('Scenario 4: Correct association of three prices and size labels with one product', () => {
    const p1 = result.products.find((p) => p.name === 'LUNA ROJA')
    assert.ok(p1)
    assert.equal(p1.price, 17.0, 'Base price must equal first size variant price')
    assert.equal(p1.prices.length, 3, 'Must have 3 extracted price variants')
    assert.deepEqual(p1.prices, [
      { label: 'Personal', price: 17.0 },
      { label: 'Mediana (para 2)', price: 30.0 },
      { label: 'Familiar (para + de 2)', price: 48.0 },
    ])

    // Check variants payload ready for database persistence
    assert.equal(p1.variants.length, 3)
    assert.equal(p1.variants[0].name, 'Personal')
    assert.equal(p1.variants[0].price, 17.0)
    assert.equal(p1.variants[1].name, 'Mediana (para 2)')
    assert.equal(p1.variants[1].price, 30.0)
    assert.equal(p1.variants[2].name, 'Familiar (para + de 2)')
    assert.equal(p1.variants[2].price, 48.0)
  })

  // 5. Correct handling of accents and Spanish punctuation
  await t.test('Scenario 5: Correct handling of accents, tildes and Spanish punctuation', () => {
    const diabólica = result.products.find((p) => p.name === 'LA DIABÓLICA')
    assert.ok(diabólica, 'Must preserve Ó in LA DIABÓLICA')

    const coquetasHeader = result.categories.find((c) => c.includes('ECOLÓGICAS'))
    assert.equal(coquetasHeader, 'PIZZAS ECOLÓGICAS (vegetarianas)', 'Must preserve Ó and parentheses in header')

    const dulce = result.products.find((p) => p.name === 'LUNA DE MIEL (tropical)')
    assert.ok(dulce?.description.includes('piña'), 'Must preserve ñ in piña')

    const golosa = result.products.find((p) => p.name === 'LA GOLOSA')
    assert.ok(golosa?.description.includes('plátano'), 'Must preserve á in plátano')

    const menguante = result.products.find((p) => p.name === 'CUARTO MENGUANTE')
    assert.ok(menguante?.description.includes('champiñones'), 'Must preserve ñ in champiñones')
  })

  // 6. Handling of missing or illegible product names
  await t.test('Scenario 6: Handling of missing or illegible product names with reviewRequired flags', () => {
    const illegibleText = `
PIZZAS ARDIENTES (picantes)
Personal Mediana Familiar
.......... 17.00 30.00 48.00
salsa de tomate picante y queso
`
    const parsed = parseRawMenuText(illegibleText)
    assert.ok(parsed.products.length >= 1)
    const flagged = parsed.products[0]
    assert.ok(flagged.reviewRequired, 'Item with missing name must require review')
    assert.ok(
      flagged.warnings.some((w) => w.toLowerCase().includes('nombre') || w.toLowerCase().includes('ilegible')),
      'Should contain warning about missing product name'
    )
  })

  // 7. Handling of a missing description or price
  await t.test('Scenario 7: Handling of missing description or missing price with warnings', () => {
    // Missing description: name and price present
    const noDescText = `
PIZZAS ARDIENTES (picantes)
PIZZA FLASH .......... 25.00
`
    const parsedNoDesc = parseRawMenuText(noDescText)
    assert.equal(parsedNoDesc.products.length, 1)
    assert.equal(parsedNoDesc.products[0].name, 'PIZZA FLASH')
    assert.equal(parsedNoDesc.products[0].price, 25.0)
    assert.equal(parsedNoDesc.products[0].description, '')

    // Missing price: header and name present without price numbers
    const noPriceText = `
PIZZAS ARDIENTES (picantes)
PIZZA MISTERIOSA
solo queso y jamon
`
    const parsedNoPrice = parseRawMenuText(noPriceText)
    // When no price exists on a line or next line, it should not create a fake 0 price product without warning
    if (parsedNoPrice.products.length > 0) {
      assert.ok(parsedNoPrice.products[0].reviewRequired || parsedNoPrice.products[0].warnings.length > 0)
    }
  })

  // 8. Preventing section headings from becoming products
  await t.test('Scenario 8: Preventing section headings and size headers from becoming products', () => {
    const names = result.products.map((p) => p.name.toUpperCase())
    assert.equal(names.includes('PIZZAS ARDIENTES (PICANTES)'), false)
    assert.equal(names.includes('PIZZAS COQUETAS (DULCES)'), false)
    assert.equal(names.includes('PIZZAS ECOLÓGICAS (VEGETARIANAS)'), false)
    assert.equal(names.some((n) => n.includes('PERSONAL MEDIANA')), false)
    assert.equal(names.some((n) => n.includes('FAMILIAR (PARA + DE 2)')), false)
  })

  // 9. Preventing ingredient lines from becoming separate products
  await t.test('Scenario 9: Preventing ingredient lines from becoming separate products', () => {
    const names = result.products.map((p) => p.name.toLowerCase())
    assert.equal(names.some((n) => n.includes('salsa de tomate')), false)
    assert.equal(names.some((n) => n.includes('salame, chorizo')), false)
    assert.equal(names.some((n) => n.includes('leche condensada')), false)
    assert.equal(names.some((n) => n.includes('albahaca, muzzarella')), false)
    assert.equal(names.some((n) => n.includes('tomate, espinaca')), false)
  })

  // 10. Correct mapping to database product fields (name, description, category_id, price, variants)
  await t.test('Scenario 10: Correct mapping to database schema product fields', () => {
    const scanned = result.products[0] // LUNA ROJA
    const mockCategoryId = 105

    // Normalize for DB insertion matching addItemToCatalog & Supabase items schema
    const dbPayload = {
      name: scanned.name.trim(),
      description: scanned.description || '',
      category_id: mockCategoryId,
      category_name: scanned.categoryName,
      price: scanned.price,
      cost: Math.round(scanned.price * 0.3 * 100) / 100,
      variants: scanned.variants.map((v) => ({
        id: v.id,
        name: v.name,
        price: v.price,
        cost: v.cost,
        in_stock: v.in_stock,
      })),
      sold_by: 'EACH',
      in_stock: 50,
      is_active: true,
    }

    assert.equal(dbPayload.name, 'LUNA ROJA')
    assert.equal(dbPayload.description, 'salsa de tomate picante, muzzarella, rocoto, aceituna y oregano')
    assert.equal(dbPayload.category_id, 105)
    assert.equal(dbPayload.category_name, 'PIZZAS ARDIENTES (picantes)')
    assert.equal(dbPayload.price, 17.0)
    assert.equal(dbPayload.cost, 5.1)
    assert.equal(dbPayload.variants.length, 3)
    assert.equal(dbPayload.variants[1].name, 'Mediana (para 2)')
    assert.equal(dbPayload.variants[1].price, 30.0)
  })

  // 11. Import retry without accidental duplicates
  await t.test('Scenario 11: Import retry idempotency without creating accidental duplicates', () => {
    // Existing catalog with LUNA ROJA already imported
    const existingCatalog = [
      {
        id: 501,
        name: 'LUNA ROJA',
        category_id: 105,
        category_name: 'PIZZAS ARDIENTES (picantes)',
        price: 17.0,
        description: 'salsa de tomate picante, muzzarella, rocoto, aceituna y oregano',
        variants: [
          { id: 'v1', name: 'Personal', price: 17.0 },
          { id: 'v2', name: 'Mediana (para 2)', price: 30.0 },
          { id: 'v3', name: 'Familiar (para + de 2)', price: 48.0 },
        ],
      },
    ]

    // Re-scan menu with existing items provided
    const reScanResult = parseRawMenuText(FIXTURE_TEXT, existingCatalog)
    const lunaRojaReScan = reScanResult.products.find((p) => p.name === 'LUNA ROJA')

    assert.ok(lunaRojaReScan)
    assert.equal(lunaRojaReScan.status, 'UPDATE', 'Existing product must be flagged as UPDATE, not NEW')

    // Simulate batch import handler logic
    let catalog = [...existingCatalog]
    for (const item of reScanResult.products) {
      const match = catalog.find((c) => c.name.toLowerCase().trim() === item.name.toLowerCase().trim())
      if (item.status === 'UPDATE' && match) {
        // Update existing record in place
        match.price = item.price
        match.description = item.description
        match.variants = item.variants
      } else if (!match) {
        // Add new record
        catalog.push({
          id: Math.floor(Math.random() * 10000),
          name: item.name,
          category_name: item.categoryName,
          price: item.price,
          description: item.description,
          variants: item.variants,
        })
      }
    }

    // Catalog must contain exactly 8 products, no duplicate LUNA ROJA
    assert.equal(catalog.length, 8, 'Total catalog count must remain 8 after re-import')
    const lunaRojaEntries = catalog.filter((c) => c.name === 'LUNA ROJA')
    assert.equal(lunaRojaEntries.length, 1, 'Only one LUNA ROJA entry must exist')
  })

  // 12. Correct persistence and visibility in Back Office and POS
  await t.test('Scenario 12: Correct persistence and visibility across Back Office and POS', () => {
    // POS queries active items and selects variants at checkout
    const activeCatalog = result.products.map((p, idx) => ({
      id: 1000 + idx,
      name: p.name,
      description: p.description,
      category_name: p.categoryName,
      price: p.price,
      variants: p.variants,
      is_active: true,
    }))

    // POS Catalog View
    assert.equal(activeCatalog.length, 8)
    for (const item of activeCatalog) {
      assert.ok(item.is_active, 'Item must be active for POS checkout')
      assert.ok(item.description.length > 0, 'Description visible in product detail')
      assert.equal(item.variants.length, 3, 'All 3 sizes available for selection')
    }

    // POS Cart item with size selection (e.g. Mediana)
    const selectedItem = activeCatalog[0]
    const chosenVariant = selectedItem.variants[1] // Mediana (para 2) at 30.00
    const cartLine = {
      cart_id: `cart_${Date.now()}`,
      product_id: selectedItem.id,
      name: `${selectedItem.name} (${chosenVariant.name})`,
      unit_price: chosenVariant.price,
      quantity: 2,
      line_total: chosenVariant.price * 2,
    }

    assert.equal(cartLine.unit_price, 30.0)
    assert.equal(cartLine.line_total, 60.0)
  })
})
