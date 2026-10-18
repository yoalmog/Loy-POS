'use client'

import React, { useState } from 'react'
import {
  Users,
  UserPlus,
  Search,
  Award,
  Phone,
  Mail,
  MapPin,
  Star,
  Plus,
  Minus,
  Edit2,
  DollarSign,
  TrendingUp,
} from 'lucide-react'
import { usePos } from '@/lib/context/PosContext'
import { Customer } from '@/lib/types'
import { idbPut } from '@/lib/db/idb'

export default function CustomersCrmPage() {
  const { customers, settings, addCustomer, refreshData } = usePos()

  const [search, setSearch] = useState('')
  const [isAddModalOpen, setIsAddModalOpen] = useState(false)
  const [adjustingCustomer, setAdjustingCustomer] = useState<Customer | null>(null)
  const [pointsDelta, setPointsDelta] = useState<number>(50)
  const [pointsReason, setPointsReason] = useState<string>('בונוס הצטרפות / פיצוי שירות')

  // New Customer Form State
  const [newName, setNewName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newEmail, setNewEmail] = useState('')
  const [newAddress, setNewAddress] = useState('')
  const [newNote, setNewNote] = useState('')

  const filtered = customers.filter(
    (c) =>
      c.name.toLowerCase().includes(search.toLowerCase()) ||
      (c.phone && c.phone.includes(search)) ||
      (c.email && c.email.toLowerCase().includes(search.toLowerCase()))
  )

  async function handleCreateCustomer(e: React.FormEvent) {
    e.preventDefault()
    if (!newName) return

    const newCust: Customer = {
      id: Date.now(),
      name: newName,
      phone: newPhone,
      email: newEmail,
      address: newAddress,
      note: newNote,
      customer_code: `CUST-${Date.now().toString().slice(-4)}`,
      points_balance: 50,
      total_visits: 1,
      total_spent: 0,
    }

    await addCustomer(newCust)
    setIsAddModalOpen(false)
    setNewName('')
    setNewPhone('')
    setNewEmail('')
    setNewAddress('')
    setNewNote('')
  }

  async function handleAdjustPoints() {
    if (!adjustingCustomer) return

    const updated: Customer = {
      ...adjustingCustomer,
      points_balance: Math.max(0, adjustingCustomer.points_balance + pointsDelta),
    }

    await idbPut('customers', updated)
    await refreshData()
    setAdjustingCustomer(null)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-slate-900">
            ניהול לקוחות ומועדון לקוחות (CRM & Loyalty)
          </h2>
          <p className="text-xs text-slate-500">
            כרטיסי לקוח, מעקב רכישות, ניהול נקודות מועדון והתאמות ידניות
          </p>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          className="py-2.5 px-5 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white font-extrabold text-xs shadow-md shadow-sky-600/20 flex items-center gap-1.5"
        >
          <UserPlus className="w-4 h-4" />
          <span>לקוח חדש</span>
        </button>
      </div>

      {/* Search Bar */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200 shadow-sm flex items-center gap-3">
        <Search className="w-4 h-4 text-slate-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="חפש לקוח לפי שם, טלפון או דוא״ל..."
          className="flex-1 text-xs bg-transparent focus:outline-none text-slate-800"
        />
      </div>

      {/* Customer Directory Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((cust) => {
          const avgTicket = cust.total_visits > 0 ? cust.total_spent / cust.total_visits : 0

          return (
            <div
              key={cust.id}
              className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm flex flex-col justify-between space-y-4"
            >
              <div>
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-sm font-black text-slate-900">{cust.name}</h3>
                    <span className="text-[10px] font-mono font-bold text-slate-400">
                      {cust.customer_code}
                    </span>
                  </div>

                  {/* Loyalty Points Badge */}
                  <div className="flex items-center gap-1.5 px-3 py-1 rounded-2xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-black">
                    <Star className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                    <span>{cust.points_balance} נק׳</span>
                  </div>
                </div>

                {/* Contact details */}
                <div className="mt-3 space-y-1 text-xs text-slate-600">
                  {cust.phone && (
                    <div className="flex items-center gap-2">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      <span>{cust.phone}</span>
                    </div>
                  )}
                  {cust.email && (
                    <div className="flex items-center gap-2">
                      <Mail className="w-3.5 h-3.5 text-slate-400" />
                      <span>{cust.email}</span>
                    </div>
                  )}
                  {cust.address && (
                    <div className="flex items-center gap-2">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      <span>{cust.address}</span>
                    </div>
                  )}
                </div>

                {cust.note && (
                  <p className="mt-2.5 p-2 rounded-xl bg-slate-50 text-[11px] text-slate-600 border border-slate-100 italic">
                    ״{cust.note}״
                  </p>
                )}
              </div>

              {/* Purchase Metrics Summary */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <div>
                  <span className="text-slate-400 text-[10px] block">רכישות מצטברות</span>
                  <span className="font-black text-slate-900">
                    {settings.currency_symbol}
                    {cust.total_spent.toFixed(2)}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 text-[10px] block">ביקורים</span>
                  <span className="font-black text-slate-900">{cust.total_visits}</span>
                </div>
                <div>
                  <span className="text-slate-400 text-[10px] block">ממוצע להזמנה</span>
                  <span className="font-black text-slate-900">
                    {settings.currency_symbol}
                    {avgTicket.toFixed(0)}
                  </span>
                </div>
              </div>

              {/* Points Adjustment Button */}
              <button
                onClick={() => setAdjustingCustomer(cust)}
                className="w-full py-2 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold border border-slate-200 flex items-center justify-center gap-1.5"
              >
                <Award className="w-3.5 h-3.5 text-amber-500" />
                <span>עדכן נקודות מועדון (+/-)</span>
              </button>
            </div>
          )
        })}
      </div>

      {/* ADD CUSTOMER MODAL */}
      {isAddModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-900">הוספת לקוח חדש למועדון</h3>

            <form onSubmit={handleCreateCustomer} className="space-y-3">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  שם מלא *
                </label>
                <input
                  type="text"
                  required
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900 font-bold"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  מספר טלפון
                </label>
                <input
                  type="tel"
                  value={newPhone}
                  onChange={(e) => setNewPhone(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  דואר אלקטרוני
                </label>
                <input
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  כתובת
                </label>
                <input
                  type="text"
                  value={newAddress}
                  onChange={(e) => setNewAddress(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  הערות פנימיות
                </label>
                <textarea
                  value={newNote}
                  onChange={(e) => setNewNote(e.target.value)}
                  placeholder="העדפות הושבה, אלרגיות וכו׳"
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
                  rows={2}
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
                >
                  ביטול
                </button>
                <button
                  type="submit"
                  className="px-6 py-2 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-md shadow-sky-600/20"
                >
                  שמור לקוח
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ADJUST POINTS MODAL */}
      {adjustingCustomer && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-black text-slate-900">
              עדכון נקודות מועדון: {adjustingCustomer.name}
            </h3>
            <p className="text-xs text-slate-500">
              יתרה נוכחית: <strong>{adjustingCustomer.points_balance}</strong> נקודות
            </p>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  הוספה או הפחתת נקודות (+/-):
                </label>
                <input
                  type="number"
                  value={pointsDelta}
                  onChange={(e) => setPointsDelta(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-base font-black text-slate-900 bg-slate-50"
                />
                <span className="text-xs text-amber-600 font-bold mt-1 block">
                  יתרה לאחר העדכון: {Math.max(0, adjustingCustomer.points_balance + pointsDelta)} נק׳
                </span>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-500 mb-1">
                  סיבת ההתאמה:
                </label>
                <input
                  type="text"
                  value={pointsReason}
                  onChange={(e) => setPointsReason(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 text-slate-900"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setAdjustingCustomer(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
              >
                ביטול
              </button>
              <button
                type="button"
                onClick={handleAdjustPoints}
                className="px-6 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-xs font-black shadow-md shadow-amber-600/20"
              >
                עדכן יתרת נקודות
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
