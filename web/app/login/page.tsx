'use client'

export const dynamic = 'force-dynamic'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase } from '@/lib/supabase/client'
import { Lock, Mail, ShieldCheck, Store, ArrowRight } from 'lucide-react'

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      })
      if (error) throw error
      router.push('/dashboard')
    } catch (err: any) {
      setError(err.message || 'Failed to sign in')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-sky-950 to-slate-900 p-4">
      <div className="max-w-md w-full bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-100">
        <div className="bg-sky-600 p-8 text-white text-center relative overflow-hidden">
          <div className="absolute -right-10 -top-10 w-40 h-40 bg-sky-500 rounded-full opacity-50 blur-2xl"></div>
          <div className="inline-flex p-3 rounded-2xl bg-white/10 backdrop-blur-md mb-4 shadow-inner">
            <Store className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight">AuraPOS Back Office</h1>
          <p className="text-sky-100 text-sm mt-1">Online Management Portal & POS Sync</p>
        </div>

        <div className="p-8">
          {error && (
            <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm flex items-center gap-3">
              <span className="font-bold">Error:</span> {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">Email Address</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Mail className="w-4 h-4" />
                </span>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@aurapos.example.com"
                  className="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-1.5">Password</label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </span>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-4 py-3 rounded-xl border border-slate-200 text-slate-800 placeholder-slate-400 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-4 bg-sky-600 hover:bg-sky-700 text-white font-bold rounded-xl shadow-lg shadow-sky-600/30 transition-all flex items-center justify-center gap-2 group"
            >
              <span>{loading ? 'Signing in...' : 'Sign In to Back Office'}</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </button>
          </form>

          <div className="mt-6 pt-6 border-t border-slate-100 text-center">
            <button
              onClick={() => router.push('/dashboard')}
              className="text-xs font-bold text-sky-600 hover:text-sky-700 hover:underline inline-flex items-center gap-1.5"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>Skip Login / Open Demo Dashboard</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
