'use client'

import React, { useState, useEffect } from 'react'
import {
  Boxes,
  AlertTriangle,
  History,
  Plus,
  Minus,
  Search,
  CheckCircle2,
  XCircle,
  ShieldCheck,
  ShieldAlert,
  ArrowUpDown,
  RefreshCw,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { getTranslation } from '@/lib/i18n'
import { Item, StockMovement, StockMovementReason, getLocalizedName, matchItemSearch } from '@/lib/types'
import { idbGetAll, idbPut } from '@/lib/db/idb'
import { syncEngine } from '@/lib/db/sync'

export default function InventoryControlPage() {
  const { items, settings, currentStaff, updateSettings, updateItemInCatalog, refreshData } =
    usePos()
  const { language } = useTheme()
  const t = getTranslation(language)

  const [activeTab, setActiveTab] = useState<'levels' | 'movements'>('levels')
  const [search, setSearch] = useState('')
  const [movements, setMovements] = useState<StockMovement[]>([])

  // Quick Adjustment Modal State
  const [adjustingItem, setAdjustingItem] = useState<Item | null>(null)
  const [adjustmentAmount, setAdjustmentAmount] = useState<number>(0)
  const [adjustmentReason, setAdjustmentReason] = useState<StockMovementReason>('ADJUSTMENT')
  const [adjustmentNote, setAdjustmentNote] = useState<string>('')

  useEffect(() => {
    async function loadMovements() {
      const data = await idbGetAll<StockMovement>('stock_movements')
      setMovements(data.reverse())
    }
    loadMovements()
  }, [])

  const filteredItems = items.filter((i) => matchItemSearch(i, search, language))

  async function handleToggleNegativeStock() {
    await updateSettings({
      ...settings,
      allow_negative_stock: !settings.allow_negative_stock,
    })
  }

  async function handleConfirmAdjustment() {
    if (!adjustingItem || adjustmentAmount === 0) return

    const prev = adjustingItem.in_stock
    const newStock = prev + adjustmentAmount

    const updatedItem: Item = {
      ...adjustingItem,
      in_stock: newStock,
    }
    await updateItemInCatalog(updatedItem)

    const mov: StockMovement = {
      id: `mov_adj_${Date.now()}_${adjustingItem.id}`,
      item_id: adjustingItem.id,
      item_name: adjustingItem.name,
      reason: adjustmentReason,
      previous_stock: prev,
      quantity_changed: adjustmentAmount,
      new_stock: newStock,
      staff_name: currentStaff.name,
      timestamp: new Date().toISOString(),
      note: adjustmentNote || undefined,
    }
    await idbPut('stock_movements', mov)
    await syncEngine.enqueue('STOCK_MOVEMENT', mov)

    setMovements([mov, ...movements])
    setAdjustingItem(null)
    setAdjustmentAmount(0)
    setAdjustmentNote('')
    await refreshData()
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            {t.inventoryManagement}
          </h2>
          <p className="text-xs text-slate-500">
            {t.inventoryOverview}
          </p>
        </div>

        {/* Negative Stock Toggle */}
        <div className="flex items-center gap-3 bg-white px-4 py-2 rounded-2xl border border-slate-200 shadow-sm">
          <div className="text-start">
            <span className="text-xs font-bold text-slate-800 block">
              {t.allowNegativeStock}
            </span>
            <span className="text-[10px] text-slate-400">
              {settings.allow_negative_stock ? t.yes : t.no}
            </span>
          </div>
          <button
            onClick={handleToggleNegativeStock}
            className={`w-12 h-6 rounded-full transition-colors relative p-0.5 ${
              settings.allow_negative_stock ? 'bg-emerald-500' : 'bg-slate-300'
            }`}
          >
            <div
              className={`w-5 h-5 rounded-full bg-white transition-transform ${
                settings.allow_negative_stock ? 'ltr:translate-x-6 rtl:-translate-x-6' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200">
        <button
          onClick={() => setActiveTab('levels')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${
            activeTab === 'levels'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Boxes className="w-4 h-4" />
          <span>{t.stockLevel} ({items.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('movements')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${
            activeTab === 'movements'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <History className="w-4 h-4" />
          <span>{t.movementHistory} ({movements.length})</span>
        </button>
      </div>

      {/* TAB 1: STOCK LEVELS */}
      {activeTab === 'levels' && (
        <div className="space-y-4">
          <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
            <Search className="w-4 h-4 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t.searchPlaceholder}
              className="flex-1 text-xs bg-transparent focus:outline-none text-slate-800"
            />
          </div>

          <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-start text-xs">
                <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-200">
                  <tr>
                    <th className="p-4">{t.productName}</th>
                    <th className="p-4">{t.sku}</th>
                    <th className="p-4">{t.trackStock}</th>
                    <th className="p-4">{t.stockLevel}</th>
                    <th className="p-4">{t.lowStockThreshold}</th>
                    <th className="p-4">{t.status}</th>
                    <th className="p-4 ltr:text-right rtl:text-left">{t.adjustStock}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredItems.map((item) => {
                    const isLow = item.track_stock && item.in_stock <= item.low_stock_threshold
                    const isOut = item.track_stock && item.in_stock <= 0

                    return (
                      <tr key={item.id} className="hover:bg-slate-50/80">
                        <td className="p-4 font-black text-slate-900">{getLocalizedName(item, language)}</td>
                        <td className="p-4 font-mono text-slate-500">{item.sku || '-'}</td>
                        <td className="p-4">
                          {item.track_stock ? (
                            <span className="text-emerald-600 font-bold">{t.yes}</span>
                          ) : (
                            <span className="text-slate-400">{t.no}</span>
                          )}
                        </td>
                        <td className="p-4 font-black text-sm text-slate-900">
                          {item.in_stock} {item.unit_of_measure}
                        </td>
                        <td className="p-4 text-slate-500 font-semibold">
                          {item.low_stock_threshold} {item.unit_of_measure}
                        </td>
                        <td className="p-4">
                          {isOut ? (
                            <span className="px-2.5 py-0.5 rounded-full bg-red-100 text-red-700 font-bold text-[10px]">
                              {t.damageLoss}
                            </span>
                          ) : isLow ? (
                            <span className="px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-700 font-bold text-[10px]">
                              {t.lowStock}
                            </span>
                          ) : (
                            <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700 font-bold text-[10px]">
                              {t.success}
                            </span>
                          )}
                        </td>
                        <td className="p-4 ltr:text-right rtl:text-left">
                          <button
                            onClick={() => {
                              setAdjustingItem(item)
                              setAdjustmentAmount(0)
                            }}
                            className="px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold text-xs"
                          >
                            {t.adjustStock} (+/-)
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: STOCK MOVEMENTS JOURNAL */}
      {activeTab === 'movements' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-start text-xs">
              <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-200">
                <tr>
                  <th className="p-4">{t.dateTime}</th>
                  <th className="p-4">{t.productName}</th>
                  <th className="p-4">{t.reason}</th>
                  <th className="p-4">{t.previousStock}</th>
                  <th className="p-4">{t.quantityChanged}</th>
                  <th className="p-4">{t.newStock}</th>
                  <th className="p-4">{t.customer}</th>
                  <th className="p-4">{t.customNotes}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {movements.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="text-center py-12 text-slate-400">
                      {t.emptyCart}
                    </td>
                  </tr>
                ) : (
                  movements.map((mov) => (
                    <tr key={mov.id} className="hover:bg-slate-50/80">
                      <td className="p-4 text-slate-500">
                        {new Date(mov.timestamp).toLocaleDateString()} •{' '}
                        {new Date(mov.timestamp).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </td>
                      <td className="p-4 font-black text-slate-900">{mov.item_name}</td>
                      <td className="p-4">
                        <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 font-bold text-[10px]">
                          {mov.reason}
                        </span>
                      </td>
                      <td className="p-4 text-slate-600">{mov.previous_stock}</td>
                      <td className="p-4 font-black">
                        <span
                          className={mov.quantity_changed >= 0 ? 'text-emerald-600' : 'text-red-600'}
                        >
                          {mov.quantity_changed >= 0 ? `+${mov.quantity_changed}` : mov.quantity_changed}
                        </span>
                      </td>
                      <td className="p-4 font-black text-slate-900">{mov.new_stock}</td>
                      <td className="p-4 text-slate-700 font-medium">{mov.staff_name}</td>
                      <td className="p-4 text-slate-500 font-mono text-[11px]">
                        {mov.reference || mov.note || '-'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* QUICK ADJUSTMENT MODAL */}
      {adjustingItem && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-900">
              {t.adjustStock}: {adjustingItem.name}
            </h3>
            <p className="text-xs text-slate-500">
              {t.stockLevel}: <strong>{adjustingItem.in_stock}</strong> {adjustingItem.unit_of_measure}
            </p>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.quantityChanged} (+ / -):
                </label>
                <input
                  type="number"
                  value={adjustmentAmount}
                  onChange={(e) => setAdjustmentAmount(Number(e.target.value))}
                  placeholder="10 / -5"
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-sm font-black bg-slate-50 text-slate-900"
                />
                <span className="text-xs text-sky-600 font-bold mt-1 block">
                  {t.newStock}: {adjustingItem.in_stock + adjustmentAmount}
                </span>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.reason}:
                </label>
                <select
                  value={adjustmentReason}
                  onChange={(e) => setAdjustmentReason(e.target.value as any)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800 font-bold"
                >
                  <option value="PURCHASE">{t.restockMovement}</option>
                  <option value="ADJUSTMENT">{t.adjustStock}</option>
                  <option value="DAMAGED">{t.damageLoss}</option>
                  <option value="STOCK_COUNT">{t.physicalAudit}</option>
                  <option value="TRANSFER">{t.filter}</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.customNotes}:
                </label>
                <input
                  type="text"
                  value={adjustmentNote}
                  onChange={(e) => setAdjustmentNote(e.target.value)}
                  placeholder="..."
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setAdjustingItem(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                {t.cancel}
              </button>
              <button
                type="button"
                onClick={handleConfirmAdjustment}
                disabled={adjustmentAmount === 0}
                className="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-md shadow-sky-600/20 disabled:opacity-50"
              >
                {t.save}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
