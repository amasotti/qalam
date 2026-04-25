# Busatan UI Redesign — Design Document
**Date:** 2026-04-25
**Status:** Approved

## Source of truth
`docs/spec/visual-design.md` + mockups in `docs/mockups/mockup-busatan-*.html`.
This document captures implementation scope, approach, and decisions not explicit in the spec.

---

## Scope

Full frontend redesign applying the Busatan design system (بُسْتَان) to every page. Both CSS and Svelte structural changes are in scope. Backend, API, and store logic untouched.

---

## Implementation approach

Foundation-first, following spec §10 migration checklist. One logical commit per layer — each commit leaves the app in a working state.

| Layer | Files touched |
|-------|--------------|
| 1. Fonts + @theme | `app.html`, `app.css`, `tokens.css` |
| 2. Color tokens | `tokens.css` |
| 3. Sidebar + shell | `+layout.svelte`, `layout.css` |
| 4. Semantic + shared components | `semantic.css`, `layout.css` |
| 5. Word detail | `words/[id]/+page.svelte`, `layout.css` |
| 6. Root detail | `roots/[id]/+page.svelte`, `layout.css` |
| 7. Text detail | `texts/[id]/+page.svelte`, `layout.css`, interlinear components |
| 8. List pages + cards | `words/+page.svelte`, `roots/+page.svelte`, `texts/+page.svelte`, `layout.css` |
| 9. Home page | `+page.svelte`, `layout.css` |
| 10. Create/edit forms | `*/new/+page.svelte`, minor token inheritance |

---

## Key decisions

### Tokens
- Replace `:root` block wholesale with Busatan HSL values (spec §2).
- Remove `.dark` block entirely — Busatan is light-only.
- Add `--font-sans: 'DM Sans'` and `--font-display: 'Lora'` to `@theme`. Keep `--font-arabic*` vars unchanged (spec §11).

### Sidebar
- Width: 220px (was 160px).
- Background: `var(--bg-dark)` — light warm, not dark.
- Brand: Amiri `قلم` 2.875rem in `--olive` + "Qalam" in Lora SC below.
- Nav links: Latin label left + Arabic label right (`justify-content: space-between`). Section group headers ("Study", "Practice") in 0.58rem uppercase `--ink-ghost`.
- Active state: `var(--olive-pale)` bg + `--olive` text.
- Bottom: vocabulary mastery progress strip with `linear-gradient(to right, var(--olive), var(--coral))`.
- Breadcrumb bar: 46px white strip, 1px bottom border, Arabic page name in Amiri — rendered in `+layout.svelte` above page content.

### Semantic + components
- Mastery: sage (mastered) → #5A9870 (known) → coral (familiar) → olive (learning) → border (new). Consistent across chips, strips, dots, bars.
- Difficulty: all levels → coral-pale/coral family.
- Dialect: all → cerulean-pale/cerulean family.
- POS: verb=coral, noun=cerulean, adjective/prep=olive, particle/conjunction=bg-darker/ink-light.
- Dict badges: simplified from per-source rainbow to single cerulean pill style. Source name in label is sufficient differentiation.
- Pronunciation button: cerulean-pale/cerulean.
- New utility classes: `.section-label`, `.meta-card`, `.meta-card-title`, `.dict-pill`.

### Detail page structure (Word, Root, Text)
All three get a 2-column grid wrapper in Svelte:
```svelte
<div class="detail-layout">
  <div class="detail-content">…</div>
  <aside class="detail-sidebar">…</aside>
</div>
```
CSS: `grid-template-columns: 1fr 272px` (word) / `1fr 288px` (root) / `1fr 260px` (text).

**Word detail right panel:** root card (olive-pale bg) + mastery gauge (5-step coral fill) + details card (POS, pattern, dialect, difficulty, date) + word family 2×2 chip grid with mastery top strips.

**Root detail right panel:** root info card (normalized form, letter count, word count) + family mastery bar chart (dot + label + bar + count per level) + key words (3 items, olive hover) + actions.

**Text detail right panel:** text details card + word mastery breakdown bars + genre/tag chips + actions (Add sentence, Re-tokenize, Practice →).

### Word detail left column
Hero: Amiri 7rem Arabic with absolute ghost letter (first char, 8rem, olive 5% opacity) + Lora italic transliteration (`--olive`) + badge row + Edit (coral) / Delete (red) buttons.

Sections (in order): Translation → Pronunciation → Examples → Dictionary sources → Annotations.

Examples: olive left-border cards (`border-left: 3px solid var(--olive)`, `bg-dark` bg).

### Root detail left column
Hero: RTL letter tiles (Amiri 4.5rem) with coral underline hover animation + ghost background (root letters ~10rem, olive 4% opacity).

Word family: `auto-fill minmax(110px,1fr)` grid. Each card: mastery top strip (3px) + white bg + coral hover border.

### Text detail left column
Sentence blocks: `grid-template-columns: 2rem 1fr`, numbered with Lora italic `--olive`. Token grid: RTL flex, bg-dark, white token cells with coral hover border + shadow. Translation block: Lora italic, olive left border. Full text panel below.

### List pages
Inherit Busatan tokens automatically from layer 1–2. Additional card updates:
- Word cards: mastery top strip (3px), hover → `border-color: var(--coral)`.
- Root cards: hover → `border-color: var(--olive)`.
- Per-section color accents (home nav cards) → map to olive/coral/cerulean.

### Home page
Verse block: Arabic in Noto Naskh, transliteration in Lora italic `--olive`. Nav cards: olive hover borders. Brand mark: Amiri, `--olive`. Background: `var(--bg)` parchment.

### Create/edit forms
No structural changes. Inherit Busatan tokens. Focus rings → olive. Buttons → coral primary, red destructive.

---

## What does NOT change
- Arabic font family names and sizes in `tokens.css`
- `semantic.css` annotation-type colors
- All backend, API types, store logic
- shadcn component internals
