'use client'

import React, { useState } from 'react'
import { ShieldAlert, X, Check, Lock } from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'

export default function OwnerAuthModal({
  isOpen,
  title = 'אישור בעל עסק נדרש',
  description = 'פעולה זו רגישה ודורשת הזנת קוד PIN של בעל העסק (ברירת מחדל: 1234)',
  onApproved,
  onClose,
}: {
  isOpen: boolean
  title?: string
  description?: string
  onApproved: () => void
  onClose: () => void
}) {
  const { verifyOwnerPin } = usePos()
  const [pin, setPin] = useState('')
  const [error, setError] = useState(false)

  if (!isOpen) return null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (verifyOwnerPin(pin)) {
      setPin('')
      setError(false)
      onApproved()
      onClose()
    } else {
      setError(true)
      setPin('')
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-sm p-6 text-center space-y-4">
        <div className="p-3.5 rounded-2xl bg-amber-50 text-amber-600 border border-amber-200 w-fit mx-auto">
          <ShieldAlert className="w-8 h-8" />
        </div>

        <div>
          <h3 className="text-base font-black text-slate-800">{title}</h3>
          <p className="text-xs text-slate-500 mt-1">{description}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="password"
            maxLength={4}
            autoFocus
            value={pin}
            onChange={(e) => {
              setPin(e.target.value)
              setError(false)
            }}
            placeholder="קוד PIN (4 ספרות)"
            className="w-full text-center text-2xl font-mono tracking-widest p-3 rounded-2xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-amber-500 bg-slate-50 font-black"
          />

          {error && (
            <p className="text-xs font-bold text-red-600">קוד PIN שגוי. נסה שוב.</p>
          )}

          <div className="flex gap-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 text-xs font-bold hover:bg-slate-50"
            >
              ביטול
            </button>
            <button
              type="submit"
              className="flex-1 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-black shadow-md shadow-amber-600/20"
            >
              אישור
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
