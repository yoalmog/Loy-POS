'use client'

import React, { useState } from 'react'
import { X, Check, Plus, Minus, Tag, MessageSquare } from 'lucide-react'
import { Item, ProductVariant, CartModifierSelection } from '@/lib/types'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'

export default function ItemCustomizationModal({
  item,
  isOpen,
  onClose,
}: {
  item: Item | null
  isOpen: boolean
  onClose: () => void
}) {
  const { addToCart, settings } = usePos()
  const { t, isRtl } = useTheme()

  if (!isOpen || !item) return null

  // Variant selection (default to first variant if exists)
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | undefined>(
    item.variants && item.variants.length > 0 ? item.variants[0] : undefined
  )

  // Modifiers
  const [selectedModifiers, setSelectedModifiers] = useState<CartModifierSelection[]>([])

  // Comment & Custom Notes
  const [comment, setComment] = useState<string>('')

  // Quantity
  const [quantity, setQuantity] = useState<number>(1)

  function toggleModifier(mod: { id: string; name: string; price: number }) {
    const exists = selectedModifiers.find((m) => m.modifier_id === mod.id)
    if (exists) {
      setSelectedModifiers(selectedModifiers.filter((m) => m.modifier_id !== mod.id))
    } else {
      setSelectedModifiers([
        ...selectedModifiers,
        { modifier_id: mod.id, name: mod.name, price: mod.price },
      ])
    }
  }

  const basePrice = selectedVariant ? selectedVariant.price : item.price
  const modifiersCost = selectedModifiers.reduce((sum, m) => sum + m.price, 0)
  const lineTotal = (basePrice + modifiersCost) * quantity

  function handleConfirm() {
    addToCart(item!, selectedVariant, selectedModifiers, comment, quantity)
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden text-start">
        {/* Header */}
        <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-sky-400">{t.cart}</span>
            <h3 className="text-lg font-black">{item.name}</h3>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5 bg-slate-50">
          {/* Variants (Size / Volume) */}
          {item.variants && item.variants.length > 0 && (
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-2">
                {t.variants}
              </label>
              <div className="grid grid-cols-2 gap-2.5">
                {item.variants.map((variant) => {
                  const isSelected = selectedVariant?.id === variant.id
                  return (
                    <button
                      key={variant.id}
                      type="button"
                      onClick={() => setSelectedVariant(variant)}
                      className={`p-3 rounded-2xl border text-start transition-all flex items-center justify-between ${
                        isSelected
                          ? 'border-sky-600 bg-sky-50 shadow-sm ring-2 ring-sky-600/20'
                          : 'border-slate-200 bg-white hover:bg-slate-100'
                      }`}
                    >
                      <span className="font-bold text-xs text-slate-800">{variant.name}</span>
                      <span className="font-extrabold text-xs text-sky-600">
                        {settings.currency_symbol}
                        {variant.price.toFixed(2)}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
          )}

          {/* Modifier Groups */}
          {item.modifier_groups && item.modifier_groups.length > 0 && (
            <div className="space-y-4">
              {item.modifier_groups.map((group) => (
                <div key={group.id} className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
                  <h4 className="text-xs font-extrabold uppercase text-slate-700 mb-2.5">
                    {group.name}
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                    {group.options.map((opt) => {
                      const isChecked = selectedModifiers.some((m) => m.modifier_id === opt.id)
                      return (
                        <button
                          key={opt.id}
                          type="button"
                          onClick={() => toggleModifier(opt)}
                          className={`p-2.5 rounded-xl border text-start flex items-center justify-between transition-all ${
                            isChecked
                              ? 'border-sky-600 bg-sky-50 text-sky-900 font-bold'
                              : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <div
                              className={`w-4 h-4 rounded-md border flex items-center justify-center ${
                                isChecked
                                  ? 'bg-sky-600 border-sky-600 text-white'
                                  : 'border-slate-300'
                              }`}
                            >
                              {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
                            </div>
                            <span className="text-xs">{opt.name}</span>
                          </div>
                          <span className="text-xs font-semibold text-slate-500">
                            {opt.price > 0 ? `+${settings.currency_symbol}${opt.price.toFixed(2)}` : `${settings.currency_symbol}0`}
                          </span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Custom Comments & Notes */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-2.5">
            <div className="flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-slate-400" />
              <label className="text-xs font-bold uppercase text-slate-600">
                {t.customNotes}
              </label>
            </div>

            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder={`${t.customNotes}...`}
              className="w-full p-3 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 bg-slate-50"
              rows={2}
            />
          </div>

          {/* Quantity Selector */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <span className="text-xs font-bold text-slate-700">{t.actions}:</span>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                className="w-9 h-9 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 flex items-center justify-center font-bold"
              >
                <Minus className="w-4 h-4" />
              </button>
              <span className="text-base font-black text-slate-900 w-6 text-center">
                {quantity}
              </span>
              <button
                type="button"
                onClick={() => setQuantity((q) => q + 1)}
                className="w-9 h-9 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 flex items-center justify-center font-bold"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-white border-t border-slate-200 flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-500">{t.subtotal}:</span>
            <div className="text-xl font-black text-sky-600">
              {settings.currency_symbol}
              {lineTotal.toFixed(2)}
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="py-2.5 px-4 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 text-xs font-bold"
            >
              {t.cancel}
            </button>
            <button
              onClick={handleConfirm}
              className="py-2.5 px-6 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-lg shadow-sky-600/25 flex items-center gap-2"
            >
              <span>{t.add}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
