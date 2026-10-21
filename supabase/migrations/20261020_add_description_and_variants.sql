-- Migration: Add description and variants to items table
-- Version: 20261020_add_description_and_variants
-- Backward-compatible, safe, and additive

ALTER TABLE IF EXISTS public.items 
ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';

ALTER TABLE IF EXISTS public.items 
ADD COLUMN IF NOT EXISTS variants JSONB DEFAULT '[]'::jsonb;

-- Ensure GIN index for variants JSONB lookup if queried
CREATE INDEX IF NOT EXISTS idx_items_variants ON public.items USING GIN (variants);
