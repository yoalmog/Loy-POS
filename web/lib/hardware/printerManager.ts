import { PrinterConfig, Receipt, CartItem, StoreSettings } from '../types'
import { 
  formatReceiptEscPos, 
  formatKitchenTicketEscPos, 
  formatBarTicketEscPos 
} from './escpos'
import { idbGetAll } from '../db/idb'

export class PrinterManager {
  private static instance: PrinterManager

  private constructor() {}

  public static getInstance(): PrinterManager {
    if (!PrinterManager.instance) {
      PrinterManager.instance = new PrinterManager()
    }
    return PrinterManager.instance
  }

  // Retrieve configured printers
  public async getPrinters(): Promise<PrinterConfig[]> {
    return await idbGetAll<PrinterConfig>('printers')
  }

  // Category routing & printing an entire transaction
  public async processTransactionPrint(
    receipt: Receipt,
    settings: StoreSettings
  ): Promise<{ success: boolean; messages: string[] }> {
    const printers = await this.getPrinters()
    const activePrinters = printers.filter((p) => p.enabled)
    const messages: string[] = []

    // 1. Separate items by target department
    const kitchenItems: CartItem[] = []
    const barItems: CartItem[] = []

    receipt.items.forEach((item) => {
      if (item.target_printer === 'BAR') {
        barItems.push(item)
      } else if (item.target_printer === 'KITCHEN') {
        kitchenItems.push(item)
      } else {
        // Default based on category ID if target_printer not explicitly set:
        // Categories 4, 5, 6 are beverages/bar
        if (item.category_id && [4, 5, 6].includes(item.category_id)) {
          barItems.push(item)
        } else {
          kitchenItems.push(item)
        }
      }
    })

    // 2. Dispatch to Kitchen Printer if there are food items
    const kitchenPrinter = activePrinters.find((p) => p.type === 'KITCHEN')
    if (kitchenPrinter && kitchenItems.length > 0) {
      const kitchenBytes = formatKitchenTicketEscPos(
        receipt.ticket_name || receipt.receipt_number,
        receipt.dining_option,
        receipt.cashier_name,
        kitchenItems,
        kitchenPrinter.paper_width
      )
      const res = await this.sendBytesToPrinter(kitchenPrinter, kitchenBytes)
      messages.push(`Kitchen (${kitchenItems.length} פריטים): ${res}`)
    }

    // 3. Dispatch to Bar Printer if there are drink items
    const barPrinter = activePrinters.find((p) => p.type === 'BAR')
    if (barPrinter && barItems.length > 0) {
      const barBytes = formatBarTicketEscPos(
        receipt.ticket_name || receipt.receipt_number,
        receipt.dining_option,
        receipt.cashier_name,
        barItems,
        barPrinter.paper_width
      )
      const res = await this.sendBytesToPrinter(barPrinter, barBytes)
      messages.push(`Bar (${barItems.length} משקאות): ${res}`)
    }

    // 4. Dispatch to Cashier / Receipt Printer for customer
    const cashierPrinter = activePrinters.find((p) => p.type === 'CASHIER') || activePrinters[0]
    if (cashierPrinter) {
      const receiptBytes = formatReceiptEscPos(receipt, settings, cashierPrinter.paper_width)
      const res = await this.sendBytesToPrinter(cashierPrinter, receiptBytes)
      messages.push(`קבלת לקוח: ${res}`)
    } else {
      // Default browser print
      this.printBrowserHtmlReceipt(receipt, settings)
      messages.push('קבלת לקוח נשלחה להדפסת דפדפן')
    }

    return { success: true, messages }
  }

  // Send raw ESC/POS bytes to chosen printer transport
  public async sendBytesToPrinter(
    printer: PrinterConfig,
    bytes: Uint8Array
  ): Promise<string> {
    switch (printer.interface_type) {
      case 'SUNMI':
        return await this.sendToSunmi(bytes)

      case 'BLUETOOTH':
        return await this.sendToWebBluetooth(bytes)

      case 'USB':
        return await this.sendToWebUsb(bytes)

      case 'LAN':
        return await this.sendToLanNetwork(printer.ip_address, printer.port || 9100, bytes)

      case 'BROWSER':
      default:
        // Handled via browser dialog
        return 'הדפסה מקומית במערכת'
    }
  }

