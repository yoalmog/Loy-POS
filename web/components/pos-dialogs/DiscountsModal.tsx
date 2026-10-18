'use client'

import React, { useState } from 'react'
import { X, Percent, Tag, Check, Sparkles } from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'

export default function DiscountsModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const {
    ticketDiscountPercent,
    ticketDiscountFixed,
    setOrderDiscount,
    applyPromotion,
    activePromotion,
    promotions,
    settings,
  } = usePos()

  const [percent, setPercent] = useState<number>(ticketDiscountPercent)
  const [fixed, setFixed] = useState<number>(ticketDiscountFixed)
  const [couponCode, setCouponCode] = useState<string>('')
  const [promoMessage, setPromoMessage] = useState<{ text: string; success: boolean } | null>(
    null
  )

  if (!isOpen) return null

  const PRESET_PERCENTS = [5, 10, 15, 20, 25, 50]
  const PRESET_FIXED = [5, 10, 20, 50]

  function handleSave() {
    setOrderDiscount(percent, fixed)
    onClose()
  }

  function handleApplyCoupon() {
    if (!couponCode) return
    const res = applyPromotion(couponCode)
    setPromoMessage({ text: res.message, success: res.success })
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md max-h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-emerald-500/20 text-emerald-400">
              <Percent className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black">הנחות ומבצעים (Discounts & Promos)</h3>
              <p className="text-xs text-slate-400">הנחה באחוזים, בסכום קבוע או קוד קופון</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 space-y-5 overflow-y-auto bg-slate-50">
          {/* Coupon Code Section */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-2.5">
            <label className="block text-xs font-bold uppercase text-slate-600">
              הזן קוד קופון ומבצע
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="למשל: HAPPYHOUR"
                value={couponCode}
                onChange={(e) => setCouponCode(e.target.value)}
                className="flex-1 p-2.5 rounded-xl border border-slate-200 text-xs text-slate-800 uppercase font-bold"
              />
              <button
                type="button"
                onClick={handleApplyCoupon}
                className="py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold"
              >
                הפעל
              </button>
            </div>

            {promoMessage && (
              <p
                className={`text-xs font-bold ${
                  promoMessage.success ? 'text-emerald-600' : 'text-red-500'
                }`}
              >
                {promoMessage.text}
              </p>
            )}

            {/* Quick Available Coupons */}
            <div className="pt-1">
              <span className="text-[11px] font-semibold text-slate-400">מבצעים קיימים: </span>
              <div className="flex flex-wrap gap-1.5 mt-1">
                {promotions.map((p) => (
                  <button
                    key={p.id}
                    onClick={() => {
                      setCouponCode(p.code)
                      applyPromotion(p.code)
                    }}
                    className="px-2 py-0.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-[10px] font-bold"
                  >
                    {p.code} ({p.name})
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Percentage Discount */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase text-slate-600">
                הנחה באחוזים (%)
              </label>
              <div className="flex items-center gap-1">
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={percent}
                  onChange={(e) => setPercent(Number(e.target.value))}
                  className="w-16 p-1 border border-slate-300 rounded-lg text-center font-bold text-xs"
                />
                <span className="text-xs font-bold">%</span>
              </div>
            </div>

            <div className="grid grid-cols-6 gap-1.5">
              {PRESET_PERCENTS.map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPercent(p)}
                  className={`py-2 rounded-xl text-xs font-extrabold transition-all ${
                    percent === p
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                  }`}
                >
                  {p}%
                </button>
              ))}
            </div>
          </div>

          {/* Fixed Amount Discount */}
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase text-slate-600">
                הנחה בסכום קבוע ({settings.currency_symbol})
              </label>
              <div className="flex items-center gap-1">
                <input
                  type="number"
                  min="0"
                  value={fixed}
                  onChange={(e) => setFixed(Number(e.target.value))}
                  className="w-20 p-1 border border-slate-300 rounded-lg text-center font-bold text-xs"
                />
                <span className="text-xs font-bold">{settings.currency_symbol}</span>
              </div>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {PRESET_FIXED.map((f) => (
                <button
                  key={f}
                  type="button"
                  onClick={() => setFixed(f)}
                  className={`py-2 rounded-xl text-xs font-extrabold transition-all ${
                    fixed === f
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                  }`}
                >
                  {settings.currency_symbol}
                  {f}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-white border-t border-slate-200 flex items-center justify-between">
          <button
            onClick={() => {
              setPercent(0)
              setFixed(0)
              setOrderDiscount(0, 0)
              onClose()
            }}
            className="text-xs text-red-500 font-bold hover:underline"
          >
            איפוס כל ההנחות
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="py-2.5 px-4 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 text-xs font-bold"
            >
              ביטול
            </button>
            <button
              onClick={handleSave}
              className="py-2.5 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-lg shadow-emerald-600/25"
            >
              החל הנחה
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
