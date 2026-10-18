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
  ShoppingCart,
  Wifi,
  WifiOff,
  Sparkles,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { supabase } from '@/lib/supabase/client'

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const router = useRouter()
  const { settings, currentStaff, syncStatus } = usePos()
  const { t, isRtl } = useTheme()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null)

  useEffect(() => {
    async function checkAuth() {
      // 1. Check Supabase active session
      try {
        const { data: { session } } = await supabase.auth.getSession()
        if (session) {
          setIsAuthenticated(true)
          return
        }
      } catch (e) {
        // Fallback to local staff session check
      }

      // 2. Check local authenticated staff in localStorage
      const staffAuth = typeof window !== 'undefined' ? localStorage.getItem('aurapos_auth_staff') : null
      if (staffAuth) {
        try {
          const parsed = JSON.parse(staffAuth)
          if (parsed && (parsed.role === 'OWNER' || parsed.role === 'MANAGER')) {
            setIsAuthenticated(true)
            return
          }
        } catch {}
      }

      // 3. Check currentStaff in PosContext
      if (currentStaff && (currentStaff.role === 'OWNER' || currentStaff.role === 'MANAGER')) {
        setIsAuthenticated(true)
        return
      }

      // Not authenticated -> redirect to login
      setIsAuthenticated(false)
      router.replace('/login')
    }

    checkAuth()
  }, [currentStaff, router])

  async function handleLogout() {
    try {
      await supabase.auth.signOut()
    } catch {}
    if (typeof window !== 'undefined') {
      localStorage.removeItem('aurapos_auth_staff')
    }
    router.replace('/login')
  }

  const navItems = [
    { label: t.dashboard, href: '/dashboard', icon: LayoutDashboard },
    { label: t.sales, href: '/dashboard/sales', icon: Receipt },
    { label: t.products, href: '/dashboard/products', icon: Package },
    { label: t.inventory, href: '/dashboard/inventory', icon: Boxes },
    { label: t.customers, href: '/dashboard/customers', icon: Users },
    { label: t.shifts, href: '/dashboard/shifts', icon: Clock },
    { label: t.reports, href: '/dashboard/reports', icon: BarChart3 },
    { label: t.settings, href: '/dashboard/settings', icon: Settings },
  ]

  if (isAuthenticated === null) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="p-4 rounded-2xl bg-sky-600 animate-pulse text-white shadow-xl shadow-sky-600/30">
            <Store className="w-8 h-8" />
          </div>
          <p className="text-xs font-bold text-slate-400">{t.loading}</p>
        </div>
      </div>
    )
  }

  if (isAuthenticated === false) {
    return null
  }

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col md:flex-row text-slate-800">
      {/* Mobile Top Header */}
      <div className="md:hidden bg-slate-900 text-white p-4 flex items-center justify-between border-b border-slate-800 sticky top-0 z-40">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-sky-600">
            <Store className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-sm">{settings.store_name}</h1>
            <p className="text-xs text-slate-400">{t.backOffice}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Link
            href="/pos"
            className="p-2 rounded-xl bg-sky-600 text-white text-xs font-bold flex items-center gap-1"
          >
            <ShoppingCart className="w-4 h-4" />
            <span>{t.register}</span>
          </Link>
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 rounded-xl bg-slate-800 text-slate-200 hover:bg-slate-700"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Sidebar */}
      <aside
        className={`fixed inset-y-0 ${
          isRtl ? 'right-0' : 'left-0'
        } z-50 w-72 bg-slate-900 text-slate-300 flex flex-col transition-transform duration-300 md:static md:translate-x-0 ${
          mobileMenuOpen
            ? 'translate-x-0'
            : isRtl
            ? 'translate-x-full'
            : '-translate-x-full'
        }`}
      >
        {/* Brand Banner */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-sky-600 shadow-lg shadow-sky-600/30">
              <Store className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="font-extrabold text-white text-sm tracking-tight">
                {settings.store_name}
              </h1>
              <p className="text-[11px] text-sky-400 font-medium">{t.backOffice}</p>
            </div>
          </div>
        </div>

        {/* Big Switch to POS Button */}
        <div className="p-4 border-b border-slate-800">
          <Link
            href="/pos"
            onClick={() => setMobileMenuOpen(false)}
            className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-extrabold text-xs shadow-lg shadow-sky-600/25 flex items-center justify-center gap-2 transition-transform active:scale-95"
          >
            <ShoppingCart className="w-4 h-4" />
            <span>{t.switchToPos}</span>
          </Link>
        </div>

        {/* Sync Status Badge */}
        <div className="px-5 py-3 bg-slate-950/60 border-b border-slate-800/80 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            {syncStatus.isOnline ? (
              <Wifi className="w-4 h-4 text-emerald-400" />
            ) : (
              <WifiOff className="w-4 h-4 text-amber-500" />
            )}
            <span className="font-semibold text-slate-300">
              {syncStatus.isOnline ? t.onlineCloud : t.offlineMode}
            </span>
          </div>
          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
            {syncStatus.isOnline ? t.online : t.offline}
          </span>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className={`flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all ${
                  isActive
                    ? 'bg-sky-600 text-white shadow-md shadow-sky-600/25'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>

        {/* Footer: Current Staff & Logout */}
        <div className="p-4 border-t border-slate-800 space-y-2">
          <div className="flex items-center gap-2.5 px-2">
            <div
              style={{ backgroundColor: currentStaff.avatarColor }}
              className="w-7 h-7 rounded-full flex items-center justify-center text-white text-[11px] font-bold"
            >
              {currentStaff.name.charAt(0)}
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold text-white truncate">{currentStaff.name}</p>
              <p className="text-[10px] text-slate-400">{currentStaff.role}</p>
            </div>
          </div>

          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-red-400 hover:bg-red-500/10 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span>{t.signOut}</span>
          </button>
        </div>
      </aside>

      {/* Main Content Viewport */}
      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        <header className="bg-white border-b border-slate-200 px-8 py-3.5 hidden md:flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Building2 className="w-5 h-5 text-sky-600" />
            <h2 className="font-extrabold text-slate-800 text-sm">
              {t.portalTitle} • {settings.store_name}
            </h2>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/pos"
              className="px-3.5 py-1.5 rounded-xl bg-sky-50 text-sky-700 border border-sky-200 text-xs font-bold hover:bg-sky-100 flex items-center gap-1.5 transition-colors"
            >
              <ShoppingCart className="w-4 h-4" />
              <span>{t.register}</span>
            </Link>
          </div>
        </header>

        <div className="flex-1 p-4 md:p-8 max-w-7xl w-full mx-auto">{children}</div>
      </main>
    </div>
  )
}
