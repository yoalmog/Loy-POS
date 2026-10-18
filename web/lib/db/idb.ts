import { 
  Item, 
  Category, 
  Customer, 
  Receipt, 
  HeldOrder, 
  StockMovement, 
  Shift, 
  Promotion, 
  StoreSettings, 
  StaffMember, 
  PrinterConfig 
} from '../types'

const DB_NAME = 'AuraPOS_DB'
const DB_VERSION = 1

export const DEFAULT_SETTINGS: StoreSettings = {
  id: 'main',
  store_name: 'AuraPOS Bistro & Bar',
  address: 'Herzl St 42, Tel Aviv-Yafo, Israel',
  phone: '+972 3-555-1234',
  email: 'contact@aurapos.co.il',
  currency: 'ILS',
  currency_symbol: '₪',
  tax_rate: 17.0, // 17% VAT in Israel
  tax_included: true,
  receipt_header: 'ברוכים הבאים ל-AuraPOS Bistro!',
  receipt_footer: 'תודה שקניתם אצלנו! נשמח לראותכם שוב.',
  return_policy: 'החזרות והחלפות בהצגת קבלה תוך 14 יום בהתאם לחוק הגנת הצרכן.',
  qr_payment_yape_name: 'AuraPOS Bistro',
  qr_payment_yape_phone: '054-987-6543',
  qr_payment_plin_name: 'AuraPOS Payments',
  qr_payment_plin_phone: '054-987-6543',
  loyalty_earn_rate: 1.0, // 1 point per ₪1
  loyalty_redemption_rate: 5.0, // ₪5 discount per 100 points
  allow_negative_stock: true,
  owner_pin: '1234'
}

export const DEFAULT_STAFF: StaffMember[] = [
  { id: '1', name: 'אלכס כהן (קופאי)', pin: '1111', role: 'CASHIER', avatarColor: '#0288D1' },
  { id: '2', name: 'שרה לוי (מנהלת)', pin: '2222', role: 'MANAGER', avatarColor: '#7C3AED' },
  { id: '3', name: 'דוד ישראלי (בעלים)', pin: '1234', role: 'OWNER', avatarColor: '#10B981' }
]

export const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, name: 'המבורגרים ובשרים', color_hex: '#EF4444', icon: 'Beef', target_printer: 'KITCHEN', sort_order: 1 },
  { id: 2, name: 'פיצות ומאפים', color_hex: '#F59E0B', icon: 'Pizza', target_printer: 'KITCHEN', sort_order: 2 },
  { id: 3, name: 'סלטים ותוספות', color_hex: '#10B981', icon: 'Salad', target_printer: 'KITCHEN', sort_order: 3 },
  { id: 4, name: 'משקאות קלים', color_hex: '#06B6D4', icon: 'CupSoda', target_printer: 'BAR', sort_order: 4 },
  { id: 5, name: 'קפה ומשקאות חמים', color_hex: '#8B5CF6', icon: 'Coffee', target_printer: 'BAR', sort_order: 5 },
  { id: 6, name: 'בירות ומשקאות אלכוהוליים', color_hex: '#F97316', icon: 'Beer', target_printer: 'BAR', sort_order: 6 },
  { id: 7, name: 'קינוחים וגלידות', color_hex: '#EC4899', icon: 'Cake', target_printer: 'KITCHEN', sort_order: 7 },
]

