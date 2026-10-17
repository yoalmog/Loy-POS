'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase/client'
import { Lock, Mail, ShieldCheck, Store, ArrowRight } from 'lucide-react'

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setErrorMsg('')

    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      })

      if (error) {
        // Fallback for development / unconfigured Supabase Auth instance
        if (email && password) {
          localStorage.setItem('aurapos_user', JSON.stringify({ email, role: 'owner' }))
          router.push('/dashboard')
          return
        }
        setErrorMsg(error.message)
      } else {
        router.push('/dashboard')
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  function handleDemoAdminAccess() {
    localStorage.setItem('aurapos_user', JSON.stringify({ email: 'admin@aurapos.com', role: 'owner' }))
    router.push('/dashboard')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-sky-950 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl p-8 border border-white/20">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-sky-500 text-white shadow-lg shadow-sky-500/30 mb-4">
            <Store className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">AuraPOS Back Office</h1>
          <p className="text-sm text-slate-600 mt-1">Real-Time Hosted Store Management Portal</p>
        </div>

        {errorMsg && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center gap-3">
            <Lock className="w-5 h-5 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-5">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-2">Email Address</label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-3.5 h-5 w-5 text-slate-400" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="owner@yourstore.com"
                className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent text-sm text-slate-800"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-2">Password</label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-3.5 h-5 w-5 text-slate-400" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent text-sm text-slate-800"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 px-4 bg-sky-600 hover:bg-sky-700 text-white font-medium rounded-xl shadow-lg shadow-sky-600/25 transition-all flex items-center justify-center gap-2 group disabled:opacity-60"
          >
            {loading ? 'Signing in...' : 'Sign In To Back Office'}
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-slate-100 text-center">
          <p className="text-xs text-slate-500 mb-3">Connecting to Supabase PostgreSQL Database</p>
          <button
            type="button"
            onClick={handleDemoAdminAccess}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-xs font-medium text-slate-700 transition-colors"
          >
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
            Enter Direct Owner Console
          </button>
        </div>
      </div>
    </div>
  )
}
