'use client'

import React, { useState } from 'react'
import {
  Clock,
  DollarSign,
  Users,
  Shield,
  ArrowDownLeft,
  ArrowUpRight,
  Lock,
  CheckCircle2,
  AlertCircle,
  Plus,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { Shift, StaffMember } from '@/lib/types'
import { idbPut, idbGetAll } from '@/lib/db/idb'

export default function ShiftsAndStaffPage() {
  const { currentShift, staffList, currentStaff, settings, refreshData } = usePos()

  const [activeTab, setActiveTab] = useState<'shift' | 'staff'>('shift')
  const [cashActionModal, setCashActionModal] = useState<'IN' | 'OUT' | null>(null)
  const [cashAmount, setCashAmount] = useState<number>(100)
  const [cashComment, setCashComment] = useState<string>('')

  // Close shift modal state
  const [isCloseShiftModalOpen, setIsCloseShiftModalOpen] = useState(false)
  const [actualCashCounted, setActualCashCounted] = useState<number>(0)

  // Start shift modal state
  const [isStartShiftModalOpen, setIsStartShiftModalOpen] = useState(false)
  const [startingCash, setStartingCash] = useState<number>(500)

  async function handleCashAction() {
    if (!currentShift || cashAmount <= 0) return

    const isPayIn = cashActionModal === 'IN'
    const updated: Shift = {
      ...currentShift,
      cash_paid_in: isPayIn
        ? currentShift.cash_paid_in + cashAmount
        : currentShift.cash_paid_in,
      cash_paid_out: !isPayIn
        ? currentShift.cash_paid_out + cashAmount
        : currentShift.cash_paid_out,
    }

    await idbPut('shifts', updated)
    await refreshData()
    setCashActionModal(null)
    setCashAmount(100)
    setCashComment('')
  }

  async function handleCloseShift() {
    if (!currentShift) return

    const updated: Shift = {
      ...currentShift,
      closed_at: new Date().toISOString(),
      actual_cash_counted: actualCashCounted,
      is_open: false,
    }

    await idbPut('shifts', updated)
    await refreshData()
    setIsCloseShiftModalOpen(false)
  }

  async function handleOpenShift() {
    const newShift: Shift = {
      id: Date.now(),
      opened_at: new Date().toISOString(),
      cashier_name: currentStaff.name,
      starting_cash: startingCash,
      cash_payments: 0,
      cash_refunds: 0,
      cash_paid_in: 0,
      cash_paid_out: 0,
      card_payments: 0,
      digital_payments: 0,
      is_open: true,
    }

    await idbPut('shifts', newShift)
    await refreshData()
    setIsStartShiftModalOpen(false)
  }

  const expectedCash = currentShift
    ? currentShift.starting_cash +
      currentShift.cash_payments -
      currentShift.cash_refunds +
      currentShift.cash_paid_in -
      currentShift.cash_paid_out
    : 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            ניהול משמרות, עובדים ומגירת מזומנים (Shifts & Staff)
          </h2>
          <p className="text-xs text-slate-500">
            פתיחת וסגירת משמרת, הפקדות ומשיכות מזומן (Pay In/Out), והרשאות PIN
          </p>
        </div>

        {/* Shift Action Button */}
        {currentShift?.is_open ? (
          <button
            onClick={() => {
              setActualCashCounted(expectedCash)
              setIsCloseShiftModalOpen(true)
            }}
            className="py-2.5 px-5 rounded-2xl bg-red-600 hover:bg-red-700 text-white font-extrabold text-xs shadow-md shadow-red-600/20 flex items-center gap-2"
          >
            <Lock className="w-4 h-4" />
            <span>סגור משמרת נוכחית</span>
          </button>
        ) : (
          <button
            onClick={() => setIsStartShiftModalOpen(true)}
            className="py-2.5 px-5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs shadow-md shadow-emerald-600/20 flex items-center gap-2"
          >
            <Clock className="w-4 h-4" />
            <span>פתח משמרת חדשה</span>
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-200">
        <button
          onClick={() => setActiveTab('shift')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${
            activeTab === 'shift'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Clock className="w-4 h-4" />
          <span>משמרת פעילה ומגירת מזומנים</span>
        </button>

        <button
          onClick={() => setActiveTab('staff')}
          className={`pb-3 px-4 font-black text-xs transition-colors flex items-center gap-2 border-b-2 ${
            activeTab === 'staff'
              ? 'border-sky-600 text-sky-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>עובדים ודרגות הרשאה (RBAC)</span>
        </button>
      </div>

      {/* TAB 1: ACTIVE SHIFT DETAILS */}
      {activeTab === 'shift' && (
        <div className="space-y-6">
          {currentShift ? (
            <div className="space-y-4">
              {/* Status Header */}
              <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="p-3 rounded-2xl bg-emerald-50 text-emerald-600">
                    <Clock className="w-6 h-6" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[10px] font-black uppercase">
                        משמרת פתוחה
                      </span>
                      <h3 className="text-base font-black text-slate-800">
                        אחראי משמרת: {currentShift.cashier_name}
                      </h3>
                    </div>
                    <p className="text-xs text-slate-400 mt-0.5">
                      נפתחה ב:{' '}
                      {new Date(currentShift.opened_at).toLocaleTimeString('he-IL', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                      , {new Date(currentShift.opened_at).toLocaleDateString('he-IL')}
                    </p>
                  </div>
                </div>

                {/* Cash Drawer In/Out Buttons */}
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setCashActionModal('IN')}
                    className="py-2 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center gap-1.5"
                  >
                    <ArrowDownLeft className="w-4 h-4 text-emerald-600" />
                    <span>הכנסת מזומן (Pay In)</span>
                  </button>

                  <button
                    onClick={() => setCashActionModal('OUT')}
                    className="py-2 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold flex items-center gap-1.5"
                  >
                    <ArrowUpRight className="w-4 h-4 text-red-600" />
                    <span>משיכת מזומן (Pay Out)</span>
                  </button>
                </div>
              </div>

              {/* Cash Ledger Breakdown Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
                  <span className="text-slate-400 text-xs font-bold uppercase block">
                    מזומן פתיחה
                  </span>
                  <div className="text-xl font-black text-slate-900 mt-1">
                    {settings.currency_symbol}
                    {currentShift.starting_cash.toFixed(2)}
                  </div>
                </div>

                <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
                  <span className="text-slate-400 text-xs font-bold uppercase block">
                    תקבולי מזומן בקופה
                  </span>
                  <div className="text-xl font-black text-emerald-600 mt-1">
                    +{settings.currency_symbol}
                    {currentShift.cash_payments.toFixed(2)}
                  </div>
                </div>

                <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
                  <span className="text-slate-400 text-xs font-bold uppercase block">
                    תשלומי אשראי ו-QR
                  </span>
                  <div className="text-xl font-black text-sky-600 mt-1">
                    {settings.currency_symbol}
                    {(currentShift.card_payments + currentShift.digital_payments).toFixed(2)}
                  </div>
                </div>

                <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
                  <span className="text-slate-400 text-xs font-bold uppercase block">
                    צפי מזומנים במגירה
                  </span>
                  <div className="text-xl font-black text-slate-900 mt-1">
                    {settings.currency_symbol}
                    {expectedCash.toFixed(2)}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-white p-12 rounded-3xl border border-slate-200 text-center space-y-3">
              <Clock className="w-12 h-12 text-slate-300 mx-auto" />
              <h3 className="text-base font-black text-slate-800">אין כרגע משמרת פתוחה</h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                לפני ביצוע עסקאות בקופה, מומלץ לפתוח משמרת ולהזין את סכום המזומן ההתחלתי.
              </p>
              <button
                onClick={() => setIsStartShiftModalOpen(true)}
                className="py-2.5 px-6 rounded-xl bg-emerald-600 text-white font-bold text-xs shadow-md"
              >
                פתח משמרת כעת
              </button>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: STAFF & RBAC PERMISSIONS */}
      {activeTab === 'staff' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {staffList.map((member) => (
            <div
              key={member.id}
              className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4"
            >
              <div className="flex items-center gap-3">
                <div
                  style={{ backgroundColor: member.avatarColor }}
                  className="w-12 h-12 rounded-2xl flex items-center justify-center text-white font-black text-lg shadow-md"
                >
                  {member.name.charAt(0)}
                </div>
                <div>
                  <h3 className="text-sm font-black text-slate-900">{member.name}</h3>
                  <span className="px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 text-[10px] font-bold">
                    דרגה: {member.role}
                  </span>
                </div>
              </div>

              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-xs space-y-1">
                <div className="flex justify-between">
                  <span className="text-slate-500">קוד PIN לכניסה:</span>
                  <span className="font-mono font-bold text-slate-800">•••• ({member.pin})</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">הרשאת שינוי מס:</span>
                  <span className="font-bold text-slate-800">
                    {member.role === 'OWNER' ? 'מורשה' : 'חסום'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">הרשאת פתיחת משמרת:</span>
                  <span className="font-bold text-slate-800">מורשה</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* PAY IN / PAY OUT MODAL */}
      {cashActionModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-900">
              {cashActionModal === 'IN' ? 'הכנסת מזומן למגירה (Pay In)' : 'משיכת מזומן מהמגירה (Pay Out)'}
            </h3>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  סכום ({settings.currency_symbol}):
                </label>
                <input
                  type="number"
                  value={cashAmount}
                  onChange={(e) => setCashAmount(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-base font-black bg-slate-50 text-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  סיבה / הערה:
                </label>
                <input
                  type="text"
                  placeholder="למשל: תוספת פריטה / תשלום לספק מזדמן"
                  value={cashComment}
                  onChange={(e) => setCashComment(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setCashActionModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                ביטול
              </button>
              <button
                type="button"
                onClick={handleCashAction}
                className="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-md shadow-sky-600/20"
              >
                בצע פעולה
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CLOSE SHIFT MODAL */}
      {isCloseShiftModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-900">סגירת משמרת וספירת קופה (Z-Report)</h3>
            <p className="text-xs text-slate-500">
              צפי מזומן מחושב במערכת:{' '}
              <strong className="text-slate-900">
                {settings.currency_symbol}
                {expectedCash.toFixed(2)}
              </strong>
            </p>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                סכום מזומן בפועל שנספר בקופה ({settings.currency_symbol}):
              </label>
              <input
                type="number"
                value={actualCashCounted}
                onChange={(e) => setActualCashCounted(Number(e.target.value))}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-base font-black bg-slate-50 text-slate-900"
              />
              <span
                className={`text-xs font-bold mt-1 block ${
                  actualCashCounted - expectedCash === 0
                    ? 'text-emerald-600'
                    : 'text-red-600'
                }`}
              >
                הפרש בקופה: {settings.currency_symbol}
                {(actualCashCounted - expectedCash).toFixed(2)}
              </span>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsCloseShiftModalOpen(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                ביטול
              </button>
              <button
                type="button"
                onClick={handleCloseShift}
                className="px-6 py-2 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-black shadow-md shadow-red-600/20"
              >
                אשר וסגור משמרת
              </button>
            </div>
          </div>
        </div>
      )}

      {/* START SHIFT MODAL */}
      {isStartShiftModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-900">פתיחת משמרת חדשה</h3>

            <div>
              <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                סכום מזומן התחלתי במגירה ({settings.currency_symbol}):
              </label>
              <input
                type="number"
                value={startingCash}
                onChange={(e) => setStartingCash(Number(e.target.value))}
                className="w-full p-2.5 rounded-xl border border-slate-200 text-base font-black bg-slate-50 text-slate-900"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsStartShiftModalOpen(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                ביטול
              </button>
              <button
                type="button"
                onClick={handleOpenShift}
                className="px-6 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-black shadow-md shadow-emerald-600/20"
              >
                פתח משמרת
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
