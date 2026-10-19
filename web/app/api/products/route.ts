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
    const activeOnly = searchParams.get('active_only') === 'true'

    let items = DEFAULT_ITEMS

    if (isSupabaseConfigured()) {
      try {
        let dbQuery = supabase.from('items').select('*').order('id', { ascending: true })
        if (categoryId) {
          dbQuery = dbQuery.eq('category_id', Number(categoryId))
        }
        if (activeOnly) {
          dbQuery = dbQuery.eq('is_active', true)
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
    if (activeOnly && !isSupabaseConfigured()) {
      filtered = filtered.filter((i) => i.is_active !== false)
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
    if (!body || !body.name || !body.name.trim()) {
      return NextResponse.json(
        { error: 'Product name is required' },
        { status: 400, headers: corsHeaders }
      )
    }

    const price = Number(body.price ?? 0)
    if (isNaN(price) || price < 0) {
      return NextResponse.json(
        { error: 'Price must be a non-negative number' },
        { status: 400, headers: corsHeaders }
      )
    }

    const cost = Number(body.cost ?? 0)
    if (isNaN(cost) || cost < 0) {
      return NextResponse.json(
        { error: 'Cost must be a non-negative number' },
        { status: 400, headers: corsHeaders }
      )
    }

    const productData = {
      name: body.name.trim(),
      name_translations: body.name_translations || { es: body.name.trim(), en: '', he: '' },
      description: body.description || '',
      description_translations: body.description_translations || { es: '', en: '', he: '' },
      category_id: body.category_id ? Number(body.category_id) : 1,
      category_name: body.category_name || 'General',
      price: price,
      cost: cost,
      sku: body.sku?.trim() || `SKU-${Date.now().toString().slice(-4)}`,
      barcode: body.barcode?.trim() || '',
      sold_by: body.sold_by === 'WEIGHT' ? 'WEIGHT' : 'EACH',
      in_stock: Number(body.in_stock ?? 0),
      low_stock_threshold: Number(body.low_stock_threshold ?? 5),
      track_stock: body.track_stock !== false,
      is_active: body.is_active !== false,
      is_favorite: Boolean(body.is_favorite),
      color_hex: body.color_hex || '#0288D1',
      unit_of_measure: body.unit_of_measure || 'item',
      tax_rate_percent: Number(body.tax_rate_percent ?? 18),
      image_url: body.image_url || '',
    }

    if (body.id) {
      (productData as any).id = body.id
    }

    if (isSupabaseConfigured()) {
      const { data, error } = await supabase.from('items').upsert([productData]).select()
      if (error) {
        return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
      }
      return NextResponse.json(data?.[0] || productData, { status: 201, headers: corsHeaders })
    }

    return NextResponse.json(
      { ...productData, id: body.id || Date.now() },
      { status: 201, headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to save product' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function PUT(request: NextRequest) {
  try {
    const body = await request.json()
    if (!body || !body.id) {
      return NextResponse.json(
        { error: 'Product ID is required for update' },
        { status: 400, headers: corsHeaders }
      )
    }

    if (body.price !== undefined) {
      const price = Number(body.price)
      if (isNaN(price) || price < 0) {
        return NextResponse.json(
          { error: 'Price must be a non-negative number' },
          { status: 400, headers: corsHeaders }
        )
      }
    }

    if (body.cost !== undefined) {
      const cost = Number(body.cost)
      if (isNaN(cost) || cost < 0) {
        return NextResponse.json(
          { error: 'Cost must be a non-negative number' },
          { status: 400, headers: corsHeaders }
        )
      }
    }

    const updates: Record<string, any> = { ...body }
    delete updates.id // do not overwrite primary key

    if (isSupabaseConfigured()) {
      const { data, error } = await supabase
        .from('items')
        .update(updates)
        .eq('id', body.id)
        .select()

      if (error) {
        return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
      }
      return NextResponse.json(data?.[0] || { id: body.id, ...updates }, { headers: corsHeaders })
    }

    return NextResponse.json({ id: body.id, ...updates }, { headers: corsHeaders })
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to update product' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    let id = searchParams.get('id')
    let archiveOnly = searchParams.get('archive') === 'true'

    if (!id) {
      try {
        const body = await request.json()
        if (body?.id) id = String(body.id)
        if (body?.archive !== undefined) archiveOnly = Boolean(body.archive)
      } catch {
        // No body
      }
    }

    if (!id) {
      return NextResponse.json(
        { error: 'Product ID is required for deletion/archival' },
        { status: 400, headers: corsHeaders }
      )
    }

    const itemIdNum = Number(id)

    if (isSupabaseConfigured()) {
      if (archiveOnly) {
        // Safe archival: Mark inactive so it cannot be sold in POS but preserves history
        const { error } = await supabase
          .from('items')
          .update({ is_active: false })
          .eq('id', itemIdNum)
        if (error) {
          return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
        }
      } else {
        // Permanent delete
        const { error } = await supabase.from('items').delete().eq('id', itemIdNum)
        if (error) {
          return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
        }
      }
    }

    return NextResponse.json(
      {
        success: true,
        item_id: itemIdNum,
        archived: archiveOnly,
      },
      { headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to delete/archive product' },
      { status: 500, headers: corsHeaders }
    )
  }
}
