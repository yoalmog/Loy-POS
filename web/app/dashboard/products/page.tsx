'use client'

import React, { useEffect, useState } from 'react'
import {
  Plus,
  Search,
  Edit2,
  Trash2,
  Package,
  Layers,
  X,
  Check,
  Tag,
  DollarSign,
  Barcode,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Item, Category } from '@/lib/types'

export default function ProductsPage() {
  const [items, setItems] = useState<Item[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('ALL')

  // Modals state
  const [showItemModal, setShowItemModal] = useState(false)
  const [editingItem, setEditingItem] = useState<Partial<Item> | null>(null)
  const [saving, setSaving] = useState(false)

  async function loadProductsAndCategories() {
    setLoading(true)
    try {
      const { data: catData } = await supabase.from('categories').select('*').order('name')
      if (catData) setCategories(catData)

      const { data: itemData } = await supabase.from('items').select('*').order('name')
      if (itemData) setItems(itemData)
    } catch (err) {
      console.error('Failed to load catalog:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProductsAndCategories()
  }, [])

  function handleOpenCreateModal() {
    setEditingItem({
      name: '',
      price: 0,
      cost: 0,
      category_name: categories.length > 0 ? categories[0].name : 'General',
      in_stock: 50,
      low_stock_threshold: 10,
      track_stock: true,
      sold_by: 'EACH',
      color_hex: '#0288D1',
      sku: '',
      barcode: '',
    })
    setShowItemModal(true)
  }

  function handleOpenEditModal(item: Item) {
    setEditingItem(item)
    setShowItemModal(true)
  }

  async function handleSaveItem(e: React.FormEvent) {
    e.preventDefault()
    if (!editingItem || !editingItem.name) return
    setSaving(true)

    try {
      const payload = {
        name: editingItem.name,
        category_name: editingItem.category_name || 'General',
        price: Number(editingItem.price) || 0,
        cost: Number(editingItem.cost) || 0,
        sku: editingItem.sku || '',
        barcode: editingItem.barcode || '',
        sold_by: editingItem.sold_by || 'EACH',
        in_stock: Number(editingItem.in_stock) || 0,
        low_stock_threshold: Number(editingItem.low_stock_threshold) || 5,
        track_stock: editingItem.track_stock ?? true,
        color_hex: editingItem.color_hex || '#0288D1',
        updated_at: new Date().toISOString(),
      }

      if (editingItem.id) {
        // Update existing item
        const { error } = await supabase.from('items').update(payload).eq('id', editingItem.id)
        if (error) throw error

        // Sync with products table for cross-compatibility
        await supabase.from('products').update({
          name: editingItem.name,
          price: Number(editingItem.price),
          category: editingItem.category_name,
          stock_level: Math.round(Number(editingItem.in_stock)),
        }).eq('id', editingItem.id)
      } else {
        // Insert new item
        const { data, error } = await supabase.from('items').insert([payload]).select()
        if (error) throw error

        if (data && data.length > 0) {
          await supabase.from('products').insert([{
            id: data[0].id,
            name: editingItem.name,
            price: Number(editingItem.price),
            category: editingItem.category_name,
            stock_level: Math.round(Number(editingItem.in_stock)),
          }])
        }
      }

      setShowItemModal(false)
      loadProductsAndCategories()
    } catch (err: any) {
      alert(`Failed to save product: ${err.message}`)
    } finally {
      setSaving(false)
    }
  }

  async function handleDeleteItem(id: number) {
    if (!confirm('Are you sure you want to delete this product from the database?')) return
    try {
      await supabase.from('items').delete().eq('id', id)
      await supabase.from('products').delete().eq('id', id)
      loadProductsAndCategories()
    } catch (err: any) {
      alert(`Delete failed: ${err.message}`)
    }
  }

  const filteredItems = items.filter((item) => {
    const matchesSearch =
      item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.sku && item.sku.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (item.barcode && item.barcode.includes(searchQuery))
    const matchesCat = selectedCategory === 'ALL' || item.category_name === selectedCategory
    return matchesSearch && matchesCat
  })

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Product & Catalog Management</h2>
          <p className="text-sm text-slate-500 mt-1">
            Create and edit product prices, categories, and barcodes (Synced in real-time with POS)
          </p>
        </div>
        <button
          onClick={handleOpenCreateModal}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-semibold rounded-xl shadow-md transition-all shrink-0"
        >
          <Plus className="w-4 h-4" />
          Add New Product
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search products, SKU, or barcode..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="flex items-center gap-2">
          <Layers className="w-4 h-4 text-slate-400" />
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="py-2 px-3 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 bg-white"
          >
            <option value="ALL">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.name}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Products Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[11px] font-bold tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Product Name</th>
                <th className="px-6 py-3.5">Category</th>
                <th className="px-6 py-3.5">Retail Price</th>
                <th className="px-6 py-3.5">Unit Cost</th>
                <th className="px-6 py-3.5">In Stock</th>
                <th className="px-6 py-3.5">SKU / Barcode</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                    Loading database products...
                  </td>
                </tr>
              ) : filteredItems.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                    No products found matching your filter.
                  </td>
                </tr>
              ) : (
                filteredItems.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 font-bold text-slate-900 flex items-center gap-2">
                      <div
                        className="w-3.5 h-3.5 rounded-full shrink-0"
                        style={{ backgroundColor: item.color_hex || '#0288D1' }}
                      />
                      <span>{item.name}</span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="px-2.5 py-1 rounded-md bg-slate-100 text-slate-700 text-xs font-medium">
                        {item.category_name || 'General'}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-extrabold text-slate-900">${(item.price || 0).toFixed(2)}</td>
                    <td className="px-6 py-4 text-slate-500">${(item.cost || 0).toFixed(2)}</td>
                    <td className="px-6 py-4">
                      <span
                        className={`font-semibold text-xs px-2.5 py-1 rounded-full ${
                          item.in_stock <= item.low_stock_threshold
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {item.in_stock} {item.sold_by === 'WEIGHT' ? 'kg' : 'units'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {item.sku ? `SKU: ${item.sku}` : ''} {item.barcode ? `• Barcode: ${item.barcode}` : ''}
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <button
                        onClick={() => handleOpenEditModal(item)}
                        className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteItem(item.id)}
                        className="p-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Product Add/Edit Modal */}
      {showItemModal && editingItem && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-900">
                {editingItem.id ? 'Edit Product Details' : 'Create New Product'}
              </h3>
              <button
                onClick={() => setShowItemModal(false)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveItem} className="py-4 space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Product Name</label>
                <input
                  type="text"
                  required
                  value={editingItem.name || ''}
                  onChange={(e) => setEditingItem({ ...editingItem, name: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  placeholder="e.g., Double Bacon Cheeseburger"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Category</label>
                  <input
                    type="text"
                    value={editingItem.category_name || ''}
                    onChange={(e) => setEditingItem({ ...editingItem, category_name: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    placeholder="e.g., Burgers"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Sold By</label>
                  <select
                    value={editingItem.sold_by || 'EACH'}
                    onChange={(e) => setEditingItem({ ...editingItem, sold_by: e.target.value as any })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 bg-white"
                  >
                    <option value="EACH">Each (Unit)</option>
                    <option value="WEIGHT">Weight (kg/lb)</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Retail Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={editingItem.price ?? 0}
                    onChange={(e) => setEditingItem({ ...editingItem, price: parseFloat(e.target.value) })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Unit Cost ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={editingItem.cost ?? 0}
                    onChange={(e) => setEditingItem({ ...editingItem, cost: parseFloat(e.target.value) })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Stock Quantity</label>
                  <input
                    type="number"
                    step="1"
                    value={editingItem.in_stock ?? 0}
                    onChange={(e) => setEditingItem({ ...editingItem, in_stock: parseFloat(e.target.value) })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Low Stock Warning Limit</label>
                  <input
                    type="number"
                    step="1"
                    value={editingItem.low_stock_threshold ?? 5}
                    onChange={(e) => setEditingItem({ ...editingItem, low_stock_threshold: parseFloat(e.target.value) })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">SKU</label>
                  <input
                    type="text"
                    value={editingItem.sku || ''}
                    onChange={(e) => setEditingItem({ ...editingItem, sku: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    placeholder="e.g., 1001"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Barcode</label>
                  <input
                    type="text"
                    value={editingItem.barcode || ''}
                    onChange={(e) => setEditingItem({ ...editingItem, barcode: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    placeholder="e.g., 200001001000"
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowItemModal(false)}
                  className="px-4 py-2 bg-slate-100 text-slate-700 rounded-xl text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-semibold shadow-md transition-all"
                >
                  {saving ? 'Saving to Database...' : 'Save Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
