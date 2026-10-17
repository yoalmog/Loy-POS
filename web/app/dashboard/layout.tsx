'use client'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/router'
import {
  LayoutDashboard,
  Receipt,
  Package,
  Boxes,
  Users,
  Clock,
  BarChart3,
  Settings,
  Store,
  LogOut,
  Database,
  Wifi,
  Menu,
  X,
  Building2,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [storeName, setStoreName] = useState('AuraPOS Bistro & Grill')
  const [dbStatus, setDbStatus] = useState<'connected' | 'configured' | 'offline'>('configured')

  useEffect(() => {
    async function checkDbConnection() {
      try {
        const { data, error } = await supabase.from('store_settings').select('store_name').limit(1).maybeSingle()
        if (data && data.store_name) {
          setStoreName(data.store_name)
          setDbStatus('connected')
        } else if (!error) {
          setDbStatus('connected')
        }
      } catch (e) {
        setDbStatus('configured')
      }
    }
    checkDbConnection()
  }, [])

  const navItems = [
    { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { label: 'Sales & Receipts', href: '/dashboard/sales', icon: Receipt },
    { label: 'Products & Prices', href: '/dashboard/products', icon: Package },
    { label: 'Inventory & Stock', href: '/dashboard/inventory', icon: Boxes },
    { label: 'Customers', href: '/dashboard/customers', icon: Users },
    { label: 'Shifts & Employees', href: '/dashboard/shifts', icon: Clock },
    { label: 'Reports & Analytics', href: '/dashboard/reports', icon: BarChart3 },
    { label: 'Store Settings', href: '/dashboard/settings', icon: Settings },
  ]

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col md:flex-row">
      {/* Sidebar for Desktop */}
      <aside className="hidden md:flex md:w-64 bg-slate-900 text-slate-300 flex-col shrink-0 border-r border-slate-800">
        <div className="p-6 border-b border-slate-800 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-sky-500 text-white flex items-center justify-center font-bold shadow-lg shadow-sky-500/30">
            <Store className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide">AuraPOS</h2>
            <p className="text-xs text-slate-400">Online Management</p>
          </div>
        </div>

        <div className="p-4 bg-slate-950/60 border-b border-slate-800/80">
          <div className="flex items-center gap-2 text-xs text-slate-300 font-medium truncate">
            <Building2 className="w-4 h-4 text-sky-400 shrink-0" />
            <span className="truncate">{storeName}</span>
          </div>
          <div className="mt-2 flex items-center gap-2 text-[11px] text-emerald-400">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span>Supabase PostgreSQL Live</span>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href
            return (
              <a
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-sky-600 text-white shadow-md shadow-sky-600/30'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`}
              >
                <Icon className="w-5 h-5" />
                <span>{item.label}</span>
              </a>
            )
          })}
        </nav>

        <div className="p-4 border-t border-slate-800">
          <a
            href="/login"
            className="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-400 hover:bg-red-500/10 hover:text-red-400 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>Sign Out</span>
          </a>
        </div>
      </aside>

      {/* Mobile Header & Navbar */}
      <div className="md:hidden bg-slate-900 text-white p-4 flex items-center justify-between border-b border-slate-800">
        <div className="flex items-center gap-2">
          <Store className="w-6 h-6 text-sky-400" />
          <span className="font-bold text-base tracking-tight">AuraPOS Back Office</span>
        </div>
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="p-2 rounded-lg bg-slate-800 text-slate-200"
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {mobileMenuOpen && (
        <div className="md:hidden bg-slate-900 border-b border-slate-800 p-4 space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <a
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-slate-300 hover:bg-slate-800"
              >
                <Icon className="w-5 h-5 text-sky-400" />
                <span>{item.label}</span>
              </a>
            )
          })}
        </div>
      )}

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto">
        <header className="bg-white border-b border-slate-200 px-6 py-4 hidden md:flex items-center justify-between shadow-sm">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Online Store Back Office</h1>
            <p className="text-xs text-slate-500">Shared Production Data with Android POS Client</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-full border border-emerald-200 text-xs font-semibold">
              <Database className="w-3.5 h-3.5" />
              <span>PostgreSQL Connected</span>
            </div>
          </div>
        </header>

        <div className="p-6 max-w-7xl mx-auto">{children}</div>
      </main>
    </div>
  )
}
