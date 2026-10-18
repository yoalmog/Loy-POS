'use client'

import React, { useState } from 'react'
import {
  Receipt as ReceiptIcon,
  Search,
  Filter,
  RotateCcw,
  Printer,
  QrCode,
  Calendar,
  CheckCircle2,
  XCircle,
  Eye,
  Check,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { Receipt } from '@/lib/types'
import ReceiptPreviewModal from '@/components/pos-dialogs/ReceiptPreviewModal'

export default function SalesHistoryPage() {
  const { receipts, settings, processRefund } = usePos()

  const [search, setSearch] = useState('')
  const [filterPayment, setFilterPayment] = useState<string>('ALL')
  const [filterStatus, setFilterStatus] = useState<string>('ALL')
  const [viewingReceipt, setViewingReceipt] = useState<Receipt | null>(null)

  // Refund Modal State
  const [refundingReceipt, setRefundingReceipt] = useState<Receipt | null>(null)
  const [refundReason, setRefundReason] = useState('פגם במוצר / טעות במנה')
  const [selectedItemIds, setSelectedItemIds] = useState<string[]>([])
  const [isProcessingRefund, setIsProcessingRefund] = useState(false)

  // Filtering
  const filteredReceipts = receipts.filter((r) => {
    // Search
    if (search.trim()) {
      const q = search.toLowerCase()
      const matchNum = r.receipt_number.toLowerCase().includes(q)
      const matchCust = r.customer_name?.toLowerCase().includes(q)
      const matchCashier = r.cashier_name.toLowerCase().includes(q)
      if (!matchNum && !matchCust && !matchCashier) return false
    }

    // Payment Filter
    if (filterPayment !== 'ALL' && r.payment_type !== filterPayment) {
      return false
    }

    // Status Filter
    if (filterStatus === 'COMPLETED' && r.is_refunded) return false
    if (filterStatus === 'REFUNDED' && !r.is_refunded) return false

    return true
  })

  function openRefundModal(receipt: Receipt) {
    setRefundingReceipt(receipt)
    setSelectedItemIds(receipt.items.map((i) => i.cart_id)) // Default select all
  }

  async function handleConfirmRefund() {
    if (!refundingReceipt) return
    setIsProcessingRefund(true)
    await processRefund(refundingReceipt.id, selectedItemIds, refundReason)
    setIsProcessingRefund(false)
    setRefundingReceipt(null)
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            יומן מכירות, קבלות והחזרים (Sales & Receipts)
          </h2>
          <p className="text-xs text-slate-500">
            צפייה בכל העסקאות, סינון לפי תאריך/אמצעי תשלום, ביצוע זיכויים חלקיים ומלאים
          </p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="bg-white p-4 rounded-3xl border border-slate-200 shadow-sm flex flex-wrap items-center justify-between gap-3">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="w-4 h-4 text-slate-400 absolute top-3 right-3" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="חפש לפי מספר קבלה, לקוח או קופאי..."
            className="w-full pr-9 pl-4 py-2 rounded-xl border border-slate-200 text-xs bg-slate-50 focus:outline-none focus:ring-2 focus:ring-sky-500 text-slate-800"
          />
        </div>

        <div className="flex items-center gap-2">
          {/* Payment Filter */}
          <select
            value={filterPayment}
            onChange={(e) => setFilterPayment(e.target.value)}
            className="p-2 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-700 font-bold"
          >
            <option value="ALL">כל אמצעי התשלום</option>
            <option value="CASH">מזומן</option>
            <option value="CARD">כרטיס אשראי</option>
            <option value="YAPE">Yape QR</option>
            <option value="PLIN">Plin QR</option>
            <option value="DIGITAL_WALLET">ארנק דיגיטלי</option>
          </select>

          {/* Status Filter */}
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="p-2 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-700 font-bold"
          >
            <option value="ALL">כל הסטטוסים</option>
            <option value="COMPLETED">הושלמו בלבד</option>
            <option value="REFUNDED">זוכו בלבד</option>
          </select>
        </div>
      </div>

      {/* Receipts Table */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-200">
              <tr>
                <th className="p-4">קבלה</th>
                <th className="p-4">תאריך ושעה</th>
                <th className="p-4">קופאי</th>
                <th className="p-4">לקוח</th>
                <th className="p-4">אמצעי תשלום</th>
                <th className="p-4">פריטים</th>
                <th className="p-4">סה״כ לתשלום</th>
                <th className="p-4">סטטוס</th>
                <th className="p-4 text-left">פעולות</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredReceipts.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-12 text-slate-400">
                    לא נמצאו קבלות התואמות לחיפוש
                  </td>
                </tr>
              ) : (
                filteredReceipts.map((rec) => (
                  <tr key={rec.id} className="hover:bg-slate-50/80">
                    <td className="p-4 font-mono font-black text-slate-900">
                      {rec.receipt_number}
                    </td>
                    <td className="p-4 text-slate-500">
                      {new Date(rec.created_at).toLocaleDateString('he-IL')} •{' '}
                      {new Date(rec.created_at).toLocaleTimeString('he-IL', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </td>
                    <td className="p-4 text-slate-700 font-semibold">{rec.cashier_name}</td>
                    <td className="p-4 text-slate-700">{rec.customer_name || 'לקוח מזדמן'}</td>
                    <td className="p-4">
                      <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 font-bold text-[10px]">
                        {rec.payment_type}
                      </span>
                    </td>
                    <td className="p-4 text-slate-600">
                      {rec.items.length} שורות ({rec.items.reduce((s, i) => s + i.quantity, 0)}{' '}
                      מנות)
                    </td>
                    <td className="p-4 font-black text-slate-900 text-sm">
                      {settings.currency_symbol}
                      {rec.total_amount.toFixed(2)}
                    </td>
                    <td className="p-4">
                      {rec.is_refunded ? (
                        <span className="px-2.5 py-0.5 rounded-full bg-red-100 text-red-700 text-[10px] font-bold">
                          זוכה ({rec.refund_reason})
                        </span>
                      ) : (
                        <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[10px] font-bold">
                          הושלם
                        </span>
                      )}
                    </td>
                    <td className="p-4 text-left">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => setViewingReceipt(rec)}
                          className="p-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600"
                          title="צפה והדפס קבלה"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        {!rec.is_refunded && (
                          <button
                            onClick={() => openRefundModal(rec)}
                            className="p-1.5 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold flex items-center gap-1"
                            title="בצע החזר / זיכוי"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                            <span>זיכוי</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Refund Modal */}
      {refundingReceipt && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="text-base font-black text-slate-900">
                  ביצוע זיכוי לקבלה #{refundingReceipt.receipt_number}
                </h3>
                <p className="text-xs text-slate-500">
                  בחר פריטים להחזרה, ציין סיבה. הפריטים יוחזרו אוטומטית למלאי.
                </p>
              </div>
              <button
                onClick={() => setRefundingReceipt(null)}
                className="p-1.5 rounded-xl bg-slate-100 text-slate-400 hover:text-slate-700"
              >
                ✕
              </button>
            </div>

            {/* Select items for partial / full refund */}
            <div className="space-y-2">
              <label className="block text-xs font-bold uppercase text-slate-500">
                בחר פריטים להחזרה:
              </label>
              <div className="space-y-1.5 max-h-48 overflow-y-auto">
                {refundingReceipt.items.map((item) => {
                  const isChecked = selectedItemIds.includes(item.cart_id)
                  return (
                    <div
                      key={item.cart_id}
                      onClick={() => {
                        if (isChecked) {
                          setSelectedItemIds(selectedItemIds.filter((id) => id !== item.cart_id))
                        } else {
                          setSelectedItemIds([...selectedItemIds, item.cart_id])
                        }
                      }}
                      className={`p-2.5 rounded-xl border cursor-pointer flex items-center justify-between text-xs ${
                        isChecked
                          ? 'border-sky-600 bg-sky-50 font-bold'
                          : 'border-slate-200 bg-white'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <div
                          className={`w-4 h-4 rounded-md border flex items-center justify-center ${
                            isChecked ? 'bg-sky-600 border-sky-600 text-white' : 'border-slate-300'
                          }`}
                        >
                          {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
                        </div>
                        <span>
                          {item.name} ({item.quantity}x)
                        </span>
                      </div>
                      <span className="font-mono">
                        {settings.currency_symbol}
                        {item.line_total.toFixed(2)}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Refund Reason */}
            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                סיבת ההחזר:
              </label>
              <select
                value={refundReason}
                onChange={(e) => setRefundReason(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-800"
              >
                <option>פגם במוצר / טעות במנה</option>
                <option>הלקוח התחרט / ביטול הזמנה</option>
                <option>חיוב שגוי בקופה</option>
                <option>אחר</option>
              </select>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setRefundingReceipt(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                ביטול
              </button>
              <button
                type="button"
                onClick={handleConfirmRefund}
                disabled={isProcessingRefund || selectedItemIds.length === 0}
                className="px-6 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-black shadow-md shadow-red-600/20 disabled:opacity-50"
              >
                {isProcessingRefund ? 'מעבד זיכוי...' : 'בצע זיכוי והחזר למלאי'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Receipt Preview Dialog */}
      <ReceiptPreviewModal
        receipt={viewingReceipt}
        isOpen={Boolean(viewingReceipt)}
        onClose={() => setViewingReceipt(null)}
      />
    </div>
  )
}