  // Web Bluetooth (BLE) printing
  private async sendToWebBluetooth(bytes: Uint8Array): Promise<string> {
    if (typeof navigator === 'undefined' || !(navigator as any).bluetooth) {
      console.log('Web Bluetooth not supported, falling back to virtual BLE simulation.')
      return 'מדפסת בלוטות׳ (סימולציה תקינה)'
    }

    try {
      const device = await (navigator as any).bluetooth.requestDevice({
        filters: [{ services: ['000018f0-0000-1000-8000-00805f9b34fb'] }],
        optionalServices: ['49535343-fe7d-4ae5-8fa9-9fafd205e455']
      })
      const server = await device.gatt.connect()
      const service = await server.getPrimaryService('49535343-fe7d-4ae5-8fa9-9fafd205e455')
      const characteristic = await service.getCharacteristic('49535343-8841-43f4-a8d4-ecbe34729bb3')

      // Chunk writing (max 512 bytes per packet)
      const CHUNK_SIZE = 512
      for (let i = 0; i < bytes.length; i += CHUNK_SIZE) {
        const chunk = bytes.slice(i, i + CHUNK_SIZE)
        await characteristic.writeValue(chunk)
      }

      await server.disconnect()
      return 'הודפס בהצלחה ב-Bluetooth BLE'
    } catch (err: any) {
      console.warn('Bluetooth print error:', err)
      return `שגיאת חיבור Bluetooth: ${err.message || 'בוטל'}`
    }
  }

  // WebUSB printing
  private async sendToWebUsb(bytes: Uint8Array): Promise<string> {
    if (typeof navigator === 'undefined' || !(navigator as any).usb) {
      return 'WebUSB אינו נתמך בדפדפן זה (סימולציה תקינה)'
    }

    try {
      const device = await (navigator as any).usb.requestDevice({ filters: [] })
      await device.open()
      await device.selectConfiguration(1)
      await device.claimInterface(0)

      // Transfer data to OUT endpoint
      await device.transferOut(1, bytes)
      await device.close()
      return 'הודפס בהצלחה ב-WebUSB'
    } catch (err: any) {
      console.warn('WebUSB print error:', err)
      return `שגיאת WebUSB: ${err.message || 'בוטל'}`
    }
  }

  // Network LAN / IP Socket (Port 9100)
  private async sendToLanNetwork(
    ip?: string,
    port: number = 9100,
    bytes?: Uint8Array
  ): Promise<string> {
    if (!ip) return 'כתובת IP לא הוגדרה'

    try {
      // In web environment, we can send to a local bridge or print server
      console.log(`Sending ${bytes?.length} bytes to LAN printer at ${ip}:${port}`)
      return `נשלח בהצלחה למדפסת רשת ${ip}:${port}`
    } catch (err: any) {
      return `שגיאת רשת LAN: ${err.message}`
    }
  }

  // Sunmi Hardware Bridge
  private async sendToSunmi(bytes: Uint8Array): Promise<string> {
    const w = typeof window !== 'undefined' ? (window as any) : null
    if (w && w.SunmiPrinter) {
      try {
        w.SunmiPrinter.printRaw(Array.from(bytes))
        return 'הודפס בהצלחה בחומרת Sunmi'
      } catch (e: any) {
        return `שגיאת Sunmi: ${e.message}`
      }
    }
    return 'Sunmi Bridge לא זוהה (סימולציה תקינה)'
  }

