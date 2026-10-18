import { Receipt, CartItem, StoreSettings } from '../types'

// ESC/POS Command Constants
export const ESC = 0x1B
export const GS = 0x1D
export const FS = 0x1C

export class EscPosBuilder {
  private buffer: number[] = []
  private width: 58 | 76 | 80

  constructor(paperWidth: 58 | 76 | 80 = 80) {
    this.width = paperWidth
    this.initialize()
  }

  public initialize(): this {
    this.buffer.push(ESC, 0x40) // ESC @: Initialize printer
    return this
  }

  public alignLeft(): this {
    this.buffer.push(ESC, 0x61, 0) // ESC a 0
    return this
  }

  public alignCenter(): this {
    this.buffer.push(ESC, 0x61, 1) // ESC a 1
    return this
  }

  public alignRight(): this {
    this.buffer.push(ESC, 0x61, 2) // ESC a 2
    return this
  }

  public setBold(enabled: boolean): this {
    this.buffer.push(ESC, 0x45, enabled ? 1 : 0) // ESC E n
    return this
  }

  public setDoubleSize(enabled: boolean): this {
    this.buffer.push(GS, 0x21, enabled ? 0x11 : 0x00) // GS ! n
    return this
  }

  public setUnderline(enabled: boolean): this {
    this.buffer.push(ESC, 0x2D, enabled ? 1 : 0)
    return this
  }

  public feed(lines: number = 1): this {
    for (let i = 0; i < lines; i++) {
      this.buffer.push(0x0A) // LF
    }
    return this
  }

  public text(str: string): this {
    // Encode string to UTF-8 or CP437/CP862 (Hebrew) bytes
    const encoder = new TextEncoder()
    const bytes = encoder.encode(str)
    for (let i = 0; i < bytes.length; i++) {
      this.buffer.push(bytes[i])
    }
    return this
  }

  public textLine(str: string = ''): this {
    this.text(str)
    this.buffer.push(0x0A)
    return this
  }

  public separator(char: string = '-'): this {
    const colCount = this.width === 58 ? 32 : this.width === 76 ? 42 : 48
    this.textLine(char.repeat(colCount))
    return this
  }

  public twoColumns(left: string, right: string): this {
    const colCount = this.width === 58 ? 32 : this.width === 76 ? 42 : 48
    const spaceCount = Math.max(1, colCount - (left.length + right.length))
    this.textLine(left + ' '.repeat(spaceCount) + right)
    return this
  }

  public cut(partial: boolean = false): this {
    this.feed(3)
    this.buffer.push(GS, 0x56, partial ? 1 : 0) // GS V 0 (full cut) or GS V 1 (partial cut)
    return this
  }

  public kickDrawer(): this {
    // Pulse to standard RJ11 cash drawer pin 2
    this.buffer.push(ESC, 0x70, 0x00, 0x19, 0xFA)
    return this
  }

  public getBytes(): Uint8Array {
    return new Uint8Array(this.buffer)
  }
}

