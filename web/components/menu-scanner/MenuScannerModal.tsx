'use client'

import React, { useState, useRef, useEffect } from 'react'
import {
  X,
  Upload,
  Camera,
  RotateCw,
  Sliders,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Package,
  Layers,
  Image as ImageIcon,
  ArrowRight,
  RefreshCw,
  Wand2,
} from 'lucide-react'
import { Item, Category } from '@/lib/types'
import { usePos } from '@/lib/context/PosContext'
import { useAccessibility } from '@/lib/context/AccessibilityContext'

interface ScannedParsedItem {
  id: string
  name: string
  category: string
  price: number
  description: string
  variants: { name: string; price: number }[]
  modifiers: { name: string; price: number }[]
  box: { x: number; y: number; width: number; height: number }
  aiImageUrl?: string
  status: 'NEW' | 'UPDATE' | 'IGNORE'
}

const SAMPLE_MENU_IMAGE =
  'https://images.unsplash.com/photo-1544025162-d76694265947?w=1200&auto=format&fit=crop&q=80'

const PHOTO_STYLES = [
  { id: 'studio', label: 'Studio Editorial', desc: 'שיש לבן נקי, תאורת סטודיו רכה, עומק שדה רדוד' },
  { id: 'rustic', label: 'Rustic Overhead', desc: 'שולחן עץ כפרי, מבט עליון (Flatlay), עשבי תיבול מפוזרים' },
  { id: 'cafe', label: 'Vibrant Cafe', desc: 'אור יום טבעי חם, רקע ביסטרו פתוח, צבעים מלאי חיים' },
  { id: 'dramatic', label: 'Dark Dramatic', desc: 'משטח אבן בזלת כהה, צללים דרמטיים, תאורת הדגשה צידית' },
]

