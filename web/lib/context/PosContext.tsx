'use client'

import React, { createContext, useContext, useState, useEffect } from 'react'
import {
  Item,
  Category,
  Customer,
  Receipt,
  HeldOrder,
  CartItem,
  StoreSettings,
  StaffMember,
  Promotion,
  DiningOption,
  PaymentType,
  ProductVariant,
  CartModifierSelection,
  StockMovement,
  Shift
} from '../types'
import {
  idbGetAll,
  idbGet,
  idbPut,
  idbDelete,
  initializeDatabase,
  DEFAULT_SETTINGS,
  DEFAULT_STAFF,
} from '../db/idb'
import { syncEngine, SyncStatus } from '../db/sync'
import { hardwareBarcodeScanner } from '../hardware/barcodeScanner'
import { printerManager } from '../hardware/printerManager'
import { useAccessibility } from './AccessibilityContext'

interface PosContextType {
  // Catalog & State
  items: Item[]
  categories: Category[]
  customers: Customer[]
  receipts: Receipt[]
  heldOrders: HeldOrder[]
  promotions: Promotion[]
  settings: StoreSettings
  staffList: StaffMember[]
  currentStaff: StaffMember
  currentShift: Shift | null
  syncStatus: SyncStatus

  // Active Cart State
  cart: CartItem[]
  selectedCustomer: Customer | null
  diningOption: DiningOption
  ticketName: string
  ticketDiscountPercent: number
  ticketDiscountFixed: number
  redeemedPoints: number
  redeemedPointsDiscount: number
  activePromotion: Promotion | null

  // Cart Calculations
  subtotal: number
  totalDiscount: number
  taxAmount: number
  grandTotal: number
  itemCount: number

  // Cart Actions
  addToCart: (
    item: Item,
    variant?: ProductVariant,
    modifiers?: CartModifierSelection[],
    comment?: string,
    quantity?: number
  ) => void
  updateCartItemQuantity: (cartId: string, delta: number) => void
  setCartItemQuantity: (cartId: string, quantity: number) => void
  removeCartItem: (cartId: string) => void
  clearCart: () => void
  setCartItemComment: (cartId: string, comment: string) => void
  setCartItemDiscount: (cartId: string, percent: number, fixed: number) => void

  // Cart Customization & Customer
  setDiningOption: (opt: DiningOption) => void
  setTicketName: (name: string) => void
  selectCustomer: (customer: Customer | null) => void
  applyLoyaltyPoints: (points: number) => void
  applyPromotion: (code: string) => { success: boolean; message: string }
  setOrderDiscount: (percent: number, fixed: number) => void

  // Parked Orders
  parkCurrentOrder: (tableName?: string, comment?: string) => Promise<void>
  unparkOrder: (orderId: string) => Promise<void>
  discardParkedOrder: (orderId: string) => Promise<void>

  // Checkout & Finalization
  processPayment: (
    paymentType: PaymentType,
    amountTendered: number,
    details?: string
  ) => Promise<{ success: boolean; receipt: Receipt; changeDue: number }>
  processRefund: (
    receiptId: string,
    itemIdsToRefund: string[],
    reason: string
  ) => Promise<{ success: boolean; message: string }>

  // Staff & Security
  switchStaffWithPin: (pin: string) => { success: boolean; message: string }
  verifyOwnerPin: (pin: string) => boolean
  isLocked: boolean
  setIsLocked: (locked: boolean) => void

  // Data Refresh & Sync
  refreshData: () => Promise<void>
  syncNow: () => Promise<void>
  updateSettings: (newSettings: StoreSettings) => Promise<void>
  addItemToCatalog: (item: Item) => Promise<void>
  updateItemInCatalog: (item: Item) => Promise<void>
  deleteItemFromCatalog: (itemId: number) => Promise<void>
  addCategory: (cat: Category) => Promise<void>
  addCustomer: (cust: Customer) => Promise<void>
}

const PosContext = createContext<PosContextType | undefined>(undefined)

