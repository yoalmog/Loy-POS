'use client'

import React, { useEffect, useState } from 'react'
import {
  Users,
  Search,
  Plus,
  Award,
  Phone,
  Mail,
  DollarSign,
  X,
  CreditCard,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Customer } from '@/lib/types'

export default function CustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Partial<Customer> | null>(null)
  const [saving, setSaving] = useState(false)

  async function fetchCustomers() {
    setLoading(true)
    try {
      const { data } = await supabase.from('customers').select('*').order('total_spent', { ascending: false })
      if (data) setCustomers(data)
    } catch (err) {
      console.error('Error fetching customers:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchCustomers()
  }, [])

  function handleOpenCreateModal() {
    setEditingCustomer({
      name: '',
      phone: '',
      email: '',
      customer_code: `LOY${Math.floor(1000 + Math.random() * 9000)}`,
      points_balance: 0,
      total_visits: 0,
      total_spent: 0,
      note: '',
    })
    setShowModal(true)
  }

  async function handleSaveCustomer(e: React.FormEvent) {
    e.preventDefault()
    if (!editingCustomer || !editingCustomer.name) return
    setSaving(true)

    try {
      const payload = {
        name: editingCustomer.name,
        phone: editingCustomer.phone || '',
        email: editingCustomer.email || '',
        customer_code: editingCustomer.customer_code || `LOY${Math.floor(1000 + Math.random() * 9000)}`,
        points_balance: Number(editingCustomer.points_balance) || 0,
        total_visits: Number(editingCustomer.total_visits) || 0,
        total_spent: Number(editingCustomer.total_spent) || 0,
        note: editingCustomer.note || '',
        updated_at: new Date().toISOString(),
      }

      if (editingCustomer.id) {
        await supabase.from('customers').update(payload).eq('id', editingCustomer.id)
      } else {
        await supabase.from('customers').insert([payload])
      }

      setShowModal(false)
      fetchCustomers()
    } catch (err: any) {
      alert(`Save customer failed: ${err.message}`)
    } finally {
      setSaving(false)
    }
  }

  const filtered = customers.filter(
    (c) =>
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (c.phone && c.phone.includes(searchQuery)) ||
      (c.customer_code && c.customer_code.toLowerCase().includes(searchQuery.toLowerCase()))
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Customer Records & Loyalty Program</h2>
          <p className="text-sm text-slate-500 mt-1">Manage store VIPs, total spend, and loyalty points</p>
        </div>
        <button
          onClick={handleOpenCreateModal}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-sky-600 text-white font-semibold rounded-xl text-sm shadow-md shrink-0"
        >
          <Plus className="w-4 h-4" />
          Add Customer
        </button>
      </div>

      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="relative">
          <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by customer name, phone, or loyalty code..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[11px] font-bold tracking-wider">
            <tr>
              <th className="px-6 py-3.5">Customer Name</th>
              <th className="px-6 py-3.5">Loyalty Code</th>
              <th className="px-6 py-3.5">Phone & Email</th>
              <th className="px-6 py-3.5">Points Balance</th>
              <th className="px-6 py-3.5">Total Visits</th>
              <th className="px-6 py-3.5">Lifetime Spend</th>
              <th className="px-6 py-3.5 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-slate-700">
            {loading ? (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                  Loading customer database...
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                  No customer records found in database.
                </td>
              </tr>
            ) : (
              filtered.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-6 py-4 font-bold text-slate-900">{c.name}</td>
                  <td className="px-6 py-4">
                    <span className="px-2.5 py-1 rounded-md bg-indigo-50 text-indigo-700 text-xs font-mono font-bold">
                      {c.customer_code}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-500">
                    <div>{c.phone || 'No phone'}</div>
                    <div>{c.email || ''}</div>
                  </td>
                  <td className="px-6 py-4 font-extrabold text-amber-600 flex items-center gap-1">
                    <Award className="w-4 h-4 text-amber-500" />
                    <span>{(c.points_balance || 0).toFixed(1)} pts</span>
                  </td>
                  <td className="px-6 py-4 font-semibold text-slate-800">{c.total_visits || 0} visits</td>
                  <td className="px-6 py-4 font-extrabold text-slate-900">${(c.total_spent || 0).toFixed(2)}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => {
                        setEditingCustomer(c)
                        setShowModal(true)
                      }}
                      className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && editingCustomer && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-900">
                {editingCustomer.id ? 'Edit Customer' : 'Add New Customer'}
              </h3>
              <button onClick={() => setShowModal(false)} className="p-1 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveCustomer} className="py-4 space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={editingCustomer.name || ''}
                  onChange={(e) => setEditingCustomer({ ...editingCustomer, name: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Phone Number</label>
                  <input
                    type="text"
                    value={editingCustomer.phone || ''}
                    onChange={(e) => setEditingCustomer({ ...editingCustomer, phone: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Email</label>
                  <input
                    type="email"
                    value={editingCustomer.email || ''}
                    onChange={(e) => setEditingCustomer({ ...editingCustomer, email: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Loyalty Points Balance</label>
                  <input
                    type="number"
                    step="0.1"
                    value={editingCustomer.points_balance ?? 0}
                    onChange={(e) => setEditingCustomer({ ...editingCustomer, points_balance: parseFloat(e.target.value) })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 font-bold"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Notes</label>
                  <input
                    type="text"
                    value={editingCustomer.note || ''}
                    onChange={(e) => setEditingCustomer({ ...editingCustomer, note: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    placeholder="e.g. VIP Member"
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 bg-slate-100 text-slate-700 rounded-xl text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-xl text-xs font-semibold shadow-md"
                >
                  {saving ? 'Saving...' : 'Save Customer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
