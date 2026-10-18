'use client'

import React, { useState } from 'react'
import { X, Search, UserPlus, Award, Phone, Mail, Check, Star } from 'lucide-react'
import { Customer } from '@/lib/types'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'

export default function CustomerSelectModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean
  onClose: () => void
}) {
  const {
    customers,
    selectedCustomer,
    selectCustomer,
    redeemedPoints,
    applyLoyaltyPoints,
    settings,
    addCustomer,
  } = usePos()

  const { t, isRtl } = useTheme()

  const [search, setSearch] = useState('')
  const [showAddForm, setShowAddForm] = useState(false)
  const [newName, setNewName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newEmail, setNewEmail] = useState('')

  if (!isOpen) return null

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
      points_balance: 50, // 50 welcome points
      total_visits: 1,
      total_spent: 0,
      customer_code: `CUST-${Date.now().toString().slice(-4)}`,
    }

    await addCustomer(newCust)
    selectCustomer(newCust)
    setShowAddForm(false)
    setNewName('')
    setNewPhone('')
    setNewEmail('')
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden text-start">
        {/* Header */}
        <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-sky-500/20 text-sky-400">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black">{t.customer} & {t.loyaltyPoints}</h3>
              <p className="text-xs text-slate-400">{t.points} & {t.redeemPoints}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search & Actions */}
        <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center gap-2">
          <div className="relative flex-1">
            <Search className={`w-4 h-4 text-slate-400 absolute top-3 ${isRtl ? 'right-3' : 'left-3'}`} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t.searchPlaceholder}
              className={`w-full py-2.5 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500 bg-white ${
                isRtl ? 'pr-9 pl-4' : 'pl-9 pr-4'
              }`}
            />
          </div>
          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className="py-2.5 px-3.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm"
          >
            <UserPlus className="w-4 h-4" />
            <span>{t.add}</span>
          </button>
        </div>

        {/* Add Customer Form */}
        {showAddForm && (
          <form onSubmit={handleCreateCustomer} className="p-4 bg-sky-50 border-b border-sky-100 space-y-3">
            <h4 className="text-xs font-black text-sky-900">{t.addCustomer}</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              <input
                type="text"
                placeholder={`${t.customerName} *`}
                required
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                className="p-2 rounded-xl border border-slate-200 text-xs bg-white text-slate-800"
              />
              <input
                type="tel"
                placeholder={t.phone}
                value={newPhone}
                onChange={(e) => setNewPhone(e.target.value)}
                className="p-2 rounded-xl border border-slate-200 text-xs bg-white text-slate-800"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="px-3 py-1.5 rounded-xl text-xs text-slate-600 hover:bg-slate-200"
              >
                {t.cancel}
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 rounded-xl bg-sky-600 text-white text-xs font-bold"
              >
                {t.saveCustomer}
              </button>
            </div>
          </form>
        )}

        {/* Customer List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-slate-100">
          {selectedCustomer && (
            <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 mb-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Star className="w-4 h-4 text-amber-500 fill-amber-500" />
                  <span className="text-xs font-bold text-amber-900">
                    {t.customer}: <strong>{selectedCustomer.name}</strong>
                  </span>
                </div>
                <button
                  onClick={() => selectCustomer(null)}
                  className="text-xs font-bold text-red-600 hover:underline"
                >
                  {t.delete}
                </button>
              </div>

              {/* Loyalty points redemption control */}
              <div className="bg-white p-2.5 rounded-xl border border-amber-100 flex items-center justify-between text-xs">
                <div>
                  <span className="text-slate-500">{t.pointsBalance}: </span>
                  <span className="font-extrabold text-amber-600">
                    {selectedCustomer.points_balance} {t.points}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <label className="text-slate-600 font-semibold">{t.redeemPoints}:</label>
                  <input
                    type="number"
                    min="0"
                    max={selectedCustomer.points_balance}
                    step="10"
                    value={redeemedPoints}
                    onChange={(e) => applyLoyaltyPoints(Number(e.target.value))}
                    className="w-20 p-1 border border-slate-300 rounded-lg text-center font-bold text-slate-800"
                  />
                  <span className="text-[11px] text-emerald-600 font-bold">
                    ({t.discount}: {settings.currency_symbol}
                    {((redeemedPoints / 100) * settings.loyalty_redemption_rate).toFixed(2)})
                  </span>
                </div>
              </div>
            </div>
          )}

          {filtered.length === 0 ? (
            <div className="text-center py-8 text-slate-400 text-xs">{t.no} {t.customers}</div>
          ) : (
            filtered.map((cust) => {
              const isSelected = selectedCustomer?.id === cust.id
              return (
                <div
                  key={cust.id}
                  onClick={() => {
                    selectCustomer(cust)
                    onClose()
                  }}
                  className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                    isSelected
                      ? 'border-sky-600 bg-sky-50 shadow-sm'
                      : 'border-slate-200 bg-white hover:bg-slate-50'
                  }`}
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-xs font-black text-slate-800">{cust.name}</h4>
                      {isSelected && (
                        <span className="px-2 py-0.5 rounded-full bg-sky-600 text-white text-[10px] font-bold">
                          ✓
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-3 text-[11px] text-slate-500 mt-1">
                      {cust.phone && (
                        <span className="flex items-center gap-1">
                          <Phone className="w-3 h-3 text-slate-400" />
                          {cust.phone}
                        </span>
                      )}
                      <span className="font-semibold text-amber-600">
                        ★ {cust.points_balance} {t.points}
                      </span>
                    </div>
                  </div>

                  <div className="text-end text-[11px] text-slate-400">
                    <div>{t.totalVisits}: {cust.total_visits}</div>
                    <div>
                      {t.totalSpent}: {settings.currency_symbol}
                      {cust.total_spent.toFixed(0)}
                    </div>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>
    </div>
  )
}
