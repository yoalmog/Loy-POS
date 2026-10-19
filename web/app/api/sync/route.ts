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
          supabase.from('items').select('*').order('id', { ascending: true }),
          supabase.from('categories').select('*').order('id', { ascending: true }),
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
    let processedCount = 0

    if (payload.receipts && Array.isArray(payload.receipts) && payload.receipts.length > 0) {
      if (isSupabaseConfigured()) {
        for (const receipt of payload.receipts) {
          // Idempotency check: see if receipt with this ID or idempotency_key already exists
          const receiptKey = receipt.idempotency_key || receipt.id
          if (receiptKey) {
            const { data: existing } = await supabase
              .from('receipts')
              .select('id')
              .or(`id.eq.${receipt.id},idempotency_key.eq.${receiptKey}`)
              .limit(1)

            if (existing && existing.length > 0) {
              // Already recorded; skip duplicate stock deduction
              continue
            }
          }

          // Insert receipt
          await supabase.from('receipts').upsert([receipt])
          processedCount++

          // Parse sold items from summary to decrement inventory safely
          if (receipt.items_summary_json) {
            try {
              const summaryItems = JSON.parse(receipt.items_summary_json)
              if (Array.isArray(summaryItems)) {
                for (const item of summaryItems) {
                  const itemId = item.item_id || item.id
                  const qty = Number(item.quantity || 1)
                  if (itemId && qty > 0) {
                    const { data: currentItem } = await supabase
                      .from('items')
                      .select('in_stock, track_stock')
                      .eq('id', itemId)
                      .single()

                    if (currentItem && currentItem.track_stock) {
                      const newStock = Math.max(0, (currentItem.in_stock || 0) - qty)
                      await supabase.from('items').update({ in_stock: newStock }).eq('id', itemId)
                    }
                  }
                }
              }
            } catch (err) {
              console.warn('Could not parse receipt item summary for stock adjustment:', err)
            }
          }
        }
      } else {
        processedCount = payload.receipts.length
      }
    }

    return NextResponse.json(
      {
        success: true,
        synced_at: Date.now(),
        received_receipts_count: payload.receipts?.length || 0,
        new_receipts_processed: processedCount,
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