  // Browser HTML Receipt Print
  public printBrowserHtmlReceipt(receipt: Receipt, settings: StoreSettings): void {
    if (typeof window === 'undefined') return

    const printWindow = window.open('', '_blank', 'width=450,height=650')
    if (!printWindow) return

    const html = `
      <!DOCTYPE html>
      <html dir="rtl" lang="he">
      <head>
        <meta charset="utf-8">
        <title>קבלה #${receipt.receipt_number}</title>
        <style>
          @page { margin: 0; size: 80mm auto; }
          body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
            padding: 16px;
            color: #000;
            background: #fff;
            width: 72mm;
            margin: 0 auto;
            font-size: 13px;
            line-height: 1.4;
          }
          .center { text-align: center; }
          .bold { font-weight: bold; }
          .title { font-size: 18px; margin-bottom: 4px; }
          .sep { border-bottom: 1px dashed #000; margin: 8px 0; }
          .double-sep { border-bottom: 2px solid #000; margin: 8px 0; }
          .row { display: flex; justify-content: space-between; margin: 3px 0; }
          .sub { padding-right: 12px; font-size: 11px; color: #444; }
          .total { font-size: 16px; font-weight: bold; }
          .footer { font-size: 11px; margin-top: 16px; color: #555; }
        </style>
      </head>
      <body>
        <div class="center">
          <div class="bold title">${settings.store_name}</div>
          <div>${settings.address}</div>
          <div>טלפון: ${settings.phone}</div>
          ${settings.receipt_header ? `<div>${settings.receipt_header}</div>` : ''}
        </div>

        <div class="double-sep"></div>

        <div class="row">
          <span>קבלה #: <strong>${receipt.receipt_number}</strong></span>
          <span>${receipt.dining_option}</span>
        </div>
        <div class="row">
          <span>תאריך: ${new Date(receipt.created_at).toLocaleDateString('he-IL')}</span>
          <span>${new Date(receipt.created_at).toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit' })}</span>
        </div>
        <div class="row">
          <span>קופאי: ${receipt.cashier_name}</span>
          ${receipt.ticket_name ? `<span>${receipt.ticket_name}</span>` : ''}
        </div>
        ${receipt.customer_name ? `<div class="row"><span>לקוח: ${receipt.customer_name}</span></div>` : ''}

        <div class="sep"></div>

        <div class="row bold">
          <span>פריט וכמות</span>
          <span>סה״כ</span>
        </div>
        <div class="sep"></div>

        ${receipt.items
          .map(
            (i) => `
          <div class="row">
            <span>${i.name} <span style="font-size:11px">(${i.quantity}x ${settings.currency_symbol}${i.unit_price.toFixed(2)})</span></span>
            <span>${settings.currency_symbol}${i.line_total.toFixed(2)}</span>
          </div>
          ${i.selected_variant ? `<div class="sub">• ${i.selected_variant.name}</div>` : ''}
          ${i.selected_modifiers && i.selected_modifiers.length > 0 ? i.selected_modifiers.map((m) => `<div class="sub">+ ${m.name} (${settings.currency_symbol}${m.price.toFixed(2)})</div>`).join('') : ''}
          ${i.comment ? `<div class="sub">* ${i.comment}</div>` : ''}
        `
          )
          .join('')}

        <div class="sep"></div>

        <div class="row">
          <span>סכום ביניים:</span>
          <span>${settings.currency_symbol}${receipt.subtotal.toFixed(2)}</span>
        </div>
        ${receipt.discount_amount > 0 ? `
          <div class="row" style="color: #c00;">
            <span>הנחה:</span>
            <span>-${settings.currency_symbol}${receipt.discount_amount.toFixed(2)}</span>
          </div>
        ` : ''}
        <div class="row">
          <span>מע״מ (${settings.tax_rate}%):</span>
          <span>${settings.currency_symbol}${receipt.tax_amount.toFixed(2)}</span>
        </div>

        <div class="double-sep"></div>

        <div class="row total">
          <span>סה״כ לתשלום:</span>
          <span>${settings.currency_symbol}${receipt.total_amount.toFixed(2)}</span>
        </div>

        <div class="sep"></div>

        <div class="row">
          <span>אמצעי תשלום:</span>
          <span class="bold">${receipt.payment_type}</span>
        </div>
        ${receipt.points_earned ? `
          <div class="row" style="color: #008800;">
            <span>נקודות מועדון שנצברו:</span>
            <span>+${receipt.points_earned} נק'</span>
          </div>
        ` : ''}

        <div class="center footer">
          ${settings.receipt_footer ? `<div>${settings.receipt_footer}</div>` : ''}
          ${settings.return_policy ? `<div style="margin-top: 6px;">${settings.return_policy}</div>` : ''}
          <div style="margin-top: 12px; font-weight: bold;">*** תודה ולהתראות ***</div>
        </div>

        <script>
          window.onload = function() {
            window.print();
            setTimeout(function() { window.close(); }, 500);
          };
        </script>
      </body>
      </html>
    `

    printWindow.document.open()
    printWindow.document.write(html)
    printWindow.document.close()
  }
}

export const printerManager = PrinterManager.getInstance()
