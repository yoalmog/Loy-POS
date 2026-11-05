import { createClient, type SupabaseClient } from '@supabase/supabase-js'
import { NextRequest, NextResponse } from 'next/server'

type AdminAuthResult =
  | { ok: true; supabase: SupabaseClient; userId: string }
  | { ok: false; response: NextResponse }

export async function requireStoreAdmin(request: NextRequest): Promise<AdminAuthResult> {
  const authorization = request.headers.get('authorization') || ''
  const match = authorization.match(/^Bearer\s+(.+)$/i)
  if (!match) {
    return { ok: false, response: NextResponse.json({ error: 'Authentication required' }, { status: 401 }) }
  }

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY
  if (!url || !anonKey || url.includes('placeholder') || anonKey.includes('placeholder')) {
    return { ok: false, response: NextResponse.json({ error: 'Supabase is not configured' }, { status: 503 }) }
  }

  const supabase = createClient(url, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
    global: { headers: { Authorization: `Bearer ${match[1]}` } },
  })

  const { data: authData, error: authError } = await supabase.auth.getUser(match[1])
  if (authError || !authData.user) {
    return { ok: false, response: NextResponse.json({ error: 'Invalid or expired session' }, { status: 401 }) }
  }

  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .select('role')
    .eq('id', authData.user.id)
    .maybeSingle()

  if (profileError || !profile || !['owner', 'admin'].includes(profile.role)) {
    return { ok: false, response: NextResponse.json({ error: 'Owner or admin access required' }, { status: 403 }) }
  }

  return { ok: true, supabase, userId: authData.user.id }
}
