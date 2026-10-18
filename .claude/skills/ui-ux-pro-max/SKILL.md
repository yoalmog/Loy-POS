---
name: ui-ux-pro-max
description: "UI/UX design intelligence for web and mobile. Includes 50+ styles, 161 color palettes, 57 font pairings, 161 product types, 99 UX guidelines across 10 stacks (React, Next.js, Vue, Svelte, SwiftUI, React Native, Flutter, Tailwind, shadcn/ui, HTML/CSS)."
---

# UI/UX Pro Max - Design Intelligence

## When to Apply

Use for UI structure, visual design, interaction patterns, or UX quality control:
- Designing new pages (Landing, Dashboard, Admin, SaaS, Mobile App)
- Creating/refactoring UI components (buttons, modals, forms, tables, charts)
- Choosing color schemes, typography systems, spacing, or layout
- Reviewing UI code for UX, accessibility, or visual consistency
- Implementing navigation, animations, or responsive behaviour
- Making product-level design decisions (style, hierarchy, brand)

Skip for: pure backend logic, API/database design, infrastructure, DevOps.

## Priority Rules (1 = highest)

| # | Category | Impact | Key Checks | Anti-Patterns |
|---|----------|--------|------------|---------------|
| 1 | Accessibility | CRITICAL | Contrast 4.5:1, Alt text, Keyboard nav, Aria-labels | No focus rings, icon-only buttons without labels |
| 2 | Touch & Interaction | CRITICAL | Min 44×44px targets, 8px+ gap, loading feedback | Hover-only actions, 0ms state changes |
| 3 | Performance | HIGH | WebP/AVIF, Lazy load, CLS < 0.1 | Layout thrashing, cumulative layout shift |
| 4 | Style Selection | HIGH | Match product type, SVG icons, consistent | Mixing flat & skeuomorphic, emoji as icons |
| 5 | Layout & Responsive | HIGH | Mobile-first, no horizontal scroll, viewport meta | Fixed px widths, disable zoom |
| 6 | Typography & Color | MEDIUM | Base 16px, line-height 1.5, semantic tokens | Text <12px, gray-on-gray, raw hex in components |
| 7 | Animation | MEDIUM | 150–300ms, transform/opacity only, motion has meaning | Animate width/height, no reduced-motion support |
| 8 | Forms & Feedback | MEDIUM | Visible labels, error near field, progressive disclosure | Placeholder-only label, errors only at top |
| 9 | Navigation | HIGH | Predictable back, bottom nav ≤5, deep links | Overloaded nav, broken back |
| 10 | Charts & Data | LOW | Legends, tooltips, accessible colors | Color-only meaning |

## Quick Reference

### Accessibility (CRITICAL)
- Contrast minimum 4.5:1 (large text 3:1)
- Visible focus rings on all interactive elements (2–4px)
- Descriptive alt text for meaningful images
- aria-label for icon-only buttons
- Tab order matches visual order
- Support system text scaling (Dynamic Type / MD)
- Respect prefers-reduced-motion

### Touch & Interaction (CRITICAL)
- Min 44×44pt (Apple) / 48×48dp (Material) touch targets
- Min 8px gap between touch targets
- Disable button during async ops; show spinner
- cursor-pointer on clickable elements
- Platform-standard gestures (swipe-back, pinch-zoom)
- Haptic feedback for confirmations

### Style Recommendations
- **Developer/CLI tools**: Dark theme (#0F172A bg, #22C55E accent), Space Grotesk + JetBrains Mono
- **SaaS/Productivity**: Clean glassmorphism, slate palette, Inter or DM Sans
- **E-commerce**: Warm tones, card-based layout, clear CTAs, Nunito
- **Finance/Fintech**: Trust blue (#2563EB), minimalism, tabular numbers, DM Sans
- **Health/Wellness**: Sage greens, clean whitespace, rounded cards, Plus Jakarta Sans
- **Creative/Design tools**: Bold accents, bento grid, gradient accents, Cal Sans + Satoshi

### Typography Rules
- Heading/body font pairing (e.g. Space Grotesk + DM Sans)
- Consistent scale: 12 14 16 18 24 32 48px
- Bold headings (600–700), Regular body (400), Medium labels (500)
- Line-height 1.5–1.75 body; letter-spacing default
- Tabular figures for prices and data columns

### Color Rules
- Define semantic tokens: primary, secondary, error, surface, on-surface
- Dark mode: desaturated/lighter tonal variants, NOT inverted
- Test contrast separately for light and dark variants
- Never convey info by color alone (add icon/text)

### Animation Rules
- Duration: 150–300ms micro; ≤400ms complex; never >500ms
- Use transform/opacity only (not width/height/top/left)
- Ease-out entering, ease-in exiting
- Spring/physics curves feel more natural than linear
- Exit animations 60–70% of enter duration
- Every animation must have meaning—no decoration only
