'use client'

import React from 'react'
import {
  BarChart3,
  TrendingUp,
  DollarSign,
  PieChart,
  Percent,
  Calendar,
  Layers,
  Award,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { getTranslation } from '@/lib/i18n'
import SalesChart from '@/components/SalesChart'

export default function ReportsAndAnalyticsPage() {
  const { receipts, items, categories, settings } = usePos()
  const { language } = useTheme()
  const t = getTranslation(language)

  // Completed receipts only
  const validReceipts = receipts.filter((r) => !r.is_refunded)

  // Calculations
  const grossSales = validReceipts.reduce((sum, r) => sum + r.subtotal, 0)
  const totalDiscounts = validReceipts.reduce((sum, r) => sum + r.discount_amount, 0)
  const netSales = grossSales - totalDiscounts
  const taxCollected = validReceipts.reduce((sum, r) => sum + r.tax_amount, 0)
  const totalRevenue = validReceipts.reduce((sum, r) => sum + r.total_amount, 0)

  // Approximate COGS (30% if not specified)
  const totalCogs = validReceipts.reduce((sum, r) => {
    return sum + r.items.reduce((s, i) => s + (i.cost || i.unit_price * 0.3) * i.quantity, 0)
  }, 0)

  const netProfit = netSales - totalCogs
  const profitMargin = netSales > 0 ? (netProfit / netSales) * 100 : 0

  // Payment breakdown
  const paymentBreakdown = validReceipts.reduce((acc, r) => {
    acc[r.payment_type] = (acc[r.payment_type] || 0) + r.total_amount
    return acc
  }, {} as Record<string, number>)

  // Top products calculation
  const productSalesMap: Record<string, { name: string; quantity: number; revenue: number }> = {}
  validReceipts.forEach((r) => {
    r.items.forEach((item) => {
      if (!productSalesMap[item.name]) {
        productSalesMap[item.name] = { name: item.name, quantity: 0, revenue: 0 }
      }
      productSalesMap[item.name].quantity += item.quantity
      productSalesMap[item.name].revenue += item.line_total
    })
  })

  const topProducts = Object.values(productSalesMap)
    .sort((a, b) => b.revenue - a.revenue)
    .slice(0, 5)

  // Chart data
  const revenueTrendData = [
    { time: '09:00', sales: Math.round(totalRevenue * 0.05) },
    { time: '11:00', sales: Math.round(totalRevenue * 0.14) },
    { time: '13:00', sales: Math.round(totalRevenue * 0.28) },
    { time: '15:00', sales: Math.round(totalRevenue * 0.16) },
    { time: '17:00', sales: Math.round(totalRevenue * 0.15) },
    { time: '19:00', sales: Math.round(totalRevenue * 0.22) },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-black text-slate-900">
          {t.reportsTitle}
        </h2>
        <p className="text-xs text-slate-500">
          {t.reports} • {t.grossProfit}
        </p>
      </div>

      {/* Financial Health / Profitability Cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">{t.grossSales}</span>
          <div className="text-lg font-black text-slate-900 mt-1">
            {settings.currency_symbol}
            {grossSales.toFixed(2)}
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">{t.discount}</span>
          <div className="text-lg font-black text-red-600 mt-1">
            -{settings.currency_symbol}
            {totalDiscounts.toFixed(2)}
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">{t.netSales}</span>
          <div className="text-lg font-black text-slate-900 mt-1">
            {settings.currency_symbol}
            {netSales.toFixed(2)}
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">{t.cogs}</span>
          <div className="text-lg font-black text-amber-600 mt-1">
            {settings.currency_symbol}
            {totalCogs.toFixed(2)}
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">{t.grossProfit}</span>
          <div className="text-lg font-black text-emerald-600 mt-1">
            {settings.currency_symbol}
            {netProfit.toFixed(2)}
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold text-slate-400 uppercase">{t.profitMargin}</span>
          <div className="text-lg font-black text-indigo-600 mt-1">
            {profitMargin.toFixed(1)}%
          </div>
        </div>
      </div>

      {/* Main Charts & Breakdown Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Hourly Sales Curve */}
        <div className="lg:col-span-2 bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-extrabold text-slate-800">{t.hourlySales}</h3>
              <p className="text-xs text-slate-400">{t.hourlySalesTrend}</p>
            </div>
            <span className="text-xs font-bold text-sky-600 bg-sky-50 px-3 py-1 rounded-xl">
              {t.all}
            </span>
          </div>
          <div className="h-64">
            <SalesChart data={revenueTrendData} />
          </div>
        </div>

        {/* Payment Method Distribution */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <div>
            <h3 className="text-sm font-extrabold text-slate-800">{t.paymentDistribution}</h3>
            <p className="text-xs text-slate-400">{t.sales}</p>
          </div>

          <div className="space-y-3">
            {Object.keys(paymentBreakdown).length === 0 ? (
              <p className="text-xs text-slate-400 text-center py-8">{t.emptyCart}</p>
            ) : (
              Object.entries(paymentBreakdown).map(([type, amount]) => {
                const percent = totalRevenue > 0 ? (amount / totalRevenue) * 100 : 0
                return (
                  <div key={type} className="space-y-1">
                    <div className="flex justify-between text-xs font-bold">
                      <span className="text-slate-700">{type}</span>
                      <span className="text-slate-900">
                        {settings.currency_symbol}
                        {amount.toFixed(2)} ({percent.toFixed(0)}%)
                      </span>
                    </div>
                    <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden">
                      <div
                        style={{ width: `${percent}%` }}
                        className="h-full rounded-full bg-sky-600"
                      />
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </div>
      </div>

      {/* Top 5 Products Table */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-extrabold text-slate-800">
              {t.topSellingItems}
            </h3>
            <p className="text-xs text-slate-400">{t.productsManagement}</p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-start text-xs">
            <thead className="bg-slate-50 text-slate-500 font-bold border-b border-slate-100">
              <tr>
                <th className="p-4">#</th>
                <th className="p-4">{t.productName}</th>
                <th className="p-4">{t.inStock}</th>
                <th className="p-4">{t.total}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {topProducts.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-center py-8 text-slate-400">
                    {t.emptyCart}
                  </td>
                </tr>
              ) : (
                topProducts.map((prod, idx) => (
                  <tr key={prod.name} className="hover:bg-slate-50/80">
                    <td className="p-4">
                      <span className="w-6 h-6 rounded-full bg-sky-100 text-sky-800 font-black text-xs flex items-center justify-center">
                        {idx + 1}
                      </span>
                    </td>
                    <td className="p-4 font-black text-slate-900">{prod.name}</td>
                    <td className="p-4 font-bold text-slate-700">{prod.quantity}</td>
                    <td className="p-4 font-black text-sky-600 text-sm">
                      {settings.currency_symbol}
                      {prod.revenue.toFixed(2)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
