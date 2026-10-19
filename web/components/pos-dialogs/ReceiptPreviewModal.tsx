'use client'

import React, { useState, useEffect } from 'react'
import { X, Printer, CheckCircle2, QrCode, Download, Share2 } from 'lucide-react'
import { Receipt, getLocalizedName } from '@/lib/types'
import { usePos } from '@/lib/context/PosContext'
import { useTheme } from '@/lib/context/ThemeContext'
import { getTranslation } from '@/lib/i18n'
import { printerManager } from '@/lib/hardware/printerManager'
import QRCode from 'qrcode'
import confetti from 'canvas-confetti'

export default function ReceiptPreviewModal({
  receipt,
  isOpen,
  onClose,
}: {
  receipt: Receipt | null
  isOpen: boolean
  onClose: () => void
}) {
  const { settings } = usePos()
  const { language } = useTheme()
  const t = getTranslation(language)
  const [qrCodeUrl, setQrCodeUrl] = useState<string>('')

  useEffect(() => {
    if (isOpen && receipt) {
      // Fire celebration confetti
      try {
        confetti({
          particleCount: 60,
          spread: 70,
          origin: { y: 0.6 },
        })
      } catch {}

      // Generate receipt QR code
      const qrData = `https://receipts.aurapos.example.com/view/${receipt.id}?num=${receipt.receipt_number}`
      QRCode.toDataURL(qrData, { width: 140, margin: 1 })
        .then((url) => setQrCodeUrl(url))
        .catch(() => {})
    }
  }, [isOpen, receipt])

  if (!isOpen || !receipt) return null

  function handlePrint() {
    printerManager.printBrowserHtmlReceipt(receipt!, settings)
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-md max-h-[92vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-emerald-600 text-white flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5" />
            <h3 className="text-base font-black">{t.completed}</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Realistic Thermal Receipt Body */}
        <div className="flex-1 overflow-y-auto p-6 bg-slate-100 flex justify-center">
          <div className="w-full max-w-[340px] bg-white p-5 rounded-2xl shadow-md border border-slate-200 text-slate-900 font-mono text-xs leading-relaxed">
            {/* Store Header */}
            <div className="text-center space-y-1 mb-3">
              <h2 className="text-base font-black font-sans">{settings.store_name}</h2>
              <p className="text-[11px] text-slate-500">{settings.address}</p>
              <p className="text-[11px] text-slate-500">{t.phone}: {settings.phone}</p>
              {settings.receipt_header && (
                <p className="text-[11px] font-sans font-bold text-slate-700 mt-1">
                  {settings.receipt_header}
                </p>
              )}
            </div>

            <div className="border-b-2 border-dashed border-slate-300 my-2.5"></div>

            {/* Receipt Info */}
            <div className="space-y-1 text-[11px]">
              <div className="flex justify-between">
                <span>{t.receiptPreview}:</span>
                <span className="font-bold">{receipt.receipt_number}</span>
              </div>
              <div className="flex justify-between">
                <span>{t.dateTime}:</span>
                <span>
                  {new Date(receipt.created_at).toLocaleDateString()} •{' '}
                  {new Date(receipt.created_at).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>
              <div className="flex justify-between">
                <span>{receipt.cashier_name}</span>
                <span>{receipt.dining_option}</span>
              </div>
              {receipt.customer_name && (
                <div className="flex justify-between font-bold text-sky-700">
                  <span>{t.customer}:</span>
                  <span>{receipt.customer_name}</span>
                </div>
              )}
            </div>

            <div className="border-b border-dashed border-slate-300 my-2.5"></div>

            {/* Item Rows */}
            <div className="space-y-2">
              <div className="flex justify-between font-bold text-slate-500 text-[10px]">
                <span>{t.productName}</span>
                <span>{t.total}</span>
              </div>

              {receipt.items.map((item, idx) => (
                <div key={idx} className="space-y-0.5">
                  <div className="flex justify-between">
                    <span>
                      {getLocalizedName(item, language)}{' '}
                      <span className="text-[10px] text-slate-500 numeric-preserve">
                        ({item.quantity}x {settings.currency_symbol}{item.unit_price.toFixed(2)})
                      </span>
                    </span>
                    <span className="font-bold numeric-preserve price-display">
                      {settings.currency_symbol}
                      {item.line_total.toFixed(2)}
                    </span>
                  </div>
                  {item.selected_variant && (
                    <div className="text-[10px] text-slate-500 rtl:pr-2 ltr:pl-2">
                      • {getLocalizedName(item.selected_variant, language)}
                    </div>
                  )}
                  {item.selected_modifiers && item.selected_modifiers.length > 0 && (
                    <div className="text-[10px] text-slate-500 rtl:pr-2 ltr:pl-2">
                      {item.selected_modifiers.map((m) => `+ ${getLocalizedName(m, language)}`).join(', ')}
                    </div>
                  )}
                  {item.comment && (
                    <div className="text-[10px] text-amber-700 font-sans rtl:pr-2 ltr:pl-2">
                      * {item.comment}
                    </div>
                  )}
                </div>
              ))}
            </div>

            <div className="border-b border-dashed border-slate-300 my-2.5"></div>

            {/* Totals */}
            <div className="space-y-1">
              <div className="flex justify-between">
                <span>{t.subtotal}:</span>
                <span>
                  {settings.currency_symbol}
                  {receipt.subtotal.toFixed(2)}
                </span>
              </div>
              {receipt.discount_amount > 0 && (
                <div className="flex justify-between text-red-600 font-bold">
                  <span>{t.discount}:</span>
                  <span>
                    -{settings.currency_symbol}
                    {receipt.discount_amount.toFixed(2)}
                  </span>
                </div>
              )}
              <div className="flex justify-between text-slate-500">
                <span>{t.tax} ({settings.tax_rate}%):</span>
                <span>
                  {settings.currency_symbol}
                  {receipt.tax_amount.toFixed(2)}
                </span>
              </div>
              <div className="border-b-2 border-slate-900 my-1.5"></div>
              <div className="flex justify-between text-sm font-black">
                <span>{t.total}:</span>
                <span>
                  {settings.currency_symbol}
                  {receipt.total_amount.toFixed(2)}
                </span>
              </div>
            </div>

            <div className="border-b border-dashed border-slate-300 my-2.5"></div>

            {/* Payment method */}
            <div className="flex justify-between text-[11px]">
              <span>{t.paymentDistribution}:</span>
              <span className="font-bold">{receipt.payment_type}</span>
            </div>
            {receipt.points_earned ? (
              <div className="flex justify-between text-[11px] text-emerald-700 font-bold">
                <span>{t.loyaltyPoints}:</span>
                <span>+{receipt.points_earned} {t.points}</span>
              </div>
            ) : null}

            {/* Receipt QR Code */}
            {qrCodeUrl && (
              <div className="my-4 text-center flex flex-col items-center">
                <img src={qrCodeUrl} alt="Digital Receipt QR" className="w-24 h-24 mx-auto" />
                <span className="text-[9px] text-slate-400 mt-1">
                  QR • {t.receiptPreview}
                </span>
              </div>
            )}

            {/* Footer */}
            <div className="text-center font-sans text-[10px] text-slate-500 space-y-1 mt-3">
              {settings.receipt_footer && <p>{settings.receipt_footer}</p>}
              {settings.return_policy && <p className="text-[9px]">{settings.return_policy}</p>}
              <p className="font-bold text-slate-800">*** {t.appName} ***</p>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-white border-t border-slate-200 flex items-center justify-between">
          <button
            onClick={onClose}
            className="py-2.5 px-5 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 text-xs font-bold"
          >
            {t.closeSale}
          </button>

          <button
            onClick={handlePrint}
            className="py-2.5 px-6 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-black shadow-lg shadow-sky-600/25 flex items-center gap-2"
          >
            <Printer className="w-4 h-4" />
            <span>{t.printReceipt}</span>
          </button>
        </div>
      </div>
    </div>
  )
}