export const DEFAULT_ITEMS: Item[] = [
  {
    id: 101,
    name: 'קלאסיק בורגר אנגוס',
    category_id: 1,
    category_name: 'המבורגרים ובשרים',
    price: 58.0,
    cost: 18.0,
    sku: 'BGR-ANG-01',
    barcode: '729000100101',
    sold_by: 'EACH',
    in_stock: 45,
    low_stock_threshold: 10,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#EF4444',
    image_url: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    variants: [
      { id: 'v1', name: '200 גרם', price: 58.0, cost: 18.0, in_stock: 30 },
      { id: 'v2', name: '300 גרם', price: 72.0, cost: 24.0, in_stock: 15 },
    ],
    modifier_groups: [
      {
        id: 1,
        name: 'תוספות מעל',
        options: [
          { id: 'm1', name: 'גבינת צ׳דר מותכת', price: 8.0 },
          { id: 'm2', name: 'בצל מקורמל', price: 6.0 },
          { id: 'm3', name: 'ביצת עין', price: 7.0 },
          { id: 'm4', name: 'רצועות אסאדו', price: 16.0 },
        ]
      },
      {
        id: 2,
        name: 'מידת עשייה',
        options: [
          { id: 'd1', name: 'Medium (M)', price: 0.0 },
          { id: 'd2', name: 'Medium-Well (MW)', price: 0.0 },
          { id: 'd3', name: 'Well-Done (WD)', price: 0.0 },
        ]
      }
    ]
  },
  {
    id: 102,
    name: 'סמאש בורגר כפול',
    category_id: 1,
    category_name: 'המבורגרים ובשרים',
    price: 64.0,
    cost: 21.0,
    sku: 'BGR-SMASH-02',
    barcode: '729000100102',
    sold_by: 'EACH',
    in_stock: 32,
    low_stock_threshold: 8,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#DC2626',
    image_url: 'https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    modifier_groups: [
      {
        id: 1,
        name: 'תוספות',
        options: [
          { id: 'sm1', name: 'אקסטרה צ׳דר', price: 8.0 },
          { id: 'sm2', name: 'חלפיניו חריף', price: 5.0 },
          { id: 'sm3', name: 'רוטב צ׳ימיצ׳ורי ביתי', price: 4.0 },
        ]
      }
    ]
  },
  {
    id: 103,
    name: 'פיצה מרגריטה נאפוליטנית',
    category_id: 2,
    category_name: 'פיצות ומאפים',
    price: 52.0,
    cost: 12.0,
    sku: 'PIZ-MARG-01',
    barcode: '729000100103',
    sold_by: 'EACH',
    in_stock: 60,
    low_stock_threshold: 12,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#F59E0B',
    image_url: 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    variants: [
      { id: 'pv1', name: 'אישית (28 ס״מ)', price: 42.0, cost: 9.0, in_stock: 35 },
      { id: 'pv2', name: 'משפחתית (36 ס״מ)', price: 62.0, cost: 15.0, in_stock: 25 },
    ],
    modifier_groups: [
      {
        id: 3,
        name: 'תוספות לפיצה',
        options: [
          { id: 'pt1', name: 'פטריות טריות', price: 6.0 },
          { id: 'pt2', name: 'זיתי קלמטה', price: 6.0 },
          { id: 'pt3', name: 'בצל סגול ועגבניות שרי', price: 5.0 },
          { id: 'pt4', name: 'מוצרלה כפולה', price: 8.0 }
        ]
      }
    ]
  },
  {
    id: 104,
    name: 'סלט קיסר קראנץ׳',
    category_id: 3,
    category_name: 'סלטים ותוספות',
    price: 46.0,
    cost: 11.0,
    sku: 'SLD-CES-01',
    barcode: '729000100104',
    sold_by: 'EACH',
    in_stock: 28,
    low_stock_threshold: 6,
    track_stock: true,
    is_favorite: false,
    is_active: true,
    color_hex: '#10B981',
    image_url: 'https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    modifier_groups: [
      {
        id: 4,
        name: 'שדרוג סלט',
        options: [
          { id: 'sl1', name: 'תוספת רצועות עוף צלויות', price: 14.0 },
          { id: 'sl2', name: 'רוטב בצד', price: 0.0 },
          { id: 'sl3', name: 'ללא קרוטונים', price: 0.0 }
        ]
      }
    ]
  },
  {
    id: 105,
    name: 'צ׳יפס הולנדי פריך',
    category_id: 3,
    category_name: 'סלטים ותוספות',
    price: 24.0,
    cost: 5.0,
    sku: 'SID-FRY-01',
    barcode: '729000100105',
    sold_by: 'EACH',
    in_stock: 80,
    low_stock_threshold: 20,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#059669',
    image_url: 'https://images.unsplash.com/photo-1576107232684-1279f3908594?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    variants: [
      { id: 'fv1', name: 'מנה קטנה', price: 18.0, cost: 3.5, in_stock: 40 },
      { id: 'fv2', name: 'מנה ענקית לחלוקה', price: 28.0, cost: 6.5, in_stock: 40 },
    ]
  },
  {
    id: 106,
    name: 'קוקה קולה זירו (זכוכית)',
    category_id: 4,
    category_name: 'משקאות קלים',
    price: 14.0,
    cost: 4.0,
    sku: 'DRK-COK-01',
    barcode: '729000100106',
    sold_by: 'EACH',
    in_stock: 120,
    low_stock_threshold: 24,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#06B6D4',
    image_url: 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17
  },
  {
    id: 107,
    name: 'קפוצ׳ינו / קפה הפוך',
    category_id: 5,
    category_name: 'קפה ומשקאות חמים',
    price: 15.0,
    cost: 3.0,
    sku: 'HOT-CAP-01',
    barcode: '729000100107',
    sold_by: 'EACH',
    in_stock: 200,
    low_stock_threshold: 30,
    track_stock: false,
    is_favorite: true,
    is_active: true,
    color_hex: '#8B5CF6',
    image_url: 'https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'cup',
    tax_rate_percent: 17,
    variants: [
      { id: 'cv1', name: 'כוס קטנה', price: 14.0, cost: 2.8, in_stock: 100 },
      { id: 'cv2', name: 'כוס גדולה', price: 17.0, cost: 3.5, in_stock: 100 },
    ],
    modifier_groups: [
      {
        id: 5,
        name: 'סוג חלב',
        options: [
          { id: 'm_reg', name: 'חלב רגיל 3%', price: 0.0 },
          { id: 'm_soy', name: 'חלב שיבולת שועל', price: 3.0 },
          { id: 'm_almond', name: 'חלב שקדים', price: 3.0 },
          { id: 'm_skim', name: 'חלב דל שומן 1%', price: 0.0 }
        ]
      }
    ]
  },
  {
    id: 108,
    name: 'בירה מהחבית - גולדסטאר Unfiltered',
    category_id: 6,
    category_name: 'בירות ומשקאות אלכוהוליים',
    price: 29.0,
    cost: 8.0,
    sku: 'ALC-BER-01',
    barcode: '729000100108',
    sold_by: 'EACH',
    in_stock: 50,
    low_stock_threshold: 15,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#F97316',
    image_url: 'https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    variants: [
      { id: 'bv1', name: 'חצי ליטר (500 מ״ל)', price: 32.0, cost: 9.0, in_stock: 30 },
      { id: 'bv2', name: 'שליש ליטר (330 מ״ל)', price: 26.0, cost: 6.5, in_stock: 20 },
    ]
  },
  {
    id: 109,
    name: 'עוגת שוקולד חמה נמסה (וולקנו)',
    category_id: 7,
    category_name: 'קינוחים וגלידות',
    price: 38.0,
    cost: 9.5,
    sku: 'DES-CHOC-01',
    barcode: '729000100109',
    sold_by: 'EACH',
    in_stock: 18,
    low_stock_threshold: 5,
    track_stock: true,
    is_favorite: true,
    is_active: true,
    color_hex: '#EC4899',
    image_url: 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600&auto=format&fit=crop&q=80',
    unit_of_measure: 'item',
    tax_rate_percent: 17,
    modifier_groups: [
      {
        id: 6,
        name: 'כדור גלידה בצד',
        options: [
          { id: 'ic1', name: 'וניל צרפתי', price: 6.0 },
          { id: 'ic2', name: 'קרמל מלוח', price: 6.0 },
          { id: 'ic3', name: 'קצפת עשירה', price: 4.0 }
        ]
      }
    ]
  }
]

