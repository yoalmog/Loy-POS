'use client'

import React from 'react'
import Link from 'next/link'
import {
  TrendingUp,
  Receipt,
  Package,
  Users,
  DollarSign,
  AlertTriangle,
  ArrowUpRight,
  ShoppingCart,
  Clock,
  Sparkles,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import SalesChart from '@/components/SalesChart'

export default function DashboardOverviewPage() {
  const { receipts, items, customers, settings } = usePos()
  const { t, isRtl } = useTheme()

  // Calculate high-level metrics
  const totalRevenue = receipts.reduce((sum, r) => sum + (r.is_refunded ? 0 : r.total_amount), 0)
  const totalOrders = receipts.filter((r) => !r.is_refunded).length
  const averageTicket = totalOrders > 0 ? totalRevenue / totalOrders : 0

  const lowStockItems = items.filter(
    (i) => i.track_stock && i.in_stock <= i.low_stock_threshold
  )

  // Hourly curve data for chart
  const hourlyData = [
    { time: '09:00', sales: Math.round(totalRevenue * 0.05) },
    { time: '11:00', sales: Math.round(totalRevenue * 0.12) },
    { time: '13:00', sales: Math.round(totalRevenue * 0.28) },
    { time: '15:00', sales: Math.round(totalRevenue * 0.18) },
    { time: '17:00', sales: Math.round(totalRevenue * 0.15) },
    { time: '19:00', sales: Math.round(totalRevenue * 0.22) },
  ]

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-sky-950 to-slate-900 text-white p-6 rounded-3xl shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <span className="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-sky-500/20 text-sky-300 border border-sky-400/30">
            {t.dashboard}
          </span>
          <h2 className="text-2xl font-black mt-2">
            {t.welcomeTitle} {settings.store_name}
          </h2>
          <p className="text-xs text-slate-300 mt-1">
            {t.welcomeSubtitle}
          </p>
        </div>

        <Link
          href="/pos"
          className="py-3 px-6 rounded-2xl bg-sky-500 hover:bg-sky-400 text-white font-extrabold text-xs shadow-lg shadow-sky-500/30 flex items-center gap-2 transition-transform active:scale-95"
        >
          <ShoppingCart className="w-4 h-4" />
          <span>{t.switchToPos}</span>
        </Link>
      </div>

      {/* KPI Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Revenue */}
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              {t.totalRevenue}
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">
              {settings.currency_symbol}
              {totalRevenue.toFixed(2)}
            </div>
            <span className="text-[11px] font-bold text-emerald-600 flex items-center gap-1 mt-1">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>{t.onlineCloud}</span>
            </span>
          </div>
          <div className="p-3.5 rounded-2xl bg-emerald-50 text-emerald-600">
            <DollarSign className="w-6 h-6" />
          </div>
        </div>

        {/* Card 2: Orders Count */}
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              {t.totalOrders}
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">{totalOrders}</div>
            <span className="text-[11px] text-slate-500 mt-1 block">
              {t.averageTicket}: {settings.currency_symbol}
              {averageTicket.toFixed(2)}
            </span>
          </div>
          <div className="p-3.5 rounded-2xl bg-sky-50 text-sky-600">
            <Receipt className="w-6 h-6" />
          </div>
        </div>

        {/* Card 3: Items in Catalog */}
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              {t.products}
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">{items.length}</div>
            <span className="text-[11px] text-slate-500 mt-1 block">
              {items.filter((i) => i.is_favorite).length} {t.favorites}
            </span>
          </div>
          <div className="p-3.5 rounded-2xl bg-purple-50 text-purple-600">
            <Package className="w-6 h-6" />
          </div>
        </div>

        {/* Card 4: Customers & Loyalty */}
        <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              {t.customers}
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">{customers.length}</div>
            <span className="text-[11px] text-amber-600 font-bold mt-1 block">
              ★ {customers.reduce((s, c) => s + c.points_balance, 0)} {t.points}
            </span>
          </div>
          <div className="p-3.5 rounded-2xl bg-amber-50 text-amber-600">
            <Users className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Main Row: Chart & Low Stock Warnings */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Sales Chart */}
        <div className="lg:col-span-2 bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-extrabold text-slate-800">{t.hourlySalesTrend}</h3>
              <p className="text-xs text-slate-400">{t.hourlySales}</p>
            </div>
            <span className="text-xs font-bold text-sky-600 bg-sky-50 px-3 py-1 rounded-xl">
              {t.status}
            </span>
          </div>
          <div className="h-64">
            <SalesChart data={hourlyData} />
          </div>
        </div>

        {/* Low Stock Alerts */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-500" />
                <h3 className="text-sm font-extrabold text-slate-800">{t.lowStockAlertCount}</h3>
              </div>
              <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                {lowStockItems.length}
              </span>
            </div>

            <div className="space-y-2.5 max-h-56 overflow-y-auto">
              {lowStockItems.length === 0 ? (
                <p className="text-xs text-slate-400 text-center py-6">
                  {t.inStock}
                </p>
              ) : (
                lowStockItems.map((item) => (
                  <div
                    key={item.id}
                    className="p-3 rounded-2xl bg-red-50/60 border border-red-100 flex items-center justify-between"
                  >
                    <div>
                      <h4 className="text-xs font-black text-slate-800">{item.name}</h4>
                      <span className="text-[10px] text-slate-500">
                        {t.lowStockThreshold}: {item.low_stock_threshold}
                      </span>
                    </div>
                    <span className="text-xs font-black text-red-600">
                      {t.inStock}: {item.in_stock}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>

          <Link
            href="/dashboard/inventory"
            className="w-full py-2.5 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50 text-xs font-bold text-center block"
          >
            {t.inventoryManagement} &rarr;
          </Link>
        </div>
      </div>

      {/* Recent Receipts Table */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-extrabold text-slate-800">{t.sales}</h3>
            <p className="text-xs text-slate-400">{t.receiptPreview}</p>
          </div>
          <Link
            href="/dashboard/sales"
            className="text-xs font-bold text-sky-600 hover:underline"
          >
            {t.sales} ({receipts.length}) &rarr;
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className={`w-full ${isRtl ? 'text-right' : 'text-left'} text-xs`}>
            <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100">
              <tr>
                <th className="p-4">{t.receiptPreview}</th>
                <th className="p-4">{t.dateTime}</th>
                <th className="p-4">{t.shifts}</th>
                <th className="p-4">{t.customer}</th>
                <th className="p-4">{t.paymentDistribution}</th>
                <th className="p-4">{t.total}</th>
                <th className="p-4">{t.status}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {receipts.slice(0, 5).map((rec) => (
                <tr key={rec.id} className="hover:bg-slate-50/70">
                  <td className="p-4 font-black font-mono text-slate-800">{rec.receipt_number}</td>
                  <td className="p-4 text-slate-500">
                    {new Date(rec.created_at).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </td>
                  <td className="p-4 text-slate-700">{rec.cashier_name}</td>
                  <td className="p-4 text-slate-700 font-medium">
                    {rec.customer_name || t.standardTier}
                  </td>
                  <td className="p-4">
                    <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 text-[11px] font-bold">
                      {rec.payment_type}
                    </span>
                  </td>
                  <td className="p-4 font-black text-slate-900">
                    {settings.currency_symbol}
                    {rec.total_amount.toFixed(2)}
                  </td>
                  <td className="p-4">
                    {rec.is_refunded ? (
                      <span className="px-2 py-0.5 rounded-full bg-red-100 text-red-700 text-[10px] font-bold">
                        {t.refund}
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[10px] font-bold">
                        {t.completed}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
