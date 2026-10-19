import { NextRequest, NextResponse } from 'next/server'
import { supabase, isSupabaseConfigured } from '@/lib/supabase/client'
import { DEFAULT_CATEGORIES } from '@/lib/db/idb'

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
        const { data, error } = await supabase.from('categories').select('*').order('sort_order', { ascending: true })
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
    if (!body || !body.name) {
      return NextResponse.json(
        { error: 'Category name is required' },
        { status: 400, headers: corsHeaders }
      )
    }

    if (isSupabaseConfigured()) {
      const { data, error } = await supabase.from('categories').upsert([body]).select()
      if (error) {
        return NextResponse.json({ error: error.message }, { status: 500, headers: corsHeaders })
      }
      return NextResponse.json(data?.[0] || body, { headers: corsHeaders })
    }

    return NextResponse.json({ ...body, id: body.id || Date.now() }, { headers: corsHeaders })
  } catch (error: any) {
    return NextResponse.json(
      { error: error?.message || 'Failed to save category' },
      { status: 500, headers: corsHeaders }
    )
  }
}
