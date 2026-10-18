'use client'

import React, { useState } from 'react'
import {
  Settings as SettingsIcon,
  Store,
  Printer,
  Eye,
  Palette,
  Globe,
  Shield,
  Save,
  CheckCircle2,
  Volume2,
  Smartphone,
  Layers,
  Sparkles,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useAccessibility } from '@/lib/context/AccessibilityContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { StoreSettings, ColorTheme, SupportedLanguage } from '@/lib/types'
import { printerManager } from '@/lib/hardware/printerManager'
import OwnerAuthModal from '@/components/pos-dialogs/OwnerAuthModal'

export default function StoreSettingsPage() {
  const { settings, updateSettings, refreshData } = usePos()
  const {
    textScale,
    setTextScale,
    contrastMode,
    setContrastMode,
    colorblindMode,
    setColorblindMode,
    touchSize,
    setTouchSize,
    soundEnabled,
    setSoundEnabled,
    hapticEnabled,
    setHapticEnabled,
    simplifiedPos,
    setSimplifiedPos,
    triggerSound,
  } = useAccessibility()

  const { mode, setMode, palette, setPalette, language, setLanguage, t } = useTheme()

  const [activeTab, setActiveTab] = useState<'store' | 'printers' | 'accessibility' | 'theme' | 'security'>('store')
  const [formData, setFormData] = useState<StoreSettings>(settings)
  const [isSaved, setIsSaved] = useState(false)
  const [isOwnerModalOpen, setIsOwnerModalOpen] = useState(false)
  const [testPrintStatus, setTestPrintStatus] = useState<string | null>(null)

  async function handleSaveSettings(e: React.FormEvent) {
    e.preventDefault()
    await updateSettings(formData)
    setIsSaved(true)
    triggerSound('success')
    setTimeout(() => setIsSaved(false), 2500)
  }

  async function handleTestPrint(target: 'CASHIER' | 'KITCHEN' | 'BAR') {
    setTestPrintStatus(`מבצע בדיקת הדפסה עבור ${target}...`)
    try {
      const printers = await printerManager.getPrinters()
      const printer = printers.find((p) => p.type === target) || printers[0]
      if (printer) {
        printerManager.printBrowserHtmlReceipt(
          {
            id: 'test_receipt',
            receipt_number: 'TEST-001',
            created_at: new Date().toISOString(),
            cashier_name: 'בדיקת מערכת',
            dining_option: 'Dine in',
            subtotal: 100.0,
            tax_amount: 17.0,
            discount_amount: 0,
            tip_amount: 0,
            total_amount: 100.0,
            payment_type: 'CASH',
            is_refunded: false,
            items: [
              {
                cart_id: 'c1',
                item_id: 1,
                name: 'בדיקת הדפסה מוצלחת',
                unit_price: 100.0,
                cost: 30.0,
                quantity: 1,
                modifiers_cost: 0,
                selected_modifiers: [],
                discount_percent: 0,
                discount_fixed: 0,
                line_total: 100.0,
              },
            ],
          },
          formData
        )
        setTestPrintStatus('הדפסת הניסיון נשלחה בהצלחה!')
      }
    } catch (err: any) {
      setTestPrintStatus(`שגיאת הדפסה: ${err.message}`)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            הגדרות מערכת וקונפיגורציה (Settings & Setup)
          </h2>
          <p className="text-xs text-slate-500">
            פרופיל החנות, חומרה ומדפסות, נגישות, שפות (i18n), ואבטחת בעל עסק
          </p>
        </div>

        {isSaved && (
          <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold animate-fade-in">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>ההגדרות נשמרו בהצלחה!</span>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200 overflow-x-auto no-scrollbar">
        <button
          onClick={() => setActiveTab('store')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 whitespace-nowrap ${
            activeTab === 'store'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Store className="w-4 h-4" />
          <span>פרטי עסק וקבלות</span>
        </button>

        <button
          onClick={() => setActiveTab('printers')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 whitespace-nowrap ${
            activeTab === 'printers'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Printer className="w-4 h-4" />
          <span>מדפסות וחומרה</span>
        </button>

        <button
          onClick={() => setActiveTab('accessibility')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 whitespace-nowrap ${
            activeTab === 'accessibility'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Eye className="w-4 h-4" />
          <span>חבילת נגישות מתקדמת</span>
        </button>

        <button
          onClick={() => setActiveTab('theme')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 whitespace-nowrap ${
            activeTab === 'theme'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Palette className="w-4 h-4" />
          <span>עיצוב ושפות ({language})</span>
        </button>

        <button
          onClick={() => setActiveTab('security')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 whitespace-nowrap ${
            activeTab === 'security'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Shield className="w-4 h-4" />
          <span>אבטחה וקוד PIN</span>
        </button>
      </div>

      {/* 1. STORE PROFILE TAB */}
      {activeTab === 'store' && (
        <form onSubmit={handleSaveSettings} className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <h3 className="text-sm font-black text-slate-900 mb-2">פרטי העסק וחשבונית</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                שם העסק / החנות *
              </label>
              <input
                type="text"
                required
                value={formData.store_name}
                onChange={(e) => setFormData({ ...formData, store_name: e.target.value })}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 font-bold text-slate-900"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                טלפון ליצירת קשר
              </label>
              <input
                type="text"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                כתובת מלאה
              </label>
              <input
                type="text"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                דואר אלקטרוני
              </label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                סמל מטבע
              </label>
              <input
                type="text"
                value={formData.currency_symbol}
                onChange={(e) =>
                  setFormData({ ...formData, currency_symbol: e.target.value })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900 font-bold"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                שיעור מע״מ / מס (%)
              </label>
              <input
                type="number"
                step="0.1"
                value={formData.tax_rate}
                onChange={(e) =>
                  setFormData({ ...formData, tax_rate: Number(e.target.value) })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900 font-bold"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                מצב מע״מ
              </label>
              <select
                value={formData.tax_included ? 'true' : 'false'}
                onChange={(e) =>
                  setFormData({ ...formData, tax_included: e.target.value === 'true' })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900 font-bold"
              >
                <option value="true">מחירים כוללים מע״מ (ישראל/אירופה)</option>
                <option value="false">הוסף מע״מ בקופה (ארה״ב)</option>
              </select>
            </div>
          </div>

          <div className="space-y-3 pt-2">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                כותרת עליונה בקבלה (Receipt Header)
              </label>
              <input
                type="text"
                value={formData.receipt_header || ''}
                onChange={(e) =>
                  setFormData({ ...formData, receipt_header: e.target.value })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                כותרת תחתונה בקבלה (Receipt Footer)
              </label>
              <input
                type="text"
                value={formData.receipt_footer || ''}
                onChange={(e) =>
                  setFormData({ ...formData, receipt_footer: e.target.value })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                מדיניות החזרות מודפסת בקבלה
              </label>
              <input
                type="text"
                value={formData.return_policy || ''}
                onChange={(e) =>
                  setFormData({ ...formData, return_policy: e.target.value })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
              />
            </div>
          </div>

          <div className="flex justify-end pt-4 border-t border-slate-100">
            <button
              type="submit"
              className="py-2.5 px-6 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white font-black text-xs shadow-md shadow-sky-600/20 flex items-center gap-2"
            >
              <Save className="w-4 h-4" />
              <span>שמור הגדרות</span>
            </button>
          </div>
        </form>
      )}

      {/* 2. PRINTERS & ROUTING TAB */}
      {activeTab === 'printers' && (
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-black text-slate-900">
                מערכת הדפסה כפולה וניתוב הזמנות (Dual Printer System)
              </h3>
              <p className="text-xs text-slate-500">
                חיבור מדפסות Bluetooth BLE, WebUSB, רשת LAN (TCP 9100) ו-Sunmi
              </p>
            </div>
          </div>

          {testPrintStatus && (
            <div className="p-3 rounded-xl bg-sky-50 text-sky-800 text-xs font-bold border border-sky-200">
              {testPrintStatus}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Cashier Printer */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-black text-xs text-slate-900">מדפסת קופה ראשית</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  פעיל
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                מיועדת לקבלות לקוח, פתיחת מגירת מזומנים וחיתוך נייר אוטומטי (80 מ״מ).
              </p>
              <button
                onClick={() => handleTestPrint('CASHIER')}
                className="w-full py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700"
              >
                בצע הדפסת ניסיון
              </button>
            </div>

            {/* Kitchen Printer */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-black text-xs text-slate-900">מדפסת מטבח (LAN 9100)</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  פעיל
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                ניתוב אוטומטי של הזמנות מנות חמות, בשרים ופיצות ישירות לשף (80 מ״מ).
              </p>
              <button
                onClick={() => handleTestPrint('KITCHEN')}
                className="w-full py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700"
              >
                בדיקת מדפסת מטבח
              </button>
            </div>

            {/* Bar Printer */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-black text-xs text-slate-900">מדפסת בר (BLE ניידת)</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  פעיל
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                ניתוב אוטומטי של בירות, קפה ומשקאות לברמן (רוחב נייר 58 מ״מ).
              </p>
              <button
                onClick={() => handleTestPrint('BAR')}
                className="w-full py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700"
              >
                בדיקת מדפסת בר
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 3. ACCESSIBILITY SUITE TAB */}
      {activeTab === 'accessibility' && (
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-6">
          <div>
            <h3 className="text-sm font-black text-slate-900">
              חבילת נגישות מתקדמת (WCAG 2.1 / Section 508)
            </h3>
            <p className="text-xs text-slate-500">
              התאמות ויזואליות, גדלי טקסט, פילטרים לעיוורון צבעים ומשוב קולי/רטט
            </p>
          </div>

          {/* Text Scaling */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              גודל טקסט (Text Scale)
            </label>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {[
                { scale: '1.0', label: 'רגיל (1.0x)' },
                { scale: '1.15', label: 'גדול (1.15x)' },
                { scale: '1.30', label: 'מוגדל (1.30x)' },
                { scale: '1.50', label: 'מקסימלי (1.50x)' },
              ].map((s) => (
                <button
                  key={s.scale}
                  type="button"
                  onClick={() => setTextScale(s.scale as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    textScale === s.scale
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {s.label}
                </button>
              ))}
            </div>
          </div>

          {/* Contrast Mode */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              מצבי ניגודיות (Contrast)
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { mode: 'normal', label: 'רגיל' },
                { mode: 'high', label: 'ניגודיות גבוהה' },
                { mode: 'ultra', label: 'ניגודיות אולטרה' },
              ].map((c) => (
                <button
                  key={c.mode}
                  type="button"
                  onClick={() => setContrastMode(c.mode as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    contrastMode === c.mode
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {c.label}
                </button>
              ))}
            </div>
          </div>

          {/* Colorblind Filters */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              התאמה לעיוורון צבעים
            </label>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-2">
              {[
                { mode: 'none', label: 'ללא סינון' },
                { mode: 'protanopia', label: 'פרוטנופיה (אדום)' },
                { mode: 'deuteranopia', label: 'דאוטרנופיה (ירוק)' },
                { mode: 'tritanopia', label: 'טריטנופיה (כחול)' },
                { mode: 'monochrome', label: 'מונוכרומטי' },
              ].map((cb) => (
                <button
                  key={cb.mode}
                  type="button"
                  onClick={() => setColorblindMode(cb.mode as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    colorblindMode === cb.mode
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {cb.label}
                </button>
              ))}
            </div>
          </div>

          {/* Touch Target Sizing */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              שטח כפתורי מגע במסך (Touch Target)
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { size: 'standard', label: 'סטנדרטי (44px)' },
                { size: 'large', label: 'מוגדל (52px)' },
                { size: 'jumbo', label: 'ג׳מבו נגיש (60px)' },
              ].map((t) => (
                <button
                  key={t.size}
                  type="button"
                  onClick={() => setTouchSize(t.size as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    touchSize === t.size
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>

          {/* Switches (Sound, Haptic, Simplified POS) */}
          <div className="space-y-3 pt-2 border-t border-slate-100">
            <label className="flex items-center justify-between p-3 rounded-2xl bg-slate-50 cursor-pointer">
              <span className="text-xs font-bold text-slate-800">
                משוב קולי וצלילי סריקה (Web Audio API)
              </span>
              <input
                type="checkbox"
                checked={soundEnabled}
                onChange={(e) => setSoundEnabled(e.target.checked)}
                className="w-4 h-4 accent-sky-600 rounded"
              />
            </label>

            <label className="flex items-center justify-between p-3 rounded-2xl bg-slate-50 cursor-pointer">
              <span className="text-xs font-bold text-slate-800">
                משוב רטט במסכי מגע (Haptic Feedback)
              </span>
              <input
                type="checkbox"
                checked={hapticEnabled}
                onChange={(e) => setHapticEnabled(e.target.checked)}
                className="w-4 h-4 accent-sky-600 rounded"
              />
            </label>

            <label className="flex items-center justify-between p-3 rounded-2xl bg-slate-50 cursor-pointer">
              <span className="text-xs font-bold text-slate-800">
                מצב קופה מופשט (Simplified POS Mode)
              </span>
              <input
                type="checkbox"
                checked={simplifiedPos}
                onChange={(e) => setSimplifiedPos(e.target.checked)}
                className="w-4 h-4 accent-sky-600 rounded"
              />
            </label>
          </div>
        </div>
      )}

      {/* 4. THEMES & LOCALIZATION TAB */}
      {activeTab === 'theme' && (
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-6">
          <div>
            <h3 className="text-sm font-black text-slate-900">
              ערכות נושא, פלטות צבעים ושפות (Themes & i18n)
            </h3>
            <p className="text-xs text-slate-500">
              תמיכה מלאה ב-9 שפות עם כיווניות RTL/LTR אוטומטית ומגוון פלטות צבעים
            </p>
          </div>

          {/* Supported Languages */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              שפת הממשק (9 שפות נתמכות)
            </label>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
              {[
                { code: 'he', name: 'עברית (RTL)' },
                { code: 'en', name: 'English (US)' },
                { code: 'es', name: 'Español' },
                { code: 'pt', name: 'Português' },
                { code: 'fr', name: 'Français' },
                { code: 'de', name: 'Deutsch' },
                { code: 'zh', name: '中文 (Simplified)' },
                { code: 'ja', name: '日本語' },
                { code: 'ar', name: 'العربية (RTL)' },
              ].map((lang) => (
                <button
                  key={lang.code}
                  type="button"
                  onClick={() => setLanguage(lang.code as SupportedLanguage)}
                  className={`p-3 rounded-2xl border font-bold text-xs flex items-center justify-between transition-all ${
                    language === lang.code
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  <span>{lang.name}</span>
                  {language === lang.code && <CheckCircle2 className="w-4 h-4 text-sky-600" />}
                </button>
              ))}
            </div>
          </div>

          {/* Display Mode */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              מצב תצוגה
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { mode: 'light', label: 'מצב בהיר (Light)' },
                { mode: 'dark', label: 'מצב כהה (Dark)' },
                { mode: 'system', label: 'סנכרן עם מערכת ההפעלה' },
              ].map((m) => (
                <button
                  key={m.mode}
                  type="button"
                  onClick={() => setMode(m.mode as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    mode === m.mode
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          {/* Color Palettes */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              פלטת צבעים מותאמת
            </label>
            <div className="grid grid-cols-2 md:grid-cols-6 gap-2">
              {[
                { id: 'blue', label: 'כחול מודרני', hex: '#0288D1' },
                { id: 'emerald', label: 'ברקת רענן', hex: '#10B981' },
                { id: 'purple', label: 'סגול רויאל', hex: '#8B5CF6' },
                { id: 'amber', label: 'כתום קפה', hex: '#F59E0B' },
                { id: 'red', label: 'אדום ביסטרו', hex: '#EF4444' },
                { id: 'midnight', label: 'שחור חצות', hex: '#0F172A' },
              ].map((pal) => (
                <button
                  key={pal.id}
                  type="button"
                  onClick={() => setPalette(pal.id as ColorTheme)}
                  className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-2 transition-all ${
                    palette === pal.id
                      ? 'border-slate-900 bg-slate-50 ring-2 ring-slate-900/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  <div
                    style={{ backgroundColor: pal.hex }}
                    className="w-6 h-6 rounded-full shadow-inner"
                  />
                  <span>{pal.label}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 5. SECURITY & OWNER AUTH */}
      {activeTab === 'security' && (
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <h3 className="text-sm font-black text-slate-900">אבטחה וקוד בעל עסק</h3>
          <p className="text-xs text-slate-500">
            פעולות רגישות (איפוס נתונים, עריכת שיעורי מס) דורשות אישור PIN בעל עסק.
          </p>

          <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs font-semibold flex items-center justify-between">
            <div>
              <p>קוד ה-PIN הנוכחי של בעל העסק הוא: <strong>{formData.owner_pin || '1234'}</strong></p>
              <p className="text-[11px] text-amber-700 mt-0.5">ניתן לעדכן אותו בכל עת.</p>
            </div>
            <button
              onClick={() => setIsOwnerModalOpen(true)}
              className="py-2 px-4 rounded-xl bg-amber-600 text-white font-bold text-xs"
            >
              בדוק אישור בעלים
            </button>
          </div>
        </div>
      )}

      {/* Owner Auth Modal Test */}
      <OwnerAuthModal
        isOpen={isOwnerModalOpen}
        onApproved={() => alert('אישור בעל עסק אומת בהצלחה!')}
        onClose={() => setIsOwnerModalOpen(false)}
      />
    </div>
  )
}
