'use client'

import React, { useState } from 'react'
import { Lock, Delete, UserCheck, ShieldAlert, KeyRound } from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'

export default function PinLockModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose?: () => void
}) {
  const { currentStaff, staffList, switchStaffWithPin, isLocked, setIsLocked } = usePos()
  const [pin, setPin] = useState<string>('')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  if (!isOpen && !isLocked) return null

  function handleNumberClick(num: string) {
    if (pin.length < 4) {
      const newPin = pin + num
      setPin(newPin)
      setErrorMsg(null)
      if (newPin.length === 4) {
        // Evaluate immediately
        const res = switchStaffWithPin(newPin)
        if (res.success) {
          setPin('')
          setIsLocked(false)
          if (onClose) onClose()
        } else {
          setErrorMsg(res.message)
          setPin('')
        }
      }
    }
  }

  function handleDelete() {
    setPin((prev) => prev.slice(0, -1))
    setErrorMsg(null)
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/90 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-sm flex flex-col items-center p-6 text-center space-y-5">
        <div className="p-4 rounded-3xl bg-slate-900 text-sky-400 shadow-xl">
          <Lock className="w-8 h-8" />
        </div>

        <div>
          <h3 className="text-xl font-black text-slate-800">נעילת קופה ומעבר קופאים</h3>
          <p className="text-xs text-slate-500 mt-1">
            הקש קוד PIN בן 4 ספרות לכניסה למערכת
          </p>
          <div className="text-[11px] text-sky-600 font-semibold mt-1">
            עובד נוכחי: {currentStaff.name} ({currentStaff.role})
          </div>
        </div>

        {/* PIN Indicators */}
        <div className="flex items-center gap-3 py-2">
          {[0, 1, 2, 3].map((idx) => (
            <div
              key={idx}
              className={`w-4 h-4 rounded-full border-2 transition-all ${
                pin.length > idx
                  ? 'bg-sky-600 border-sky-600 scale-110'
                  : 'border-slate-300 bg-slate-100'
              }`}
            />
          ))}
        </div>

        {errorMsg && (
          <div className="p-2 px-4 rounded-xl bg-red-50 text-red-600 text-xs font-bold border border-red-200">
            {errorMsg}
          </div>
        )}

        {/* 10-Digit Numeric Keypad */}
        <div className="grid grid-cols-3 gap-2.5 w-full max-w-[260px]">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
            <button
              key={digit}
              type="button"
              onClick={() => handleNumberClick(digit)}
              className="h-14 rounded-2xl bg-slate-100 hover:bg-slate-200 active:scale-95 text-slate-800 font-black text-xl transition-all shadow-sm"
            >
              {digit}
            </button>
          ))}
          <button
            type="button"
            onClick={() => setPin('')}
            className="h-14 rounded-2xl bg-slate-50 hover:bg-slate-100 text-slate-400 text-xs font-bold"
          >
            נקה
          </button>
          <button
            type="button"
            onClick={() => handleNumberClick('0')}
            className="h-14 rounded-2xl bg-slate-100 hover:bg-slate-200 active:scale-95 text-slate-800 font-black text-xl transition-all shadow-sm"
          >
            0
          </button>
          <button
            type="button"
            onClick={handleDelete}
            className="h-14 rounded-2xl bg-slate-50 hover:bg-slate-100 text-slate-600 flex items-center justify-center"
          >
            <Delete className="w-5 h-5" />
          </button>
        </div>

        {/* Quick Staff Demo Legend */}
        <div className="text-[11px] text-slate-400 pt-1">
          קודים לדוגמה: קופאי (1111) • מנהלת (2222) • בעלים (1234)
        </div>
      </div>
    </div>
  )
}