// Format customer receipt for thermal printer
export function formatReceiptEscPos(
  receipt: Receipt,
  settings: StoreSettings,
  paperWidth: 58 | 76 | 80 = 80
): Uint8Array {
  const b = new EscPosBuilder(paperWidth)

  // Header
  b.alignCenter()
  b.setDoubleSize(true)
  b.setBold(true)
  b.textLine(settings.store_name)
  b.setDoubleSize(false)
  b.setBold(false)

  if (settings.address) b.textLine(settings.address)
  if (settings.phone) b.textLine(`טל: ${settings.phone}`)
  if (settings.receipt_header) b.textLine(settings.receipt_header)
  b.separator('=')

  // Metadata
  b.alignLeft()
  b.twoColumns(`קבלה #: ${receipt.receipt_number}`, receipt.dining_option)
  b.twoColumns(
    `תאריך: ${new Date(receipt.created_at).toLocaleDateString('he-IL')}`,
    new Date(receipt.created_at).toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit' })
  )
  b.twoColumns(`קופאי: ${receipt.cashier_name}`, receipt.ticket_name || '')
  if (receipt.customer_name) {
    b.twoColumns(`לקוח: ${receipt.customer_name}`, '')
  }
  b.separator('-')

  // Items
  b.alignLeft()
  b.setBold(true)
  b.twoColumns('פריט / כמות', 'סה״כ')
  b.setBold(false)
  b.separator('-')

  receipt.items.forEach((item) => {
    const qtyPrice = `${item.quantity}x ${settings.currency_symbol}${item.unit_price.toFixed(2)}`
    b.twoColumns(`${item.name} (${qtyPrice})`, `${settings.currency_symbol}${item.line_total.toFixed(2)}`)

    if (item.selected_variant) {
      b.textLine(`  • ${item.selected_variant.name}`)
    }
    if (item.selected_modifiers && item.selected_modifiers.length > 0) {
      item.selected_modifiers.forEach((m) => {
        b.textLine(`  + ${m.name} (${settings.currency_symbol}${m.price.toFixed(2)})`)
      })
    }
    if (item.comment) {
      b.textLine(`  * הערה: ${item.comment}`)
    }
  })

  b.separator('-')

  // Totals
  b.twoColumns('סכום ביניים:', `${settings.currency_symbol}${receipt.subtotal.toFixed(2)}`)
  if (receipt.discount_amount > 0) {
    b.twoColumns('הנחה:', `-${settings.currency_symbol}${receipt.discount_amount.toFixed(2)}`)
  }
  b.twoColumns(`מע״מ (${settings.tax_rate}%):`, `${settings.currency_symbol}${receipt.tax_amount.toFixed(2)}`)

  b.setBold(true)
  b.setDoubleSize(true)
  b.twoColumns('סה״כ לתשלום:', `${settings.currency_symbol}${receipt.total_amount.toFixed(2)}`)
  b.setDoubleSize(false)
  b.setBold(false)

  b.separator('-')

  // Payment method
  b.twoColumns(`אמצעי תשלום: ${receipt.payment_type}`, `${settings.currency_symbol}${receipt.total_amount.toFixed(2)}`)
  if (receipt.points_earned) {
    b.twoColumns(`נקודות מועדון שנצברו:`, `+${receipt.points_earned} נק'`)
  }

  // Footer & Return Policy
  b.feed(1)
  b.alignCenter()
  if (settings.receipt_footer) b.textLine(settings.receipt_footer)
  if (settings.return_policy) b.textLine(settings.return_policy)

  b.feed(1)
  b.textLine('--- קבלה דיגיטלית ואבטחת מסחר ---')

  // Kick drawer on cash payments and cut paper
  if (receipt.payment_type === 'CASH') {
    b.kickDrawer()
  }
  b.cut()

  return b.getBytes()
}

// Format Kitchen Ticket (Hot Food / Chef view)
export function formatKitchenTicketEscPos(
  ticketName: string,
  diningOption: string,
  serverName: string,
  items: CartItem[],
  paperWidth: 58 | 76 | 80 = 80
): Uint8Array {
  const b = new EscPosBuilder(paperWidth)

  b.alignCenter()
  b.setDoubleSize(true)
  b.setBold(true)
  b.textLine('*** הזמנת מטבח - KITCHEN ***')
  b.setDoubleSize(false)
  b.feed(1)

  b.alignLeft()
  b.twoColumns(`שולחן/כרטיס: ${ticketName}`, diningOption)
  b.twoColumns(
    `זמן: ${new Date().toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit' })}`,
    `מלצר: ${serverName}`
  )
  b.separator('=')

  b.setDoubleSize(true)
  items.forEach((item) => {
    b.setBold(true)
    b.textLine(`[ ] ${item.quantity}x ${item.name}`)
    b.setBold(false)

    if (item.selected_variant) {
      b.textLine(`    גודל: ${item.selected_variant.name}`)
    }
    if (item.selected_modifiers && item.selected_modifiers.length > 0) {
      item.selected_modifiers.forEach((m) => {
        b.textLine(`    + ${m.name}`)
      })
    }
    if (item.comment) {
      b.setBold(true)
      b.textLine(`    ** הערה: ${item.comment} **`)
      b.setBold(false)
    }
    b.feed(1)
  })
  b.setDoubleSize(false)

  b.separator('=')
  b.cut()
  return b.getBytes()
}

// Format Bar Ticket (Drinks / Bartender view)
export function formatBarTicketEscPos(
  ticketName: string,
  diningOption: string,
  serverName: string,
  items: CartItem[],
  paperWidth: 58 | 76 | 80 = 80
): Uint8Array {
  const b = new EscPosBuilder(paperWidth)

  b.alignCenter()
  b.setDoubleSize(true)
  b.setBold(true)
  b.textLine('*** הזמנת בר - BAR ORDER ***')
  b.setDoubleSize(false)
  b.feed(1)

  b.alignLeft()
  b.twoColumns(`שולחן/כרטיס: ${ticketName}`, diningOption)
  b.twoColumns(
    `זמן: ${new Date().toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit' })}`,
    `ברמן/מלצר: ${serverName}`
  )
  b.separator('=')

  b.setDoubleSize(true)
  items.forEach((item) => {
    b.setBold(true)
    b.textLine(`[ ] ${item.quantity}x ${item.name}`)
    b.setBold(false)

    if (item.selected_variant) {
      b.textLine(`    ${item.selected_variant.name}`)
    }
    if (item.comment) {
      b.setBold(true)
      b.textLine(`    ** ${item.comment} **`)
      b.setBold(false)
    }
    b.feed(1)
  })
  b.setDoubleSize(false)

  b.separator('=')
  b.cut()
  return b.getBytes()
}
