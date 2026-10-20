'use client'

import React, { useState, useMemo } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import {
  Search,
  ShoppingCart,
  Trash2,
  Plus,
  Minus,
  Sparkles,
  Percent,
  Clock,
  User,
  CreditCard,
  Barcode,
  Lock,
  Layers,
  ArrowRight,
  Wifi,
  WifiOff,
  Star,
  Settings,
  MessageSquare,
  HelpCircle,
  Menu,
  X,
  Volume2,
  VolumeX,
  Sliders,
  Check,
  ChevronDown,
  LayoutDashboard,
  Receipt as ReceiptIcon,
  Package,
  Boxes,
  Users,
  BarChart3,
  Store,
  Building2,
  PanelLeftClose,
  PanelLeftOpen,
  LogOut,
} from 'lucide-react'
import { Item, Category, Receipt, getLocalizedName, matchItemSearch } from '@/lib/types'
import { usePos } from '@/lib/context/PosContext'
import { useAccessibility } from '@/lib/context/AccessibilityContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { supabase } from '@/lib/supabase/client'
import { PosTokens } from '@/lib/tokens'

// Modals
import ItemCustomizationModal from '@/components/pos-dialogs/ItemCustomizationModal'
import CustomerSelectModal from '@/components/pos-dialogs/CustomerSelectModal'
import HeldOrdersModal from '@/components/pos-dialogs/HeldOrdersModal'
import DiscountsModal from '@/components/pos-dialogs/DiscountsModal'
import CheckoutModal from '@/components/pos-dialogs/CheckoutModal'
import ReceiptPreviewModal from '@/components/pos-dialogs/ReceiptPreviewModal'
import PinLockModal from '@/components/pos-dialogs/PinLockModal'
import CameraScannerModal from '@/components/pos-dialogs/CameraScannerModal'
import MenuScannerModal from '@/components/menu-scanner/MenuScannerModal'

