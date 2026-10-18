'use client'

import React, { useState } from 'react'
import {
  X,
  Banknote,
  CreditCard,
  QrCode,
  Smartphone,
  Building,
  Award,
  Layers,
  CheckCircle2,
  Printer,
  ChevronLeft,
} from 'lucide-react'
import { PaymentType, Receipt } from '@/lib/types'
import { usePos } from '@/lib/context/PosContext'
import QRCode from 'qrcode'

export default function CheckoutModal({
  isOpen,
  onClose,
  onCompleted,
}: {
  isOpen: boolean
  onClose: () => void
  onCompleted: (receipt: Receipt) => void
}) {
  const {
    grandTotal,
    subtotal,
    totalDiscount,
    taxAmount,
    settings,
    selectedCustomer,
    redeemedPoints,
    processPayment,
  } = usePos()

  const [paymentType, setPaymentType] = useState<PaymentType>('CASH')
  const [tenderedAmount, setTenderedAmount] = useState<number>(grandTotal)
  const [cardAuthRef, setCardAuthRef] = useState<string>('')
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>('')
  const [qrConfirmed, setQrConfirmed] = useState<boolean>(false)
  const [isProcessing, setIsProcessing] = useState<boolean>(false)

  // Generate QR code for Yape/Plin
  React.useEffect(() => {
    if (paymentType === 'YAPE' || paymentType === 'PLIN') {
      const qrPayload = `POS-PAY:${paymentType}|AMOUNT:${grandTotal}|PHONE:${settings.qr_payment_yape_phone}|STORE:${settings.store_name}`
      QRCode.toDataURL(qrPayload, { width: 220, margin: 1 })
        .then((url) => setQrCodeDataUrl(url))
        .catch((err) => console.error('QR code error:', err))
    }
  }, [paymentType, grandTotal, settings])

  // Update tendered default when modal opens
  React.useEffect(() => {
    setTenderedAmount(grandTotal)
  }, [grandTotal, isOpen])

  if (!isOpen) return null

  const FAST_CASH = [
    grandTotal, // Exact
    Math.ceil(grandTotal / 10) * 10,
    Math.ceil(grandTotal / 50) * 50,
    100,
    200,
    500,
  ].filter((val, idx, self) => val >= grandTotal && self.indexOf(val) === idx)

  const changeDue = Math.max(0, tenderedAmount - grandTotal)

  async function handleFinalizePayment() {
    setIsProcessing(true)

    let details = ''
    if (paymentType === 'CASH') {
      details = JSON.stringify({ tendered: tenderedAmount, changeDue })
    } else if (paymentType === 'CARD') {
      details = JSON.stringify({ authRef: cardAuthRef || `AUTH-${Date.now().toString().slice(-4)}` })
    } else if (paymentType === 'YAPE' || paymentType === 'PLIN') {
      details = JSON.stringify({
        provider: paymentType,
        confirmed: true,
        accountName: settings.qr_payment_yape_name,
        phone: settings.qr_payment_yape_phone,
      })
    }

    try {
      const res = await processPayment(paymentType, tenderedAmount, details)
      setIsProcessing(false)
      if (res.success) {
        onCompleted(res.receipt)
        onClose()
      }
    } catch (err) {
      setIsProcessing(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-2xl max-h-[92vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-sky-600 shadow-md">
              <Banknote className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="text-lg font-black">סיום עסקה ותשלום (Checkout)</h3>
              <p className="text-xs text-slate-400">
                בחר אמצעי תשלום, קבל מזומן או סרוק קוד QR דיגיטלי
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-50">
          {/* Order Summary Card */}
          <div className="bg-gradient-to-r from-sky-600 to-indigo-700 text-white p-5 rounded-3xl shadow-lg flex items-center justify-between">
            <div>
              <span className="text-xs font-bold text-sky-200 uppercase tracking-wider">
                סה״כ לתשלום
              </span>
              <div className="text-3xl font-black mt-0.5">
                {settings.currency_symbol}
                {grandTotal.toFixed(2)}
              </div>
              <div className="text-xs text-sky-100 flex items-center gap-3 mt-1 font-medium">
                <span>סכום ביניים: {settings.currency_symbol}{subtotal.toFixed(2)}</span>
                {totalDiscount > 0 && <span>• הנחה: -{settings.currency_symbol}{totalDiscount.toFixed(2)}</span>}
                <span>• מע״מ ({settings.tax_rate}%): {settings.currency_symbol}{taxAmount.toFixed(2)}</span>
              </div>
            </div>

            {selectedCustomer && (
              <div className="text-left bg-white/10 backdrop-blur-md px-3.5 py-2 rounded-2xl border border-white/20">
                <span className="text-[10px] text-sky-200 uppercase font-bold block">לקוח מועדון</span>
                <span className="text-xs font-black">{selectedCustomer.name}</span>
                {redeemedPoints > 0 && (
                  <span className="text-[11px] text-emerald-300 font-bold block">
                    מומשו {redeemedPoints} נק׳
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Payment Method Selector Grid */}
          <div>
            <label className="block text-xs font-bold uppercase text-slate-500 mb-2.5">
              בחר אמצעי תשלום (Tender Type)
            </label>
            <div className="grid grid-cols-3 md:grid-cols-6 gap-2">
              <button
                type="button"
                onClick={() => setPaymentType('CASH')}
                className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-1.5 transition-all ${
                  paymentType === 'CASH'
                    ? 'border-sky-600 bg-sky-50 text-sky-900 shadow-sm ring-2 ring-sky-600/20'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <Banknote className="w-5 h-5 text-emerald-600" />
                <span>מזומן</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentType('CARD')}
                className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-1.5 transition-all ${
                  paymentType === 'CARD'
                    ? 'border-sky-600 bg-sky-50 text-sky-900 shadow-sm ring-2 ring-sky-600/20'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <CreditCard className="w-5 h-5 text-sky-600" />
                <span>כרטיס אשראי</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentType('YAPE')}
                className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-1.5 transition-all ${
                  paymentType === 'YAPE'
                    ? 'border-purple-600 bg-purple-50 text-purple-900 shadow-sm ring-2 ring-purple-600/20'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <QrCode className="w-5 h-5 text-purple-600" />
                <span>Yape QR</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentType('PLIN')}
                className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-1.5 transition-all ${
                  paymentType === 'PLIN'
                    ? 'border-blue-600 bg-blue-50 text-blue-900 shadow-sm ring-2 ring-blue-600/20'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <QrCode className="w-5 h-5 text-blue-600" />
                <span>Plin QR</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentType('DIGITAL_WALLET')}
                className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-1.5 transition-all ${
                  paymentType === 'DIGITAL_WALLET'
                    ? 'border-sky-600 bg-sky-50 text-sky-900 shadow-sm ring-2 ring-sky-600/20'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <Smartphone className="w-5 h-5 text-amber-600" />
                <span>Apple/Google</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentType('BANK_TRANSFER')}
                className={`p-3 rounded-2xl border font-bold text-xs flex flex-col items-center gap-1.5 transition-all ${
                  paymentType === 'BANK_TRANSFER'
                    ? 'border-sky-600 bg-sky-50 text-sky-900 shadow-sm ring-2 ring-sky-600/20'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <Building className="w-5 h-5 text-indigo-600" />
                <span>העברה</span>
              </button>
            </div>
          </div>

          {/* METHOD-SPECIFIC PANELS */}

          {/* 1. CASH PANEL */}
          {paymentType === 'CASH' && (
            <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase text-slate-600">
                  סכום שהתקבל במזומן:
                </span>
                <div className="flex items-center gap-2">
                  <span className="text-xl font-black text-slate-800">
                    {settings.currency_symbol}
                  </span>
                  <input
                    type="number"
                    step="0.5"
                    value={tenderedAmount}
                    onChange={(e) => setTenderedAmount(Number(e.target.value))}
                    className="w-32 p-2 rounded-xl border border-slate-300 text-xl font-black text-slate-900 text-center focus:ring-2 focus:ring-sky-500"
                  />
                </div>
              </div>

              {/* Fast Cash Tender Buttons */}
              <div className="grid grid-cols-3 md:grid-cols-6 gap-2">
                {FAST_CASH.map((amount) => (
                  <button
                    key={amount}
                    type="button"
                    onClick={() => setTenderedAmount(amount)}
                    className={`py-2.5 rounded-xl border font-black text-xs transition-all ${
                      tenderedAmount === amount
                        ? 'border-emerald-600 bg-emerald-600 text-white shadow-md'
                        : 'border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-800'
                    }`}
                  >
                    {amount === grandTotal ? 'בדיוק' : `${settings.currency_symbol}${amount}`}
                  </button>
                ))}
              </div>

              {/* Live Change Calculator Banner */}
              <div
                className={`p-4 rounded-2xl flex items-center justify-between border ${
                  changeDue >= 0
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                    : 'bg-red-50 border-red-200 text-red-900'
                }`}
              >
                <div>
                  <span className="text-xs font-bold uppercase">
                    {changeDue > 0 ? 'עודף להחזיר ללקוח:' : 'חסר לתשלום:'}
                  </span>
                  <div className="text-2xl font-black">
                    {settings.currency_symbol}
                    {Math.abs(changeDue).toFixed(2)}
                  </div>
                </div>
                {changeDue > 0 && (
                  <div className="text-xs font-bold px-3 py-1.5 rounded-xl bg-emerald-600 text-white shadow-sm">
                    מגירת המזומנים תיפתח אוטומטית
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 2. DIGITAL QR PAYMENTS (YAPE & PLIN) */}
          {(paymentType === 'YAPE' || paymentType === 'PLIN') && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-center gap-6">
              <div className="p-3 bg-white border border-slate-200 rounded-2xl shadow-inner shrink-0">
                {qrCodeDataUrl ? (
                  <img
                    src={qrCodeDataUrl}
                    alt={`${paymentType} QR Code`}
                    className="w-44 h-44 rounded-xl"
                  />
                ) : (
                  <div className="w-44 h-44 bg-slate-100 flex items-center justify-center text-slate-400">
                    <QrCode className="w-12 h-12 animate-pulse" />
                  </div>
                )}
              </div>

              <div className="space-y-3 flex-1 text-right">
                <span className="px-3 py-1 rounded-full text-xs font-extrabold uppercase bg-purple-100 text-purple-800">
                  תשלום דיגיטלי {paymentType} QR
                </span>
                <h4 className="text-lg font-black text-slate-800">סרוק מהנייד לביצוע תשלום</h4>
                <div className="text-xs text-slate-600 space-y-1">
                  <div>
                    בעל החשבון: <strong>{settings.qr_payment_yape_name}</strong>
                  </div>
                  <div>
                    מספר טלפון: <strong>{settings.qr_payment_yape_phone}</strong>
                  </div>
                  <div>
                    סכום לתשלום: <strong>{settings.currency_symbol}{grandTotal.toFixed(2)}</strong>
                  </div>
                </div>

                <div className="pt-2">
                  <label className="flex items-center gap-2 cursor-pointer bg-slate-50 p-3 rounded-xl border border-slate-200">
                    <input
                      type="checkbox"
                      checked={qrConfirmed}
                      onChange={(e) => setQrConfirmed(e.target.checked)}
                      className="w-4 h-4 accent-purple-600 rounded"
                    />
                    <span className="text-xs font-bold text-slate-700">
                      הלקוח ביצע את ההעברה והציג אישור תשלום בנייד
                    </span>
                  </label>
                </div>
              </div>
            </div>
          )}

          {/* 3. CREDIT CARD PANEL */}
          {paymentType === 'CARD' && (
            <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-sm space-y-4">
              <h4 className="text-xs font-bold uppercase text-slate-600">
                פרטי עסקה באשראי (EMV / Contactless)
              </h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-500 mb-1">מספר אסמכתא / אישור:</label>
                  <input
                    type="text"
                    placeholder="למשל: 084920"
                    value={cardAuthRef}
                    onChange={(e) => setCardAuthRef(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-500 mb-1">סוג כרטיס:</label>
                  <select className="w-full p-2.5 rounded-xl border border-slate-200 text-xs bg-slate-50">
                    <option>כרטיס מקומי / שובר</option>
                    <option>Visa</option>
                    <option>MasterCard</option>
                    <option>American Express</option>
                  </select>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 bg-white border-t border-slate-200 flex items-center justify-between">
          <button
            onClick={onClose}
            className="py-2.5 px-4 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 text-xs font-bold"
          >
            חזור לעגלה
          </button>

          <button
            onClick={handleFinalizePayment}
            disabled={
              isProcessing ||
              (paymentType === 'CASH' && tenderedAmount < grandTotal) ||
              ((paymentType === 'YAPE' || paymentType === 'PLIN') && !qrConfirmed)
            }
            className="py-3 px-8 rounded-2xl bg-sky-600 hover:bg-sky-700 text-white text-sm font-black shadow-lg shadow-sky-600/25 flex items-center gap-2 disabled:opacity-50 transition-transform active:scale-95"
          >
            <CheckCircle2 className="w-5 h-5" />
            <span>
              {isProcessing ? 'מעבד עסקה...' : `אשר תשלום (${settings.currency_symbol}${grandTotal.toFixed(2)})`}
            </span>
          </button>
        </div>
      </div>
    </div>
  )
}
