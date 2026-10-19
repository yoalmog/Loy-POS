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

  const {
    mode,
    setMode,
    palette,
    setPalette,
    customHex,
    setCustomHex,
    useCustomHex,
    setUseCustomHex,
    catalogBg,
    setCatalogBg,
    cardAppearance,
    setCardAppearance,
    language,
    setLanguage,
    t,
    restoreDefaults,
  } = useTheme()

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
    setTestPrintStatus(`${t.testPrint} (${target})...`)
    try {
      const printers = await printerManager.getPrinters()
      const printer = printers.find((p) => p.type === target) || printers[0]
      if (printer) {
        printerManager.printBrowserHtmlReceipt(
          {
            id: 'test_receipt',
            receipt_number: 'TEST-001',
            created_at: new Date().toISOString(),
            cashier_name: t.appName,
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
                name: t.testPrint,
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
        setTestPrintStatus(t.success)
      }
    } catch (err: any) {
      setTestPrintStatus(`${t.error}: ${err.message}`)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            {t.storeSettings}
          </h2>
          <p className="text-xs text-slate-500">
            {t.storeProfile} • {t.printers} • {t.accessibility} • i18n
          </p>
        </div>

        {isSaved && (
          <div className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold animate-fade-in">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{t.success}!</span>
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
          <span>{t.storeProfile}</span>
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
          <span>{t.printers}</span>
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
          <span>{t.accessibility}</span>
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
          <span>{t.settings} ({language.toUpperCase()})</span>
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
          <span>{t.ownerAuthRequired}</span>
        </button>
      </div>

      {/* 1. STORE PROFILE TAB */}
      {activeTab === 'store' && (
        <form onSubmit={handleSaveSettings} className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <h3 className="text-sm font-black text-slate-900 mb-2">{t.storeProfile}</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                {t.storeName} *
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
                {t.storePhone}
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
                {t.storeAddress}
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
                {t.storeEmail}
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
                {t.currencySymbol}
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
                {t.taxRatePercent} (%)
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
                {t.tax}
              </label>
              <select
                value={formData.tax_included ? 'true' : 'false'}
                onChange={(e) =>
                  setFormData({ ...formData, tax_included: e.target.value === 'true' })
                }
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900 font-bold"
              >
                <option value="true">{t.yes}</option>
                <option value="false">{t.no}</option>
              </select>
            </div>
          </div>

          <div className="space-y-3 pt-2">
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                {t.receiptHeader}
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
                {t.receiptFooter}
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
                {t.returnPolicy}
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
              <span>{t.save}</span>
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
                {t.printers}
              </h3>
              <p className="text-xs text-slate-500">
                {t.cashierPrinter} • {t.kitchenPrinter} • {t.barPrinter}
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
                <span className="font-black text-xs text-slate-900">{t.cashierPrinter}</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  {t.success}
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                {t.receiptPreview} (80mm)
              </p>
              <button
                onClick={() => handleTestPrint('CASHIER')}
                className="w-full py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700"
              >
                {t.testPrint}
              </button>
            </div>

            {/* Kitchen Printer */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-black text-xs text-slate-900">{t.kitchenPrinter}</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  {t.success}
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                {t.kitchenOrder} (LAN 9100)
              </p>
              <button
                onClick={() => handleTestPrint('KITCHEN')}
                className="w-full py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700"
              >
                {t.testPrint}
              </button>
            </div>

            {/* Bar Printer */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-black text-xs text-slate-900">{t.barPrinter}</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  {t.success}
                </span>
              </div>
              <p className="text-[11px] text-slate-500">
                {t.bar} (BLE 58mm)
              </p>
              <button
                onClick={() => handleTestPrint('BAR')}
                className="w-full py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700"
              >
                {t.testPrint}
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
              {t.accessibility} (WCAG 2.1)
            </h3>
            <p className="text-xs text-slate-500">
              {t.textScale} • {t.contrast} • {t.touchTarget}
            </p>
          </div>

          {/* Text Scaling */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              {t.textScale}
            </label>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {[
                { scale: '1.0', label: '1.0x' },
                { scale: '1.15', label: '1.15x' },
                { scale: '1.30', label: '1.30x' },
                { scale: '1.50', label: '1.50x' },
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
              {t.contrast}
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { mode: 'normal', label: t.standardTier },
                { mode: 'high', label: t.silverTier },
                { mode: 'ultra', label: t.goldTier },
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
              {t.colorblind}
            </label>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-2">
              {[
                { mode: 'none', label: t.all },
                { mode: 'protanopia', label: 'Protanopia' },
                { mode: 'deuteranopia', label: 'Deuteranopia' },
                { mode: 'tritanopia', label: 'Tritanopia' },
                { mode: 'monochrome', label: 'Monochrome' },
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
              {t.touchTarget}
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { size: 'standard', label: '44px' },
                { size: 'large', label: '52px' },
                { size: 'jumbo', label: '60px' },
              ].map((tItem) => (
                <button
                  key={tItem.size}
                  type="button"
                  onClick={() => setTouchSize(tItem.size as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    touchSize === tItem.size
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {tItem.label}
                </button>
              ))}
            </div>
          </div>

          {/* Switches (Sound, Haptic, Simplified POS) */}
          <div className="space-y-3 pt-2 border-t border-slate-100">
            <label className="flex items-center justify-between p-3 rounded-2xl bg-slate-50 cursor-pointer">
              <span className="text-xs font-bold text-slate-800">
                {t.soundEffects}
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
                {t.hapticFeedback}
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
                {t.simplifiedPos}
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
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-black text-slate-900">
                {t.settings} • {t.theme}
              </h3>
              <p className="text-xs text-slate-500">
                i18n • RTL / LTR • {language.toUpperCase()}
              </p>
            </div>
            <button
              type="button"
              onClick={restoreDefaults}
              className="px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700 transition-colors"
            >
              Restaurar Apariencia por Defecto
            </button>
          </div>

          {/* Supported Languages */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              {t.language} (Español Predeterminado)
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
              {[
                { code: 'es', name: 'Español (Predeterminado)' },
                { code: 'en', name: 'English (US)' },
                { code: 'he', name: 'עברית (RTL)' },
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
                      : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <span>{lang.name}</span>
                  {language === lang.code && <CheckCircle2 className="w-4 h-4 text-sky-600" />}
                </button>
              ))}
            </div>
          </div>

          {/* Display Mode (Light / Dark / System) */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              {t.displayMode}
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { mode: 'light', label: t.lightMode },
                { mode: 'dark', label: t.darkMode },
                { mode: 'system', label: t.systemMode },
              ].map((m) => (
                <button
                  key={m.mode}
                  type="button"
                  onClick={() => setMode(m.mode as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs transition-all ${
                    mode === m.mode
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
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
              {t.colorPalettes}
            </label>
            <div className="grid grid-cols-2 md:grid-cols-6 gap-2">
              {[
                { id: 'blue', label: 'Blue POS', hex: '#0288D1' },
                { id: 'emerald', label: 'Emerald', hex: '#10B981' },
                { id: 'purple', label: 'Purple', hex: '#8B5CF6' },
                { id: 'amber', label: 'Amber', hex: '#F59E0B' },
                { id: 'red', label: 'Crimson', hex: '#EF4444' },
                { id: 'midnight', label: 'Midnight', hex: '#0F172A' },
              ].map((pal) => (
                <button
                  key={pal.id}
                  type="button"
                  onClick={() => {
                    setPalette(pal.id as ColorTheme)
                    setUseCustomHex(false)
                  }}
                  className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-2 transition-all ${
                    palette === pal.id && !useCustomHex
                      ? 'border-slate-900 bg-slate-50 ring-2 ring-slate-900/20'
                      : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
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

          {/* Custom Accent Color */}
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs font-black text-slate-900 block">Color de Énfasis Personalizado (Custom Color)</span>
                <span className="text-[11px] text-slate-500">Aplica cualquier código HEX como color primario del sistema</span>
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <span className="text-xs font-bold text-slate-700">Activar</span>
                <input
                  type="checkbox"
                  checked={useCustomHex}
                  onChange={(e) => setUseCustomHex(e.target.checked)}
                  className="w-4 h-4 accent-sky-600 rounded"
                />
              </label>
            </div>
            {useCustomHex && (
              <div className="flex items-center gap-3 pt-2">
                <input
                  type="color"
                  value={customHex}
                  onChange={(e) => setCustomHex(e.target.value)}
                  className="w-12 h-10 rounded-xl cursor-pointer border border-slate-300"
                />
                <input
                  type="text"
                  value={customHex}
                  onChange={(e) => setCustomHex(e.target.value)}
                  placeholder="#0288D1"
                  className="w-32 p-2 rounded-xl border border-slate-300 text-xs font-mono font-bold uppercase"
                />
                <div
                  style={{ backgroundColor: customHex }}
                  className="px-3 py-1.5 rounded-xl text-white text-xs font-bold shadow-sm"
                >
                  Muestra
                </div>
              </div>
            )}
          </div>

          {/* Cashier Catalog Background */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              Fondo del Catálogo de la Caja (Catalog Background)
            </label>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {[
                { id: 'default', label: 'Predeterminado (Slate 100)', desc: 'Gris neutro suave' },
                { id: 'subtle', label: 'Sutil (Slate 50)', desc: 'Claridad máxima' },
                { id: 'slate', label: 'Pizarra (Slate 200)', desc: 'Mayor contraste' },
                { id: 'pure', label: 'Puro (White)', desc: 'Blanco limpio' },
              ].map((bg) => (
                <button
                  key={bg.id}
                  type="button"
                  onClick={() => setCatalogBg(bg.id as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs text-start transition-all ${
                    catalogBg === bg.id
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <span className="block text-xs font-black">{bg.label}</span>
                  <span className="text-[10px] text-slate-400 font-normal">{bg.desc}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Card Appearance */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
              Apariencia de Tarjetas de Productos (Card Appearance)
            </label>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {[
                { id: 'default', label: 'Clásico (Default)', desc: 'Bordes suaves con sombra ligera' },
                { id: 'compact', label: 'Compacto (Compact)', desc: 'Espaciado optimizado' },
                { id: 'bordered', label: 'Borde Sólido (Bordered)', desc: 'Contorno de alto contraste' },
                { id: 'elevated', label: 'Elevado (Elevated)', desc: 'Esquinas redondeadas y sombra flotante' },
              ].map((card) => (
                <button
                  key={card.id}
                  type="button"
                  onClick={() => setCardAppearance(card.id as any)}
                  className={`p-3 rounded-2xl border font-bold text-xs text-start transition-all ${
                    cardAppearance === card.id
                      ? 'border-sky-600 bg-sky-50 text-sky-900 ring-2 ring-sky-600/20'
                      : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <span className="block text-xs font-black">{card.label}</span>
                  <span className="text-[10px] text-slate-400 font-normal">{card.desc}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Live Appearance Preview */}
          <div className="p-5 rounded-3xl border border-slate-200 bg-slate-50 space-y-3">
            <span className="text-xs font-extrabold uppercase text-slate-500 block">
              Vista Previa en Vivo (Live Appearance Preview)
            </span>
            <div className={`p-4 rounded-2xl border border-slate-200 flex flex-wrap items-center gap-4 pos-catalog-container`}>
              <div className="w-48 bg-white p-3 rounded-xl border border-slate-200 shadow-sm pos-product-card space-y-2">
                <div
                  style={{ backgroundColor: useCustomHex ? customHex : undefined }}
                  className="w-full h-16 rounded-lg bg-sky-600 flex items-center justify-center text-white font-black text-sm"
                >
                  Hamburguesa
                </div>
                <div>
                  <h4 className="text-xs font-black text-slate-900">Hamburguesa Clásica</h4>
                  <span className="text-xs font-extrabold text-sky-600">S/ 58.00</span>
                </div>
              </div>
              <div className="space-y-2">
                <button
                  style={{ backgroundColor: useCustomHex ? customHex : undefined }}
                  className="px-4 py-2 rounded-xl bg-sky-600 text-white font-bold text-xs shadow-sm"
                >
                  Botón Primario POS
                </button>
                <p className="text-[11px] text-slate-500">
                  Paleta activa: <strong>{useCustomHex ? `Personalizado (${customHex})` : palette}</strong> • Tarjeta: <strong>{cardAppearance}</strong> • Fondo: <strong>{catalogBg}</strong>
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 5. SECURITY & OWNER AUTH */}
      {activeTab === 'security' && (
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <h3 className="text-sm font-black text-slate-900">{t.ownerAuthRequired}</h3>
          <p className="text-xs text-slate-500">
            {t.enterPin}
          </p>

          <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs font-semibold flex items-center justify-between">
            <div>
              <p>PIN: <strong>•••• ({formData.owner_pin || '1234'})</strong></p>
            </div>
            <button
              onClick={() => setIsOwnerModalOpen(true)}
              className="py-2 px-4 rounded-xl bg-amber-600 text-white font-bold text-xs"
            >
              {t.confirm} PIN
            </button>
          </div>
        </div>
      )}

      {/* Owner Auth Modal Test */}
      <OwnerAuthModal
        isOpen={isOwnerModalOpen}
        onApproved={() => alert(t.success)}
        onClose={() => setIsOwnerModalOpen(false)}
      />
    </div>
  )
}
