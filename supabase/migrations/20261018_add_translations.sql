-- Migration: Additive Multilingual Translations and Appearance Storage
-- Version: 20261018_add_translations
-- Backward-compatible, safe, and reversible
-- Preserves all existing IDs, prices, SKUs, barcodes, and business records

-- 1. Add name_translations and description_translations to items
ALTER TABLE IF EXISTS public.items 
ADD COLUMN IF NOT EXISTS name_translations JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS public.items 
ADD COLUMN IF NOT EXISTS description_translations JSONB DEFAULT '{}'::jsonb;

-- 2. Add name_translations to categories
ALTER TABLE IF EXISTS public.categories 
ADD COLUMN IF NOT EXISTS name_translations JSONB DEFAULT '{}'::jsonb;

-- 3. Add name_translations to products (if present)
ALTER TABLE IF EXISTS public.products 
ADD COLUMN IF NOT EXISTS name_translations JSONB DEFAULT '{}'::jsonb;

-- 4. Add name_translations to modifier_groups & modifiers
ALTER TABLE IF EXISTS public.modifier_groups 
ADD COLUMN IF NOT EXISTS name_translations JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS public.modifiers 
ADD COLUMN IF NOT EXISTS name_translations JSONB DEFAULT '{}'::jsonb;

-- 5. Add language & appearance preferences to profiles
ALTER TABLE IF EXISTS public.profiles 
ADD COLUMN IF NOT EXISTS preferred_language TEXT DEFAULT 'es';

ALTER TABLE IF EXISTS public.profiles 
ADD COLUMN IF NOT EXISTS appearance_settings JSONB DEFAULT '{
  "mode": "light",
  "palette": "blue",
  "catalog_bg": "default",
  "card_appearance": "default"
}'::jsonb;

-- 6. GIN indexes for fast localized search and indexing
CREATE INDEX IF NOT EXISTS idx_items_name_translations ON public.items USING GIN (name_translations);
CREATE INDEX IF NOT EXISTS idx_categories_name_translations ON public.categories USING GIN (name_translations);
