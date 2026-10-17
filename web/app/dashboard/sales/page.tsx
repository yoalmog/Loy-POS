'use client'

import React, { useEffect, useState } from 'react'
import {
  Search,
  Filter,
  Receipt as ReceiptIcon,
  RotateCcw,
  Calendar,
  CreditCard,
  Banknote,
  X,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Receipt } from '@/lib/types'

export default function SalesPage() {
  const [receipts, setReceipts] = useState<Receipt[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [paymentFilter, setPaymentFilter] = useState('ALL')
  const [selectedReceipt, setSelectedReceipt] = useState<Receipt | null>(null)
  const [refundLoading, setRefundLoading] = useState(false)
  const [message, setMessage] = useState('')

  async function fetchSalesReceipts() {
    setLoading(true)
    try {
      let query = supabase.from('receipts').select('*').order('created_at', { ascending: false })
      if (paymentFilter !== 'ALL') {
        query = query.eq('payment_type', paymentFilter)
      }
      const { data, error } = await query
      if (data) {
        setReceipts(data)
      }
    } catch (err) {
      console.error('Failed to fetch receipts:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSalesReceipts()
  }, [paymentFilter])

  async function handleRefundReceipt(receiptId: string) {
    setRefundLoading(true)
    setMessage('')
    try {
      const { error } = await supabase
        .from('receipts')
        .update({
          is_refunded: true,
          refund_timestamp: new Date().toISOString(),
        })
        .eq('id', receiptId)

      if (error) throw error

      setMessage('Receipt successfully refunded.')
      setSelectedReceipt(null)
      fetchSalesReceipts()
    } catch (err: any) {
      setMessage(`Refund failed: ${err.message}`)
    } finally {
      setRefundLoading(false)
    }
  }

  const filteredReceipts = receipts.filter(
    (r) =>
      r.receipt_number.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (r.customer_name && r.customer_name.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (r.cashier_name && r.cashier_name.toLowerCase().includes(searchQuery.toLowerCase()))
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Sales Transactions & Receipts</h2>
          <p className="text-sm text-slate-500 mt-1">Audit, filter, inspect, and refund completed store receipts</p>
        </div>
      </div>

      {message && (
        <div className="p-4 rounded-xl bg-sky-50 border border-sky-200 text-sky-800 text-sm flex items-center justify-between">
          <span>{message}</span>
          <button onClick={() => setMessage('')} className="text-sky-600 font-bold text-xs">Dismiss</button>
        </div>
      )}

      {/* Filters & Search */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by receipt #, customer, or cashier..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={paymentFilter}
            onChange={(e) => setPaymentFilter(e.target.value)}
            className="py-2 px-3 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 bg-white"
          >
            <option value="ALL">All Payment Types</option>
            <option value="CASH">Cash Sales</option>
            <option value="CARD">Card / Digital Sales</option>
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[11px] font-bold tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Receipt #</th>
                <th className="px-6 py-3.5">Date & Time</th>
                <th className="px-6 py-3.5">Cashier</th>
                <th className="px-6 py-3.5">Customer</th>
                <th className="px-6 py-3.5">Payment</th>
                <th className="px-6 py-3.5">Total Amount</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-slate-400">
                    Loading database transactions...
                  </td>
                </tr>
              ) : filteredReceipts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-slate-400">
                    No matching sales receipts found in database.
                  </td>
                </tr>
              ) : (
                filteredReceipts.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 font-bold text-slate-900">{r.receipt_number}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {new Date(r.created_at).toLocaleString()}
                    </td>
                    <td className="px-6 py-4">{r.cashier_name}</td>
                    <td className="px-6 py-4 text-slate-600">{r.customer_name || 'Walk-in Guest'}</td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 text-xs font-medium">
                        {r.payment_type === 'CASH' ? <Banknote className="w-3.5 h-3.5 text-emerald-600" /> : <CreditCard className="w-3.5 h-3.5 text-sky-600" />}
                        {r.payment_type}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-extrabold text-slate-900">
                      ${(r.total_amount || 0).toFixed(2)}
                    </td>
                    <td className="px-6 py-4">
                      {r.is_refunded ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-red-100 text-red-700 text-xs font-semibold">
                          Refunded
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-semibold">
                          Paid
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => setSelectedReceipt(r)}
                        className="px-3 py-1.5 rounded-lg bg-sky-50 text-sky-700 hover:bg-sky-100 font-medium text-xs transition-colors"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Receipt Details Drawer Modal */}
      {selectedReceipt && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Receipt Details</h3>
                <p className="text-xs text-slate-500">{selectedReceipt.receipt_number}</p>
              </div>
              <button
                onClick={() => setSelectedReceipt(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="py-4 space-y-3 text-sm">
              <div className="flex justify-between text-xs text-slate-500">
                <span>Date & Time</span>
                <span className="font-medium text-slate-800">{new Date(selectedReceipt.created_at).toLocaleString()}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-500">
                <span>Cashier</span>
                <span className="font-medium text-slate-800">{selectedReceipt.cashier_name}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-500">
                <span>Customer</span>
                <span className="font-medium text-slate-800">{selectedReceipt.customer_name || 'Walk-in'}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-500">
                <span>Dining Option</span>
                <span className="font-medium text-slate-800">{selectedReceipt.dining_option}</span>
              </div>

              <div className="my-3 p-3 bg-slate-50 rounded-xl text-xs space-y-1 text-slate-600">
                <div className="font-semibold text-slate-800 mb-1">Purchased Items Summary:</div>
                <p className="italic">{selectedReceipt.items_summary_json || 'Detailed line items recorded in POS'}</p>
              </div>

              <div className="pt-3 border-t border-slate-100 space-y-1 text-xs">
                <div className="flex justify-between">
                  <span>Subtotal</span>
                  <span>${(selectedReceipt.subtotal || 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Tax Amount</span>
                  <span>${(selectedReceipt.tax_amount || 0).toFixed(2)}</span>
                </div>
                {selectedReceipt.tip_amount > 0 && (
                  <div className="flex justify-between">
                    <span>Tip</span>
                    <span>${selectedReceipt.tip_amount.toFixed(2)}</span>
                  </div>
                )}
                <div className="flex justify-between text-base font-bold text-slate-900 pt-2 border-t border-slate-200">
                  <span>Total Paid</span>
                  <span>${(selectedReceipt.total_amount || 0).toFixed(2)}</span>
                </div>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
              {!selectedReceipt.is_refunded && (
                <button
                  onClick={() => handleRefundReceipt(selectedReceipt.id)}
                  disabled={refundLoading}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-semibold shadow-md flex items-center gap-1.5 transition-all"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  {refundLoading ? 'Processing Refund...' : 'Issue Full Refund'}
                </button>
              )}
              <button
                onClick={() => setSelectedReceipt(null)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-medium"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
