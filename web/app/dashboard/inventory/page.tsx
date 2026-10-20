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
  Truck,
  PackagePlus,
  FileText,
  DollarSign,
  User,
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

  // Add Supply / Receive Inventory Modal State
  const [isSupplyModalOpen, setIsSupplyModalOpen] = useState(false)
  const [supplyItemId, setSupplyItemId] = useState<number | null>(null)
  const [supplyQuantity, setSupplyQuantity] = useState<number>(10)
  const [supplyCost, setSupplyCost] = useState<string>('')
  const [supplySupplier, setSupplySupplier] = useState<string>('')
  const [supplyInvoice, setSupplyInvoice] = useState<string>('')
  const [supplyNote, setSupplyNote] = useState<string>('')

  useEffect(() => {
    async function loadMovements() {
      const data = await idbGetAll<StockMovement>('stock_movements')
      setMovements(data.reverse())
    }
    loadMovements()
  }, [])

  const filteredItems = items.filter((i) => matchItemSearch(i, search, language))
  const selectedSupplyItem = items.find((i) => i.id === supplyItemId) || items[0] || null

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

  async function handleConfirmSupply() {
    if (!selectedSupplyItem || supplyQuantity <= 0) return

    const prev = selectedSupplyItem.in_stock
    const newStock = prev + supplyQuantity
    const parsedCost = supplyCost !== '' ? parseFloat(supplyCost) : undefined

    const updatedItem: Item = {
      ...selectedSupplyItem,
      in_stock: newStock,
      cost: parsedCost !== undefined && !isNaN(parsedCost) && parsedCost >= 0 ? parsedCost : selectedSupplyItem.cost,
    }
    await updateItemInCatalog(updatedItem)

    const noteParts: string[] = []
    if (supplySupplier.trim()) noteParts.push(`Supplier: ${supplySupplier.trim()}`)
    if (supplyInvoice.trim()) noteParts.push(`Invoice: ${supplyInvoice.trim()}`)
    if (supplyNote.trim()) noteParts.push(supplyNote.trim())

    const mov: StockMovement = {
      id: `mov_sup_${Date.now()}_${selectedSupplyItem.id}`,
      item_id: selectedSupplyItem.id,
      item_name: selectedSupplyItem.name,
      reason: 'PURCHASE',
      previous_stock: prev,
      quantity_changed: supplyQuantity,
      new_stock: newStock,
      staff_name: currentStaff.name,
      timestamp: new Date().toISOString(),
      reference: supplyInvoice.trim() || undefined,
      note: noteParts.join(' | ') || undefined,
    }
    await idbPut('stock_movements', mov)
    await syncEngine.enqueue('STOCK_MOVEMENT', mov)

    setMovements([mov, ...movements])
    setIsSupplyModalOpen(false)
    setSupplyQuantity(10)
    setSupplyCost('')
    setSupplySupplier('')
    setSupplyInvoice('')
    setSupplyNote('')
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

        {/* Header Actions: Add Supply Button & Negative Stock Toggle */}
        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={() => {
              if (items.length > 0) {
                setSupplyItemId(items[0].id)
                setSupplyCost(items[0].cost?.toString() || '')
              }
              setIsSupplyModalOpen(true)
            }}
            className="px-4 py-2.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs flex items-center gap-2 shadow-md shadow-emerald-600/25 transition-all"
            title="Receive Inventory Supply"
          >
            <PackagePlus className="w-4 h-4" />
            <span>{t.restockMovement}</span>
          </button>

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
              className={`w-12 h-6 rounded-full transition-colors relative p-0.5 ${settings.allow_negative_stock ? 'bg-emerald-500' : 'bg-slate-300'
                }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white transition-transform ${settings.allow_negative_stock ? 'ltr:translate-x-6 rtl:-translate-x-6' : 'translate-x-0'
                  }`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200">
        <button
          onClick={() => setActiveTab('levels')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${activeTab === 'levels'
            ? 'border-sky-600 text-sky-600'
            : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
        >
          <Boxes className="w-4 h-4" />
          <span>{t.stockLevel} ({items.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('movements')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${activeTab === 'movements'
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
                    <th className="p-4 ltr:text-right rtl:text-left">{t.actions}</th>
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
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => {
                                setSupplyItemId(item.id)
                                setSupplyCost(item.cost?.toString() || '')
                                setIsSupplyModalOpen(true)
                              }}
                              className="px-3 py-1.5 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 font-bold text-xs flex items-center gap-1 transition-colors"
                              title="Add Supply / Receive Items"
                            >
                              <Plus className="w-3.5 h-3.5" />
                              <span>{t.restockMovement.split('/')[0].trim()}</span>
                            </button>
                            <button
                              onClick={() => {
                                setAdjustingItem(item)
                                setAdjustmentAmount(0)
                              }}
                              className="px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold text-xs"
                            >
                              {t.adjustStock} (+/-)
                            </button>
                          </div>
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

      {/* DEDICATED ADD SUPPLY / RECEIVE INVENTORY MODAL */}
      {isSupplyModalOpen && selectedSupplyItem && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-2xl bg-emerald-100 text-emerald-700">
                <PackagePlus className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-black text-slate-900">
                  {t.restockMovement} (Add Supply)
                </h3>
                <p className="text-xs text-slate-500">
                  Record incoming inventory delivery from supplier
                </p>
              </div>
            </div>

            <div className="space-y-3.5">
              {/* Product Selector */}
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.productName}:
                </label>
                <select
                  value={selectedSupplyItem.id}
                  onChange={(e) => {
                    const id = Number(e.target.value)
                    setSupplyItemId(id)
                    const found = items.find((i) => i.id === id)
                    if (found) setSupplyCost(found.cost?.toString() || '')
                  }}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900 font-bold"
                >
                  {items.map((item) => (
                    <option key={item.id} value={item.id}>
                      {getLocalizedName(item, language)} (Current: {item.in_stock} {item.unit_of_measure}) - SKU: {item.sku || 'N/A'}
                    </option>
                  ))}
                </select>
              </div>

              {/* Quantity Added */}
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  Supply Quantity Received (+):
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={supplyQuantity}
                    onChange={(e) => setSupplyQuantity(Math.max(1, Number(e.target.value)))}
                    className="flex-1 p-2.5 rounded-xl border border-slate-200 text-sm font-black bg-slate-50 text-slate-900"
                    placeholder="e.g. 50"
                  />
                  <div className="flex gap-1.5">
                    {[10, 25, 50, 100].map((preset) => (
                      <button
                        key={preset}
                        type="button"
                        onClick={() => setSupplyQuantity(preset)}
                        className={`px-2.5 py-2 rounded-xl text-xs font-bold border transition-colors ${supplyQuantity === preset
                          ? 'bg-emerald-600 text-white border-emerald-600'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                          }`}
                      >
                        +{preset}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Unit Purchase Cost & Supplier */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.cost} (Unit Cost):
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={supplyCost}
                    onChange={(e) => setSupplyCost(e.target.value)}
                    placeholder={selectedSupplyItem.cost?.toString() || '0.00'}
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800 font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    Supplier / Vendor:
                  </label>
                  <input
                    type="text"
                    value={supplySupplier}
                    onChange={(e) => setSupplySupplier(e.target.value)}
                    placeholder="e.g. Acme Supplies Ltd."
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800"
                  />
                </div>
              </div>

              {/* Invoice Number & Notes */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    Invoice / Order #:
                  </label>
                  <input
                    type="text"
                    value={supplyInvoice}
                    onChange={(e) => setSupplyInvoice(e.target.value)}
                    placeholder="e.g. PO-89201"
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.customNotes}:
                  </label>
                  <input
                    type="text"
                    value={supplyNote}
                    onChange={(e) => setSupplyNote(e.target.value)}
                    placeholder="e.g. Batch #4"
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800"
                  />
                </div>
              </div>

              {/* Stock Preview Calculation Box */}
              <div className="p-3.5 rounded-2xl bg-emerald-50/80 border border-emerald-200/80 flex items-center justify-between text-xs">
                <div>
                  <span className="text-slate-500 block">Current Stock:</span>
                  <span className="font-black text-slate-800 text-sm">
                    {selectedSupplyItem.in_stock} {selectedSupplyItem.unit_of_measure}
                  </span>
                </div>
                <div className="text-emerald-700 font-black text-base">+</div>
                <div>
                  <span className="text-emerald-700 font-bold block">Added Supply:</span>
                  <span className="font-black text-emerald-800 text-sm">
                    +{supplyQuantity} {selectedSupplyItem.unit_of_measure}
                  </span>
                </div>
                <div className="text-emerald-700 font-black text-base">=</div>
                <div>
                  <span className="text-sky-700 font-bold block">{t.newStock}:</span>
                  <span className="font-black text-sky-900 text-sm">
                    {selectedSupplyItem.in_stock + supplyQuantity} {selectedSupplyItem.unit_of_measure}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsSupplyModalOpen(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                {t.cancel}
              </button>
              <button
                type="button"
                onClick={handleConfirmSupply}
                disabled={supplyQuantity <= 0}
                className="px-6 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-md shadow-emerald-600/20 disabled:opacity-50"
              >
                Confirm Supply & Restock
              </button>
            </div>
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
