export interface Category {
  id: number
  name: string
  color_hex?: string
  created_at?: string
}

export interface Product {
  id: number
  name: string
  price: number
  category: string
  stock_level: number
  image_url?: string
  created_at?: string
  updated_at?: string
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
  sold_by?: 'EACH' | 'WEIGHT'
  in_stock: number
  low_stock_threshold: number
  track_stock: boolean
  color_hex?: string
  modifier_ids?: string
  created_at?: string
  updated_at?: string
}

export interface Customer {
  id: number
  name: string
  phone?: string
  email?: string
  customer_code?: string
  points_balance: number
  total_visits: number
  total_spent: number
  note?: string
  created_at?: string
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
  actual_cash_counted?: number
  is_open: boolean
}

export interface Receipt {
  id: string
  receipt_number: string
  created_at: string
  shift_id?: number
  cashier_name: string
  ticket_name?: string
  dining_option: string
  subtotal: number
  tax_amount: number
  discount_amount: number
  tip_amount: number
  total_amount: number
  payment_type: 'CASH' | 'CARD' | 'OTHER'
  payment_details_json?: string
  customer_id?: number
  customer_name?: string
  is_refunded: boolean
  refund_timestamp?: string
  items_summary_json?: string
  idempotency_key?: string
}

export interface ReceiptItem {
  id?: number
  receipt_id: string
  item_id?: number
  item_name: string
  unit_price: number
  quantity: number
  selected_modifiers_json?: string
  modifiers_cost?: number
  comment?: string
  discount_percent?: number
  line_total: number
}

export interface Timecard {
  id: number
  employee_name: string
  clock_in_time: string
  clock_out_time?: string
  total_hours: number
  is_clocked_in: boolean
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
  receipt_header?: string
  receipt_footer?: string
  updated_at?: string
}