export default function PosPage() {
  const router = useRouter()
  const {
    items,
    categories,
    cart,
    addToCart,
    updateCartItemQuantity,
    removeCartItem,
    clearCart,
    subtotal,
    totalDiscount,
    taxAmount,
    grandTotal,
    itemCount,
    diningOption,
    setDiningOption,
    ticketName,
    selectedCustomer,
    heldOrders,
    currentStaff,
    currentShift,
    syncStatus,
    settings,
    setIsLocked,
    syncNow,
  } = usePos()

  const {
    simplifiedPos,
    touchSize,
    soundEnabled,
    setSoundEnabled,
    announce,
    triggerSound,
    triggerHaptic,
  } = useAccessibility()

  const { t, isRtl, language } = useTheme()

  // Local UI State
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | 'ALL' | 'FAVORITES'>('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [customizingItem, setCustomizingItem] = useState<Item | null>(null)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  // Modals visibility
  const [isCustomerModalOpen, setIsCustomerModalOpen] = useState(false)
  const [isHeldOrdersModalOpen, setIsHeldOrdersModalOpen] = useState(false)
  const [isDiscountsModalOpen, setIsDiscountsModalOpen] = useState(false)
  const [isCheckoutModalOpen, setIsCheckoutModalOpen] = useState(false)
  const [isReceiptModalOpen, setIsReceiptModalOpen] = useState(false)
  const [completedReceipt, setCompletedReceipt] = useState<Receipt | null>(null)
  const [isCameraScannerOpen, setIsCameraScannerOpen] = useState(false)
  const [isMenuScannerOpen, setIsMenuScannerOpen] = useState(false)
  const [isMobileCartOpen, setIsMobileCartOpen] = useState(false)

  // Filter items
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      if (!item.is_active) return false

      // Category match
      if (selectedCategoryId === 'FAVORITES' && !item.is_favorite) return false
      if (
        typeof selectedCategoryId === 'number' &&
        item.category_id !== selectedCategoryId
      ) {
        return false
      }

      // Multilingual search match (localized names, SKU, barcode)
      if (searchQuery.trim()) {
        if (!matchItemSearch(item, searchQuery, language)) return false
      }

      return true
    })
  }, [items, selectedCategoryId, searchQuery, language])

  function handleItemClick(item: Item) {
    if (
      (item.variants && item.variants.length > 0) ||
      (item.modifier_groups && item.modifier_groups.length > 0)
    ) {
      setCustomizingItem(item)
    } else {
      addToCart(item)
    }
  }

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
    { label: t.register, href: '/pos', icon: ShoppingCart, isActive: true },
    { label: t.dashboard, href: '/dashboard', icon: LayoutDashboard },
    { label: t.sales, href: '/dashboard/sales', icon: ReceiptIcon },
    { label: t.products, href: '/dashboard/products', icon: Package },
    { label: t.inventory, href: '/dashboard/inventory', icon: Boxes },
    { label: t.customers, href: '/dashboard/customers', icon: Users },
    { label: t.shifts, href: '/dashboard/shifts', icon: Clock },
    { label: t.reports, href: '/dashboard/reports', icon: BarChart3 },
    { label: t.settings, href: '/dashboard/settings', icon: Settings },
  ]

  // Touch size class
  const touchPadding =
    touchSize === 'jumbo' ? 'p-4 min-h-[64px]' : touchSize === 'large' ? 'p-3 min-h-[52px]' : 'p-2.5 min-h-[44px]'

  return (
    <div className="h-screen w-screen flex bg-slate-100 overflow-hidden text-slate-800 select-none">
      {/* Mobile Backdrop */}
      {mobileMenuOpen && (
        <div
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs z-40 md:hidden"
        />
      )}

      {/* Sidebar Navigation (Matching Back Office) */}
      <aside
        className={`fixed inset-y-0 ${
          isRtl ? 'right-0' : 'left-0'
        } z-50 bg-slate-900 text-slate-300 flex flex-col transition-all duration-300 md:static md:translate-x-0 ${
          sidebarCollapsed ? 'md:w-20' : 'md:w-64'
        } ${
          mobileMenuOpen
            ? 'w-72 translate-x-0'
            : isRtl
            ? 'translate-x-full md:translate-x-0'
            : '-translate-x-full md:translate-x-0'
        }`}
      >
        {/* Brand Header */}
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className={`flex items-center gap-3 ${sidebarCollapsed ? 'md:justify-center md:w-full' : ''}`}>
            <div className="p-2.5 rounded-2xl bg-sky-600 shadow-lg shadow-sky-600/30 shrink-0">
              <Store className="w-5 h-5 text-white" />
            </div>
            {(!sidebarCollapsed || mobileMenuOpen) && (
              <div className="min-w-0">
                <h1 className="font-extrabold text-white text-sm tracking-tight truncate">
                  {settings.store_name}
                </h1>
                <p className="text-[11px] text-sky-400 font-medium truncate">
                  {t.portalTitle}
                </p>
              </div>
            )}
          </div>
          <button
            onClick={() => setMobileMenuOpen(false)}
            className="md:hidden p-1.5 rounded-lg text-slate-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Active POS Mode Banner */}
        {(!sidebarCollapsed || mobileMenuOpen) ? (
          <div className="mx-3 my-3 p-3 rounded-2xl bg-gradient-to-r from-sky-600/20 to-indigo-600/20 border border-sky-500/30 flex items-center justify-between shadow-xs">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-xs font-black text-white">{t.register}</span>
            </div>
            <span className="px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider bg-sky-500 text-white shadow-xs">
              POS
            </span>
          </div>
        ) : (
          <div className="flex justify-center my-3">
            <div className="w-3 h-3 rounded-full bg-emerald-400 animate-pulse shadow-sm shadow-emerald-400/50" title={t.register} />
          </div>
        )}

        {/* Sync Status Badge */}
        {(!sidebarCollapsed || mobileMenuOpen) ? (
          <div className="px-4 py-2.5 bg-slate-950/60 border-t border-b border-slate-800/80 flex items-center justify-between text-xs">
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
              {syncStatus.isOnline ? 'Online' : 'Offline'}
            </span>
          </div>
        ) : (
          <div className="py-2.5 flex justify-center border-t border-b border-slate-800/80">
            {syncStatus.isOnline ? (
              <span title={t.onlineCloud}>
                <Wifi className="w-4 h-4 text-emerald-400" />
              </span>
            ) : (
              <span title={t.offlineMode}>
                <WifiOff className="w-4 h-4 text-amber-500" />
              </span>
            )}
          </div>
        )}

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-3 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = item.isActive
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                title={item.label}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-bold transition-all ${
                  isActive
                    ? 'bg-sky-600 text-white shadow-md shadow-sky-600/25'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/60'
                } ${sidebarCollapsed && !mobileMenuOpen ? 'justify-center px-0' : ''}`}
              >
                <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                {(!sidebarCollapsed || mobileMenuOpen) && <span className="truncate">{item.label}</span>}
              </Link>
            )
          })}
        </nav>

        {/* Footer: Current Staff & Logout/Lock */}
        <div className="p-3 border-t border-slate-800 space-y-2">
          <div className={`flex items-center gap-2.5 px-1.5 ${sidebarCollapsed && !mobileMenuOpen ? 'justify-center' : ''}`}>
            <div
              style={{ backgroundColor: currentStaff.avatarColor }}
              className="w-7 h-7 rounded-full flex items-center justify-center text-white text-[11px] font-bold shrink-0"
            >
              {currentStaff.name.charAt(0)}
            </div>
            {(!sidebarCollapsed || mobileMenuOpen) && (
              <div className="min-w-0 flex-1">
                <p className="text-xs font-bold text-white truncate">{currentStaff.name}</p>
                <p className="text-[10px] text-slate-400 truncate">{currentStaff.role}</p>
              </div>
            )}
          </div>

          {(!sidebarCollapsed || mobileMenuOpen) ? (
            <div className="flex items-center gap-2 pt-1">
              <button
                onClick={() => setIsLocked(true)}
                className="flex-1 flex items-center justify-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-bold bg-slate-800 text-slate-300 hover:bg-slate-700 transition-colors"
                title={t.lockScreen}
              >
                <Lock className="w-3.5 h-3.5 text-amber-400" />
                <span>{t.lockScreen}</span>
              </button>
              <button
                onClick={handleLogout}
                className="p-1.5 rounded-xl text-red-400 hover:bg-red-500/10 transition-colors"
                title={t.signOut}
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-1 pt-1">
              <button
                onClick={() => setIsLocked(true)}
                className="p-2 rounded-xl text-amber-400 hover:bg-slate-800"
                title={t.lockScreen}
              >
                <Lock className="w-4 h-4" />
              </button>
              <button
                onClick={handleLogout}
                className="p-2 rounded-xl text-red-400 hover:bg-red-500/10"
                title={t.signOut}
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </aside>

      {/* Main Content Area: Top Header + POS Workspace */}
      <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden">
        {/* 1. TOP STATUS & NAVIGATION BAR (Back Office Design) */}
        <header
          style={{
            height: PosTokens.topBarHeight,
            paddingLeft: PosTokens.topBarPadding,
            paddingRight: PosTokens.topBarPadding,
          }}
          className="bg-white border-b border-slate-200 flex items-center justify-between shrink-0 shadow-xs z-30"
        >
          {/* Left / Start: Brand & Cashier */}
          <div className="flex items-center" style={{ gap: PosTokens.spacingMedium }}>
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="md:hidden p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
              title="Menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            <button
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="hidden md:flex p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 hover:text-slate-900 transition-colors"
              title={sidebarCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
            >
              {sidebarCollapsed ? (
                <PanelLeftOpen className="w-4 h-4" />
              ) : (
                <PanelLeftClose className="w-4 h-4" />
              )}
            </button>

            <div className="flex items-center" style={{ gap: PosTokens.spacingMedium }}>
              <div className="p-1.5 rounded-xl bg-sky-50 text-sky-600 border border-sky-100 hidden sm:flex">
                <Store className="w-4 h-4" />
              </div>
              <div className="flex items-center" style={{ gap: PosTokens.spacingMedium }}>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-sky-100/80 text-sky-800 border border-sky-200">
                  {t.register}
                </span>
              </div>
            </div>

            <span className="hidden xl:inline-block text-xs text-slate-400">
              • {currentStaff.name} ({currentStaff.role})
            </span>
          </div>

          {/* Center: Search Bar */}
          <div className="flex-1 max-w-sm mx-4 relative hidden sm:block">
            <Search className="w-4 h-4 text-slate-400 absolute top-2.5 rtl:right-3 ltr:left-3" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={t.searchPlaceholder}
              className="w-full rtl:pr-9 rtl:pl-4 ltr:pl-9 ltr:pr-4 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-50 focus:bg-white text-slate-800 placeholder-slate-400 text-xs border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute rtl:left-2.5 ltr:right-2.5 top-2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Right / End: Actions */}
          <div className="flex items-center" style={{ gap: PosTokens.spacingSmall }}>
            {/* Barcode Camera Scanner */}
            <button
              onClick={() => setIsCameraScannerOpen(true)}
              className="p-2 md:px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold border border-slate-200 flex items-center gap-1.5 transition-colors"
              title={t.cameraScanner}
            >
              <Barcode className="w-4 h-4 text-sky-600" />
              <span className="hidden md:inline">{t.cameraScanner}</span>
            </button>

            {/* AI Menu Scanner */}
            <button
              onClick={() => setIsMenuScannerOpen(true)}
              className="p-2 md:px-3 rounded-xl bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm shadow-sky-600/20 transition-all"
              title={t.aiMenuScanner}
            >
              <Sparkles className="w-4 h-4 text-amber-300" />
              <span className="hidden lg:inline">{t.aiMenuScanner}</span>
            </button>

            {/* Held Orders Badge */}
            <button
              onClick={() => setIsHeldOrdersModalOpen(true)}
              className="p-2 md:px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold border border-slate-200 flex items-center gap-1.5 relative transition-colors"
              title={t.heldOrders}
            >
              <Clock className="w-4 h-4 text-amber-500" />
              <span className="hidden md:inline">{t.heldOrders}</span>
              {heldOrders.length > 0 && (
                <span className="absolute -top-1 -right-1 px-1.5 py-0.5 rounded-full bg-amber-500 text-slate-950 text-[10px] font-black animate-bounce">
                  {heldOrders.length}
                </span>
              )}
            </button>

            {/* Sound Toggle */}
            <button
              onClick={() => setSoundEnabled(!soundEnabled)}
              className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 border border-slate-200 transition-colors"
              title={t.soundEffects}
            >
              {soundEnabled ? (
                <Volume2 className="w-4 h-4 text-emerald-600" />
              ) : (
                <VolumeX className="w-4 h-4 text-slate-400" />
              )}
            </button>

            {/* Cloud Sync Status Pill */}
            <button
              onClick={syncNow}
              className={`p-2 rounded-xl text-xs font-bold flex items-center gap-1.5 border transition-colors ${
                syncStatus.isOnline
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                  : 'bg-red-50 text-red-700 border-red-200 hover:bg-red-100'
              }`}
              title={
                syncStatus.isOnline
                  ? `${t.onlineCloud} (${syncStatus.pendingCount})`
                  : t.offlineMode
              }
            >
              {syncStatus.isOnline ? (
                <Wifi className="w-4 h-4 text-emerald-600" />
              ) : (
                <WifiOff className="w-4 h-4 text-red-500" />
              )}
              {syncStatus.pendingCount > 0 && (
                <span className="px-1.5 py-0.2 rounded-full bg-amber-500 text-black text-[10px]">
                  {syncStatus.pendingCount}
                </span>
              )}
            </button>

            {/* Lock Screen PIN */}
            <button
              onClick={() => setIsLocked(true)}
              className="p-2 rounded-xl bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 text-xs font-bold transition-colors"
              title={t.lockScreen}
            >
              <Lock className="w-4 h-4" />
            </button>

            {/* Mobile Cart Trigger */}
            <button
              onClick={() => setIsMobileCartOpen(!isMobileCartOpen)}
              className="lg:hidden p-2 rounded-xl bg-sky-600 text-white shadow-sm shadow-sky-600/30 relative"
            >
              <ShoppingCart className="w-4 h-4" />
              {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 px-1.5 py-0.5 rounded-full bg-red-500 text-white text-[10px] font-black">
                  {itemCount}
                </span>
              )}
            </button>
          </div>
        </header>

        {/* 2. MAIN REGISTER WORKSPACE: SPLIT GRID + CART */}
        <div className="flex-1 flex overflow-hidden">
          {/* MAIN COLUMN: PRODUCT CATALOG GRID & CATEGORY CHIPS */}
          <main className="flex-1 flex flex-col min-w-0 bg-slate-100 overflow-hidden">
          {/* Category Chips Bar */}
          <div className="bg-white border-b border-slate-200 px-4 py-2.5 flex items-center gap-2 overflow-x-auto no-scrollbar shrink-0 shadow-xs">
            <button
              onClick={() => setSelectedCategoryId('ALL')}
              className={`px-4 py-2 rounded-xl text-xs font-extrabold whitespace-nowrap transition-all ${
                selectedCategoryId === 'ALL'
                  ? 'bg-sky-600 text-white shadow-md shadow-sky-600/25'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              {t.allCategories}
            </button>

            <button
              onClick={() => setSelectedCategoryId('FAVORITES')}
              className={`px-3.5 py-2 rounded-xl text-xs font-extrabold whitespace-nowrap flex items-center gap-1.5 transition-all ${
                selectedCategoryId === 'FAVORITES'
                  ? 'bg-amber-500 text-black shadow-md shadow-amber-500/25'
                  : 'bg-amber-50 text-amber-800 border border-amber-200 hover:bg-amber-100'
              }`}
            >
              <Star className="w-3.5 h-3.5 fill-current" />
              <span>{t.favorites}</span>
            </button>

            {categories.map((cat) => {
              const isSelected = selectedCategoryId === cat.id
              return (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategoryId(cat.id)}
                  style={{
                    backgroundColor: isSelected ? cat.color_hex || '#0288D1' : undefined,
                    color: isSelected ? '#FFFFFF' : undefined,
                  }}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all flex items-center gap-1.5 ${
                    isSelected
                      ? 'shadow-md scale-102'
                      : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {!isSelected && (
                    <span
                      style={{ backgroundColor: cat.color_hex || '#0288D1' }}
                      className="w-2 h-2 rounded-full inline-block shrink-0"
                    />
                  )}
                  <span>{getLocalizedName(cat, language)}</span>
                </button>
              )
            })}
          </div>

          {/* Interactive Products Grid */}
          <div className="flex-1 p-4 overflow-y-auto pos-catalog-container">
            {filteredItems.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-slate-400 space-y-2">
                <Search className="w-12 h-12 opacity-30" />
                <p className="text-sm font-bold">{t.searchPlaceholder}</p>
                <button
                  onClick={() => {
                    setSelectedCategoryId('ALL')
                    setSearchQuery('')
                  }}
                  className="text-xs text-sky-600 font-bold hover:underline"
                >
                  {t.allCategories}
                </button>
              </div>
            ) : (
              <div
                className={`grid gap-3.5 ${
                  simplifiedPos
                    ? 'grid-cols-2 md:grid-cols-3'
                    : 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5'
                }`}
              >
                {filteredItems.map((item) => {
                  const hasVariants = item.variants && item.variants.length > 0
                  const isLowStock =
                    item.track_stock && item.in_stock <= item.low_stock_threshold
                  const localizedCatName = getLocalizedName(categories.find(c => c.id === item.category_id), language) || item.category_name
                  const localizedItemName = getLocalizedName(item, language)

                  return (
                    <div
                      key={item.id}
                      onClick={() => handleItemClick(item)}
                      className={`group bg-white rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md hover:border-sky-300 transition-all duration-200 cursor-pointer flex flex-col overflow-hidden relative active:scale-98 pos-product-card ${touchPadding}`}
                    >
                      {/* Favorite indicator badge */}
                      {item.is_favorite && (
                        <div className="absolute top-2.5 start-2.5 z-10 p-1 rounded-full bg-white/90 backdrop-blur-md shadow-sm">
                          <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                        </div>
                      )}

                      {/* Low stock warning badge */}
                      {isLowStock && (
                        <div className="absolute top-2.5 end-2.5 z-10 px-2 py-0.5 rounded-full bg-red-600 text-white text-[9px] font-black uppercase shadow-sm">
                          {t.lowStock} ({item.in_stock})
                        </div>
                      )}

                      {/* Product Image or Color Swatch */}
                      {!simplifiedPos && (
                        <div className="w-full h-28 sm:h-32 bg-slate-100 overflow-hidden relative shrink-0">
                          {item.image_url ? (
                            <img
                              src={item.image_url}
                              alt={localizedItemName}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                            />
                          ) : (
                            <div
                              style={{ backgroundColor: item.color_hex || '#0288D1' }}
                              className="w-full h-full opacity-80 flex items-center justify-center text-white font-black text-2xl"
                            >
                              {localizedItemName.charAt(0)}
                            </div>
                          )}
                        </div>
                      )}

                      {/* Content */}
                      <div className="p-3 flex-1 flex flex-col justify-between">
                        <div>
                          <div className="flex items-center justify-between">
                            <span className="text-[10px] font-extrabold text-sky-600 uppercase tracking-wider truncate">
                              {localizedCatName}
                            </span>
                            {hasVariants && (
                              <span className="text-[10px] font-bold text-sky-600 bg-sky-50 px-1.5 py-0.5 rounded-md border border-sky-100">
                                {t.variants}
                              </span>
                            )}
                          </div>
                          <h4 className="text-xs font-bold text-slate-900 mt-1 line-clamp-2 leading-tight">
                            {localizedItemName}
                          </h4>
                        </div>

                        <div className="mt-2.5 flex items-center justify-between pt-2 border-t border-slate-100">
                          <span className="text-sm font-black text-slate-900 numeric-preserve price-display">
                            {settings.currency_symbol}
                            {item.price.toFixed(2)}
                          </span>

                          <div className="w-7 h-7 rounded-xl bg-sky-50 group-hover:bg-sky-600 text-sky-700 group-hover:text-white flex items-center justify-center transition-colors shadow-xs">
                            <Plus className="w-4 h-4 stroke-[2.5]" />
                          </div>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </main>

        {/* ASIDE COLUMN: ACTIVE CART & CHECKOUT CONTROLLER */}
        <aside
          className={`w-full lg:w-96 bg-white border-s border-slate-200 flex flex-col h-full shadow-xl shrink-0 transition-transform duration-300 fixed inset-y-0 end-0 z-40 lg:static ${
            isMobileCartOpen ? 'translate-x-0' : 'rtl:translate-x-full ltr:-translate-x-full lg:translate-x-0'
          }`}
        >
          {/* Cart Header */}
          <div className="p-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-xl bg-sky-600 text-white shadow-sm">
                <ShoppingCart className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-black text-slate-900">{ticketName}</h3>
                <p className="text-[11px] text-slate-500">{itemCount} {t.cart}</p>
              </div>
            </div>

            {/* Dining Options Segment */}
            <div className="flex bg-slate-200 p-0.5 rounded-xl text-[10px] font-bold">
              {(['Dine in', 'Takeout', 'Delivery'] as const).map((opt) => (
                <button
                  key={opt}
                  onClick={() => setDiningOption(opt)}
                  className={`px-2 py-1 rounded-lg transition-all ${
                    diningOption === opt
                      ? 'bg-white text-slate-900 shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {opt === 'Dine in' ? t.dineIn : opt === 'Takeout' ? t.takeout : t.delivery}
                </button>
              ))}
            </div>

            <button
              onClick={() => setIsMobileCartOpen(false)}
              className="lg:hidden p-1.5 rounded-lg text-slate-400 hover:bg-slate-200"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Customer / Loyalty Assignment Bar */}
          <div className="px-4 py-2 bg-sky-50/50 border-b border-slate-200 flex items-center justify-between">
            <button
              onClick={() => setIsCustomerModalOpen(true)}
              className="flex items-center gap-2 text-xs font-bold text-sky-700 hover:text-sky-900"
            >
              <User className="w-4 h-4 text-sky-600" />
              <span>
                {selectedCustomer ? selectedCustomer.name : `${t.customer} (+)`}
              </span>
            </button>

            {selectedCustomer && (
              <span className="text-[11px] font-extrabold text-amber-600">
                ★ {selectedCustomer.points_balance} {t.points}
              </span>
            )}
          </div>

          {/* Cart Items List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-2.5">
            {cart.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-slate-400 space-y-2 text-center p-6">
                <ShoppingCart className="w-12 h-12 opacity-20" />
                <p className="text-xs font-bold text-slate-500">{t.emptyCart}</p>
                <p className="text-[11px] text-slate-400">
                  {t.cameraScanner}
                </p>
              </div>
            ) : (
              cart.map((line) => (
                <div
                  key={line.cart_id}
                  className="bg-slate-50 p-3 rounded-2xl border border-slate-200/80 shadow-sm space-y-1.5"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="text-xs font-black text-slate-900">{getLocalizedName(line, language)}</h4>
                      {line.selected_variant && (
                        <span className="text-[10px] text-slate-500 block">
                          • {getLocalizedName(line.selected_variant, language)}
                        </span>
                      )}
                      {line.selected_modifiers && line.selected_modifiers.length > 0 && (
                        <span className="text-[10px] text-slate-500 block">
                          {line.selected_modifiers.map((m) => `+ ${getLocalizedName(m, language)}`).join(', ')}
                        </span>
                      )}
                      {line.comment && (
                        <span className="text-[10px] text-amber-700 font-bold block">
                          * {line.comment}
                        </span>
                      )}
                    </div>

                    <span className="text-xs font-black text-slate-900 numeric-preserve price-display">
                      {settings.currency_symbol}
                      {line.line_total.toFixed(2)}
                    </span>
                  </div>

                  {/* Quantity & Actions Bar */}
                  <div className="flex items-center justify-between pt-1">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => updateCartItemQuantity(line.cart_id, -1)}
                        className="w-7 h-7 rounded-lg bg-white border border-slate-200 hover:bg-slate-100 flex items-center justify-center text-slate-700 font-bold shadow-sm"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <span className="text-xs font-black text-slate-900 w-5 text-center">
                        {line.quantity}
                      </span>
                      <button
                        onClick={() => updateCartItemQuantity(line.cart_id, 1)}
                        className="w-7 h-7 rounded-lg bg-white border border-slate-200 hover:bg-slate-100 flex items-center justify-center text-slate-700 font-bold shadow-sm"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <button
                      onClick={() => removeCartItem(line.cart_id)}
                      className="p-1 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50"
                      title={t.delete}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Cart Totals & Summary */}
          {cart.length > 0 && (
            <div className="p-4 bg-slate-50 border-t border-slate-200 space-y-2 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>{t.subtotal}:</span>
                <span className="font-bold">
                  {settings.currency_symbol}
                  {subtotal.toFixed(2)}
                </span>
              </div>

              {totalDiscount > 0 && (
                <div className="flex justify-between text-red-600 font-bold">
                  <span>{t.discount}:</span>
                  <span>
                    -{settings.currency_symbol}
                    {totalDiscount.toFixed(2)}
                  </span>
                </div>
              )}

              <div className="flex justify-between text-slate-500 text-[11px]">
                <span>{t.tax} ({settings.tax_rate}%):</span>
                <span>
                  {settings.currency_symbol}
                  {taxAmount.toFixed(2)}
                </span>
              </div>

              <div className="border-t border-slate-200 pt-2 flex justify-between items-baseline">
                <span className="text-sm font-black text-slate-900">{t.total}:</span>
                <span className="text-2xl font-black text-sky-600">
                  {settings.currency_symbol}
                  {grandTotal.toFixed(2)}
                </span>
              </div>
            </div>
          )}

          {/* Order Action Buttons */}
          <div className="p-4 bg-white border-t border-slate-200 space-y-2">
            <div className="grid grid-cols-3 gap-2">
              <button
                onClick={() => setIsHeldOrdersModalOpen(true)}
                disabled={cart.length === 0}
                className="py-2.5 rounded-xl border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-50 disabled:opacity-40 transition-colors flex flex-col items-center gap-1"
              >
                <Clock className="w-4 h-4 text-amber-500" />
                <span>{t.parkOrder}</span>
              </button>

              <button
                onClick={() => setIsDiscountsModalOpen(true)}
                disabled={cart.length === 0}
                className="py-2.5 rounded-xl border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-50 disabled:opacity-40 transition-colors flex flex-col items-center gap-1"
              >
                <Percent className="w-4 h-4 text-emerald-500" />
                <span>{t.discount}</span>
              </button>

              <button
                onClick={clearCart}
                disabled={cart.length === 0}
                className="py-2.5 rounded-xl border border-slate-200 text-red-600 font-bold text-xs hover:bg-red-50 disabled:opacity-40 transition-colors flex flex-col items-center gap-1"
              >
                <Trash2 className="w-4 h-4" />
                <span>{t.clearCart}</span>
              </button>
            </div>

            {/* Charge / Pay Primary Button */}
            <button
              onClick={() => setIsCheckoutModalOpen(true)}
              disabled={cart.length === 0}
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-sky-600 to-indigo-600 hover:from-sky-500 hover:to-indigo-500 text-white font-black text-base shadow-xl shadow-sky-600/25 flex items-center justify-center gap-2 disabled:opacity-40 transition-transform active:scale-95"
            >
              <CreditCard className="w-5 h-5" />
              <span>
                {t.charge} {settings.currency_symbol}
                {grandTotal.toFixed(2)}
              </span>
            </button>
          </div>
        </aside>
      </div>
      </div>

      {/* MODALS INTEGRATION */}
      <ItemCustomizationModal
        item={customizingItem}
        isOpen={Boolean(customizingItem)}
        onClose={() => setCustomizingItem(null)}
      />

      <CustomerSelectModal
        isOpen={isCustomerModalOpen}
        onClose={() => setIsCustomerModalOpen(false)}
      />

      <HeldOrdersModal
        isOpen={isHeldOrdersModalOpen}
        onClose={() => setIsHeldOrdersModalOpen(false)}
      />

      <DiscountsModal
        isOpen={isDiscountsModalOpen}
        onClose={() => setIsDiscountsModalOpen(false)}
      />

      <CheckoutModal
        isOpen={isCheckoutModalOpen}
        onClose={() => setIsCheckoutModalOpen(false)}
        onCompleted={(rec) => {
          setCompletedReceipt(rec)
          setIsReceiptModalOpen(true)
        }}
      />

      <ReceiptPreviewModal
        receipt={completedReceipt}
        isOpen={isReceiptModalOpen}
        onClose={() => {
          setIsReceiptModalOpen(false)
          setCompletedReceipt(null)
        }}
      />

      <CameraScannerModal
        isOpen={isCameraScannerOpen}
        onClose={() => setIsCameraScannerOpen(false)}
      />

      <MenuScannerModal
        isOpen={isMenuScannerOpen}
        onClose={() => setIsMenuScannerOpen(false)}
      />

      <PinLockModal isOpen={false} />
    </div>
  )
}
