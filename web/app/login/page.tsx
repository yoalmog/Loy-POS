'use client'

export const dynamic = 'force-dynamic'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { supabase, isSupabaseConfigured } from '@/lib/supabase/client'
import { usePos } from '@/lib/context/PosContext'
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
  const { staffList, settings, switchStaffWithPin } = usePos()

  const [authMode, setAuthMode] = useState<'pin' | 'email'>('pin')
  const [pin, setPin] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Handle Staff PIN Login
  function handlePinLogin(e?: React.FormEvent) {
    if (e) e.preventDefault()
    if (!pin || pin.length < 4) {
      setError('אנא הזן קוד PIN בעל 4 ספרות')
      return
    }

    setLoading(true)
    setError(null)

    // Check staff list & owner pin
    const staff = staffList.find((s) => s.pin === pin)
    const isOwner = pin === settings.owner_pin || pin === '1234'

    if (staff || isOwner) {
      const activeStaff = staff || {
        id: 'owner-root',
        name: 'בעלים / מנהל ראשי',
        role: 'OWNER' as const,
        pin: settings.owner_pin || '1234',
        avatarColor: '#10B981',
      }

      // Store authenticated session
      localStorage.setItem('aurapos_auth_staff', JSON.stringify(activeStaff))
      switchStaffWithPin(pin)

      if (activeStaff.role === 'CASHIER') {
        setSuccessMsg('קופאי מורשה. מעביר למסך קופה...')
        setTimeout(() => {
          router.replace('/pos')
        }, 600)
      } else {
        setSuccessMsg(`שלום ${activeStaff.name}! מעביר לדשבורד...`)
        setTimeout(() => {
          router.replace('/dashboard')
        }, 600)
      }
    } else {
      setError('קוד PIN שגוי. נסה שוב או פנה למנהל המערכת.')
      setLoading(false)
    }
  }

  // Handle Cloud Supabase Email / Password Login
  async function handleEmailLogin(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      if (!isSupabaseConfigured()) {
        throw new Error('חיבור Supabase טרם הוגדר בשרת. אנא התחבר באמצעות קוד PIN של הצוות.')
      }

      const { data, error: authError } = await supabase.auth.signInWithPassword({
        email,
        password,
      })

      if (authError) throw authError

      // Fetch profile role if available
      const { data: profile } = await supabase
        .from('profiles')
        .select('role, full_name')
        .eq('id', data.user.id)
        .single()

      const role = profile?.role || 'admin'
      const name = profile?.full_name || data.user.email?.split('@')[0] || 'מנהל'

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
      setError(err.message || 'ההתחברות נכשלה. בדוק את הדוא״ל והסיסמה.')
      setLoading(false)
    }
  }

  function handleKeypadClick(digit: string) {
    if (pin.length < 4) {
      const nextPin = pin + digit
      setPin(nextPin)
      if (nextPin.length === 4) {
        setTimeout(() => {
          // Auto submit when 4 digits reached
          const staff = staffList.find((s) => s.pin === nextPin)
          const isOwner = nextPin === settings.owner_pin || nextPin === '1234'
          if (staff || isOwner) {
            const activeStaff = staff || {
              id: 'owner-root',
              name: 'בעלים / מנהל ראשי',
              role: 'OWNER' as const,
              pin: settings.owner_pin || '1234',
              avatarColor: '#10B981',
            }
            localStorage.setItem('aurapos_auth_staff', JSON.stringify(activeStaff))
            switchStaffWithPin(nextPin)
            if (activeStaff.role === 'CASHIER') {
              router.replace('/pos')
            } else {
              router.replace('/dashboard')
            }
          } else {
            setError('קוד PIN שגוי')
            setPin('')
          }
        }, 150)
      }
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-sky-950 p-4 font-sans dir-rtl text-right">
      <div className="max-w-md w-full bg-slate-900/90 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-slate-800 text-slate-100">
        {/* Header Banner */}
        <div className="bg-gradient-to-r from-sky-600 to-indigo-600 p-7 text-white text-center relative overflow-hidden">
          <div className="absolute -right-10 -top-10 w-36 h-36 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
          <div className="inline-flex p-3 rounded-2xl bg-white/15 backdrop-blur-md mb-3 shadow-inner">
            <Store className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-black tracking-tight">{settings.store_name}</h1>
          <p className="text-sky-100 text-xs mt-1 font-medium">
            כניסה מאובטחת לפורטל הניהול והבקרה (Back Office)
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
            <span>קוד PIN צוות</span>
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
            <span>דוא״ל וסיסמה (ענן)</span>
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
                  הזן קוד PIN מנהל / בעלים (4 ספרות)
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
                <p className="font-bold text-slate-300">הרשאות כניסה ברירת מחדל:</p>
                <p>• בעלים / דשבורד מלא: <code className="text-sky-400 font-mono">1234</code></p>
                <p>• מנהלת (שרה לוי): <code className="text-sky-400 font-mono">2222</code></p>
                <p>• קופאי (אלכס): <code className="text-slate-400 font-mono">1111</code> (מופנה לקופה בלבד)</p>
              </div>
            </div>
          ) : (
            <form onSubmit={handleEmailLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1.5">כתובת אימייל</label>
                <div className="relative">
                  <span className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-500">
                    <Mail className="w-4 h-4" />
                  </span>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="admin@aurapos.example.com"
                    className="w-full pr-10 pl-4 py-3 rounded-xl bg-slate-950/80 border border-slate-700 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-300 mb-1.5">סיסמה</label>
                <div className="relative">
                  <span className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-500">
                    <Lock className="w-4 h-4" />
                  </span>
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pr-10 pl-4 py-3 rounded-xl bg-slate-950/80 border border-slate-700 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-4 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white font-bold rounded-xl shadow-lg shadow-sky-600/30 transition-all flex items-center justify-center gap-2 group mt-2"
              >
                <span>{loading ? 'מאמת נתונים...' : 'התחבר לדשבורד'}</span>
                <ArrowRight className="w-4 h-4 group-hover:-translate-x-1 transition-transform rotate-180" />
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
