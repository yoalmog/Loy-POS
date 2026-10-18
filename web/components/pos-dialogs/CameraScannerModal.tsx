'use client'

import React, { useRef, useState, useEffect } from 'react'
import { X, Camera, RefreshCw, Barcode, CheckCircle2 } from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useAccessibility } from '@/lib/context/AccessibilityContext'
import { useTheme } from '@/lib/context/ThemeContext'

export default function CameraScannerModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const { items, addToCart } = usePos()
  const { announce, triggerSound, triggerHaptic } = useAccessibility()
  const { t, isRtl } = useTheme()

  const videoRef = useRef<HTMLVideoElement | null>(null)
  const [stream, setStream] = useState<MediaStream | null>(null)
  const [manualCode, setManualCode] = useState<string>('')
  const [detectedItemName, setDetectedItemName] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen) {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices
          .getUserMedia({ video: { facingMode: 'environment' } })
          .then((s) => {
            setStream(s)
            if (videoRef.current) {
              videoRef.current.srcObject = s
            }
          })
          .catch((err) => {
            console.log('Camera access notice:', err)
          })
      }
    } else {
      if (stream) {
        stream.getTracks().forEach((track) => track.stop())
        setStream(null)
      }
    }

    return () => {
      if (stream) {
        stream.getTracks().forEach((track) => track.stop())
      }
    }
  }, [isOpen])

  if (!isOpen) return null

  function handleCodeSubmit(code: string) {
    const trimmed = code.trim()
    const found = items.find(
      (i) => i.barcode === trimmed || i.sku?.toLowerCase() === trimmed.toLowerCase()
    )

    if (found) {
      addToCart(found)
      triggerSound('beep')
      triggerHaptic(50)
      setDetectedItemName(found.name)
      announce(found.name)
      setTimeout(() => {
        setDetectedItemName(null)
        onClose()
      }, 1000)
    } else {
      triggerSound('error')
      announce(trimmed)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md overflow-hidden flex flex-col text-start">
        {/* Header */}
        <div className="p-4 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Camera className="w-5 h-5 text-sky-400" />
            <h3 className="text-base font-black">{t.cameraScanner}</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Video Viewport */}
        <div className="bg-black relative h-64 flex items-center justify-center overflow-hidden">
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="w-full h-full object-cover"
          />

          {/* Aiming Reticle Overlay */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="w-56 h-36 border-2 border-sky-400/80 rounded-2xl relative shadow-2xl shadow-sky-500/30">
              <div className="absolute top-0 left-0 right-0 h-0.5 bg-red-500 animate-pulse"></div>
            </div>
          </div>

          {detectedItemName && (
            <div className="absolute inset-0 bg-emerald-600/90 flex flex-col items-center justify-center text-white p-4 text-center z-10 animate-fade-in">
              <CheckCircle2 className="w-12 h-12 mb-2" />
              <h4 className="text-lg font-black">{detectedItemName}</h4>
              <p className="text-xs text-emerald-100">{t.cart} ✓</p>
            </div>
          )}
        </div>

        {/* Manual SKU / Barcode input */}
        <div className="p-4 bg-slate-50 border-t border-slate-200 space-y-3">
          <label className="block text-xs font-bold uppercase text-slate-500">
            {t.sku} / {t.barcode}:
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder={`${t.sku} / ${t.barcode}...`}
              value={manualCode}
              onChange={(e) => setManualCode(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleCodeSubmit(manualCode)
              }}
              className="flex-1 p-2.5 rounded-xl border border-slate-300 text-xs text-slate-800 bg-white"
            />
            <button
              onClick={() => handleCodeSubmit(manualCode)}
              className="py-2.5 px-4 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold shadow-sm"
            >
              {t.add}
            </button>
          </div>

          {/* Quick Demo Barcodes */}
          <div className="pt-1">
            <span className="text-[10px] text-slate-400 block mb-1">{t.barcode}:</span>
            <div className="flex flex-wrap gap-1.5">
              {items.slice(0, 4).map((it) => (
                <button
                  key={it.id}
                  onClick={() => handleCodeSubmit(it.barcode || it.sku || '')}
                  className="px-2 py-1 rounded-lg bg-white border border-slate-200 text-[10px] font-mono font-bold text-slate-700 hover:bg-slate-100"
                >
                  {it.name.split(' ')[0]}: {it.barcode}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
