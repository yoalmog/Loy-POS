'use client'

import React, { useEffect, useState } from 'react'
import {
  BarChart3,
  Download,
  Calendar,
  PieChart as PieIcon,
  TrendingUp,
  DollarSign,
  FileSpreadsheet,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Receipt } from '@/lib/types'

export default function ReportsPage() {
  const [receipts, setReceipts] = useState<Receipt[]>([])
  const [loading, setLoading] = useState(true)

  async function fetchReportsData() {
    setLoading(true)
    try {
      const { data } = await supabase.from('receipts').select('*').order('created_at', { ascending: false })
      if (data) setReceipts(data)
    } catch (err) {
      console.error('Error fetching reports data:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchReportsData()
  }, [])

  const grossSales = receipts.reduce((acc, r) => acc + (r.subtotal || 0), 0)
  const totalTax = receipts.reduce((acc, r) => acc + (r.tax_amount || 0), 0)
  const totalTips = receipts.reduce((acc, r) => acc + (r.tip_amount || 0), 0)
  const netSales = receipts.reduce((acc, r) => acc + (r.total_amount || 0), 0)

  const cashCount = receipts.filter((r) => r.payment_type === 'CASH').length
  const cardCount = receipts.filter((r) => r.payment_type === 'CARD').length

  function exportToCsv() {
    if (receipts.length === 0) return
    const headers = ['Receipt #', 'Date', 'Cashier', 'Payment Type', 'Subtotal', 'Tax', 'Total']
    const rows = receipts.map((r) => [
      r.receipt_number,
      new Date(r.created_at).toLocaleString(),
      r.cashier_name,
      r.payment_type,
      (r.subtotal || 0).toFixed(2),
      (r.tax_amount || 0).toFixed(2),
      (r.total_amount || 0).toFixed(2),
    ])

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n')
    const encodedUri = encodeURI(csvContent)
    const link = document.createElement('a')
    link.setAttribute('href', encodedUri)
    link.setAttribute('download', `AuraPOS_Sales_Report_${new Date().toISOString().slice(0, 10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Financial Reports & Analytics</h2>
          <p className="text-sm text-slate-500 mt-1">Calculated directly from real database records</p>
        </div>
        <button
          onClick={exportToCsv}
          disabled={receipts.length === 0}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white font-semibold rounded-xl text-sm shadow-md transition-all shrink-0 disabled:opacity-50"
        >
          <FileSpreadsheet className="w-4 h-4" />
          Export Sales CSV
        </button>
      </div>

      {/* Financial Summary Breakdown */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-xs font-bold uppercase text-slate-500">Gross Subtotal</span>
          <div className="text-2xl font-extrabold text-slate-900 mt-2">${grossSales.toFixed(2)}</div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-xs font-bold uppercase text-slate-500">Tax Collected</span>
          <div className="text-2xl font-extrabold text-slate-900 mt-2">${totalTax.toFixed(2)}</div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-xs font-bold uppercase text-slate-500">Tips Received</span>
          <div className="text-2xl font-extrabold text-slate-900 mt-2">${totalTips.toFixed(2)}</div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-xs font-bold uppercase text-slate-500">Net Sales Total</span>
          <div className="text-2xl font-extrabold text-emerald-600 mt-2">${netSales.toFixed(2)}</div>
        </div>
      </div>

      {/* Payment Type Distribution */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h3 className="text-lg font-bold text-slate-900 mb-4">Payment Method Breakdown</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-between">
            <div>
              <div className="text-sm font-bold text-emerald-900">Cash Transactions</div>
              <div className="text-xs text-emerald-700 mt-0.5">{cashCount} receipts completed</div>
            </div>
            <div className="text-xl font-extrabold text-emerald-900">
              ${receipts.filter((r) => r.payment_type === 'CASH').reduce((a, b) => a + (b.total_amount || 0), 0).toFixed(2)}
            </div>
          </div>

          <div className="p-4 rounded-xl bg-sky-50 border border-sky-100 flex items-center justify-between">
            <div>
              <div className="text-sm font-bold text-sky-900">Card / Digital Payments</div>
              <div className="text-xs text-sky-700 mt-0.5">{cardCount} receipts completed</div>
            </div>
            <div className="text-xl font-extrabold text-sky-900">
              ${receipts.filter((r) => r.payment_type === 'CARD').reduce((a, b) => a + (b.total_amount || 0), 0).toFixed(2)}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
