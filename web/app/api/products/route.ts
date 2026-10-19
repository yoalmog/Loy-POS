import { NextRequest, NextResponse } from 'next/server'
import { supabase, isSupabaseConfigured } from '@/lib/supabase/client'
import { DEFAULT_ITEMS } from '@/lib/db/idb'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey',
}

export async function OPTIONS() {
  return NextResponse.json({}, { headers: corsHeaders })
}

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const categoryId = searchParams.get('category_id')
    const query = searchParams.get('q') || searchParams.get('search')

    let items = DEFAULT_ITEMS

    if (isSupabaseConfigured()) {
      try {
        let dbQuery = supabase.from('items').select('*')
        if (categoryId) {
          dbQuery = dbQuery.eq('category_id', Number(categoryId))
        }
        const { data, error } = await dbQuery
        if (!error && data && data.length > 0) {
          items = data
        }
      } catch (err) {
        console.warn('Supabase items fetch failed, falling back to canonical catalog:', err)
      }
    }

    let filtered = items
    if (categoryId && !isSupabaseConfigured()) {
      filtered = filtered.filter((i) => i.category_id === Number(categoryId))
    }
    if (query && query.trim()) {
      const q = query.toLowerCase().trim()
      filtered = filtered.filter((i) => {
        const nameMatch = i.name?.toLowerCase().includes(q)
        const skuMatch = i.sku?.toLowerCase().includes(q)
        const barcodeMatch = i.barcode?.includes(q)
        const transMatch = Object.values(i.name_translations || {}).some(
          (t) => typeof t === 'string' && t.toLowerCase().includes(q)
        )
        return nameMatch || skuMatch || barcodeMatch || transMatch
      })
    }

    return NextResponse.json(filtered, { headers: corsHeaders })
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to fetch products' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    if (!body || !body.name) {
      return NextResponse.json(
        { error: 'Product name is required' },
        { status: 400, headers: corsHeaders }
      )
    }

    if (isSupabaseConfigured()) {
      const { data, error } = await supabase.from('items').upsert([body]).select()
      if (error) {
        return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
      }
      return NextResponse.json(data?.[0] || body, { headers: corsHeaders })
    }

    return NextResponse.json({ ...body, id: body.id || Date.now() }, { headers: corsHeaders })
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to save product' },
      { status: 500, headers: corsHeaders }
    )
  }
}