export const DEFAULT_CUSTOMERS: Customer[] = [
  {
    id: 1,
    name: 'יוסי כהן',
    phone: '050-1234567',
    email: 'yossi.cohen@example.com',
    address: 'דיזנגוף 100, תל אביב',
    customer_code: 'CUST-1001',
    points_balance: 340,
    total_visits: 12,
    total_spent: 1850.0,
    note: 'לקוח VIP קבוע, מעדיף שולחן שקט בפינה'
  },
  {
    id: 2,
    name: 'נועה אברהם',
    phone: '052-7654321',
    email: 'noa.avraham@example.com',
    address: 'רוטשילד 45, תל אביב',
    customer_code: 'CUST-1002',
    points_balance: 180,
    total_visits: 6,
    total_spent: 920.0,
    note: 'צמחונית, רגישה לבוטנים'
  },
  {
    id: 3,
    name: 'מיכאל לוי',
    phone: '054-3322114',
    email: 'michael.levy@example.com',
    address: 'אבן גבירול 80, תל אביב',
    customer_code: 'CUST-1003',
    points_balance: 620,
    total_visits: 21,
    total_spent: 3100.0,
    note: 'מזמין בדרך כלל משלוחים לאזור המרכז'
  }
]

export const DEFAULT_PROMOTIONS: Promotion[] = [
  {
    id: 'promo1',
    code: 'HAPPYHOUR',
    name: 'Happy Hour - 15% הנחה על הכל',
    discount_type: 'PERCENT',
    value: 15.0,
    min_order_amount: 50.0,
    max_discount_cap: 100.0,
    is_active: true
  },
  {
    id: 'promo2',
    code: 'WELCOME20',
    name: 'קופון הצטרפות מועדון - ₪20 הנחה',
    discount_type: 'FIXED',
    value: 20.0,
    min_order_amount: 100.0,
    is_active: true
  }
]

