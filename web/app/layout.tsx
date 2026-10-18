import '../styles/globals.css'
import type { Metadata } from 'next'
import { AccessibilityProvider } from '@/lib/context/AccessibilityContext'
import { ThemeProvider } from '@/lib/context/ThemeContext'
import { PosProvider } from '@/lib/context/PosContext'

export const dynamic = 'force-dynamic'

export const metadata: Metadata = {
  title: 'AuraPOS - קופה רושמת חכמה וניהול עסק מתקדם',
  description: 'מערכת קופה רושמת (POS) ו-Back Office מלאה עם סורק תפריטים מבוסס AI, הדפסה כפולה וסנכרון אופליין',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="he" dir="rtl">
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
