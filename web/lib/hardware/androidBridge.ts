export interface AndroidBridge {
  openCameraScanner?: () => void
  printEscPos?: (base64Data: string) => void
  kickCashDrawer?: () => void
  vibrate?: (ms: number) => void
  isNativeApp?: () => boolean
}

export function getAndroidBridge(): AndroidBridge | null {
  if (typeof window === 'undefined') return null
  const w = window as any
  if (w.AndroidBridge) return w.AndroidBridge
  if (w.LoyPOS) return w.LoyPOS
  return null
}

export function isRunningInAndroidApp(): boolean {
  const bridge = getAndroidBridge()
  return bridge !== null && (bridge.isNativeApp ? bridge.isNativeApp() : true)
}

export function nativePrint(base64Data: string): boolean {
  const bridge = getAndroidBridge()
  if (bridge && bridge.printEscPos) {
    try {
      bridge.printEscPos(base64Data)
      return true
    } catch (e) {
      console.warn('Native print error:', e)
    }
  }
  return false
}

export function nativeVibrate(ms: number = 50): void {
  const bridge = getAndroidBridge()
  if (bridge && bridge.vibrate) {
    try {
      bridge.vibrate(ms)
      return
    } catch {}
  }
  if (typeof navigator !== 'undefined' && navigator.vibrate) {
    try {
      navigator.vibrate(ms)
    } catch {}
  }
}
