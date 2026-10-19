import { NextRequest, NextResponse } from 'next/server'
import { supabase, isSupabaseConfigured } from '@/lib/supabase/client'
import { DEFAULT_CATEGORIES, DEFAULT_ITEMS } from '@/lib/db/idb'

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
    let categories = DEFAULT_CATEGORIES

    if (isSupabaseConfigured()) {
      try {
        const { data, error } = await supabase.from('categories').select('*').order('id', { ascending: true })
        if (!error && data && data.length > 0) {
          categories = data
        }
      } catch (err) {
        console.warn('Supabase categories fetch failed, falling back to canonical categories:', err)
      }
    }

    return NextResponse.json(categories, { headers: corsHeaders })
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to fetch categories' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    if (!body || !body.name || !body.name.trim()) {
      return NextResponse.json(
        { error: 'Category name is required' },
        { status: 400, headers: corsHeaders }
      )
    }

    const categoryData = {
      name: body.name.trim(),
      color_hex: body.color_hex || '#0288D1',
      name_translations: body.name_translations || { es: body.name.trim(), en: '', he: '' },
      target_printer: body.target_printer || 'KITCHEN',
    }

    if (body.id) {
      (categoryData as any).id = body.id
    }

    if (isSupabaseConfigured()) {
      const { data, error } = await supabase.from('categories').upsert([categoryData]).select()
      if (error) {
        return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
      }
      return NextResponse.json(data?.[0] || categoryData, { status: 201, headers: corsHeaders })
    }

    return NextResponse.json(
      { ...categoryData, id: body.id || Date.now() },
      { status: 201, headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to create category' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function PUT(request: NextRequest) {
  try {
    const body = await request.json()
    if (!body || !body.id) {
      return NextResponse.json(
        { error: 'Category ID is required for update' },
        { status: 400, headers: corsHeaders }
      )
    }
    if (body.name !== undefined && !body.name.trim()) {
      return NextResponse.json(
        { error: 'Category name cannot be empty' },
        { status: 400, headers: corsHeaders }
      )
    }

    const updates: Record<string, any> = {}
    if (body.name !== undefined) updates.name = body.name.trim()
    if (body.color_hex !== undefined) updates.color_hex = body.color_hex
    if (body.name_translations !== undefined) updates.name_translations = body.name_translations
    if (body.target_printer !== undefined) updates.target_printer = body.target_printer

    if (isSupabaseConfigured()) {
      const { data, error } = await supabase
        .from('categories')
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
      { error: error?.message || 'Failed to update category' },
      { status: 500, headers: corsHeaders }
    )
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    let id = searchParams.get('id')
    let reassignToId = searchParams.get('reassign_to_id')
    let force = searchParams.get('force') === 'true'

    // Also allow body parameters if query string is empty
    if (!id) {
      try {
        const body = await request.json()
        if (body?.id) id = String(body.id)
        if (body?.reassign_to_id) reassignToId = String(body.reassign_to_id)
        if (body?.force !== undefined) force = Boolean(body.force)
      } catch {
        // No body provided
      }
    }

    if (!id) {
      return NextResponse.json(
        { error: 'Category ID is required for deletion' },
        { status: 400, headers: corsHeaders }
      )
    }

    const categoryIdNum = Number(id)

    // Check for assigned items
    let assignedCount = 0
    if (isSupabaseConfigured()) {
      const { count } = await supabase
        .from('items')
        .select('*', { count: 'exact', head: true })
        .eq('category_id', categoryIdNum)
      assignedCount = count || 0
    } else {
      assignedCount = DEFAULT_ITEMS.filter((i) => i.category_id === categoryIdNum).length
    }

    // Safety policy: do not orphan products silently
    if (assignedCount > 0) {
      if (reassignToId) {
        // Reassign items to specified category
        const targetId = Number(reassignToId)
        if (isSupabaseConfigured()) {
          await supabase
            .from('items')
            .update({ category_id: targetId })
            .eq('category_id', categoryIdNum)
        }
      } else if (force) {
        // Force fallback to general/uncategorized (category_id = 1 or null)
        if (isSupabaseConfigured()) {
          await supabase
            .from('items')
            .update({ category_id: 1, category_name: 'General' })
            .eq('category_id', categoryIdNum)
        }
      } else {
        return NextResponse.json(
          {
            error: `Category has ${assignedCount} assigned product(s). Please reassign products before deletion or provide reassign_to_id.`,
            assigned_count: assignedCount,
            requires_reassignment: true,
          },
          { status: 409, headers: corsHeaders }
        )
      }
    }

    // Delete category
    if (isSupabaseConfigured()) {
      const { error } = await supabase.from('categories').delete().eq('id', categoryIdNum)
      if (error) {
        return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
      }
    }

    return NextResponse.json(
      {
        success: true,
        deleted_category_id: categoryIdNum,
        reassigned_products_count: assignedCount,
      },
      { headers: corsHeaders }
    )
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to delete category' },
      { status: 500, headers: corsHeaders }
    )
  }
}
