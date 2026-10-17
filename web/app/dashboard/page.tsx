'use client'

import React, { useEffect, useState } from 'react'
import {
  DollarSign,
  ShoppingCart,
  TrendingUp,
  Clock,
  ArrowUpRight,
  RefreshCw,
  AlertCircle,
  ReceiptCheck,
  Package,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Receipt, Item } from '@/lib/types'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts'

export default function DashboardPage() {
  const [loading, setLoading] = useState(true)
  const [totalRevenue, setTotalRevenue] = useState(0)
  const [transactionCount, setTransactionCount] = useState(0)
  const [avgTicket, setAvgTicket] = useState(0)
  const [recentReceipts, setRecentReceipts] = useState<Receipt[]>([])
  const [chartData, setChartData] = useState<{ time: string; sales: number }[]>([])
  const [lowStockCount, setLowStockCount] = useState(0)

  async function loadDashboardData() {
    setLoading(true)
    try {
      // 1. Fetch Receipts
      const { data: receiptsData, error: receiptsError } = await supabase
        .from('receipts')
        .select('*')
        .order('created_at', { ascending: false })

      if (receiptsData && receiptsData.length > 0) {
        const total = receiptsData.reduce((acc, r) => acc + (r.total_amount || 0), 0)
        const count = receiptsData.length
        setTotalRevenue(total)
        setTransactionCount(count)
        setAvgTicket(count > 0 ? total / count : 0)
        setRecentReceipts(receiptsData.slice(0, 5))

        // Aggregate by date/hour for chart
        const salesByHour: { [key: string]: number } = {}
        receiptsData.forEach((r) => {
          const dateStr = new Date(r.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          salesByHour[dateStr] = (salesByHour[dateStr] || 0) + (r.total_amount || 0)
        })

        const formattedChart = Object.keys(salesByHour).map((key) => ({
          time: key,
          sales: salesByHour[key],
        }))
        setChartData(formattedChart.length > 0 ? formattedChart : generateDefaultTrend())
      } else {
        setChartData([])
      }

      // 2. Fetch Low Stock Count
      const { data: itemsData } = await supabase
        .from('items')
        .select('*')
        .filter('track_stock', 'eq', true)

      if (itemsData) {
        const low = itemsData.filter((i: Item) => i.in_stock <= i.low_stock_threshold).length
        setLowStockCount(low)
      }
    } catch (err) {
      console.error('Error loading dashboard data:', err)
    } finally {
      setLoading(false)
    }
  }

  function generateDefaultTrend() {
    return [
      { time: '09:00 AM', sales: 0 },
      { time: '11:00 AM', sales: 0 },
      { time: '01:00 PM', sales: 0 },
      { time: '03:00 PM', sales: 0 },
      { time: '05:00 PM', sales: 0 },
    ]
  }

  useEffect(() => {
    loadDashboardData()
  }, [])

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Executive Sales Summary</h2>
          <p className="text-sm text-slate-500 mt-1">Live synchronized data from Android POS & Supabase PostgreSQL</p>
        </div>
        <button
          onClick={loadDashboardData}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium transition-all shadow-md shrink-0"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh Metrics
        </button>
      </div>

      {/* KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Gross Sales</span>
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <DollarSign className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-slate-900">${totalRevenue.toFixed(2)}</div>
            <p className="text-xs text-slate-500 mt-1">Total revenue recorded in database</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Total Transactions</span>
            <div className="w-10 h-10 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
              <ShoppingCart className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-slate-900">{transactionCount}</div>
            <p className="text-xs text-slate-500 mt-1">Completed orders</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Average Order Value</span>
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-slate-900">${avgTicket.toFixed(2)}</div>
            <p className="text-xs text-slate-500 mt-1">Revenue per receipt</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Low Stock Items</span>
            <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
              <Package className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-slate-900">{lowStockCount}</div>
            <p className="text-xs text-amber-600 font-medium mt-1">Requires restock attention</p>
          </div>
        </div>
      </div>

      {/* Chart & Recent Receipts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Sales Trend Line Chart */}
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="text-lg font-bold text-slate-900">Sales Trend & Hourly Revenue</h3>
            <p className="text-xs text-slate-500 mt-0.5">Real-time revenue timeline generated from database receipts</p>
          </div>

          <div className="h-72 mt-6">
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="time" stroke="#64748b" fontSize={12} />
                  <YAxis stroke="#64748b" fontSize={12} tickFormatter={(val) => `$${val}`} />
                  <Tooltip formatter={(value: any) => [`$${Number(value).toFixed(2)}`, 'Revenue']} />
                  <Line type="monotone" dataKey="sales" stroke="#0288d1" strokeWidth={3} dot={{ r: 5 }} />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center border-2 border-dashed border-slate-100 rounded-xl text-slate-400 text-sm">
                <p>No transactions completed yet in database</p>
                <p className="text-xs text-slate-400 mt-1">Complete a sale on Android POS to see live chart</p>
              </div>
            )}
          </div>
        </div>

        {/* Recent Transactions List */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">Recent Receipts</h3>
              <a href="/dashboard/sales" className="text-xs font-semibold text-sky-600 hover:text-sky-700">
                View All
              </a>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">Latest sales synced from POS</p>
          </div>

          <div className="mt-6 space-y-3 flex-1 overflow-y-auto max-h-[300px]">
            {recentReceipts.length > 0 ? (
              recentReceipts.map((r) => (
                <div key={r.id} className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/50 flex items-center justify-between">
                  <div>
                    <div className="font-bold text-sm text-slate-900">{r.receipt_number}</div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      {new Date(r.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} • {r.payment_type}
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-extrabold text-sm text-slate-900">${(r.total_amount || 0).toFixed(2)}</div>
                    <span className="inline-block text-[10px] px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-semibold">
                      Completed
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-12 text-center text-slate-400 text-xs border border-dashed border-slate-200 rounded-xl">
                No receipts recorded yet
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