export const DEFAULT_PRINTERS: PrinterConfig[] = [
  {
    id: 'p1',
    name: 'מדפסת קופה ראשית (Cashier)',
    type: 'CASHIER',
    interface_type: 'BROWSER',
    paper_width: 80,
    auto_cut: true,
    cash_drawer_kick: true,
    enabled: true
  },
  {
    id: 'p2',
    name: 'מדפסת מטבח (Kitchen LAN 9100)',
    type: 'KITCHEN',
    interface_type: 'LAN',
    ip_address: '192.168.1.200',
    port: 9100,
    paper_width: 80,
    auto_cut: true,
    cash_drawer_kick: false,
    enabled: true
  },
  {
    id: 'p3',
    name: 'מדפסת בר ניידת (Bar BLE Bluetooth)',
    type: 'BAR',
    interface_type: 'BLUETOOTH',
    paper_width: 58,
    auto_cut: false,
    cash_drawer_kick: false,
    enabled: true
  }
]

// Open IndexedDB database with schema
function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') {
      return reject(new Error('IndexedDB only available in browser'))
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
      const db = (event.target as IDBOpenDBRequest).result

      const stores = [
        'items',
        'categories',
        'customers',
        'receipts',
        'held_orders',
        'stock_movements',
        'shifts',
        'promotions',
        'store_settings',
        'staff',
        'printers',
        'offline_queue'
      ]

      stores.forEach((storeName) => {
        if (!db.objectStoreNames.contains(storeName)) {
          db.createObjectStore(storeName, { 
            keyPath: storeName === 'store_settings' ? 'id' : 'id' 
          })
        }
      })
    }

    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

