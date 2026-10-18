'use client'

import React, { createContext, useContext, useState, useEffect } from 'react'
import { ColorTheme, SupportedLanguage } from '../types'
import { translations, TranslationDictionary, isRtlLanguage } from '../i18n'

interface ThemeContextType {
  mode: 'light' | 'dark' | 'system'
  setMode: (mode: 'light' | 'dark' | 'system') => void
  resolvedTheme: 'light' | 'dark'
  palette: ColorTheme
  setPalette: (palette: ColorTheme) => void
  customHex: string
  setCustomHex: (hex: string) => void
  language: SupportedLanguage
  setLanguage: (lang: SupportedLanguage) => void
  t: TranslationDictionary
  isRtl: boolean
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setMode] = useState<'light' | 'dark' | 'system'>('light')
  const [palette, setPalette] = useState<ColorTheme>('blue')
  const [customHex, setCustomHex] = useState<string>('#0288D1')
  const [language, setLanguage] = useState<SupportedLanguage>('he')
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light')

  // Load preferences
  useEffect(() => {
    if (typeof window !== 'undefined') {
      try {
        const savedMode = localStorage.getItem('aurapos_mode') as 'light' | 'dark' | 'system'
        if (savedMode) setMode(savedMode)

        const savedPalette = localStorage.getItem('aurapos_palette') as ColorTheme
        if (savedPalette) setPalette(savedPalette)

        const savedHex = localStorage.getItem('aurapos_custom_hex')
        if (savedHex) setCustomHex(savedHex)

        const savedLang = localStorage.getItem('aurapos_language') as SupportedLanguage
        if (savedLang) setLanguage(savedLang)
      } catch {}
    }
  }, [])

  // Resolve light/dark mode
  useEffect(() => {
    if (typeof window === 'undefined') return

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const updateTheme = () => {
      let isDark = false
      if (mode === 'dark') isDark = true
      else if (mode === 'light') isDark = false
      else isDark = mediaQuery.matches

      setResolvedTheme(isDark ? 'dark' : 'light')
      if (isDark) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    }

    updateTheme()
    mediaQuery.addEventListener('change', updateTheme)
    return () => mediaQuery.removeEventListener('change', updateTheme)
  }, [mode])

  // Apply palette & RTL attributes
  useEffect(() => {
    if (typeof window === 'undefined') return

    localStorage.setItem('aurapos_mode', mode)
    localStorage.setItem('aurapos_palette', palette)
    localStorage.setItem('aurapos_custom_hex', customHex)
    localStorage.setItem('aurapos_language', language)

    const root = document.documentElement
    const rtl = isRtlLanguage(language)
    root.setAttribute('dir', rtl ? 'rtl' : 'ltr')
    root.setAttribute('lang', language)

    // Palette class
    root.classList.remove('theme-blue', 'theme-emerald', 'theme-purple', 'theme-amber', 'theme-red', 'theme-midnight')
    root.classList.add(`theme-${palette}`)
  }, [mode, palette, customHex, language])

  const t = translations[language] || translations.he
  const isRtl = isRtlLanguage(language)

  return (
    <ThemeContext.Provider
      value={{
        mode,
        setMode,
        resolvedTheme,
        palette,
        setPalette,
        customHex,
        setCustomHex,
        language,
        setLanguage,
        t,
        isRtl,
      }}
    >
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider')
  }
  return context
}
