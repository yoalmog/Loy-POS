import '../styles/globals.css'
import type { Metadata } from 'next'

export const dynamic = 'force-dynamic'

export const metadata: Metadata = {
  title: 'AuraPOS Online Back Office',
  description: 'Real-time online management dashboard and store configuration for AuraPOS',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="antialiased bg-slate-50 text-slate-900">{children}</body>
    </html>
  )
}
