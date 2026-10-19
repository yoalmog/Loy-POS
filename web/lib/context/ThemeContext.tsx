'use client'

import React, { createContext, useContext, useState, useEffect } from 'react'
import { ColorTheme, SupportedLanguage, CatalogBgStyle, CardAppearance } from '../types'
import { translations, TranslationDictionary, isRtlLanguage, getTranslation } from '../i18n'

export interface AppearanceState {
  mode: 'light' | 'dark' | 'system'
  palette: ColorTheme
  customHex: string
  useCustomHex: boolean
  catalogBg: CatalogBgStyle
  cardAppearance: CardAppearance
}

export const DEFAULT_APPEARANCE: AppearanceState = {
  mode: 'light',
  palette: 'blue',
  customHex: '#0288D1',
  useCustomHex: false,
  catalogBg: 'default',
  cardAppearance: 'default',
}

interface ThemeContextType {
  mode: 'light' | 'dark' | 'system'
  setMode: (mode: 'light' | 'dark' | 'system') => void
  resolvedTheme: 'light' | 'dark'
  palette: ColorTheme
  setPalette: (palette: ColorTheme) => void
  customHex: string
  setCustomHex: (hex: string) => void
  useCustomHex: boolean
  setUseCustomHex: (val: boolean) => void
  catalogBg: CatalogBgStyle
  setCatalogBg: (bg: CatalogBgStyle) => void
  cardAppearance: CardAppearance
  setCardAppearance: (appearance: CardAppearance) => void
  language: SupportedLanguage
  setLanguage: (lang: SupportedLanguage) => void
  t: TranslationDictionary
  isRtl: boolean
  preview: Partial<AppearanceState> | null
  setPreview: (preview: Partial<AppearanceState> | null) => void
  restoreDefaults: () => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  // Spanish ('es') is the mandatory default language for all new installations
  const [language, setLanguageState] = useState<SupportedLanguage>('es')
  const [mode, setModeState] = useState<'light' | 'dark' | 'system'>('light')
  const [palette, setPaletteState] = useState<ColorTheme>('blue')
  const [customHex, setCustomHexState] = useState<string>('#0288D1')
  const [useCustomHex, setUseCustomHexState] = useState<boolean>(false)
  const [catalogBg, setCatalogBgState] = useState<CatalogBgStyle>('default')
  const [cardAppearance, setCardAppearanceState] = useState<CardAppearance>('default')
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light')
  const [preview, setPreview] = useState<Partial<AppearanceState> | null>(null)

  // 1. Initial Load: Precedence: explicit preference in localStorage -> Spanish / default
  useEffect(() => {
    if (typeof window === 'undefined') return
    try {
      const savedLang = localStorage.getItem('aurapos_language') as SupportedLanguage
      if (savedLang && (translations[savedLang] || savedLang === 'es' || savedLang === 'en' || savedLang === 'he')) {
        setLanguageState(savedLang)
      } else {
        // Save Spanish as initial default
        localStorage.setItem('aurapos_language', 'es')
      }

      const savedMode = localStorage.getItem('aurapos_mode') as 'light' | 'dark' | 'system'
      if (savedMode) setModeState(savedMode)

      const savedPalette = localStorage.getItem('aurapos_palette') as ColorTheme
      if (savedPalette) setPaletteState(savedPalette)

      const savedHex = localStorage.getItem('aurapos_custom_hex')
      if (savedHex) setCustomHexState(savedHex)

      const savedUseHex = localStorage.getItem('aurapos_use_custom_hex') === 'true'
      setUseCustomHexState(savedUseHex)

      const savedCatalogBg = localStorage.getItem('aurapos_catalog_bg') as CatalogBgStyle
      if (savedCatalogBg) setCatalogBgState(savedCatalogBg)

      const savedCardAppearance = localStorage.getItem('aurapos_card_appearance') as CardAppearance
      if (savedCardAppearance) setCardAppearanceState(savedCardAppearance)
    } catch (e) {
      console.warn('Could not access localStorage for theme settings:', e)
    }
  }, [])

