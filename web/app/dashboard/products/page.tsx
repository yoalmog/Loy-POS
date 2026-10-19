'use client'

import React, { useState } from 'react'
import {
  Package,
  Plus,
  Search,
  Sparkles,
  Edit2,
  Trash2,
  Star,
  Layers,
  Printer,
  Check,
  X,
  Tag,
  Upload,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { Item, Category, getLocalizedName, matchItemSearch } from '@/lib/types'
import MenuScannerModal from '@/components/menu-scanner/MenuScannerModal'

export default function ProductsManagementPage() {
  const {
    items,
    categories,
    settings,
    addItemToCatalog,
    updateItemInCatalog,
    deleteItemFromCatalog,
    addCategory,
  } = usePos()
  const { t, isRtl, language } = useTheme()

  const [activeTab, setActiveTab] = useState<'products' | 'categories'>('products')
  const [search, setSearch] = useState('')
  const [isMenuScannerOpen, setIsMenuScannerOpen] = useState(false)

  // Product Add / Edit Modal State
  const [editingItem, setEditingItem] = useState<Item | null>(null)
  const [isProductModalOpen, setIsProductModalOpen] = useState(false)

  // Category Add Modal State
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false)
  const [newCatName, setNewCatName] = useState('')
  const [newCatNameEs, setNewCatNameEs] = useState('')
  const [newCatNameEn, setNewCatNameEn] = useState('')
  const [newCatNameHe, setNewCatNameHe] = useState('')
  const [newCatColor, setNewCatColor] = useState('#0288D1')
  const [newCatPrinter, setNewCatPrinter] = useState<'CASHIER' | 'KITCHEN' | 'BAR'>('KITCHEN')

  // Filtered Products (Multilingual search across ES, EN, HE, SKU, barcode)
  const filteredItems = items.filter((i) => matchItemSearch(i, search, language))

  function openNewProductModal() {
    setEditingItem({
      id: Date.now(),
      name: '',
      name_translations: { es: '', en: '', he: '' },
      description: '',
      description_translations: { es: '', en: '', he: '' },
      category_id: categories[0]?.id || 1,
      category_name: categories[0]?.name || t.allCategories,
      price: 0,
      cost: 0,
      sku: `SKU-${Date.now().toString().slice(-4)}`,
      barcode: `7290${Math.floor(Math.random() * 90000000 + 10000000)}`,
      sold_by: 'EACH',
      in_stock: 50,
      low_stock_threshold: 10,
      track_stock: true,
      is_active: true,
      is_favorite: false,
      color_hex: '#0288D1',
      unit_of_measure: 'item',
      tax_rate_percent: 18,
      variants: [],
      modifier_groups: [],
    })
    setIsProductModalOpen(true)
  }

  async function handleSaveProduct(e: React.FormEvent) {
    e.preventDefault()
    if (!editingItem) return

    const categoryObj = categories.find((c) => c.id === editingItem.category_id)
    const effectiveName =
      editingItem.name_translations?.es || editingItem.name || 'Nuevo Producto'

    const toSave: Item = {
      ...editingItem,
      name: effectiveName,
      name_translations: {
        es: editingItem.name_translations?.es || effectiveName,
        en: editingItem.name_translations?.en || '',
        he: editingItem.name_translations?.he || '',
      },
      category_name: categoryObj ? categoryObj.name : editingItem.category_name,
    }

    const exists = items.some((i) => i.id === toSave.id)
    if (exists) {
      await updateItemInCatalog(toSave)
    } else {
      await addItemToCatalog(toSave)
    }

    setIsProductModalOpen(false)
    setEditingItem(null)
  }

  async function handleSaveCategory(e: React.FormEvent) {
    e.preventDefault()
    const effectiveName = newCatNameEs || newCatName
    if (!effectiveName) return

    const newCat: Category = {
      id: Date.now(),
      name: effectiveName,
      name_translations: {
        es: newCatNameEs || effectiveName,
        en: newCatNameEn || '',
        he: newCatNameHe || '',
      },
      color_hex: newCatColor,
      target_printer: newCatPrinter,
      sort_order: categories.length + 1,
    }
    await addCategory(newCat)
    setIsCategoryModalOpen(false)
    setNewCatName('')
    setNewCatNameEs('')
    setNewCatNameEn('')
    setNewCatNameHe('')
  }

  return (
    <div className="space-y-6">
      {/* Top Controls Bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            {t.productsManagement}
          </h2>
          <p className="text-xs text-slate-500">
            {t.products} &bull; {t.categoriesTab} &bull; {t.variants} &bull; {t.modifierGroups}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* AI Menu Scanner Launcher */}
          <button
            onClick={() => setIsMenuScannerOpen(true)}
            className="py-2.5 px-4 rounded-2xl bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-extrabold text-xs shadow-md shadow-sky-600/20 flex items-center gap-2"
          >
            <Sparkles className="w-4 h-4 text-amber-300" />
            <span>{t.aiMenuScanner}</span>
          </button>

          {activeTab === 'products' ? (
            <button
              onClick={openNewProductModal}
              className="py-2.5 px-5 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white font-extrabold text-xs shadow-md shadow-sky-600/20 flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" />
              <span>{t.addProduct}</span>
            </button>
          ) : (
            <button
              onClick={() => setIsCategoryModalOpen(true)}
              className="py-2.5 px-5 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white font-extrabold text-xs shadow-md shadow-sky-600/20 flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" />
              <span>{t.add} ({t.categoriesTab})</span>
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200">
        <button
          onClick={() => setActiveTab('products')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${
            activeTab === 'products'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Package className="w-4 h-4" />
          <span>{t.productsTab} ({items.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('categories')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${
            activeTab === 'categories'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>{t.categoriesTab} ({categories.length})</span>
        </button>
      </div>

      {/* PRODUCTS TAB */}
      {activeTab === 'products' && (
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
              <table className={`w-full ${isRtl ? 'text-right' : 'text-left'} text-xs`}>
                <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-200">
                  <tr>
                    <th className="p-4">{t.productName}</th>
                    <th className="p-4">{t.categoryName}</th>
                    <th className="p-4">{t.price}</th>
                    <th className="p-4">{t.cost}</th>
                    <th className="p-4">{t.sku} / {t.barcode}</th>
                    <th className="p-4">{t.stockLevel}</th>
                    <th className="p-4">{t.favorites}</th>
                    <th className={`p-4 ${isRtl ? 'text-left' : 'text-right'}`}>{t.actions}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredItems.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50/80">
                      <td className="p-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-200">
                            {item.image_url ? (
                              <img
                                src={item.image_url}
                                alt={item.name}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div
                                style={{ backgroundColor: item.color_hex || '#0288D1' }}
                                className="w-full h-full flex items-center justify-center text-white font-bold"
                              >
                                {item.name.charAt(0)}
                              </div>
                            )}
                          </div>
                          <div>
                            <h4 className="font-extrabold text-slate-900">{getLocalizedName(item, language)}</h4>
                            {item.variants && item.variants.length > 0 && (
                              <span className="text-[10px] text-sky-600 font-bold">
                                {item.variants.length} {t.variants}
                              </span>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="p-4">
                        <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 font-bold text-[10px]">
                          {getLocalizedName(categories.find((c) => c.id === item.category_id), language) || item.category_name}
                        </span>
                      </td>
                      <td className="p-4 font-black text-slate-900">
                        {settings.currency_symbol}
                        {item.price.toFixed(2)}
                      </td>
                      <td className="p-4 text-slate-500">
                        {settings.currency_symbol}
                        {(item.cost || 0).toFixed(2)}
                      </td>
                      <td className="p-4 font-mono text-[11px] text-slate-500">
                        {item.sku || '-'} / {item.barcode || '-'}
                      </td>
                      <td className="p-4">
                        {item.track_stock ? (
                          <span
                            className={`font-black ${
                              item.in_stock <= item.low_stock_threshold
                                ? 'text-red-600'
                                : 'text-slate-800'
                            }`}
                          >
                            {item.in_stock} {item.unit_of_measure}
                          </span>
                        ) : (
                          <span className="text-slate-400">-</span>
                        )}
                      </td>
                      <td className="p-4">
                        {item.is_favorite ? (
                          <Star className="w-4 h-4 text-amber-500 fill-amber-500" />
                        ) : (
                          <span className="text-slate-300">-</span>
                        )}
                      </td>
                      <td className={`p-4 ${isRtl ? 'text-left' : 'text-right'}`}>
                        <div className={`flex items-center ${isRtl ? 'justify-end' : 'justify-start'} gap-1.5`}>
                          <button
                            onClick={() => {
                              setEditingItem(item)
                              setIsProductModalOpen(true)
                            }}
                            className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-600"
                            title={t.editProduct}
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => {
                              if (confirm(t.confirmDeleteProduct)) {
                                deleteItemFromCatalog(item.id)
                              }
                            }}
                            className="p-1.5 rounded-lg hover:bg-red-50 text-red-500"
                            title={t.deleteProduct}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* CATEGORIES TAB */}
      {activeTab === 'categories' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {categories.map((cat) => (
            <div
              key={cat.id}
              className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div
                    style={{ backgroundColor: cat.color_hex || '#0288D1' }}
                    className="w-10 h-10 rounded-2xl flex items-center justify-center text-white font-bold shadow-md"
                  >
                    <Layers className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm font-black text-slate-800">{cat.name}</h3>
                    <span className="text-[11px] text-slate-400">
                      {items.filter((i) => i.category_id === cat.id).length} {t.products}
                    </span>
                  </div>
                </div>
              </div>

              {/* Target Printer Routing */}
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 text-slate-600 font-bold">
                  <Printer className="w-4 h-4 text-sky-600" />
                  <span>{t.targetPrinter}:</span>
                </div>
                <span className="px-2.5 py-0.5 rounded-full bg-sky-100 text-sky-800 font-extrabold text-[10px]">
                  {cat.target_printer === 'KITCHEN'
                    ? t.kitchenPrinter
                    : cat.target_printer === 'BAR'
                    ? t.barPrinter
                    : t.cashierPrinter}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* PRODUCT ADD / EDIT MODAL */}
      {isProductModalOpen && editingItem && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-xl max-h-[92vh] flex flex-col overflow-hidden">
            <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
              <h3 className="text-base font-black">
                {editingItem.id ? t.editProduct : t.addProduct}
              </h3>
              <button
                onClick={() => setIsProductModalOpen(false)}
                className="p-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveProduct} className="flex-1 overflow-y-auto p-6 space-y-4 bg-slate-50">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.productName} *
                </label>
                <input
                  type="text"
                  required
                  value={editingItem.name}
                  onChange={(e) => setEditingItem({ ...editingItem, name: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                />
              </div>

              {/* Multilingual Product Names & Localized Business Data */}
              <div className="p-3.5 rounded-2xl bg-white border border-slate-200 shadow-sm space-y-2.5">
                <span className="text-[11px] font-black uppercase text-slate-700 block">
                  Traducciones de Nombre / Localized Display Names (ES / EN / HE)
                </span>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                      Español (es)
                    </label>
                    <input
                      type="text"
                      placeholder="Ej. Hamburguesa Clásica"
                      value={editingItem.name_translations?.es || ''}
                      onChange={(e) => {
                        const nt = { ...(editingItem.name_translations || {}), es: e.target.value }
                        setEditingItem({
                          ...editingItem,
                          name: e.target.value || editingItem.name,
                          name_translations: nt,
                        })
                      }}
                      className="w-full p-2 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800 font-bold"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                      English (en)
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Classic Burger"
                      value={editingItem.name_translations?.en || ''}
                      onChange={(e) => {
                        const nt = { ...(editingItem.name_translations || {}), en: e.target.value }
                        setEditingItem({ ...editingItem, name_translations: nt })
                      }}
                      className="w-full p-2 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800 font-bold"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                      עברית (he)
                    </label>
                    <input
                      type="text"
                      placeholder="למשל: המבורגר קלאסי"
                      value={editingItem.name_translations?.he || ''}
                      onChange={(e) => {
                        const nt = { ...(editingItem.name_translations || {}), he: e.target.value }
                        setEditingItem({ ...editingItem, name_translations: nt })
                      }}
                      className="w-full p-2 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800 font-bold text-right"
                      dir="rtl"
                    />
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.categoryName}
                  </label>
                  <select
                    value={editingItem.category_id}
                    onChange={(e) =>
                      setEditingItem({
                        ...editingItem,
                        category_id: Number(e.target.value),
                      })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.products}
                  </label>
                  <select
                    value={editingItem.unit_of_measure}
                    onChange={(e) =>
                      setEditingItem({
                        ...editingItem,
                        unit_of_measure: e.target.value as any,
                      })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                  >
                    <option value="item">Each</option>
                    <option value="kg">Kg</option>
                    <option value="lb">Lb</option>
                    <option value="cup">Cup</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.price} ({settings.currency_symbol}) *
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    required
                    value={editingItem.price}
                    onChange={(e) =>
                      setEditingItem({ ...editingItem, price: Number(e.target.value) })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.cost} ({settings.currency_symbol})
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    value={editingItem.cost || 0}
                    onChange={(e) =>
                      setEditingItem({ ...editingItem, cost: Number(e.target.value) })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.sku}
                  </label>
                  <input
                    type="text"
                    value={editingItem.sku || ''}
                    onChange={(e) => setEditingItem({ ...editingItem, sku: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.barcode}
                  </label>
                  <input
                    type="text"
                    value={editingItem.barcode || ''}
                    onChange={(e) =>
                      setEditingItem({ ...editingItem, barcode: e.target.value })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.stockLevel}
                  </label>
                  <input
                    type="number"
                    value={editingItem.in_stock}
                    onChange={(e) =>
                      setEditingItem({ ...editingItem, in_stock: Number(e.target.value) })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                    {t.lowStockThreshold}
                  </label>
                  <input
                    type="number"
                    value={editingItem.low_stock_threshold}
                    onChange={(e) =>
                      setEditingItem({
                        ...editingItem,
                        low_stock_threshold: Number(e.target.value),
                      })
                    }
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                  />
                </div>
              </div>

              <div className="flex items-center gap-4 pt-2">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={editingItem.is_favorite || false}
                    onChange={(e) =>
                      setEditingItem({ ...editingItem, is_favorite: e.target.checked })
                    }
                    className="w-4 h-4 accent-amber-500"
                  />
                  <span className="text-xs font-bold text-slate-700">{t.favorites}</span>
                </label>

                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={editingItem.track_stock}
                    onChange={(e) =>
                      setEditingItem({ ...editingItem, track_stock: e.target.checked })
                    }
                    className="w-4 h-4 accent-sky-600"
                  />
                  <span className="text-xs font-bold text-slate-700">{t.trackStock}</span>
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setIsProductModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
                >
                  {t.cancel}
                </button>
                <button
                  type="submit"
                  className="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-md shadow-sky-600/20"
                >
                  {t.save}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CATEGORY ADD MODAL */}
      {isCategoryModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-800">{t.add} ({t.categoriesTab})</h3>
            <form onSubmit={handleSaveCategory} className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  Nombre de Categoría (Español) *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej. Hamburguesas y Carnes"
                  value={newCatNameEs || newCatName}
                  onChange={(e) => {
                    setNewCatName(e.target.value)
                    setNewCatNameEs(e.target.value)
                  }}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 font-bold"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                    English (en)
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Burgers & Meats"
                    value={newCatNameEn}
                    onChange={(e) => setNewCatNameEn(e.target.value)}
                    className="w-full p-2 rounded-xl border border-slate-200 text-xs bg-white text-slate-800"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                    עברית (he)
                  </label>
                  <input
                    type="text"
                    placeholder="למשל: המבורגרים ובשרים"
                    value={newCatNameHe}
                    onChange={(e) => setNewCatNameHe(e.target.value)}
                    className="w-full p-2 rounded-xl border border-slate-200 text-xs bg-white text-slate-800 text-right"
                    dir="rtl"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.colorTag}
                </label>
                <input
                  type="color"
                  value={newCatColor}
                  onChange={(e) => setNewCatColor(e.target.value)}
                  className="w-full h-10 p-1 rounded-xl border border-slate-200 cursor-pointer"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  {t.targetPrinter}
                </label>
                <select
                  value={newCatPrinter}
                  onChange={(e) => setNewCatPrinter(e.target.value as any)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-white text-slate-800"
                >
                  <option value="KITCHEN">{t.kitchenPrinter}</option>
                  <option value="BAR">{t.barPrinter}</option>
                  <option value="CASHIER">{t.cashierPrinter}</option>
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCategoryModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
                >
                  {t.cancel}
                </button>
                <button
                  type="submit"
                  className="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-md shadow-sky-600/20"
                >
                  {t.save}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* AI Menu Scanner Modal */}
      <MenuScannerModal
        isOpen={isMenuScannerOpen}
        onClose={() => setIsMenuScannerOpen(false)}
      />
    </div>
  )
}
