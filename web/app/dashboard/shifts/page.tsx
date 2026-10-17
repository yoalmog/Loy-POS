'use client'

import React, { useEffect, useState } from 'react'
import {
  Clock,
  Banknote,
  Calendar,
  User,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  LogOut,
  LogIn,
} from 'lucide-react'
import { supabase } from '@/lib/supabase/client'
import { Shift, Timecard } from '@/lib/types'

export default function ShiftsPage() {
  const [shifts, setShifts] = useState<Shift[]>([])
  const [timecards, setTimecards] = useState<Timecard[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<'shifts' | 'timecards'>('shifts')

  async function loadShiftsData() {
    setLoading(true)
    try {
      const { data: shiftData } = await supabase.from('shifts').select('*').order('opened_at', { ascending: false })
      if (shiftData) setShifts(shiftData)

      const { data: timecardData } = await supabase.from('timecards').select('*').order('clock_in_time', { ascending: false })
      if (timecardData) setTimecards(timecardData)
    } catch (err) {
      console.error('Error fetching shifts/timecards:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadShiftsData()
  }, [])

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Shifts & Employee Management</h2>
          <p className="text-sm text-slate-500 mt-1">Cash drawer register shifts, cash variance, and employee timecards</p>
        </div>
        <button
          onClick={loadShiftsData}
          className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-xl text-sm font-medium shrink-0"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh History
        </button>
      </div>

      {/* Tabs Header */}
      <div className="flex border-b border-slate-200 space-x-6 text-sm font-semibold">
        <button
          onClick={() => setActiveTab('shifts')}
          className={`pb-3 transition-colors ${
            activeTab === 'shifts' ? 'text-sky-600 border-b-2 border-sky-600' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          Drawer Shifts ({shifts.length})
        </button>
        <button
          onClick={() => setActiveTab('timecards')}
          className={`pb-3 transition-colors ${
            activeTab === 'timecards' ? 'text-sky-600 border-b-2 border-sky-600' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          Employee Timecards ({timecards.length})
        </button>
      </div>

      {activeTab === 'shifts' ? (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[11px] font-bold tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Shift ID</th>
                <th className="px-6 py-3.5">Cashier</th>
                <th className="px-6 py-3.5">Opened At</th>
                <th className="px-6 py-3.5">Starting Cash</th>
                <th className="px-6 py-3.5">Cash Payments</th>
                <th className="px-6 py-3.5">Card Payments</th>
                <th className="px-6 py-3.5">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                    Loading shift history...
                  </td>
                </tr>
              ) : shifts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                    No drawer shifts recorded in database.
                  </td>
                </tr>
              ) : (
                shifts.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 font-bold text-slate-900">Shift #{s.id}</td>
                    <td className="px-6 py-4 font-medium">{s.cashier_name}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">{new Date(s.opened_at).toLocaleString()}</td>
                    <td className="px-6 py-4">${(s.starting_cash || 0).toFixed(2)}</td>
                    <td className="px-6 py-4 text-emerald-700 font-semibold">${(s.cash_payments || 0).toFixed(2)}</td>
                    <td className="px-6 py-4 text-sky-700 font-semibold">${(s.card_payments || 0).toFixed(2)}</td>
                    <td className="px-6 py-4">
                      {s.is_open ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
                          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                          Shift Active
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-medium">
                          Closed
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[11px] font-bold tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Employee Name</th>
                <th className="px-6 py-3.5">Clock In Time</th>
                <th className="px-6 py-3.5">Clock Out Time</th>
                <th className="px-6 py-3.5">Total Shift Hours</th>
                <th className="px-6 py-3.5">Session Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-slate-400">
                    Loading employee timecards...
                  </td>
                </tr>
              ) : timecards.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-slate-400">
                    No timecard sessions recorded.
                  </td>
                </tr>
              ) : (
                timecards.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4 font-bold text-slate-900">{t.employee_name}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">{new Date(t.clock_in_time).toLocaleString()}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {t.clock_out_time ? new Date(t.clock_out_time).toLocaleString() : '—'}
                    </td>
                    <td className="px-6 py-4 font-extrabold text-slate-900">{t.total_hours ? `${t.total_hours} hrs` : 'In Progress'}</td>
                    <td className="px-6 py-4">
                      {t.is_clocked_in ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
                          Clocked In
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 text-xs font-medium">
                          Completed
                        </span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
