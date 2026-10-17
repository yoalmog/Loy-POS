'use client'

export const dynamic = 'force-dynamic'

import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
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
  const router = useRouter()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [storeName, setStoreName] = useState('AuraPOS Bistro & Grill')
  const [dbStatus, setDbStatus] = useState<'connected' | 'configured'>('connected')

  useEffect(() => {
    async function checkDbConnection() {
      try {
        const { data } = await supabase.from('store_settings').select('store_name').limit(1).maybeSingle()
        if (data && data.store_name) {
          setStoreName(data.store_name)
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

  async function handleLogout() {
    await supabase.auth.signOut()
    router.push('/login')
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col md:flex-row">
      <div className="md:hidden bg-slate-900 text-white p-4 flex items-center justify-between border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-sky-600">
            <Store className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-sm">{storeName}</h1>
            <p className="text-xs text-slate-400">Back Office</p>
          </div>
        </div>
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="p-2 rounded-xl bg-slate-800 text-slate-200 hover:bg-slate-700"
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-72 bg-slate-900 text-slate-300 flex flex-col transition-transform duration-300 md:static md:translate-x-0 ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="p-6 border-b border-slate-800 hidden md:flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-sky-600 shadow-lg shadow-sky-600/30">
            <Store className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="font-extrabold text-white text-base tracking-tight">{storeName}</h1>
            <p className="text-xs text-sky-400 font-medium">Online Back Office</p>
          </div>
        </div>

        <div className="px-6 py-4 bg-slate-950/50 border-b border-slate-800/60 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Database className="w-4 h-4 text-sky-400" />
            <span className="text-xs font-semibold text-slate-300">Supabase DB</span>
          </div>
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
            {dbStatus}
          </span>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition-all ${
                  isActive
                    ? 'bg-sky-600 text-white shadow-lg shadow-sky-600/25'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>

        <div className="p-4 border-t border-slate-800">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold text-red-400 hover:bg-red-500/10 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        <header className="bg-white border-b border-slate-200 px-8 py-4 hidden md:flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Building2 className="w-5 h-5 text-sky-600" />
            <h2 className="font-bold text-slate-800 text-lg">AuraPOS Management Portal</h2>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-100 text-xs font-semibold">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>Live Android POS Sync Active</span>
            </div>
          </div>
        </header>

        <div className="flex-1 p-6 md:p-10 max-w-7xl w-full mx-auto">{children}</div>
      </main>
    </div>
  )
}
