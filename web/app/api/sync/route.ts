import { NextRequest, NextResponse } from 'next/server'
import { supabase, isSupabaseConfigured } from '@/lib/supabase/client'
import { DEFAULT_ITEMS, DEFAULT_CATEGORIES, DEFAULT_SETTINGS } from '@/lib/db/idb'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey',
}

export async function OPTIONS() {
  return NextResponse.json({}, { headers: corsHeaders })
}

export async function GET() {
  try {
    let items = DEFAULT_ITEMS
    let categories = DEFAULT_CATEGORIES
    let settings = DEFAULT_SETTINGS

    if (isSupabaseConfigured()) {
      try {
        const [itemsRes, catsRes, settingsRes] = await Promise.allSettled([
          supabase.from('items').select('*'),
          supabase.from('categories').select('*'),
          supabase.from('store_settings').select('*').eq('id', 'main').single(),
        ])

        if (itemsRes.status === 'fulfilled' && itemsRes.value.data && itemsRes.value.data.length > 0) {
          items = itemsRes.value.data
        }
        if (catsRes.status === 'fulfilled' && catsRes.value.data && catsRes.value.data.length > 0) {
          categories = catsRes.value.data
        }
        if (settingsRes.status === 'fulfilled' && settingsRes.value.data) {
          settings = { ...DEFAULT_SETTINGS, ...settingsRes.value.data }
        }
      } catch (err) {
        console.warn('Supabase sync read fallback to canonical catalog:', err)
      }
    }

    return NextResponse.json(
      {
        items,
        categories,
        settings,
        timestamp: Date.now(),
        server_status: 'online',
      },
      { headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Sync failed' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function POST(request: NextRequest) {
  try {
    const payload = await request.json()
    // Process receipts, shift updates or offline orders if passed
    if (payload.receipts && Array.isArray(payload.receipts) && isSupabaseConfigured()) {
      await supabase.from('receipts').upsert(payload.receipts)
    }

    return NextResponse.json(
      {
        success: true,
        synced_at: Date.now(),
        received_receipts_count: payload.receipts?.length || 0,
      },
      { headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to process sync push' },
      { status: 500, headers: corsHeaders }
    )
  }
}