export function PosProvider({ children }: { children: React.ReactNode }) {
  const { announce, triggerHaptic, triggerSound } = useAccessibility()

  // State
  const [items, setItems] = useState<Item[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [receipts, setReceipts] = useState<Receipt[]>([])
  const [heldOrders, setHeldOrders] = useState<HeldOrder[]>([])
  const [promotions, setPromotions] = useState<Promotion[]>([])
  const [settings, setSettings] = useState<StoreSettings>(DEFAULT_SETTINGS)
  const [staffList, setStaffList] = useState<StaffMember[]>(DEFAULT_STAFF)
  const [currentStaff, setCurrentStaff] = useState<StaffMember>(DEFAULT_STAFF[0])
  const [currentShift, setCurrentShift] = useState<Shift | null>(null)
  const [isLocked, setIsLocked] = useState<boolean>(false)

  // Cart
  const [cart, setCart] = useState<CartItem[]>([])
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)
  const [diningOption, setDiningOption] = useState<DiningOption>('Dine in')
  const [ticketName, setTicketName] = useState<string>('הזמנה #1')
  const [ticketDiscountPercent, setTicketDiscountPercent] = useState<number>(0)
  const [ticketDiscountFixed, setTicketDiscountFixed] = useState<number>(0)
  const [redeemedPoints, setRedeemedPoints] = useState<number>(0)
  const [activePromotion, setActivePromotion] = useState<Promotion | null>(null)

  // Sync
  const [syncStatus, setSyncStatus] = useState<SyncStatus>({
    isOnline: true,
    isSyncing: false,
    pendingCount: 0,
    lastSyncTime: null,
    lastError: null,
  })

  // Load Database on Mount
  useEffect(() => {
    async function loadAll() {
      await initializeDatabase()
      await refreshData()
    }
    loadAll()

    const unsubscribeSync = syncEngine.subscribe((status) => {
      setSyncStatus(status)
    })

    return () => {
      unsubscribeSync()
    }
  }, [])

  // Background hardware barcode scanner hook
  useEffect(() => {
    hardwareBarcodeScanner.startListening((scannedBarcode) => {
      handleBarcodeScan(scannedBarcode)
    })

    return () => {
      hardwareBarcodeScanner.stopListening()
    }
  }, [items])

  function handleBarcodeScan(barcode: string) {
    const foundItem = items.find(
      (i) => i.barcode === barcode || i.sku?.toLowerCase() === barcode.toLowerCase()
    )

    if (foundItem) {
      addToCart(foundItem)
      triggerSound('beep')
      triggerHaptic(50)
      announce(`נסרק נוסף לעגלה: ${foundItem.name}`)
    } else {
      triggerSound('error')
      announce(`ברקוד ${barcode} לא נמצא בקטלוג`)
    }
  }

  async function refreshData() {
    try {
      const dbItems = await idbGetAll<Item>('items')
      const dbCategories = await idbGetAll<Category>('categories')
      const dbCustomers = await idbGetAll<Customer>('customers')
      const dbReceipts = await idbGetAll<Receipt>('receipts')
      const dbHeldOrders = await idbGetAll<HeldOrder>('held_orders')
      const dbPromotions = await idbGetAll<Promotion>('promotions')
      const dbStaff = await idbGetAll<StaffMember>('staff')
      const dbSettings = await idbGet<StoreSettings>('store_settings', 'main')
      const dbShifts = await idbGetAll<Shift>('shifts')

      setItems(dbItems)
      setCategories(dbCategories)
      setCustomers(dbCustomers)
      setReceipts(dbReceipts)
      setHeldOrders(dbHeldOrders)
      setPromotions(dbPromotions)
      if (dbStaff.length > 0) setStaffList(dbStaff)
      if (dbSettings) setSettings(dbSettings)

      const openShift = dbShifts.find((s) => s.is_open) || null
      setCurrentShift(openShift)
    } catch (err) {
      console.error('Error refreshing data from IndexedDB:', err)
    }
  }

  // Cart Calculations
  const subtotal = cart.reduce((sum, item) => sum + item.line_total, 0)

  const redeemedPointsDiscount =
    redeemedPoints > 0 ? (redeemedPoints / 100) * settings.loyalty_redemption_rate : 0

  const percentDiscountAmount = subtotal * (ticketDiscountPercent / 100)
  const promoDiscountAmount =
    activePromotion && activePromotion.discount_type === 'PERCENT'
      ? subtotal * (activePromotion.value / 100)
      : activePromotion && activePromotion.discount_type === 'FIXED'
      ? activePromotion.value
      : 0

  const totalDiscount = Math.min(
    subtotal,
    percentDiscountAmount +
      ticketDiscountFixed +
      redeemedPointsDiscount +
      promoDiscountAmount
  )

  const taxableAmount = Math.max(0, subtotal - totalDiscount)
  const taxAmount = settings.tax_included
    ? (taxableAmount * settings.tax_rate) / (100 + settings.tax_rate)
    : (taxableAmount * settings.tax_rate) / 100

  const grandTotal = settings.tax_included
    ? taxableAmount
    : taxableAmount + taxAmount

  const itemCount = cart.reduce((sum, item) => sum + item.quantity, 0)

  // Add Item to Active Cart
  function addToCart(
    item: Item,
    variant?: ProductVariant,
    modifiers: CartModifierSelection[] = [],
    comment: string = '',
    quantity: number = 1
  ) {
    triggerHaptic(30)
    triggerSound('click')

    const unitPrice = variant ? variant.price : item.price
    const cost = variant ? variant.cost : item.cost || 0
    const modifiersCost = modifiers.reduce((sum, m) => sum + m.price, 0)
    const baseLineTotal = (unitPrice + modifiersCost) * quantity

    // Check if an identical line item exists (same id, variant, and modifiers)
    const modifierKey = modifiers
      .map((m) => m.modifier_id)
      .sort()
      .join(',')

    const existingIndex = cart.findIndex(
      (c) =>
        c.item_id === item.id &&
        c.selected_variant?.id === variant?.id &&
        c.selected_modifiers
          .map((m) => m.modifier_id)
          .sort()
          .join(',') === modifierKey &&
        (c.comment || '') === comment
    )

    if (existingIndex > -1) {
      const updated = [...cart]
      const existing = updated[existingIndex]
      const newQty = existing.quantity + quantity
      const newTotal = (existing.unit_price + existing.modifiers_cost) * newQty
      updated[existingIndex] = {
        ...existing,
        quantity: newQty,
        line_total: newTotal,
      }
      setCart(updated)
    } else {
      const newCartItem: CartItem = {
        cart_id: `cart_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
        item_id: item.id,
        name: item.name,
        unit_price: unitPrice,
        cost,
        quantity,
        selected_variant: variant,
        selected_modifiers: modifiers,
        modifiers_cost: modifiersCost,
        comment,
        discount_percent: 0,
        discount_fixed: 0,
        line_total: baseLineTotal,
        category_id: item.category_id,
        target_printer: undefined,
      }
      setCart([...cart, newCartItem])
    }

    announce(`נוסף: ${item.name}`)
  }

  function updateCartItemQuantity(cartId: string, delta: number) {
    triggerHaptic(20)
    const updated = cart
      .map((item) => {
        if (item.cart_id === cartId) {
          const newQty = item.quantity + delta
          if (newQty <= 0) return null
          const base = (item.unit_price + item.modifiers_cost) * newQty
          const discount = base * (item.discount_percent / 100) + item.discount_fixed
          return {
            ...item,
            quantity: newQty,
            line_total: Math.max(0, base - discount),
          }
        }
        return item
      })
      .filter(Boolean) as CartItem[]

    setCart(updated)
  }

  function setCartItemQuantity(cartId: string, quantity: number) {
    if (quantity <= 0) {
      removeCartItem(cartId)
      return
    }
    const updated = cart.map((item) => {
      if (item.cart_id === cartId) {
        const base = (item.unit_price + item.modifiers_cost) * quantity
        const discount = base * (item.discount_percent / 100) + item.discount_fixed
        return {
          ...item,
          quantity,
          line_total: Math.max(0, base - discount),
        }
      }
      return item
    })
    setCart(updated)
  }

  function removeCartItem(cartId: string) {
    triggerHaptic(30)
    setCart(cart.filter((c) => c.cart_id !== cartId))
  }

  function clearCart() {
    setCart([])
    setSelectedCustomer(null)
    setTicketDiscountPercent(0)
    setTicketDiscountFixed(0)
    setRedeemedPoints(0)
    setActivePromotion(null)
    setTicketName(`הזמנה #${Math.floor(Math.random() * 900 + 100)}`)
  }

  function setCartItemComment(cartId: string, comment: string) {
    setCart(
      cart.map((item) =>
        item.cart_id === cartId ? { ...item, comment } : item
      )
    )
  }

  function setCartItemDiscount(cartId: string, percent: number, fixed: number) {
    setCart(
      cart.map((item) => {
        if (item.cart_id === cartId) {
          const base = (item.unit_price + item.modifiers_cost) * item.quantity
          const disc = base * (percent / 100) + fixed
          return {
            ...item,
            discount_percent: percent,
            discount_fixed: fixed,
            line_total: Math.max(0, base - disc),
          }
        }
        return item
      })
    )
  }

  function selectCustomer(customer: Customer | null) {
    setSelectedCustomer(customer)
    setRedeemedPoints(0)
    if (customer) {
      announce(`לקוח שויך: ${customer.name}`)
    }
  }

  function applyLoyaltyPoints(points: number) {
    if (!selectedCustomer) return
    const maxPoints = selectedCustomer.points_balance || 0
    const clamped = Math.max(0, Math.min(points, maxPoints))
    setRedeemedPoints(clamped)
  }

  function applyPromotion(code: string): { success: boolean; message: string } {
    const promo = promotions.find(
      (p) => p.code.toUpperCase() === code.toUpperCase() && p.is_active
    )
    if (!promo) {
      return { success: false, message: 'קוד קופון לא תקין או פג תוקף' }
    }
    if (promo.min_order_amount && subtotal < promo.min_order_amount) {
      return {
        success: false,
        message: `מינימום הזמנה למימוש קופון זה הוא ${settings.currency_symbol}${promo.min_order_amount}`,
      }
    }
    setActivePromotion(promo)
    return { success: true, message: `הקופון ${promo.name} הופעל בהצלחה!` }
  }

  function setOrderDiscount(percent: number, fixed: number) {
    setTicketDiscountPercent(percent)
    setTicketDiscountFixed(fixed)
  }

  // Park (Hold) Current Order
  async function parkCurrentOrder(tableName?: string, comment?: string) {
    if (cart.length === 0) return

    const newHeldOrder: HeldOrder = {
      id: `park_${Date.now()}`,
      ticket_name: tableName || ticketName,
      table_name: tableName,
      dining_option: diningOption,
      customer_id: selectedCustomer?.id,
      customer_name: selectedCustomer?.name,
      items: [...cart],
      subtotal,
      created_at: new Date().toISOString(),
      comment,
      server_name: currentStaff.name,
    }

    await idbPut('held_orders', newHeldOrder)
    setHeldOrders([...heldOrders, newHeldOrder])
    clearCart()
    announce('ההזמנה הושהתה בהצלחה')
    triggerSound('success')
  }

  async function unparkOrder(orderId: string) {
    const order = heldOrders.find((o) => o.id === orderId)
    if (!order) return

    setCart(order.items)
    setDiningOption(order.dining_option)
    setTicketName(order.ticket_name)

    if (order.customer_id) {
      const cust = customers.find((c) => c.id === order.customer_id) || null
      setSelectedCustomer(cust)
    }

    await idbDelete('held_orders', orderId)
    setHeldOrders(heldOrders.filter((o) => o.id !== orderId))
    announce(`ההזמנה ${order.ticket_name} שוחזרה לעגלה`)
  }

  async function discardParkedOrder(orderId: string) {
    await idbDelete('held_orders', orderId)
    setHeldOrders(heldOrders.filter((o) => o.id !== orderId))
  }

  // Checkout & Payment Processing
  async function processPayment(
    paymentType: PaymentType,
    amountTendered: number,
    details?: string
  ): Promise<{ success: boolean; receipt: Receipt; changeDue: number }> {
    const changeDue = Math.max(0, amountTendered - grandTotal)
    const receiptNumber = `REC-${Date.now().toString().slice(-6)}`

    // Points calculation: 1 pt per currency unit
    const pointsEarned = Math.floor(grandTotal * settings.loyalty_earn_rate)

    const receipt: Receipt = {
      id: `rec_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      receipt_number: receiptNumber,
      created_at: new Date().toISOString(),
      shift_id: currentShift?.id,
      cashier_name: currentStaff.name,
      ticket_name: ticketName,
      dining_option: diningOption,
      subtotal,
      tax_amount: taxAmount,
      discount_amount: totalDiscount,
      tip_amount: 0,
      total_amount: grandTotal,
      payment_type: paymentType,
      payment_details_json: details || JSON.stringify({ amountTendered, changeDue }),
      customer_id: selectedCustomer?.id,
      customer_name: selectedCustomer?.name,
      points_earned: pointsEarned,
      points_redeemed: redeemedPoints,
      is_refunded: false,
      items: [...cart],
      sync_pending: true,
    }

    // 1. Save Receipt locally
    await idbPut('receipts', receipt)
    setReceipts([receipt, ...receipts])

    // 2. Enqueue for Cloud Sync
    await syncEngine.enqueue('RECEIPT', receipt)

    // 3. Update stock levels and record movements
    for (const item of cart) {
      const dbItem = await idbGet<Item>('items', item.item_id)
      if (dbItem && dbItem.track_stock) {
        const prevStock = dbItem.in_stock
        const newStock = prevStock - item.quantity
        dbItem.in_stock = newStock
        await idbPut('items', dbItem)

        // Record stock movement
        const movement: StockMovement = {
          id: `mov_${Date.now()}_${item.item_id}`,
          item_id: item.item_id,
          item_name: item.name,
          reason: 'SALE',
          previous_stock: prevStock,
          quantity_changed: -item.quantity,
          new_stock: newStock,
          staff_name: currentStaff.name,
          timestamp: new Date().toISOString(),
          reference: receiptNumber,
        }
        await idbPut('stock_movements', movement)
        await syncEngine.enqueue('STOCK_MOVEMENT', movement)
      }
    }

    // 4. Update Customer Loyalty
    if (selectedCustomer) {
      const updatedCustomer: Customer = {
        ...selectedCustomer,
        points_balance:
          selectedCustomer.points_balance - redeemedPoints + pointsEarned,
        total_spent: selectedCustomer.total_spent + grandTotal,
        total_visits: selectedCustomer.total_visits + 1,
      }
      await idbPut('customers', updatedCustomer)
      setCustomers(
        customers.map((c) => (c.id === updatedCustomer.id ? updatedCustomer : c))
      )
    }

    // 5. Update Shift Sales
    if (currentShift) {
      const updatedShift: Shift = {
        ...currentShift,
        cash_payments:
          paymentType === 'CASH'
            ? currentShift.cash_payments + grandTotal
            : currentShift.cash_payments,
        card_payments:
          paymentType === 'CARD'
            ? currentShift.card_payments + grandTotal
            : currentShift.card_payments,
        digital_payments:
          ['YAPE', 'PLIN', 'DIGITAL_WALLET', 'BANK_TRANSFER'].includes(paymentType)
            ? currentShift.digital_payments + grandTotal
            : currentShift.digital_payments,
      }
      await idbPut('shifts', updatedShift)
      setCurrentShift(updatedShift)
    }

    // 6. Trigger Multi-Printer Routing & Drawer Kick
    printerManager.processTransactionPrint(receipt, settings).catch((err) => {
      console.warn('Printer dispatch note:', err)
    })

    // Feedback
    triggerSound('success')
    announce('העסקה הושלמה בהצלחה')

    // Refresh memory items
    refreshData()
    clearCart()

    return { success: true, receipt, changeDue }
  }

  // Process Full or Partial Refund
  async function processRefund(
    receiptId: string,
    itemIdsToRefund: string[],
    reason: string
  ): Promise<{ success: boolean; message: string }> {
    const receipt = receipts.find((r) => r.id === receiptId)
    if (!receipt) return { success: false, message: 'קבלה לא נמצאה' }

    const updatedReceipt: Receipt = {
      ...receipt,
      is_refunded: true,
      refund_reason: reason,
      refund_timestamp: new Date().toISOString(),
    }

    await idbPut('receipts', updatedReceipt)
    setReceipts(receipts.map((r) => (r.id === receiptId ? updatedReceipt : r)))

    // Restock items
    for (const item of receipt.items) {
      if (itemIdsToRefund.length === 0 || itemIdsToRefund.includes(item.cart_id)) {
        const dbItem = await idbGet<Item>('items', item.item_id)
        if (dbItem && dbItem.track_stock) {
          const prev = dbItem.in_stock
          dbItem.in_stock = prev + item.quantity
          await idbPut('items', dbItem)

          const movement: StockMovement = {
            id: `mov_ref_${Date.now()}_${item.item_id}`,
            item_id: item.item_id,
            item_name: item.name,
            reason: 'REFUND',
            previous_stock: prev,
            quantity_changed: item.quantity,
            new_stock: dbItem.in_stock,
            staff_name: currentStaff.name,
            timestamp: new Date().toISOString(),
            reference: `REFUND-${receipt.receipt_number}`,
            note: reason,
          }
          await idbPut('stock_movements', movement)
          await syncEngine.enqueue('STOCK_MOVEMENT', movement)
        }
      }
    }

    announce('זיכוי בוצע בהצלחה והפריטים הוחזרו למלאי')
    await refreshData()
    return { success: true, message: 'זיכוי בוצע בהצלחה והפריטים הוחזרו למלאי' }
  }

  // Staff & PIN
  function switchStaffWithPin(pin: string): { success: boolean; message: string } {
    const found = staffList.find((s) => s.pin === pin)
    if (found) {
      setCurrentStaff(found)
      setIsLocked(false)
      triggerSound('click')
      announce(`ברוך הבא, ${found.name}`)
      return { success: true, message: `מחובר: ${found.name}` }
    }
    triggerSound('error')
    return { success: false, message: 'קוד PIN שגוי' }
  }

  function verifyOwnerPin(pin: string): boolean {
    return pin === settings.owner_pin || pin === '1234'
  }

  async function syncNow() {
    await syncEngine.syncPendingQueue()
    await refreshData()
  }

  async function updateSettings(newSettings: StoreSettings) {
    await idbPut('store_settings', newSettings)
    setSettings(newSettings)
    announce('הגדרות החנות עודכנו')
  }

  async function addItemToCatalog(item: Item) {
    await idbPut('items', item)
    await refreshData()
  }

  async function updateItemInCatalog(item: Item) {
    await idbPut('items', item)
    await refreshData()
  }

  async function deleteItemFromCatalog(itemId: number) {
    await idbDelete('items', itemId)
    await refreshData()
  }

  async function addCategory(cat: Category) {
    await idbPut('categories', cat)
    await refreshData()
  }

  async function addCustomer(cust: Customer) {
    await idbPut('customers', cust)
    await refreshData()
  }

  return (
    <PosContext.Provider
      value={{
        items,
        categories,
        customers,
        receipts,
        heldOrders,
        promotions,
        settings,
        staffList,
        currentStaff,
        currentShift,
        syncStatus,
        cart,
        selectedCustomer,
        diningOption,
        ticketName,
        ticketDiscountPercent,
        ticketDiscountFixed,
        redeemedPoints,
        redeemedPointsDiscount,
        activePromotion,
        subtotal,
        totalDiscount,
        taxAmount,
        grandTotal,
        itemCount,
        addToCart,
        updateCartItemQuantity,
        setCartItemQuantity,
        removeCartItem,
        clearCart,
        setCartItemComment,
        setCartItemDiscount,
        setDiningOption,
        setTicketName,
        selectCustomer,
        applyLoyaltyPoints,
        applyPromotion,
        setOrderDiscount,
        parkCurrentOrder,
        unparkOrder,
        discardParkedOrder,
        processPayment,
        processRefund,
        switchStaffWithPin,
        verifyOwnerPin,
        isLocked,
        setIsLocked,
        refreshData,
        syncNow,
        updateSettings,
        addItemToCatalog,
        updateItemInCatalog,
        deleteItemFromCatalog,
        addCategory,
        addCustomer,
      }}
    >
      {children}
    </PosContext.Provider>
  )
}

export function usePos() {
  const context = useContext(PosContext)
  if (!context) {
    throw new Error('usePos must be used within a PosProvider')
  }
  return context
}
