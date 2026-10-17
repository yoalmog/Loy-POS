'use client'

import React, { useEffect, useState } from 'react'
import {
  Store,
  Save,
  Database,
  CheckCircle2,
  Users,
  ShieldCheck,
  Building2,
  Percent,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { StoreSettings } from '@/lib/types'

export default function SettingsPage() {
  const [settings, setSettings] = useState<StoreSettings>({
    id: 'main',
    store_name: 'AuraPOS Bistro & Grill',
    address: '124 Market Street, San Francisco, CA',
    phone: '+1 (555) 019-2831',
    email: 'contact@aurapos.example.com',
    currency: 'USD',
    currency_symbol: '$',
    tax_rate: 8.25,
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')

  async function loadStoreSettings() {
    setLoading(true)
    try {
      const { data } = await supabase.from('store_settings').select('*').eq('id', 'main').maybeSingle()
      if (data) {
        setSettings(data)
      }
    } catch (err) {
      console.error('Error fetching settings:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadStoreSettings()
  }, [])

  async function handleSaveSettings(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setMessage('')
    try {
      const payload = {
        id: 'main',
        store_name: settings.store_name,
        address: settings.address,
        phone: settings.phone,
        email: settings.email,
        currency: settings.currency,
        currency_symbol: settings.currency_symbol,
        tax_rate: Number(settings.tax_rate) || 8.25,
        updated_at: new Date().toISOString(),
      }

      const { error } = await supabase.from('store_settings').upsert(payload)
      if (error) throw error

      setMessage('Store settings updated and synchronized with POS.')
    } catch (err: any) {
      setMessage(`Save failed: ${err.message}`)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Store Settings & Configuration</h2>
        <p className="text-sm text-slate-500 mt-1">Configure business parameters, tax rates, and database credentials</p>
      </div>

      {message && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          <span>{message}</span>
        </div>
      )}

      {/* Store Info Form */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <h3 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
          <Building2 className="w-5 h-5 text-sky-600" />
          Store Identity & Taxes
        </h3>

        <form onSubmit={handleSaveSettings} className="space-y-4 text-sm">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Store Name</label>
            <input
              type="text"
              required
              value={settings.store_name}
              onChange={(e) => setSettings({ ...settings, store_name: e.target.value })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Phone Number</label>
              <input
                type="text"
                value={settings.phone}
                onChange={(e) => setSettings({ ...settings, phone: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Store Email</label>
              <input
                type="email"
                value={settings.email}
                onChange={(e) => setSettings({ ...settings, email: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Physical Address</label>
            <input
              type="text"
              value={settings.address}
              onChange={(e) => setSettings({ ...settings, address: e.target.value })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Currency Code</label>
              <input
                type="text"
                value={settings.currency}
                onChange={(e) => setSettings({ ...settings, currency: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-600 mb-1">Sales Tax Rate (%)</label>
              <input
                type="number"
                step="0.01"
                value={settings.tax_rate}
                onChange={(e) => setSettings({ ...settings, tax_rate: parseFloat(e.target.value) })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 font-bold"
              />
            </div>
          </div>

          <div className="pt-4 flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2.5 bg-sky-600 hover:bg-sky-700 text-white rounded-xl font-semibold text-sm shadow-md flex items-center gap-2 transition-all"
            >
              <Save className="w-4 h-4" />
              {saving ? 'Saving...' : 'Save Configuration'}
            </button>
          </div>
        </form>
      </div>

      {/* Backend & Database Status Card */}
      <div className="bg-slate-900 text-white p-6 rounded-2xl border border-slate-800 shadow-xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sky-500 text-white flex items-center justify-center font-bold shadow-lg shadow-sky-500/30">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base">Hosted Backend Status</h3>
              <p className="text-xs text-slate-400">PostgreSQL / Supabase REST API Service</p>
            </div>
          </div>
          <span className="px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-bold">
            Active
          </span>
        </div>

        <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 text-xs font-mono text-slate-300 space-y-2">
          <div><span className="text-slate-500">Database Engine:</span> PostgreSQL 15.1</div>
          <div><span className="text-slate-500">Migration Script:</span> /supabase/migrations/20261016_initial_schema.sql</div>
          <div><span className="text-slate-500">Row Level Security:</span> Enabled with Anon Public Key</div>
        </div>
      </div>
    </div>
  )
}
