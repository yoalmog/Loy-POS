'use client'

import React, { createContext, useContext, useState, useEffect } from 'react'
import { 
  TextScale, 
  ContrastMode, 
  ColorblindMode, 
  TouchTargetSize 
} from '../types'
import { playSound } from '../util/sound'
import { nativeVibrate } from '../hardware/androidBridge'

interface AccessibilityContextType {
  textScale: TextScale
  setTextScale: (scale: TextScale) => void
  contrastMode: ContrastMode
  setContrastMode: (mode: ContrastMode) => void
  colorblindMode: ColorblindMode
  setColorblindMode: (mode: ColorblindMode) => void
  touchSize: TouchTargetSize
  setTouchSize: (size: TouchTargetSize) => void
  soundEnabled: boolean
  setSoundEnabled: (enabled: boolean) => void
  hapticEnabled: boolean
  setHapticEnabled: (enabled: boolean) => void
  simplifiedPos: boolean
  setSimplifiedPos: (enabled: boolean) => void
  announce: (message: string) => void
  triggerHaptic: (ms?: number) => void
  triggerSound: (type: 'beep' | 'success' | 'error' | 'click') => void
}

const AccessibilityContext = createContext<AccessibilityContextType | undefined>(undefined)

export function AccessibilityProvider({ children }: { children: React.ReactNode }) {
  const [textScale, setTextScale] = useState<TextScale>('1.0')
  const [contrastMode, setContrastMode] = useState<ContrastMode>('normal')
  const [colorblindMode, setColorblindMode] = useState<ColorblindMode>('none')
  const [touchSize, setTouchSize] = useState<TouchTargetSize>('standard')
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true)
  const [hapticEnabled, setHapticEnabled] = useState<boolean>(true)
  const [simplifiedPos, setSimplifiedPos] = useState<boolean>(false)
  const [announcement, setAnnouncement] = useState<string>('')

  // Load saved preferences
  useEffect(() => {
    if (typeof window !== 'undefined') {
      try {
        const savedScale = localStorage.getItem('aurapos_text_scale') as TextScale
        if (savedScale) setTextScale(savedScale)

        const savedContrast = localStorage.getItem('aurapos_contrast') as ContrastMode
        if (savedContrast) setContrastMode(savedContrast)

        const savedColorblind = localStorage.getItem('aurapos_colorblind') as ColorblindMode
        if (savedColorblind) setColorblindMode(savedColorblind)

        const savedTouch = localStorage.getItem('aurapos_touch_size') as TouchTargetSize
        if (savedTouch) setTouchSize(savedTouch)

        const savedSound = localStorage.getItem('aurapos_sound')
        if (savedSound !== null) setSoundEnabled(savedSound === 'true')

        const savedHaptic = localStorage.getItem('aurapos_haptic')
        if (savedHaptic !== null) setHapticEnabled(savedHaptic === 'true')

        const savedSimplified = localStorage.getItem('aurapos_simplified')
        if (savedSimplified !== null) setSimplifiedPos(savedSimplified === 'true')
      } catch {}
    }
  }, [])

  // Persist changes and apply classes to root
  useEffect(() => {
    if (typeof window === 'undefined') return

    localStorage.setItem('aurapos_text_scale', textScale)
    localStorage.setItem('aurapos_contrast', contrastMode)
    localStorage.setItem('aurapos_colorblind', colorblindMode)
    localStorage.setItem('aurapos_touch_size', touchSize)
    localStorage.setItem('aurapos_sound', String(soundEnabled))
    localStorage.setItem('aurapos_haptic', String(hapticEnabled))
    localStorage.setItem('aurapos_simplified', String(simplifiedPos))

    // Apply scale to document root
    const root = document.documentElement
    root.style.fontSize = textScale === '1.50' ? '19px' : textScale === '1.30' ? '18px' : textScale === '1.15' ? '17px' : '16px'

    // Remove existing filter classes
    root.classList.remove('contrast-high', 'contrast-ultra', 'cb-protanopia', 'cb-deuteranopia', 'cb-tritanopia', 'cb-monochrome')

    if (contrastMode === 'high') root.classList.add('contrast-high')
    if (contrastMode === 'ultra') root.classList.add('contrast-ultra')

    if (colorblindMode !== 'none') {
      root.classList.add(`cb-${colorblindMode}`)
    }
  }, [textScale, contrastMode, colorblindMode, touchSize, soundEnabled, hapticEnabled, simplifiedPos])

  function announce(msg: string) {
    setAnnouncement(msg)
    // Clear after 3 seconds
    setTimeout(() => {
      setAnnouncement('')
    }, 3000)
  }

  function triggerHaptic(ms: number = 40) {
    if (hapticEnabled) {
      nativeVibrate(ms)
    }
  }

  function triggerSound(type: 'beep' | 'success' | 'error' | 'click') {
    if (soundEnabled) {
      playSound(type)
    }
  }

  return (
    <AccessibilityContext.Provider
      value={{
        textScale,
        setTextScale,
        contrastMode,
        setContrastMode,
        colorblindMode,
        setColorblindMode,
        touchSize,
        setTouchSize,
        soundEnabled,
        setSoundEnabled,
        hapticEnabled,
        setHapticEnabled,
        simplifiedPos,
        setSimplifiedPos,
        announce,
        triggerHaptic,
        triggerSound,
      }}
    >
      {/* SVG Filters for Colorblindness */}
      <svg className="sr-only" aria-hidden="true">
        <filter id="protanopia-filter">
          <feColorMatrix
            type="matrix"
            values="0.567, 0.433, 0, 0, 0
                    0.558, 0.442, 0, 0, 0
                    0, 0.242, 0.758, 0, 0
                    0, 0, 0, 1, 0"
          />
        </filter>
        <filter id="deuteranopia-filter">
          <feColorMatrix
            type="matrix"
            values="0.625, 0.375, 0, 0, 0
                    0.70, 0.30, 0, 0, 0
                    0, 0.30, 0.70, 0, 0
                    0, 0, 0, 1, 0"
          />
        </filter>
        <filter id="tritanopia-filter">
          <feColorMatrix
            type="matrix"
            values="0.95, 0.05, 0, 0, 0
                    0, 0.433, 0.567, 0, 0
                    0, 0.475, 0.525, 0, 0
                    0, 0, 0, 1, 0"
          />
        </filter>
        <filter id="monochrome-filter">
          <feColorMatrix
            type="matrix"
            values="0.299, 0.587, 0.114, 0, 0
                    0.299, 0.587, 0.114, 0, 0
                    0.299, 0.587, 0.114, 0, 0
                    0, 0, 0, 1, 0"
          />
        </filter>
      </svg>

      {/* Screen Reader polite live announcer */}
      <div 
        role="status" 
        aria-live="polite" 
        aria-atomic="true" 
        className="sr-only"
      >
        {announcement}
      </div>

      {children}
    </AccessibilityContext.Provider>
  )
}

export function useAccessibility() {
  const context = useContext(AccessibilityContext)
  if (!context) {
    throw new Error('useAccessibility must be used within an AccessibilityProvider')
  }
  return context
}