  // Public setters that persist to localStorage
  const setLanguage = (lang: SupportedLanguage) => {
    setLanguageState(lang)
    try {
      localStorage.setItem('aurapos_language', lang)
    } catch {}
  }

  const setMode = (m: 'light' | 'dark' | 'system') => {
    setModeState(m)
    try {
      localStorage.setItem('aurapos_mode', m)
    } catch {}
  }

  const setPalette = (p: ColorTheme) => {
    setPaletteState(p)
    try {
      localStorage.setItem('aurapos_palette', p)
    } catch {}
  }

  const setCustomHex = (hex: string) => {
    setCustomHexState(hex)
    try {
      localStorage.setItem('aurapos_custom_hex', hex)
    } catch {}
  }

  const setUseCustomHex = (val: boolean) => {
    setUseCustomHexState(val)
    try {
      localStorage.setItem('aurapos_use_custom_hex', String(val))
    } catch {}
  }

  const setCatalogBg = (bg: CatalogBgStyle) => {
    setCatalogBgState(bg)
    try {
      localStorage.setItem('aurapos_catalog_bg', bg)
    } catch {}
  }

  const setCardAppearance = (app: CardAppearance) => {
    setCardAppearanceState(app)
    try {
      localStorage.setItem('aurapos_card_appearance', app)
    } catch {}
  }

  const restoreDefaults = () => {
    setMode('light')
    setPalette('blue')
    setCustomHex('#0288D1')
    setUseCustomHex(false)
    setCatalogBg('default')
    setCardAppearance('default')
    setPreview(null)
  }

  // Active or previewed appearance values
  const effectiveMode = preview?.mode || mode
  const effectivePalette = preview?.palette || palette
  const effectiveCustomHex = preview?.customHex || customHex
  const effectiveUseCustomHex = preview?.useCustomHex !== undefined ? preview.useCustomHex : useCustomHex
  const effectiveCatalogBg = preview?.catalogBg || catalogBg
  const effectiveCardAppearance = preview?.cardAppearance || cardAppearance

  // Resolve light/dark mode
  useEffect(() => {
    if (typeof window === 'undefined') return

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const updateTheme = () => {
      let isDark = false
      if (effectiveMode === 'dark') isDark = true
      else if (effectiveMode === 'light') isDark = false
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
  }, [effectiveMode])

  // Apply palette, custom hex, catalog bg, card appearance, & RTL attributes
  useEffect(() => {
    if (typeof window === 'undefined') return

    const root = document.documentElement
    const rtl = isRtlLanguage(language)
    root.setAttribute('dir', rtl ? 'rtl' : 'ltr')
    root.setAttribute('lang', language)

    // Palette class
    root.classList.remove('theme-blue', 'theme-emerald', 'theme-purple', 'theme-amber', 'theme-red', 'theme-midnight')
    root.classList.add(`theme-${effectivePalette}`)

    // Catalog background class
    root.classList.remove('catalog-bg-default', 'catalog-bg-subtle', 'catalog-bg-slate', 'catalog-bg-pure')
    root.classList.add(`catalog-bg-${effectiveCatalogBg}`)

    // Card appearance class
    root.classList.remove('card-appearance-default', 'card-appearance-compact', 'card-appearance-bordered', 'card-appearance-elevated')
    root.classList.add(`card-appearance-${effectiveCardAppearance}`)

    // Custom primary color variable if enabled
    if (effectiveUseCustomHex && effectiveCustomHex) {
      root.style.setProperty('--color-primary', effectiveCustomHex)
    } else {
      root.style.removeProperty('--color-primary')
    }
  }, [language, effectivePalette, effectiveCatalogBg, effectiveCardAppearance, effectiveUseCustomHex, effectiveCustomHex])

  const t = getTranslation(language)
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
        useCustomHex,
        setUseCustomHex,
        catalogBg,
        setCatalogBg,
        cardAppearance,
        setCardAppearance,
        language,
        setLanguage,
        t,
        isRtl,
        preview,
        setPreview,
        restoreDefaults,
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
