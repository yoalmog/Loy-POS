import '../styles/globals.css'
import type { Metadata } from 'next'
import { AccessibilityProvider } from '@/lib/context/AccessibilityContext'
import { ThemeProvider } from '@/lib/context/ThemeContext'
import { PosProvider } from '@/lib/context/PosContext'

export const dynamic = 'force-dynamic'

export const metadata: Metadata = {
  title: 'AuraPOS - Sistema POS Inteligente y Gestión Empresarial',
  description: 'Sistema completo de punto de venta (POS) y Back Office con escaneo con IA, soporte multilingüe y sincronización offline',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="es" dir="ltr">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Assistant:wght@300;400;600;700;800&family=Inter:wght@400;500;600;700;800&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="antialiased bg-slate-50 text-slate-900 font-sans">
        <AccessibilityProvider>
          <ThemeProvider>
            <PosProvider>
              {children}
            </PosProvider>
          </ThemeProvider>
        </AccessibilityProvider>
      </body>
    </html>
  )
}