export default function MenuScannerModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const { items, categories, addItemToCatalog, addCategory } = usePos()
  const { announce, triggerSound } = useAccessibility()

  // State
  const [step, setStep] = useState<'upload' | 'process' | 'studio' | 'review'>('upload')
  const [imageSrc, setImageSrc] = useState<string>(SAMPLE_MENU_IMAGE)
  const [rotation, setRotation] = useState<number>(0)
  const [deskew, setDeskew] = useState<number>(0)
  const [contrast, setContrast] = useState<number>(100)
  const [zoom, setZoom] = useState<number>(1)
  const [pan, setPan] = useState<{ x: number; y: number }>({ x: 0, y: 0 })
  const [isProcessingOcr, setIsProcessingOcr] = useState<boolean>(false)
  const [parsedItems, setParsedItems] = useState<ScannedParsedItem[]>([])
  const [selectedStyle, setSelectedStyle] = useState<string>('studio')
  const [isGeneratingAi, setIsGeneratingAi] = useState<boolean>(false)
  const [selectedItemForStudio, setSelectedItemForStudio] = useState<string | null>(null)

  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Draw image on canvas with transforms
  useEffect(() => {
    if (!canvasRef.current || !imageSrc || step !== 'process') return

    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = imageSrc
    img.onload = () => {
      canvas.width = 800
      canvas.height = 550

      ctx.clearRect(0, 0, canvas.width, canvas.height)
      ctx.save()

      // Apply zoom & pan
      ctx.translate(canvas.width / 2 + pan.x, canvas.height / 2 + pan.y)
      ctx.scale(zoom, zoom)
      ctx.rotate(((rotation + deskew) * Math.PI) / 180)

      // Contrast filter
      ctx.filter = `contrast(${contrast}%)`

      // Draw image centered
      const drawWidth = 500
      const drawHeight = (500 * img.height) / img.width
      ctx.drawImage(img, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight)

      // Draw OCR Bounding boxes overlay
      if (parsedItems.length > 0) {
        ctx.filter = 'none'
        parsedItems.forEach((item, index) => {
          ctx.strokeStyle = '#0288D1'
          ctx.lineWidth = 2
          ctx.fillStyle = 'rgba(2, 136, 209, 0.2)'
          const b = item.box
          ctx.fillRect(b.x - drawWidth / 2, b.y - drawHeight / 2, b.width, b.height)
          ctx.strokeRect(b.x - drawWidth / 2, b.y - drawHeight / 2, b.width, b.height)

          // Badge tag
          ctx.fillStyle = '#0288D1'
          ctx.fillRect(b.x - drawWidth / 2, b.y - drawHeight / 2 - 18, 22, 18)
          ctx.fillStyle = '#FFFFFF'
          ctx.font = 'bold 11px sans-serif'
          ctx.fillText(`${index + 1}`, b.x - drawWidth / 2 + 6, b.y - drawHeight / 2 - 5)
        })
      }

      ctx.restore()
    }
  }, [imageSrc, rotation, deskew, contrast, zoom, pan, parsedItems, step])

  if (!isOpen) return null

  function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (event) => {
        setImageSrc(event.target?.result as string)
        setStep('process')
      }
      reader.readAsDataURL(file)
    }
  }

  function handleRotate() {
    setRotation((prev) => (prev + 90) % 360)
  }

  // Simulated OCR structural parser
  function runOcrExtraction() {
    setIsProcessingOcr(true)
    triggerSound('beep')

    setTimeout(() => {
      const extracted: ScannedParsedItem[] = [
        {
          id: 'sc1',
          name: 'אנטרקוט פרימיום 350 גרם',
          category: 'המבורגרים ובשרים',
          price: 135.0,
          description: 'סטייק אנטרקוט מיושן מובחר, צרוב על גריל פחמים בליווי צ׳יפס ורוטב פלפלת',
          variants: [
            { name: '350 גרם', price: 135.0 },
            { name: '500 גרם ענק', price: 185.0 },
          ],
          modifiers: [
            { name: 'מידת עשייה: Medium', price: 0.0 },
            { name: 'תוספת פטריות חמות', price: 12.0 },
          ],
          box: { x: 50, y: 70, width: 400, height: 65 },
          aiImageUrl:
            'https://images.unsplash.com/photo-1600891964599-f61ba0e24092?w=600&auto=format&fit=crop&q=80',
          status: 'NEW',
        },
        {
          id: 'sc2',
          name: 'ריזוטו פטריות כמהין',
          category: 'פיצות ומאפים',
          price: 68.0,
          description: 'אורז ארבוריו איטלקי, תערובת פטריות יער, חמאה ופרמז׳ן רג׳יאנו מיושן 24 חודש',
          variants: [],
          modifiers: [{ name: 'תוספת מחית כמהין', price: 10.0 }],
          box: { x: 50, y: 160, width: 400, height: 60 },
          aiImageUrl:
            'https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?w=600&auto=format&fit=crop&q=80',
          status: 'NEW',
        },
        {
          id: 'sc3',
          name: 'סביצ׳ה דג ים ים-תיכוני',
          category: 'סלטים ותוספות',
          price: 54.0,
          description: 'קוביות מוסר ים טרי, כוסברה, בצל סגול, צ׳ילי אדום וקרם אבוקדו בליווי טוסטונים',
          variants: [],
          modifiers: [{ name: 'ללא כוסברה', price: 0.0 }],
          box: { x: 50, y: 250, width: 400, height: 60 },
          aiImageUrl:
            'https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=600&auto=format&fit=crop&q=80',
          status: 'NEW',
        },
        {
          id: 'sc4',
          name: 'קוקטייל פסיפלורה מוחיטו',
          category: 'בירות ומשקאות אלכוהוליים',
          price: 42.0,
          description: 'רום לבן, מחית פסיפלורה טבעית, נענע רעננה, ליים וסודה',
          variants: [],
          modifiers: [{ name: 'אקסטרה רום', price: 12.0 }],
          box: { x: 50, y: 340, width: 400, height: 55 },
          aiImageUrl:
            'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=600&auto=format&fit=crop&q=80',
          status: 'NEW',
        },
      ]

      // Check against existing catalog
      const updated = extracted.map((sc) => {
        const match = items.find((i) => i.name.toLowerCase() === sc.name.toLowerCase())
        if (match) {
          return { ...sc, status: 'UPDATE' as const }
        }
        return sc
      })

      setParsedItems(updated)
      setIsProcessingOcr(false)
      announce(`זוהו בהצלחה ${updated.length} פריטים בתפריט!`)
      triggerSound('success')
    }, 1200)
  }

  // Gemini AI Food Photo Generator
  function generateGeminiImage(itemId: string) {
    setIsGeneratingAi(true)
    triggerSound('beep')

    setTimeout(() => {
      const generatedPhotos: Record<string, string> = {
        sc1: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop&q=80',
        sc2: 'https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?w=600&auto=format&fit=crop&q=80',
        sc3: 'https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=600&auto=format&fit=crop&q=80',
        sc4: 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=600&auto=format&fit=crop&q=80',
      }

      setParsedItems((prev) =>
        prev.map((item) =>
          item.id === itemId
            ? { ...item, aiImageUrl: generatedPhotos[itemId] || item.aiImageUrl }
            : item
        )
      )
      setIsGeneratingAi(false)
      triggerSound('success')
      announce('תמונת מזון מקצועית נוצרה בהצלחה!')
    }, 1500)
  }

  // Batch import to store catalog
  async function handleBatchImport() {
    const importable = parsedItems.filter((i) => i.status !== 'IGNORE')

    for (const sc of importable) {
      // Find or create category
      let cat = categories.find((c) => c.name === sc.category)
      let catId = cat ? cat.id : 1

      if (!cat) {
        catId = Date.now() + Math.floor(Math.random() * 100)
        await addCategory({
          id: catId,
          name: sc.category,
          color_hex: '#0288D1',
          target_printer: 'KITCHEN',
        })
      }

      const newItem: Item = {
        id: Date.now() + Math.floor(Math.random() * 1000),
        name: sc.name,
        category_id: catId,
        category_name: sc.category,
        price: sc.price,
        cost: Math.round(sc.price * 0.3),
        sku: `SKU-${Math.floor(Math.random() * 9000 + 1000)}`,
        barcode: `7290${Math.floor(Math.random() * 90000000 + 10000000)}`,
        sold_by: 'EACH',
        in_stock: 50,
        low_stock_threshold: 10,
        track_stock: true,
        is_active: true,
        is_favorite: true,
        image_url: sc.aiImageUrl,
        variants: sc.variants.map((v, i) => ({
          id: `var_${Date.now()}_${i}`,
          name: v.name,
          price: v.price,
          cost: Math.round(v.price * 0.3),
          in_stock: 25,
        })),
        modifier_groups:
          sc.modifiers.length > 0
            ? [
                {
                  id: Date.now(),
                  name: 'תוספות שנסרקו',
                  options: sc.modifiers.map((m, i) => ({
                    id: `mod_${i}`,
                    name: m.name,
                    price: m.price,
                  })),
                },
              ]
            : undefined,
      }

      await addItemToCatalog(newItem)
    }

    triggerSound('success')
    announce(`יובאו בהצלחה ${importable.length} פריטים לקטלוג!`)
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-5xl max-h-[92vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 bg-gradient-to-r from-sky-600 to-indigo-700 text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-white/10 backdrop-blur-md">
              <Sparkles className="w-6 h-6 text-sky-200" />
            </div>
            <div>
              <h2 className="text-lg font-black tracking-tight">
                סורק תפריטים מבוסס AI וסטודיו לתמונות (Gemini)
              </h2>
              <p className="text-xs text-sky-100">
                עיבוד תמונה, מנוע OCR מבני, יצירת תמונות מזון מקצועיות וייבוא לקטלוג
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition-colors"
          >
            <X className="w-5 h-5 text-white" />
          </button>
        </div>

        {/* Step Navigation Tabs */}
        <div className="border-b border-slate-200 bg-slate-50 px-6 py-3 flex items-center gap-2 text-xs font-bold">
          <button
            onClick={() => setStep('upload')}
            className={`px-3 py-1.5 rounded-xl transition-all ${
              step === 'upload'
                ? 'bg-sky-600 text-white shadow'
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            1. העלאת תפריט
          </button>
          <ArrowRight className="w-3.5 h-3.5 text-slate-400 rtl:rotate-180" />
          <button
            onClick={() => setStep('process')}
            className={`px-3 py-1.5 rounded-xl transition-all ${
              step === 'process'
                ? 'bg-sky-600 text-white shadow'
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            2. עיבוד תמונה ו-OCR
          </button>
          <ArrowRight className="w-3.5 h-3.5 text-slate-400 rtl:rotate-180" />
          <button
            onClick={() => setStep('studio')}
            className={`px-3 py-1.5 rounded-xl transition-all ${
              step === 'studio'
                ? 'bg-sky-600 text-white shadow'
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            3. Gemini AI Food Studio
          </button>
          <ArrowRight className="w-3.5 h-3.5 text-slate-400 rtl:rotate-180" />
          <button
            onClick={() => setStep('review')}
            className={`px-3 py-1.5 rounded-xl transition-all ${
              step === 'review'
                ? 'bg-sky-600 text-white shadow'
                : 'text-slate-600 hover:bg-slate-200'
            }`}
          >
            4. סקירה וייבוא לקטלוג ({parsedItems.length})
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 bg-slate-100">
          {/* STEP 1: UPLOAD */}
          {step === 'upload' && (
            <div className="flex flex-col items-center justify-center py-12 px-4 max-w-xl mx-auto text-center space-y-6">
              <div className="p-5 rounded-3xl bg-sky-50 text-sky-600 border border-sky-200">
                <Upload className="w-12 h-12 stroke-[1.5]" />
              </div>
              <div>
                <h3 className="text-xl font-extrabold text-slate-800">
                  צילום או העלאת תמונת תפריט
                </h3>
                <p className="text-sm text-slate-500 mt-1">
                  העלה תצלום תפריט, פלייר או עלון מסעדה. המערכת תזהה את המנות, הווריאנטים והמחירים באופן אוטומטי.
                </p>
              </div>

              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileUpload}
                accept="image/*"
                className="hidden"
              />

              <div className="flex flex-wrap items-center justify-center gap-3 w-full">
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="flex-1 py-3.5 px-6 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-sm shadow-lg shadow-sky-600/25 flex items-center justify-center gap-2 transition-transform active:scale-95"
                >
                  <Upload className="w-4 h-4" />
                  <span>בחר קובץ תמונה</span>
                </button>
                <button
                  onClick={() => {
                    setImageSrc(SAMPLE_MENU_IMAGE)
                    setStep('process')
                  }}
                  className="py-3.5 px-6 rounded-2xl bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 font-bold text-sm shadow-sm flex items-center justify-center gap-2"
                >
                  <Sparkles className="w-4 h-4 text-amber-500" />
                  <span>השתמש בתפריט לדוגמה</span>
                </button>
              </div>
            </div>
          )}

          {/* STEP 2: IMAGE PRE-PROCESSING & OCR */}
          {step === 'process' && (
            <div className="space-y-4">
              {/* Canvas Controls Toolbar */}
              <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleRotate}
                    className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold flex items-center gap-1.5"
                    title="סובב 90 מעלות"
                  >
                    <RotateCw className="w-4 h-4" />
                    <span>סובב 90° ({rotation}°)</span>
                  </button>

                  <button
                    onClick={() => setZoom((z) => Math.min(2.5, z + 0.2))}
                    className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs"
                    title="הגדל"
                  >
                    <ZoomIn className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setZoom((z) => Math.max(0.5, z - 0.2))}
                    className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs"
                    title="הקטן"
                  >
                    <ZoomOut className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => {
                      setZoom(1)
                      setPan({ x: 0, y: 0 })
                    }}
                    className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs"
                    title="אפס"
                  >
                    <Maximize2 className="w-4 h-4" />
                  </button>
                </div>

                {/* Deskew & Contrast Sliders */}
                <div className="flex items-center gap-4 text-xs font-semibold text-slate-600">
                  <div className="flex items-center gap-2">
                    <span>יישור זווית:</span>
                    <input
                      type="range"
                      min="-15"
                      max="15"
                      value={deskew}
                      onChange={(e) => setDeskew(Number(e.target.value))}
                      className="w-20 accent-sky-600"
                    />
                    <span className="w-6 text-left">{deskew}°</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span>ניגודיות ל-OCR:</span>
                    <input
                      type="range"
                      min="80"
                      max="200"
                      value={contrast}
                      onChange={(e) => setContrast(Number(e.target.value))}
                      className="w-20 accent-sky-600"
                    />
                    <span className="w-8 text-left">{contrast}%</span>
                  </div>
                </div>

                {/* Trigger OCR Button */}
                <button
                  onClick={runOcrExtraction}
                  disabled={isProcessingOcr}
                  className="py-2 px-5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs shadow-md shadow-sky-600/20 flex items-center gap-2 disabled:opacity-50"
                >
                  {isProcessingOcr ? (
                    <RefreshCw className="w-4 h-4 animate-spin" />
                  ) : (
                    <Sparkles className="w-4 h-4 text-sky-200" />
                  )}
                  <span>{isProcessingOcr ? 'מפענח OCR...' : 'בצע פענוח OCR מבני'}</span>
                </button>
              </div>

              {/* Interactive Canvas Viewport */}
              <div className="bg-slate-900 rounded-3xl overflow-hidden border border-slate-300 shadow-inner flex items-center justify-center p-2 relative min-h-[450px]">
                <canvas
                  ref={canvasRef}
                  className="rounded-2xl max-w-full shadow-lg cursor-grab active:cursor-grabbing"
                  onMouseDown={(e) => {
                    const startX = e.clientX - pan.x
                    const startY = e.clientY - pan.y
                    const onMove = (moveEvent: MouseEvent) => {
                      setPan({
                        x: moveEvent.clientX - startX,
                        y: moveEvent.clientY - startY,
                      })
                    }
                    const onUp = () => {
                      window.removeEventListener('mousemove', onMove)
                      window.removeEventListener('mouseup', onUp)
                    }
                    window.addEventListener('mousemove', onMove)
                    window.addEventListener('mouseup', onUp)
                  }}
                />

                {parsedItems.length > 0 && (
                  <div className="absolute bottom-4 right-4 bg-white/95 backdrop-blur-md px-4 py-2.5 rounded-2xl shadow-xl border border-slate-200 flex items-center gap-3">
                    <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                    <span className="text-xs font-bold text-slate-800">
                      זוהו {parsedItems.length} פריטים עם תיבות גבול (Bounding Boxes)!
                    </span>
                    <button
                      onClick={() => setStep('studio')}
                      className="py-1 px-3 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold"
                    >
                      המשך לסטודיו לתמונות &larr;
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* STEP 3: GEMINI AI FOOD STUDIO */}
          {step === 'studio' && (
            <div className="space-y-6">
              <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-2xl bg-amber-50 text-amber-600 border border-amber-200">
                    <Wand2 className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="text-base font-extrabold text-slate-800">
                      Gemini AI Product Image Studio
                    </h3>
                    <p className="text-xs text-slate-500">
                      הפק תצלומי מנות פוטו-ריאליסטיים עבור הפריטים שנסרקו בתפריט לפי סגנון צילום מוגדר
                    </p>
                  </div>
                </div>

                {/* Photography Style Grid */}
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
                    בחר סגנון צילום קולינרי
                  </label>
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                    {PHOTO_STYLES.map((st) => (
                      <div
                        key={st.id}
                        onClick={() => setSelectedStyle(st.id)}
                        className={`p-3.5 rounded-2xl border cursor-pointer transition-all ${
                          selectedStyle === st.id
                            ? 'border-sky-600 bg-sky-50/50 shadow-md ring-2 ring-sky-600/20'
                            : 'border-slate-200 bg-white hover:bg-slate-50'
                        }`}
                      >
                        <h4 className="text-xs font-black text-slate-800">{st.label}</h4>
                        <p className="text-[11px] text-slate-500 mt-1 leading-relaxed">
                          {st.desc}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Items List with AI Image Generator */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {parsedItems.map((item) => (
                  <div
                    key={item.id}
                    className="bg-white p-4 rounded-3xl border border-slate-200 shadow-sm flex items-start gap-4"
                  >
                    <div className="w-24 h-24 rounded-2xl bg-slate-100 overflow-hidden relative shrink-0 border border-slate-200">
                      {item.aiImageUrl ? (
                        <img
                          src={item.aiImageUrl}
                          alt={item.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex flex-col items-center justify-center text-slate-400">
                          <ImageIcon className="w-6 h-6 mb-1" />
                          <span className="text-[10px]">ללא תמונה</span>
                        </div>
                      )}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                          {item.category}
                        </span>
                        <span className="text-sm font-extrabold text-sky-600">
                          ₪{item.price.toFixed(2)}
                        </span>
                      </div>
                      <h4 className="text-sm font-black text-slate-800 truncate mt-1">
                        {item.name}
                      </h4>
                      <p className="text-xs text-slate-500 line-clamp-2 mt-0.5">
                        {item.description}
                      </p>

                      <div className="mt-3 flex items-center gap-2">
                        <button
                          onClick={() => generateGeminiImage(item.id)}
                          disabled={isGeneratingAi}
                          className="py-1.5 px-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold text-xs shadow-sm flex items-center gap-1.5 transition-colors disabled:opacity-50"
                        >
                          <Sparkles className="w-3.5 h-3.5" />
                          <span>צור תמונת AI ({PHOTO_STYLES.find((s) => s.id === selectedStyle)?.label})</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="flex justify-end pt-2">
                <button
                  onClick={() => setStep('review')}
                  className="py-3 px-8 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-sm shadow-lg shadow-sky-600/25 flex items-center gap-2"
                >
                  <span>המשך לסקירה וייבוא לקטלוג</span>
                  <ArrowRight className="w-4 h-4 rtl:rotate-180" />
                </button>
              </div>
            </div>
          )}

          {/* STEP 4: REVIEW & BATCH CATALOG IMPORT */}
          {step === 'review' && (
            <div className="space-y-4">
              <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
                <div>
                  <h3 className="text-base font-extrabold text-slate-800">
                    השוואת פריטים לקטלוג קיים וייבוא
                  </h3>
                  <p className="text-xs text-slate-500">
                    בדוק את שמות המנות, המחירים והווריאנטים לפני הוספתם הסופית לקופה.
                  </p>
                </div>
                <button
                  onClick={handleBatchImport}
                  className="py-2.5 px-6 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-lg shadow-emerald-600/20 flex items-center gap-2"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>ייבא {parsedItems.filter((i) => i.status !== 'IGNORE').length} פריטים לקטלוג</span>
                </button>
              </div>

              <div className="space-y-3">
                {parsedItems.map((item, idx) => (
                  <div
                    key={item.id}
                    className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4"
                  >
                    <div className="flex items-center gap-4 w-full md:w-auto">
                      <div className="w-16 h-16 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-200">
                        {item.aiImageUrl ? (
                          <img
                            src={item.aiImageUrl}
                            alt={item.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-slate-400">
                            <Package className="w-6 h-6" />
                          </div>
                        )}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-slate-400">#{idx + 1}</span>
                          <h4 className="text-sm font-extrabold text-slate-800">{item.name}</h4>
                          <span
                            className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase ${
                              item.status === 'NEW'
                                ? 'bg-emerald-50 text-emerald-600 border border-emerald-200'
                                : item.status === 'UPDATE'
                                ? 'bg-amber-50 text-amber-600 border border-amber-200'
                                : 'bg-slate-100 text-slate-400'
                            }`}
                          >
                            {item.status === 'NEW'
                              ? 'מוצר חדש'
                              : item.status === 'UPDATE'
                              ? 'קיים - עדכן'
                              : 'התעלם'}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">
                          קטגוריה: <strong>{item.category}</strong> • מחיר מומלץ:{' '}
                          <strong>₪{item.price.toFixed(2)}</strong>
                        </p>
                        {item.variants.length > 0 && (
                          <p className="text-[11px] text-sky-600 font-medium">
                            וריאנטים: {item.variants.map((v) => `${v.name} (₪${v.price})`).join(', ')}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 w-full md:w-auto justify-end">
                      <button
                        onClick={() =>
                          setParsedItems((prev) =>
                            prev.map((p) =>
                              p.id === item.id ? { ...p, status: 'NEW' } : p
                            )
                          )
                        }
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                          item.status === 'NEW'
                            ? 'bg-emerald-600 text-white shadow'
                            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                        }`}
                      >
                        צור חדש
                      </button>
                      <button
                        onClick={() =>
                          setParsedItems((prev) =>
                            prev.map((p) =>
                              p.id === item.id ? { ...p, status: 'IGNORE' } : p
                            )
                          )
                        }
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                          item.status === 'IGNORE'
                            ? 'bg-red-500 text-white shadow'
                            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                        }`}
                      >
                        התעלם
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
