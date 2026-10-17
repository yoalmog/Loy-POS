'use client'

import React, { useEffect, useState } from 'react'
import {
  Boxes,
  AlertTriangle,
  PlusCircle,
  RefreshCw,
  Search,
  CheckCircle2,
  Package,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Item } from '@/lib/types'

export default function InventoryPage() {
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedItem, setSelectedItem] = useState<Item | null>(null)
  const [addedStock, setAddedStock] = useState(10)
  const [updating, setUpdating] = useState(false)

  async function fetchInventory() {
    setLoading(true)
    try {
      const { data, error } = await supabase.from('items').select('*').order('in_stock', { ascending: true })
      if (data) setItems(data)
    } catch (err) {
      console.error('Failed to load inventory:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchInventory()
  }, [])

  async function handleRestockStock() {
    if (!selectedItem) return
    setUpdating(true)
    try {
      const newStock = Number(selectedItem.in_stock) + Number(addedStock)
      const { error } = await supabase
        .from('items')
        .update({ in_stock: newStock, updated_at: new Date().toISOString() })
        .eq('id', selectedItem.id)

      if (error) throw error

      await supabase.from('products').update({ stock_level: Math.round(newStock) }).eq('id', selectedItem.id)

      setSelectedItem(null)
      fetchInventory()
    } catch (err: any) {
      alert(`Restock failed: ${err.message}`)
    } finally {
      setUpdating(false)
    }
  }

  const lowStockItems = items.filter((i) => i.track_stock && i.in_stock <= i.low_stock_threshold)
  const filteredItems = items.filter((i) => i.name.toLowerCase().includes(searchQuery.toLowerCase()))

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Inventory & Stock Tracking</h2>
          <p className="text-sm text-slate-500 mt-1">Real-time stock level monitoring and restock adjustment</p>
        </div>
        <button
          onClick={fetchInventory}
          className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-xl text-sm font-medium shrink-0"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh Stock
        </button>
      </div>

      {/* Low Stock Alert Box */}
      {lowStockItems.length > 0 && (
        <div className="p-5 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500 text-white flex items-center justify-center font-bold shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-base">{lowStockItems.length} Products Running Low on Stock</h4>
              <p className="text-xs text-amber-700 mt-0.5">
                {lowStockItems.map((i) => i.name).slice(0, 3).join(', ')}
                {lowStockItems.length > 3 ? ` and ${lowStockItems.length - 3} others` : ''}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Inventory Search */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative">
          <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search inventory items..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[11px] font-bold tracking-wider">
            <tr>
              <th className="px-6 py-3.5">Item Name</th>
              <th className="px-6 py-3.5">Category</th>
              <th className="px-6 py-3.5">Stock Level</th>
              <th className="px-6 py-3.5">Warning Threshold</th>
              <th className="px-6 py-3.5">Stock Status</th>
              <th className="px-6 py-3.5 text-right">Adjust Stock</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-slate-700">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                  Loading stock levels...
                </td>
              </tr>
            ) : filteredItems.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                  No inventory records found.
                </td>
              </tr>
            ) : (
              filteredItems.map((item) => {
                const isLow = item.track_stock && item.in_stock <= item.low_stock_threshold
                return (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 font-bold text-slate-900">{item.name}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">{item.category_name || 'General'}</td>
                    <td className="px-6 py-4 font-extrabold text-slate-900">
                      {item.in_stock} {item.sold_by === 'WEIGHT' ? 'kg' : 'units'}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">{item.low_stock_threshold}</td>
                    <td className="px-6 py-4">
                      {isLow ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 text-xs font-semibold">
                          <AlertTriangle className="w-3 h-3" /> Low Stock
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-semibold">
                          <CheckCircle2 className="w-3 h-3" /> In Stock
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => {
                          setSelectedItem(item)
                          setAddedStock(10)
                        }}
                        className="px-3 py-1.5 rounded-lg bg-sky-50 text-sky-700 hover:bg-sky-100 font-medium text-xs transition-colors flex items-center gap-1 ml-auto"
                      >
                        <PlusCircle className="w-3.5 h-3.5" />
                        Restock
                      </button>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Adjust Stock Modal */}
      {selectedItem && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200">
            <h3 className="text-lg font-bold text-slate-900 mb-1">Restock {selectedItem.name}</h3>
            <p className="text-xs text-slate-500 mb-4">Current stock: {selectedItem.in_stock} units</p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Quantity to Add</label>
                <input
                  type="number"
                  value={addedStock}
                  onChange={(e) => setAddedStock(parseInt(e.target.value) || 0)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 font-bold text-slate-900"
                />
              </div>

              <div className="p-3 bg-sky-50 rounded-xl text-xs text-sky-800 font-medium">
                New stock total will be: <span className="font-bold">{Number(selectedItem.in_stock) + Number(addedStock)}</span>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  onClick={() => setSelectedItem(null)}
                  className="px-4 py-2 bg-slate-100 text-slate-700 rounded-xl text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  onClick={handleRestockStock}
                  disabled={updating}
                  className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-semibold shadow-md"
                >
                  {updating ? 'Updating...' : 'Confirm Restock'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
