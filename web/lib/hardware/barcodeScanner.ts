export class HardwareBarcodeScanner {
  private buffer: string = ''
  private lastKeyTime: number = 0
  private onScanCallback: ((barcode: string) => void) | null = null
  private maxIntervalMs: number = 60 // Hardware scanners send keystrokes < 50ms apart
  private isListening: boolean = false

  constructor() {
    this.handleKeyDown = this.handleKeyDown.bind(this)
  }

  public startListening(onScan: (barcode: string) => void) {
    if (typeof window === 'undefined') return
    this.onScanCallback = onScan

    if (!this.isListening) {
      window.addEventListener('keydown', this.handleKeyDown, true)
      this.isListening = true
    }
  }

  public stopListening() {
    if (typeof window === 'undefined') return
    if (this.isListening) {
      window.removeEventListener('keydown', this.handleKeyDown, true)
      this.isListening = false
    }
    this.buffer = ''
  }

  private handleKeyDown(e: KeyboardEvent) {
    // If the active element is an input or textarea that is not a barcode target,
    // we still let the scanner capture if the speed is fast (<40ms)
    const currentTime = Date.now()
    const timeDiff = currentTime - this.lastKeyTime

    // Reset buffer if delay is too long (human typing)
    if (this.lastKeyTime !== 0 && timeDiff > this.maxIntervalMs) {
      this.buffer = ''
    }
    this.lastKeyTime = currentTime

    if (e.key === 'Enter') {
      if (this.buffer.length >= 3) {
        // Successful barcode scanned!
        const scannedCode = this.buffer.trim()
        this.buffer = ''
        if (this.onScanCallback) {
          e.preventDefault()
          e.stopPropagation()
          this.onScanCallback(scannedCode)
        }
      } else {
        this.buffer = ''
      }
    } else if (e.key.length === 1) {
      // Regular printable character
      this.buffer += e.key
    }
  }
}

export const hardwareBarcodeScanner = new HardwareBarcodeScanner()
