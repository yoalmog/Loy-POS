-- Supabase PostgreSQL Initial Migration for AuraPOS
-- Version: 20261016_initial_schema.sql

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Profiles & Roles Table (Extends Supabase Auth users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    full_name TEXT NOT NULL DEFAULT '',
    role TEXT NOT NULL CHECK (role IN ('owner', 'admin', 'cashier', 'manager')) DEFAULT 'cashier',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Store Settings Table
CREATE TABLE IF NOT EXISTS public.store_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_name TEXT NOT NULL DEFAULT 'AuraPOS Store',
    currency_symbol TEXT NOT NULL DEFAULT '$',
    tax_rate NUMERIC(5, 4) NOT NULL DEFAULT 0.0825,
    dining_options TEXT[] NOT NULL DEFAULT ARRAY['DINE_IN', 'TAKEOUT', 'DELIVERY'],
    receipt_header TEXT DEFAULT 'Thank you for dining with us!',
    receipt_footer TEXT DEFAULT 'Please come again!',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Categories Table
CREATE TABLE IF NOT EXISTS public.categories (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    color_hex TEXT NOT NULL DEFAULT '#2E7D32',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Products Catalog Table
CREATE TABLE IF NOT EXISTS public.products (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    category TEXT NOT NULL DEFAULT 'General',
    stock_level INT NOT NULL DEFAULT 0,
    image_url TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. Detailed Items Table (POS Catalog)
CREATE TABLE IF NOT EXISTS public.items (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    category_id BIGINT REFERENCES public.categories(id) ON DELETE SET NULL,
    category_name TEXT NOT NULL DEFAULT 'General',
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    cost NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    sku TEXT DEFAULT '',
    barcode TEXT DEFAULT '',
    sold_by TEXT NOT NULL CHECK (sold_by IN ('EACH', 'WEIGHT')) DEFAULT 'EACH',
    in_stock NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    low_stock_threshold NUMERIC(10, 2) NOT NULL DEFAULT 5.00,
    track_stock BOOLEAN NOT NULL DEFAULT true,
    color_hex TEXT NOT NULL DEFAULT '#4CAF50',
    modifier_ids TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. Modifier Groups & Modifiers
CREATE TABLE IF NOT EXISTS public.modifier_groups (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    options_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.modifiers (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    category_id BIGINT REFERENCES public.categories(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. Shifts Table
CREATE TABLE IF NOT EXISTS public.shifts (
    id BIGSERIAL PRIMARY KEY,
    opened_at BIGINT NOT NULL,
    closed_at BIGINT,
    cashier_name TEXT NOT NULL,
    starting_cash NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    cash_payments NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    cash_refunds NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    cash_paid_in NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    cash_paid_out NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    card_payments NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    actual_cash_counted NUMERIC(10, 2),
    is_open BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. Cash Actions Table
CREATE TABLE IF NOT EXISTS public.cash_actions (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT REFERENCES public.shifts(id) ON DELETE CASCADE,
    is_pay_in BOOLEAN NOT NULL DEFAULT true,
    amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    comment TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. Customers Table
CREATE TABLE IF NOT EXISTS public.customers (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT DEFAULT '',
    email TEXT DEFAULT '',
    customer_code TEXT UNIQUE,
    points_balance NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_visits INT NOT NULL DEFAULT 0,
    total_spent NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    note TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Loyalty Points Ledger
CREATE TABLE IF NOT EXISTS public.loyalty_points (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES public.customers(id) ON DELETE CASCADE,
    points NUMERIC(10, 2) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Receipts (Sales Header) Table
CREATE TABLE IF NOT EXISTS public.receipts (
    id BIGSERIAL PRIMARY KEY,
    receipt_number TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    timestamp_ms BIGINT NOT NULL,
    shift_id BIGINT,
    cashier_name TEXT NOT NULL,
    ticket_name TEXT NOT NULL DEFAULT 'Ticket',
    dining_option TEXT NOT NULL DEFAULT 'DINE_IN',
    subtotal NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    tip_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    payment_type TEXT NOT NULL DEFAULT 'CASH',
    payment_details_json JSONB DEFAULT '{}'::jsonb,
    customer_id BIGINT REFERENCES public.customers(id) ON DELETE SET NULL,
    customer_name TEXT,
    is_refunded BOOLEAN NOT NULL DEFAULT false,
    refund_timestamp BIGINT,
    items_summary_json JSONB DEFAULT '[]'::jsonb,
    idempotency_key UUID UNIQUE DEFAULT gen_random_uuid()
);

-- 12. Timecards Table
CREATE TABLE IF NOT EXISTS public.timecards (
    id BIGSERIAL PRIMARY KEY,
    employee_name TEXT NOT NULL,
    clock_in_time BIGINT NOT NULL,
    clock_out_time BIGINT,
    total_hours NUMERIC(6, 2) DEFAULT 0.00,
    is_clocked_in BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create Indexes for High Performance Queries
CREATE INDEX IF NOT EXISTS idx_items_category_id ON public.items(category_id);
CREATE INDEX IF NOT EXISTS idx_items_barcode ON public.items(barcode);
CREATE INDEX IF NOT EXISTS idx_items_sku ON public.items(sku);
CREATE INDEX IF NOT EXISTS idx_receipts_created_at ON public.receipts(created_at);
CREATE INDEX IF NOT EXISTS idx_receipts_customer_id ON public.receipts(customer_id);
CREATE INDEX IF NOT EXISTS idx_receipts_receipt_number ON public.receipts(receipt_number);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON public.customers(phone);

-- ENABLE ROW LEVEL SECURITY (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modifier_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modifiers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shifts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cash_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.loyalty_points ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.timecards ENABLE ROW LEVEL SECURITY;

-- RLS POLICIES (Allow authenticated access and anon public read/write for POS sync)
CREATE POLICY "Allow public read access to categories" ON public.categories FOR SELECT USING (true);
CREATE POLICY "Allow public insert/update to categories" ON public.categories FOR ALL USING (true);

CREATE POLICY "Allow public read access to products" ON public.products FOR SELECT USING (true);
CREATE POLICY "Allow public insert/update to products" ON public.products FOR ALL USING (true);

CREATE POLICY "Allow public read access to items" ON public.items FOR SELECT USING (true);
CREATE POLICY "Allow public insert/update to items" ON public.items FOR ALL USING (true);

CREATE POLICY "Allow public read access to modifiers" ON public.modifiers FOR SELECT USING (true);
CREATE POLICY "Allow public write access to modifiers" ON public.modifiers FOR ALL USING (true);

CREATE POLICY "Allow public access to shifts" ON public.shifts FOR ALL USING (true);
CREATE POLICY "Allow public access to cash_actions" ON public.cash_actions FOR ALL USING (true);

CREATE POLICY "Allow public access to customers" ON public.customers FOR ALL USING (true);
CREATE POLICY "Allow public access to loyalty_points" ON public.loyalty_points FOR ALL USING (true);

CREATE POLICY "Allow public access to receipts" ON public.receipts FOR ALL USING (true);
CREATE POLICY "Allow public access to timecards" ON public.timecards FOR ALL USING (true);
CREATE POLICY "Allow public access to store_settings" ON public.store_settings FOR ALL USING (true);

-- Insert Default Store Settings Row if none exists
INSERT INTO public.store_settings (store_name, currency_symbol, tax_rate)
VALUES ('AuraPOS Store', '$', 0.0825)
ON CONFLICT DO NOTHING;
