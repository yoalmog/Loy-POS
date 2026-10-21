import test from 'node:test'
import assert from 'node:assert/strict'
import { parseRawMenuText } from '../lib/ocr/menuParser.ts'

test('Menu OCR & Structured Field Mapping Acceptance Fixture', async (t) => {
  const fixtureText = `
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

  const result = parseRawMenuText(fixtureText)
  console.log('PARSED PRODUCTS:', JSON.stringify(result.products.map(p => ({ name: p.name, desc: p.description, prices: p.prices, cat: p.categoryName })), null, 2))

  await t.test('1. Extracts exactly eight products', () => {
    assert.equal(result.products.length, 8, `Expected 8 products, got ${result.products.length}`)
  })

  await t.test('2. Extracts all three categories preserving Spanish accents and parenthetical labels', () => {
    assert.deepEqual(result.categories, [
      'PIZZAS ARDIENTES (picantes)',
      'PIZZAS COQUETAS (dulces)',
      'PIZZAS ECOLÓGICAS (vegetarianas)'
    ])
  })

  await t.test('3. Correct extraction for Category: PIZZAS ARDIENTES (picantes)', () => {
    const p1 = result.products.find(p => p.name === 'LUNA ROJA')
    assert.ok(p1, 'LUNA ROJA must exist')
    assert.equal(p1.categoryName, 'PIZZAS ARDIENTES (picantes)')
    assert.equal(p1.description, 'salsa de tomate picante, muzzarella, rocoto, aceituna y oregano')
    assert.deepEqual(p1.prices.map(pr => pr.price), [17.00, 30.00, 48.00])
    assert.equal(p1.prices[0].label, 'Personal')
    assert.equal(p1.prices[1].label, 'Mediana (para 2)')
    assert.equal(p1.prices[2].label, 'Familiar (para + de 2)')

    const p2 = result.products.find(p => p.name === 'LA DIABÓLICA')
    assert.ok(p2, 'LA DIABÓLICA must exist')
    assert.equal(p2.categoryName, 'PIZZAS ARDIENTES (picantes)')
    assert.equal(p2.description, 'salame, chorizo ahumado, muzzarella, rocoto y oregano')
    assert.deepEqual(p2.prices.map(pr => pr.price), [19.00, 33.00, 52.00])

    const p3 = result.products.find(p => p.name === 'LA SALVAJE')
    assert.ok(p3, 'LA SALVAJE must exist')
    assert.equal(p3.categoryName, 'PIZZAS ARDIENTES (picantes)')
    assert.equal(p3.description, 'salame, chorizo, tocino, cabanossi, muzzarella, rocoto, aceituna y oregano')
    assert.deepEqual(p3.prices.map(pr => pr.price), [20.00, 35.00, 55.00])
  })

  await t.test('4. Correct extraction for Category: PIZZAS COQUETAS (dulces)', () => {
    const p4 = result.products.find(p => p.name === 'LUNA DE MIEL (tropical)')
    assert.ok(p4, 'LUNA DE MIEL (tropical) must exist')
    assert.equal(p4.categoryName, 'PIZZAS COQUETAS (dulces)')
    assert.equal(p4.description, 'piña, durazno, leche condensada, muzzarella y canela')
    assert.deepEqual(p4.prices.map(pr => pr.price), [20.00, 35.00, 55.00])

    const p5 = result.products.find(p => p.name === 'LA GOLOSA')
    assert.ok(p5, 'LA GOLOSA must exist')
    assert.equal(p5.categoryName, 'PIZZAS COQUETAS (dulces)')
    assert.equal(p5.description, 'plátano, fresas, pasas, chocolate, muzzarella y canela')
    assert.deepEqual(p5.prices.map(pr => pr.price), [20.00, 35.00, 55.00])
  })

  await t.test('5. Correct extraction for Category: PIZZAS ECOLÓGICAS (vegetarianas)', () => {
    const p6 = result.products.find(p => p.name === 'LUNA NUEVA (margherita)')
    assert.ok(p6, 'LUNA NUEVA (margherita) must exist')
    assert.equal(p6.categoryName, 'PIZZAS ECOLÓGICAS (vegetarianas)')
    assert.equal(p6.description, 'albahaca, muzzarella, pimiento y oregano')
    assert.deepEqual(p6.prices.map(pr => pr.price), [16.00, 30.00, 48.00])

    const p7 = result.products.find(p => p.name === 'LA VERDE (napolitana)')
    assert.ok(p7, 'LA VERDE (napolitana) must exist')
    assert.equal(p7.categoryName, 'PIZZAS ECOLÓGICAS (vegetarianas)')
    assert.equal(p7.description, 'tomate, albahaca, muzzarella, aceitunas, pimiento y oregano')
    assert.deepEqual(p7.prices.map(pr => pr.price), [18.00, 32.00, 50.00])

    const p8 = result.products.find(p => p.name === 'CUARTO MENGUANTE')
    assert.ok(p8, 'CUARTO MENGUANTE must exist')
    assert.equal(p8.categoryName, 'PIZZAS ECOLÓGICAS (vegetarianas)')
    assert.equal(p8.description, 'tomate, espinaca, champiñones, muzzarella, aceitunas, pimiento y oregano')
    assert.deepEqual(p8.prices.map(pr => pr.price), [19.00, 33.00, 52.00])
  })

  await t.test('6. Prevents section headings or size headers from becoming products', () => {
    assert.equal(result.products.some(p => p.name.includes('PIZZAS ARDIENTES')), false)
    assert.equal(result.products.some(p => p.name.includes('PIZZAS COQUETAS')), false)
    assert.equal(result.products.some(p => p.name.includes('PIZZAS ECOLÓGICAS')), false)
    assert.equal(result.products.some(p => p.name.includes('Personal Mediana')), false)
  })

  await t.test('7. Prevents ingredient lines from becoming separate products', () => {
    assert.equal(result.products.some(p => p.name.includes('salsa de tomate picante')), false)
    assert.equal(result.products.some(p => p.name.includes('salame, chorizo')), false)
    assert.equal(result.products.some(p => p.name.includes('piña, durazno')), false)
  })

  await t.test('8. Handles missing prices or missing product names with review flags', () => {
    const edgeCaseText = `
PIZZAS CLASICAS
Margarita Simple
salsa de tomate y queso

15.00 25.00
`
    const edgeResult = parseRawMenuText(edgeCaseText)
    assert.ok(edgeResult.products.length >= 1)
  })
})
