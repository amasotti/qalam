# Visual Design System — Rihla (رحلة)

> **Status:** Approved direction. Reference mockups in `docs/mockups/mockup-3-rihla.html`, `mockup-rihla-root.html`, `mockup-rihla-text.html`.
>
> This document is the source of truth for implementing the Rihla design system into the live SvelteKit frontend. When implementing, read the mockup HTML files for exact CSS values — this spec captures intent, rationale, and component contracts.

---

## 1. Design Principles

- **Warm editorial, not SaaS.** Every decision should feel like a well-typeset study journal, not a dashboard.
- **Arabic text is primary.** Arabic script is always larger, more prominent, and given more line-height than Latin text at the same level of the hierarchy.
- **Terracotta leads, not green.** The current app uses forest green (#16a34a) as primary. Rihla replaces this with terracotta (`--terra: #B84020`) throughout.
- **Right panel for metadata.** Detail pages use a 2-column layout: wide left column for content, narrow right column for contextual metadata cards.
- **Mastery communicates at a glance.** Color-coded mastery strips, dot indicators, and gauge steps must be consistent across all surfaces where words appear.

---

## 2. Color Tokens

Replace the current `tokens.css` `:root` block with the following. Keep the same CSS variable names where possible so shadcn components keep working; add Rihla-specific tokens below.

```css
:root {
  /* ── Base ── */
  --background:        0 0% 98%;       /* #FAF7F2 — warm sand, not pure white */
  --foreground:        20 15% 9%;      /* #1A1510 — warm near-black ink */
  --card:              0 0% 100%;
  --card-foreground:   20 15% 9%;
  --border:            38 30% 80%;     /* #DDD0BA — warm sand border */
  --input:             38 30% 80%;
  --ring:              16 67% 42%;     /* terra — focus ring */
  --radius:            0.625rem;       /* 10px — slightly more rounded than current */

  /* ── Primary: Terracotta (replaces forest green) ── */
  --primary:           16 67% 42%;    /* #B84020 */
  --primary-foreground: 0 0% 98%;

  /* ── Secondary ── */
  --secondary:         20 10% 27%;    /* #4A3C2C — warm dark brown */
  --secondary-foreground: 0 0% 98%;

  /* ── Muted ── */
  --muted:             38 30% 92%;    /* #F0E8D8 — sand-dark */
  --muted-foreground:  25 18% 48%;    /* #7A6A58 */

  /* ── Accent ── */
  --accent:            38 30% 87%;    /* #E0D0B4 — sand-darker, hover states */
  --accent-foreground: 16 67% 42%;   /* terra */

  /* ── Destructive ── */
  --destructive:       0 72% 51%;
  --destructive-foreground: 0 0% 98%;

  /* ── Sidebar — keep light in Rihla ── */
  --sidebar:           38 30% 92%;    /* #F0E8D8 — warm sand, NOT dark */
  --sidebar-foreground: 20 15% 29%;  /* #4A3C2C */
  --sidebar-primary:   16 67% 42%;
  --sidebar-primary-foreground: 0 0% 98%;
  --sidebar-accent:    38 30% 87%;
  --sidebar-accent-foreground: 16 67% 42%;
  --sidebar-border:    38 30% 80%;
  --sidebar-ring:      16 67% 42%;

  /* ── Rihla-specific tokens (not in shadcn mapping) ── */
  --terra:             #B84020;
  --terra-soft:        #D05030;
  --terra-pale:        #F5DDD6;
  --ocean:             #2A5878;
  --ocean-soft:        #3A6A8A;
  --ocean-pale:        #D0E8F5;
  --sage:              #4A7258;
  --sage-pale:         #D8EDE0;
  --amber:             #C07820;
  --amber-pale:        #FBF0DC;
  --sand:              #FAF7F2;
  --sand-dark:         #F0E8D8;
  --sand-darker:       #E0D0B4;
  --ink:               #1A1510;
  --ink-mid:           #4A3C2C;
  --ink-light:         #7A6A58;
}
```

**Remove** the `.dark` mode block — Rihla is light-only. Single-user app, no dark mode needed.

---

## 3. Typography

### Font stack

```css
@theme {
  --font-sans: 'DM Sans', ui-sans-serif, system-ui, sans-serif;

  /* Latin display/body — replaces system serif */
  --font-display: 'Lora', Georgia, serif;

  /* Arabic — unchanged from current */
  --font-arabic:         'Noto Naskh Arabic', 'Lateef', 'Amiri', serif;
  --font-arabic-display: 'Amiri', 'Noto Naskh Arabic', serif;
  --font-arabic-text:    'Noto Naskh Arabic', 'Amiri', serif;
}
```

**Google Fonts import** (add to `app.html`):
```html
<link href="https://fonts.googleapis.com/css2?family=Lora:ital,wght@0,400;0,500;0,600;0,700;1,400;1,500&family=DM+Sans:opsz,wght@9..40,300;9..40,400;9..40,500&family=Amiri:ital,wght@0,400;0,700;1,400&family=Noto+Naskh+Arabic:wght@400;500;600;700&display=swap" rel="stylesheet">
```

### Usage rules

| Context | Font | Size | Weight | Notes |
|---------|------|------|--------|-------|
| Body / UI copy | DM Sans | 14–15px | 400 | Default |
| Section labels | DM Sans | 0.62rem | 500 | UPPERCASE, 0.2em spacing |
| Page titles | Lora | 1.75rem | 600 | letter-spacing: -0.01em |
| Translation display | Lora | 1.1–1.3rem | 400 italic | |
| Word detail hero | Amiri | 7rem | 400 | direction: rtl |
| Root letter tiles | Amiri | 4.5rem | 400 | direction: rtl |
| Sentence Arabic | Noto Naskh Arabic | 1.75rem | 500 | line-height: 2 |
| Token Arabic | Noto Naskh Arabic | 1.1rem | 400 | |
| Full text Arabic | Noto Naskh Arabic | 1.5rem | 400 | line-height: 2.2 |
| Transliterations | Lora | 0.8–0.875rem | 400 italic | color: var(--terra) |
| Metadata keys | DM Sans | 0.58–0.62rem | 500 | UPPERCASE, 0.15em spacing |

---

## 4. Layout System

### App shell

```
┌─────────────────────────────────────────────────────┐
│  Sidebar (220px, fixed)  │  Main content (flex: 1)  │
│  background: sand-dark   │  margin-left: 220px       │
└─────────────────────────────────────────────────────┘
```

**Sidebar** (`--sidebar-width: 220px`):
- Background: `var(--sand-dark)` (#F0E8D8) — warm, not dark.
- Brand: Amiri قلم at 2.75rem in `--terra`, "Qalam" in Lora SC below.
- Nav links: justify-content: space-between; Latin label left, Arabic label right. Active state: `var(--terra-pale)` background, terra text.
- Progress strip at bottom: vocabulary mastery bar (terra→gold gradient fill).

**Breadcrumb bar**: 46px tall, white background, 1px bottom border. Arabic current-page label uses `font-family: 'Amiri'`.

### Detail page grid (word, root, text)

```
┌──────────────────────────┬────────────────┐
│  Content  (1fr)          │  Right panel   │
│  padding: 2.5rem 3rem    │  260–280px     │
│  border-right: 1px solid │  padding:      │
│  var(--border)           │  2.5rem 1.75rem│
└──────────────────────────┴────────────────┘
```

Grid: `grid-template-columns: 1fr 260px` (word/text) or `1fr 280px` (root).

---

## 5. Component Library

### Buttons

```css
/* Base */
padding: 0.45–0.5rem 1.1–1.25rem;
border-radius: 6px;
font-family: 'DM Sans';
font-size: 0.78–0.8rem;
font-weight: 500;
border: 1px solid var(--border);
background: transparent;
color: var(--ink-mid);
transition: all 150ms;

/* Primary */
background: var(--terra);
color: white;
border-color: var(--terra);
:hover { background: var(--terra-soft); }

/* Destructive */
/* Use 2-step confirm pattern — first click shows "Confirm delete" variant */
```

### Chips / Badges

All chips: `border-radius: 999px`, `font-size: 0.7–0.72rem`, `font-weight: 500`, `border: 1px solid`.

| Variant | Background | Text | Border |
|---------|-----------|------|--------|
| dialect-ocean (MSA) | `--ocean-pale` | `--ocean` | rgba(42,88,120,0.2) |
| difficulty-amber | `--amber-pale` | `--amber` | rgba(192,120,32,0.25) |
| mastery-gold | `--amber-pale` / `--sage-pale` (by level) | varies | varies |
| tag/muted | `--sand-dark` | `--ink-light` | `--border` |

**Mastery color mapping** (apply consistently everywhere):

| Level | Color token | Background |
|-------|-------------|-----------|
| mastered | `--sage` | `--sage-pale` |
| known | `#8BC48A` | `#E0F0DE` |
| familiar | `--amber` | `--amber-pale` |
| learning | `--terra` | `--terra-pale` |
| new | `--border` / `--sand-darker` | `--sand-dark` |

### Section label pattern

Used before every major content section:

```html
<div class="section-label">Examples</div>
```

```css
.section-label {
  font-size: 0.62rem;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--ink-light);
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}
.section-label::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}
```

### Metadata cards (right panel)

```css
.meta-card {
  padding: 1.125–1.25rem;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: white;
  box-shadow: 0 1px 4px rgba(26,21,16,0.04);
}
.meta-card-title {
  font-family: 'Lora', serif;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 0.875–1rem;
}
```

Meta rows inside cards: label (0.58rem, uppercase, `--ink-light`) + value (0.82rem, `--ink-mid`).

---

## 6. Page: Word Detail (`/words/[id]`)

### Hero section (left column, above the fold)

```
┌──────────────────────────────────────────────────────┐
│  [ghost letter watermark — first letter, 8rem, 4% opacity, right-aligned]  │
│                                                       │
│  كَتَبَ    ← Amiri, 7rem, ink, direction:rtl          │
│  kataba   ← Lora italic, 1.3rem, terra               │
│                                                       │
│  [Verb] [MSA] [Intermediate] [Familiar]  [Edit][Del] │
│       chips row                    action buttons    │
└──────────────────────────────────────────────────────┘
border-bottom: 1px solid --border; padding-bottom: 2.5rem; margin-bottom: 3rem
```

**Ghost letter**: `position: absolute`, `font-family: 'Amiri'`, `font-size: 8rem`, `color: var(--terra)`, `opacity: 0.04`, `right: -1rem`, `top: -1.5rem`, `user-select: none`.

### Content sections (left column)

Sections in order: Translation → Examples → Dictionary sources → Annotations.

**Examples**: left-border-accented cards.
```css
.example-item {
  padding: 1.25rem;
  border-left: 3px solid var(--terra);
  background: var(--sand-dark);
  border-radius: 0 8px 8px 0;
  margin-bottom: 0.75rem;
}
```
Arabic line: Noto Naskh Arabic 1.625rem, rtl. Transliteration: Lora italic 0.8rem terra. Translation: DM Sans 0.85rem ink-mid.

### Right panel cards

1. **Root card** — saffron background (`--amber-pale`), root Arabic large (`Amiri` 1.75rem), "View family →" link.
2. **Pronunciation card** — ocean palette, play icon + "Listen on Forvo".
3. **Mastery gauge** — 5-step indicator:
   ```html
   <div class="mastery-steps">  <!-- 5 × step divs -->
   ```
   Steps: `height: 5px`, `flex: 1`, filled = `background: var(--terra)`, empty = `background: var(--border)`.
4. **Details card** — POS, dialect, difficulty, date added.

---

## 7. Page: Root Detail (`/roots/[id]`)

### Hero section

Three letter tiles + metadata, separated from content by border-bottom.

```
← ب — ت — ك →   (direction: rtl, displayed left-to-right as RTL)
  b       t       k
[transliterations below each letter]

                      [normalized: كَتَبَ]
                      [k–t–b · 3 letters]
                                [Edit][Delete]
```

**Letter tile**:
```css
.root-letter-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 0 2.5rem;
  position: relative;
}
/* Underline animation on hover */
.root-letter-tile::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%; right: 50%;
  height: 2px;
  background: var(--terra);
  border-radius: 1px;
  transition: left 200ms, right 200ms;
}
.root-letter-tile:hover::after { left: 0; right: 0; }
```

Arabic letter: `Amiri` 4.5rem. Transliteration below: `Lora` 0.72rem italic terra.

**Ghost background**: same pattern as word hero — first letter of root at ~10rem opacity 0.04.

### Word family grid

```css
.word-family-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 0.75rem;
}
```

Each card:
```css
.word-chip {
  border: 1px solid var(--border);
  border-radius: 10px;
  background: white;
  box-shadow: 0 1px 3px rgba(26,21,16,0.04);
  padding: 1rem 0.75rem;
  text-align: center;
  /* mastery top strip */
  position: relative;
  overflow: hidden;
}
.word-chip::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  border-radius: 10px 10px 0 0;
  background: [mastery color per table in §5];
}
```

Card content (top → bottom): Arabic (`Amiri` 1.75rem) → transliteration (Lora italic 0.65rem terra) → translation (DM Sans 0.68rem ink-light) → POS chip → mastery chip.

### Right panel cards

1. **Root info** — normalized form, letter count, word family count.
2. **Family mastery bar chart** — one row per mastery level with colored dot, label, horizontal bar, count.
3. **Key words** — 3 quick-access link items with Arabic + translation.
4. **Actions** — "Add word to family", "Practice this root".

---

## 8. Page: Text Detail (`/texts/[id]`)

### Header

Title in Lora 1.75rem. Chips row: difficulty, dialect, tags. Comments block: `font-style: italic`, `border-left: 2px solid var(--border)`, `padding-left: 1rem`.

### Interlinear section — layout per sentence

```
┌───┬────────────────────────────────────────────────┐
│ ١ │  Arabic text (Noto Naskh Arabic 1.75rem, rtl)  │
│   │  Transliteration (Lora italic, terra)           │
│   │                                                 │
│   │  ┌ Token grid (RTL flex, sand-dark bg) ───────┐ │
│   │  │  [ك‎ة] [مَعَ] [أ] [اللي] ...               │ │
│   │  └────────────────────────────────────────────┘ │
│   │                                                 │
│   │  ▏ Translation (Lora italic, terra border-left) │
│   │  Notes (italic, ink-light)                      │
└───┴────────────────────────────────────────────────┘
```

**Sentence block** grid: `grid-template-columns: 2rem 1fr`, `gap: 0 1rem`, `padding: 1.75rem 0`, `border-bottom: 1px solid var(--border)`.

Sentence number: DM Sans 0.7rem, `--ink-light`, opacity 0.6.

**Token grid**:
```css
.token-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  direction: rtl;
  padding: 0.875rem;
  background: var(--sand-dark);
  border-radius: 8px;
  border: 1px solid var(--border);
}
```

**Token cell** (each word):
```css
.token-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 0.5rem 0.625rem;
  border-radius: 5px;
  background: white;
  border: 1px solid var(--border);
  min-width: 3.5rem;
  text-align: center;
  cursor: default;
  transition: border-color 120ms, box-shadow 120ms;
}
.token-cell:hover {
  border-color: var(--terra);
  box-shadow: 0 2px 8px rgba(184,64,32,0.1);
}
```

Token cell content (top → bottom):
1. Arabic word — Noto Naskh Arabic 1.1rem, `--ink`
2. Transliteration — Lora 0.6rem italic, `--terra`
3. POS chip — 0.55rem, color-coded (see §5 + table below)
4. Gloss — DM Sans 0.62rem, `--ink-mid`, `max-width: 5rem`, ellipsis

**POS color mapping**:
| POS | Background | Text |
|-----|-----------|------|
| verb | `--terra-pale` | `--terra` |
| noun | `--ocean-pale` | `--ocean` |
| adjective | `--sage-pale` | `--sage` |
| particle/conjunction | `--sand-dark` | `--ink-light` |
| prep / prep+n | `--amber-pale` | `--amber` |

**Translation block**:
```css
.sentence-translation {
  font-size: 0.875rem;
  color: var(--ink-mid);
  padding: 0.625rem 0.875rem;
  border-left: 2px solid var(--terra);
  background: rgba(184,64,32,0.03);
  border-radius: 0 4px 4px 0;
  font-family: 'Lora', serif;
  font-style: italic;
}
```

"Trans." label inside: DM Sans, 0.6rem, UPPERCASE, 0.12em spacing, terra, font-style: normal, margin-right: 0.5rem.

### Full text panel

Below the interlinear section, separated by `section-label`:
- Arabic body: Noto Naskh Arabic 1.5rem, rtl, line-height: 2.2
- Transliteration: Lora italic 0.9rem terra, line-height: 1.9
- Translation: DM Sans 0.9rem ink-mid, line-height: 1.75

### Right panel cards

1. **Text details** — dialect, difficulty, sentence count, token count, date added.
2. **Word mastery breakdown** — horizontal bar per level with label, bar, count.
3. **Tags** — chip pills.
4. **Actions** — "Add sentence", "Re-tokenize all", "Practice words →" link.

---

## 9. CSS File Reorganisation

Current structure:
```
src/styles/tokens.css      ← replace :root block (§2 above)
src/styles/semantic.css    ← update mastery/difficulty/dialect colors to match Rihla palette
src/styles/layout.css      ← full rewrite per §4–8
src/app.css                ← add Google Fonts import, touch nothing else
```

`semantic.css` changes needed: mastery badge colors must use `--sage`/amber/terra/ocean palette instead of current green shades. Dialect badge colors can stay since they're distinct per dialect; just verify they read well on sand background.

---

## 10. Migration Checklist

When implementing, work in this order to avoid regressions:

1. **Fonts** — add Google Fonts `<link>` in `app.html`, update `tokens.css` `@theme` block.
2. **Color tokens** — replace `:root` in `tokens.css`. Verify shadcn components still render (they use HSL variables).
3. **Sidebar** — update `layout.css` sidebar classes. Sidebar becomes light sand, not dark.
4. **Breadcrumb** — update or add breadcrumb bar component.
5. **Buttons/chips/badges** — update `semantic.css` + shadcn Button overrides.
6. **Word detail** — rewrite `.page-word-detail`, `.word-hero`, `.word-info`, `.word-examples-list` CSS blocks.
7. **Root detail** — rewrite `.page-root-detail`, `.root-hero`, `.word-family-grid`, `.word-chip` blocks.
8. **Text detail** — rewrite text page CSS + `InterlinearSentence.svelte` styles + `TokenGrid.svelte` styles + `FullTextPanel.svelte` styles.
9. **Meta sidebar cards** — add new `.meta-card` pattern to `layout.css`, update page svelte files to use 2-column grid.
10. **Home page** — update `.home-shell` to use sand background and terra accent.

---

## 11. What NOT to Change

- Arabic font family names (`--font-arabic`, `--font-arabic-display`, `--font-arabic-text`) — keep as-is, already correct.
- Arabic font sizes and line-heights in `tokens.css`.
- Dialect badge colors (they're distinct by design and not related to mastery).
- All backend, API types, store logic — purely a CSS/layout change.
- `semantic.css` annotation-type colors.
- The shadcn component files themselves — only override via CSS variables and wrapper classes.
