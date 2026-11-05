'use client'

export const dynamic = 'force-dynamic'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase, isSupabaseConfigured } from '@/lib/supabase/client'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { getTranslation } from '@/lib/i18n'
import {
  Lock,
  Mail,
  ShieldCheck,
  Store,
  ArrowRight,
  KeyRound,
  UserCheck,
  AlertCircle,
  Sparkles,
} from 'lucide-react'

export default function LoginPage() {
  const router = useRouter()
  const { settings, activateStaffMember } = usePos()
  const { language } = useTheme()
  const t = getTranslation(language)

  const [authMode, setAuthMode] = useState<'pin' | 'email'>('pin')
  const [pin, setPin] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Staff PINs are verified by the database; never compare against local/demo PINs.
  async function authenticateStaffPin(candidatePin: string) {
    if (!/^\\d{4,8}$/.test(candidatePin)) {
      setError(t.enterPin)
      return
    }
    setLoading(true)
    setError(null)
    try {
      if (!isSupabaseConfigured()) throw new Error('Staff PIN login requires an online connection.')
      const { data, error: pinError } = await supabase.rpc('verify_staff_pin', { p_pin: candidatePin })
      if (pinError) throw pinError
      const member = Array.isArray(data) ? data[0] : null
      if (!member?.id || !member?.display_name || !['cashier', 'manager'].includes(member.role)) {
        throw new Error(t.error)
      }
      const activeStaff = {
        id: member.id,
        name: member.display_name,
        role: member.role === 'manager' ? ('MANAGER' as const) : ('CASHIER' as const),
        pin: '',
        avatarColor: '#0288D1',
      }
      activateStaffMember(activeStaff)
      localStorage.setItem('aurapos_auth_staff', JSON.stringify(activeStaff))
      setSuccessMsg(t.success)
      setTimeout(() => router.replace('/pos'), 500)
    } catch (err: any) {
      setError(err.message || t.error)
      setPin('')
    } finally {
      setLoading(false)
    }
  }

  function handlePinLogin(e?: React.FormEvent) {
    if (e) e.preventDefault()
    void authenticateStaffPin(pin)
  }

  // Handle Cloud Supabase Email / Password Login
  async function handleEmailLogin(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      if (!isSupabaseConfigured()) {
        throw new Error(t.offlineMode)
      }

      const { data, error: authError } = await supabase.auth.signInWithPassword({
        email,
        password,
      })

      if (authError) throw authError

      // Owner/admin access requires an explicit trusted profile role.
      const { data: profile, error: profileError } = await supabase
        .from('profiles')
        .select('role, full_name')
        .eq('id', data.user.id)
        .single()

      if (profileError || !profile || !['owner', 'admin'].includes(profile.role)) {
        await supabase.auth.signOut()
        throw new Error('This account is not assigned the owner/admin role.')
      }

      const role = profile.role
      const name = profile.full_name || data.user.email?.split('@')[0] || 'Admin'

      const authStaff = {
        id: data.user.id,
        name,
        role: role === 'cashier' ? ('CASHIER' as const) : ('OWNER' as const),
        pin: 'cloud',
        avatarColor: '#0288D1',
      }
      localStorage.setItem('aurapos_auth_staff', JSON.stringify(authStaff))

      if (role === 'cashier') {
        router.replace('/pos')
      } else {
        router.replace('/dashboard')
      }
    } catch (err: any) {
      setError(err.message || t.error)
      setLoading(false)
    }
  }

  function handleKeypadClick(digit: string) {
    if (loading || pin.length >= 8) return
    const nextPin = pin + digit
    setPin(nextPin)
    if (nextPin.length >= 4) {
      setTimeout(() => { void authenticateStaffPin(nextPin) }, 150)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-sky-950 p-4 font-sans text-start">
      <div className="max-w-md w-full bg-slate-900/90 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-slate-800 text-slate-100">
        {/* Header Banner */}
        <div className="bg-gradient-to-r from-sky-600 to-indigo-600 p-7 text-white text-center relative overflow-hidden">
          <div className="absolute -right-10 -top-10 w-36 h-36 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
          <div className="inline-flex p-3 rounded-2xl bg-white/15 backdrop-blur-md mb-3 shadow-inner">
            <Store className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-black tracking-tight">{settings.store_name}</h1>
          <p className="text-sky-100 text-xs mt-1 font-medium">
            {t.portalTitle}
          </p>
        </div>

        {/* Tab Selection */}
        <div className="flex border-b border-slate-800 bg-slate-950/40">
          <button
            onClick={() => {
              setAuthMode('pin')
              setError(null)
            }}
            className={`flex-1 py-3.5 text-xs font-bold flex items-center justify-center gap-2 transition-all ${
              authMode === 'pin'
                ? 'text-sky-400 border-b-2 border-sky-500 bg-sky-500/10'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <KeyRound className="w-4 h-4" />
            <span>PIN ({t.customer})</span>
          </button>
          <button
            onClick={() => {
              setAuthMode('email')
              setError(null)
            }}
            className={`flex-1 py-3.5 text-xs font-bold flex items-center justify-center gap-2 transition-all ${
              authMode === 'email'
                ? 'text-sky-400 border-b-2 border-sky-500 bg-sky-500/10'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Mail className="w-4 h-4" />
            <span>{t.onlineCloud}</span>
          </button>
        </div>

        <div className="p-7">
          {error && (
            <div className="mb-5 p-3.5 rounded-xl bg-red-500/15 border border-red-500/30 text-red-300 text-xs flex items-center gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          {successMsg && (
            <div className="mb-5 p-3.5 rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2.5">
              <Sparkles className="w-4 h-4 shrink-0 text-emerald-400" />
              <span>{successMsg}</span>
            </div>
          )}

          {authMode === 'pin' ? (
            <div className="space-y-6">
              {/* PIN Display */}
              <div className="text-center">
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
                  {t.enterPin} (4 digits)
                </label>
                <div className="flex justify-center gap-3">
                  {[0, 1, 2, 3].map((idx) => (
                    <div
                      key={idx}
                      className={`w-12 h-14 rounded-2xl flex items-center justify-center text-xl font-bold border-2 transition-all ${
                        pin[idx]
                          ? 'border-sky-500 bg-sky-500/20 text-white shadow-lg shadow-sky-500/20 scale-105'
                          : 'border-slate-800 bg-slate-950/60 text-slate-500'
                      }`}
                    >
                      {pin[idx] ? '●' : '○'}
                    </div>
                  ))}
                </div>
              </div>

              {/* Numeric Keypad */}
              <div className="grid grid-cols-3 gap-2.5 max-w-xs mx-auto">
                {['1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', '0', '⌫'].map((btn) => (
                  <button
                    key={btn}
                    type="button"
                    onClick={() => {
                      if (btn === 'C') {
                        setPin('')
                        setError(null)
                      } else if (btn === '⌫') {
                        setPin((p) => p.slice(0, -1))
                      } else {
                        handleKeypadClick(btn)
                      }
                    }}
                    className={`h-12 rounded-xl text-sm font-bold transition-all active:scale-95 flex items-center justify-center ${
                      btn === 'C'
                        ? 'bg-red-500/20 text-red-300 hover:bg-red-500/30'
                        : btn === '⌫'
                        ? 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                        : 'bg-slate-800/80 hover:bg-slate-700 text-white shadow-sm'
                    }`}
                  >
                    {btn}
                  </button>
                ))}
              </div>

              <div className="bg-slate-950/60 p-3.5 rounded-2xl border border-slate-800 text-[11px] text-slate-400 space-y-1">
                <p className="font-bold text-slate-300">{t.ownerAuthRequired}:</p>
                <p>Use your assigned staff PIN. Owners sign in with their Supabase email and password.</p>
              </div>
            </div>
          ) : (
            <form onSubmit={handleEmailLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1.5">{t.email}</label>
                <div className="relative">
                  <span className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-slate-500">
                    <Mail className="w-4 h-4" />
                  </span>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="admin@aurapos.example.com"
                    className="w-full ps-10 pe-4 py-3 rounded-xl bg-slate-950/80 border border-slate-700 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1.5">{t.enterPin}</label>
                <div className="relative">
                  <span className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-slate-500">
                    <Lock className="w-4 h-4" />
                  </span>
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full ps-10 pe-4 py-3 rounded-xl bg-slate-950/80 border border-slate-700 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-4 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white font-bold rounded-xl shadow-lg shadow-sky-600/30 transition-all flex items-center justify-center gap-2 group mt-2"
              >
                <span>{loading ? t.loading : t.dashboard}</span>
                <ArrowRight className="w-4 h-4 ltr:group-hover:translate-x-1 rtl:group-hover:-translate-x-1 transition-transform rtl:rotate-180" />
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
