'use client'

import React, { useState } from 'react'
import { X, Clock, Play, Trash2, User, Table, ShoppingBag } from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'

export default function HeldOrdersModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const { heldOrders, unparkOrder, discardParkedOrder, parkCurrentOrder, cart, settings } =
    usePos()

  const { t, isRtl } = useTheme()

  const [tableName, setTableName] = useState('')
  const [comment, setComment] = useState('')

  if (!isOpen) return null

  async function handleParkCurrent() {
    await parkCurrentOrder(tableName || undefined, comment || undefined)
    setTableName('')
    setComment('')
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden text-start">
        {/* Header */}
        <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black">{t.heldOrders}</h3>
              <p className="text-xs text-slate-400">{t.parkOrder}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Park Current Cart Form (if cart has items) */}
        {cart.length > 0 && (
          <div className="p-4 bg-amber-50/70 border-b border-amber-200 space-y-2.5">
            <h4 className="text-xs font-black text-amber-900">
              {t.parkOrder} ({cart.length})
            </h4>
            <div className="grid grid-cols-2 gap-2">
              <input
                type="text"
                placeholder={`${t.tableNumber} / ${t.customer}`}
                value={tableName}
                onChange={(e) => setTableName(e.target.value)}
                className="p-2 rounded-xl border border-amber-200 text-xs bg-white text-slate-800"
              />
              <input
                type="text"
                placeholder={t.customNotes}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="p-2 rounded-xl border border-amber-200 text-xs bg-white text-slate-800"
              />
            </div>
            <div className="flex justify-end">
              <button
                onClick={handleParkCurrent}
                className="py-1.5 px-4 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-sm"
              >
                {t.parkOrder}
              </button>
            </div>
          </div>
        )}

        {/* Held Orders List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-100">
          {heldOrders.length === 0 ? (
            <div className="text-center py-12 text-slate-400 space-y-2">
              <ShoppingBag className="w-10 h-10 mx-auto opacity-30" />
              <p className="text-xs">{t.no} {t.heldOrders}</p>
            </div>
          ) : (
            heldOrders.map((order) => {
              const timeStr = new Date(order.created_at).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              })
              const itemCount = order.items.reduce((s, i) => s + i.quantity, 0)

              return (
                <div
                  key={order.id}
                  className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs font-black text-slate-800">
                        {order.ticket_name}
                      </h4>
                      <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[10px] font-bold">
                        {order.dining_option}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 text-[11px] text-slate-500">
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3 text-slate-400" />
                        {timeStr}
                      </span>
                      <span>• {itemCount}</span>
                      {order.customer_name && <span>• {t.customer}: {order.customer_name}</span>}
                    </div>

                    {order.comment && (
                      <p className="text-[11px] text-amber-700 font-medium">
                        {t.customNotes}: {order.comment}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-sm font-black text-slate-900 mx-2">
                      {settings.currency_symbol}
                      {order.subtotal.toFixed(2)}
                    </span>
                    <button
                      onClick={async () => {
                        await unparkOrder(order.id)
                        onClose()
                      }}
                      className="p-2.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold shadow-sm flex items-center gap-1"
                      title={t.cart}
                    >
                      <Play className="w-3.5 h-3.5 fill-white" />
                      <span>{t.confirm}</span>
                    </button>
                    <button
                      onClick={() => discardParkedOrder(order.id)}
                      className="p-2.5 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs"
                      title={t.delete}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}