// Generic IndexedDB operations
export async function idbGetAll<T>(storeName: string): Promise<T[]> {
  try {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly')
      const store = tx.objectStore(storeName)
      const request = store.getAll()
      request.onsuccess = () => resolve(request.result || [])
      request.onerror = () => reject(request.error)
    })
  } catch (err) {
    console.warn(`IndexedDB getAll error for ${storeName}:`, err)
    return []
  }
}

export async function idbGet<T>(storeName: string, key: string | number): Promise<T | null> {
  try {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readonly')
      const store = tx.objectStore(storeName)
      const request = store.get(key)
      request.onsuccess = () => resolve(request.result || null)
      request.onerror = () => reject(request.error)
    })
  } catch (err) {
    console.warn(`IndexedDB get error for ${storeName} key ${key}:`, err)
    return null
  }
}

export async function idbPut<T>(storeName: string, value: T): Promise<void> {
  try {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite')
      const store = tx.objectStore(storeName)
      const request = store.put(value)
      request.onsuccess = () => resolve()
      request.onerror = () => reject(request.error)
    })
  } catch (err) {
    console.warn(`IndexedDB put error for ${storeName}:`, err)
  }
}

export async function idbDelete(storeName: string, key: string | number): Promise<void> {
  try {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite')
      const store = tx.objectStore(storeName)
      const request = store.delete(key)
      request.onsuccess = () => resolve()
      request.onerror = () => reject(request.error)
    })
  } catch (err) {
    console.warn(`IndexedDB delete error for ${storeName} key ${key}:`, err)
  }
}

export async function idbClear(storeName: string): Promise<void> {
  try {
    const db = await openDB()
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite')
      const store = tx.objectStore(storeName)
      const request = store.clear()
      request.onsuccess = () => resolve()
      request.onerror = () => reject(request.error)
    })
  } catch (err) {
    console.warn(`IndexedDB clear error for ${storeName}:`, err)
  }
}

// Database initialization & default seeding
export async function initializeDatabase(): Promise<void> {
  if (typeof window === 'undefined') return

  try {
    const settings = await idbGet<StoreSettings>('store_settings', 'main')
    if (!settings) {
      await idbPut('store_settings', DEFAULT_SETTINGS)
    }

    const items = await idbGetAll<Item>('items')
    if (items.length === 0) {
      for (const item of DEFAULT_ITEMS) {
        await idbPut('items', item)
      }
    }

    const categories = await idbGetAll<Category>('categories')
    if (categories.length === 0) {
      for (const cat of DEFAULT_CATEGORIES) {
        await idbPut('categories', cat)
      }
    }

    const customers = await idbGetAll<Customer>('customers')
    if (customers.length === 0) {
      for (const cust of DEFAULT_CUSTOMERS) {
        await idbPut('customers', cust)
      }
    }

    const promotions = await idbGetAll<Promotion>('promotions')
    if (promotions.length === 0) {
      for (const promo of DEFAULT_PROMOTIONS) {
        await idbPut('promotions', promo)
      }
    }

    const staff = await idbGetAll<StaffMember>('staff')
    if (staff.length === 0) {
      for (const s of DEFAULT_STAFF) {
        await idbPut('staff', s)
      }
    }

    const printers = await idbGetAll<PrinterConfig>('printers')
    if (printers.length === 0) {
      for (const p of DEFAULT_PRINTERS) {
        await idbPut('printers', p)
      }
    }

    const shifts = await idbGetAll<Shift>('shifts')
    if (shifts.length === 0) {
      const initialShift: Shift = {
        id: 1,
        opened_at: new Date().toISOString(),
        cashier_name: 'אלכס כהן (קופאי)',
        starting_cash: 500.0,
        cash_payments: 0.0,
        cash_refunds: 0.0,
        cash_paid_in: 0.0,
        cash_paid_out: 0.0,
        card_payments: 0.0,
        digital_payments: 0.0,
        is_open: true
      }
      await idbPut('shifts', initialShift)
    }
  } catch (error) {
    console.error('Failed to initialize local IndexedDB:', error)
  }
}
