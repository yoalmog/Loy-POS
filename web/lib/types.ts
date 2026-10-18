export type DiningOption = 'Dine in' | 'Takeout' | 'Delivery'

export type SoldBy = 'EACH' | 'WEIGHT' | 'VOLUME'

export type PaymentType = 
  | 'CASH' 
  | 'CARD' 
  | 'YAPE' 
  | 'PLIN' 
  | 'DIGITAL_WALLET' 
  | 'BANK_TRANSFER' 
  | 'LOYALTY_POINTS' 
  | 'SPLIT'

export type StaffRole = 'CASHIER' | 'MANAGER' | 'OWNER'

export interface StaffMember {
  id: string
  name: string
  pin: string // 4-digit PIN
  role: StaffRole
  avatarColor: string
}

export interface Category {
  id: number
  name: string
  color_hex?: string
  icon?: string
  parent_id?: number | null
  target_printer?: 'CASHIER' | 'KITCHEN' | 'BAR' | 'NONE'
  sort_order?: number
  created_at?: string
}

export interface ProductVariant {
  id: string
  name: string // e.g. "Small", "Regular", "Large"
  price: number
  cost: number
  sku?: string
  barcode?: string
  in_stock: number
}

export interface ModifierOption {
  id: string
  name: string
  price: number
}

export interface ModifierGroup {
  id: number
  name: string
  options: ModifierOption[]
  min_selection?: number
  max_selection?: number
  required?: boolean
}

export interface Item {
  id: number
  name: string
  category_id?: number
  category_name?: string
  price: number
  cost?: number
  sku?: string
  barcode?: string
  sold_by?: SoldBy
  in_stock: number
  low_stock_threshold: number
  track_stock: boolean
  is_favorite?: boolean
  is_active?: boolean
  color_hex?: string
  image_url?: string
  variants?: ProductVariant[]
  modifier_groups?: ModifierGroup[]
  unit_of_measure?: 'item' | 'kg' | 'lb' | 'oz' | 'cup'
  tax_rate_percent?: number
  created_at?: string
  updated_at?: string
}

export interface CartModifierSelection {
  modifier_id: string
  name: string
  price: number
}

export interface CartItem {
  cart_id: string
  item_id: number
  name: string
  unit_price: number
  cost: number
  quantity: number
  selected_variant?: ProductVariant
  selected_modifiers: CartModifierSelection[]
  modifiers_cost: number
  comment?: string
  discount_percent: number
  discount_fixed: number
  line_total: number
  category_id?: number
  target_printer?: 'CASHIER' | 'KITCHEN' | 'BAR' | 'NONE'
}

export interface Customer {
  id: number
  name: string
  phone?: string
  email?: string
  address?: string
  customer_code?: string
  points_balance: number
  total_visits: number
  total_spent: number
  note?: string
  created_at?: string
}

export interface HeldOrder {
  id: string
  ticket_name: string
  table_name?: string
  dining_option: DiningOption
  customer_id?: number
  customer_name?: string
  items: CartItem[]
  subtotal: number
  created_at: string
  comment?: string
  server_name?: string
}

export interface SplitPaymentDetail {
  tender: PaymentType
  amount: number
  reference?: string
}

export interface Receipt {
  id: string
  receipt_number: string
  created_at: string
  shift_id?: number
  cashier_name: string
  ticket_name?: string
  dining_option: DiningOption
  subtotal: number
  tax_amount: number
  discount_amount: number
  tip_amount: number
  total_amount: number
  payment_type: PaymentType
  payment_details_json?: string
  customer_id?: number
  customer_name?: string
  points_earned?: number
  points_redeemed?: number
  is_refunded: boolean
  refund_reason?: string
  refund_timestamp?: string
  items_summary_json?: string
  items: CartItem[]
  sync_pending?: boolean
}

export type StockMovementReason = 
  | 'SALE' 
  | 'REFUND' 
  | 'ADJUSTMENT' 
  | 'PURCHASE' 
  | 'TRANSFER' 
  | 'DAMAGED' 
  | 'STOCK_COUNT'

export interface StockMovement {
  id: string
  item_id: number
  item_name: string
  reason: StockMovementReason
  previous_stock: number
  quantity_changed: number
  new_stock: number
  staff_name: string
  timestamp: string
  reference?: string
  note?: string
}

export interface Shift {
  id: number
  opened_at: string
  closed_at?: string
  cashier_name: string
  starting_cash: number
  cash_payments: number
  cash_refunds: number
  cash_paid_in: number
  cash_paid_out: number
  card_payments: number
  digital_payments: number
  actual_cash_counted?: number
  is_open: boolean
}

export interface CashAction {
  id: string
  shift_id: number
  is_pay_in: boolean
  amount: number
  comment: string
  timestamp: string
  staff_name: string
}

export interface Promotion {
  id: string
  code: string
  name: string
  discount_type: 'PERCENT' | 'FIXED'
  value: number
  min_order_amount?: number
  max_discount_cap?: number
  is_active: boolean
}

export interface PrinterConfig {
  id: string
  name: string
  type: 'CASHIER' | 'KITCHEN' | 'BAR' | 'STICKER' | 'REPORTS'
  interface_type: 'BLUETOOTH' | 'USB' | 'LAN' | 'SUNMI' | 'BROWSER'
  ip_address?: string
  port?: number
  paper_width: 58 | 76 | 80
  auto_cut: boolean
  cash_drawer_kick: boolean
  enabled: boolean
}

export interface StoreSettings {
  id: string
  store_name: string
  address: string
  phone: string
  email: string
  currency: string
  currency_symbol: string
  tax_rate: number
  tax_included: boolean
  receipt_header?: string
  receipt_footer?: string
  return_policy?: string
  qr_payment_yape_name?: string
  qr_payment_yape_phone?: string
  qr_payment_plin_name?: string
  qr_payment_plin_phone?: string
  loyalty_earn_rate: number // points per $1
  loyalty_redemption_rate: number // $ discount per 100 points
  allow_negative_stock: boolean
  owner_pin: string
}

export type SupportedLanguage = 
  | 'he' 
  | 'en' 
  | 'es' 
  | 'pt' 
  | 'fr' 
  | 'de' 
  | 'zh' 
  | 'ja' 
  | 'ar'

export type ColorTheme = 
  | 'blue' 
  | 'emerald' 
  | 'purple' 
  | 'amber' 
  | 'red' 
  | 'midnight'

export type ColorblindMode = 
  | 'none' 
  | 'protanopia' 
  | 'deuteranopia' 
  | 'tritanopia' 
  | 'monochrome'

export type TextScale = '1.0' | '1.15' | '1.30' | '1.50'

export type ContrastMode = 'normal' | 'high' | 'ultra'

export type TouchTargetSize = 'standard' | 'large' | 'jumbo'
