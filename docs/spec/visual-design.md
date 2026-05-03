# Visual Design System — Busatan (بُسْتَان)

> **Status:** Approved direction. Reference mockups in `docs/mockups/mockup-busatan-word.html`, `mockup-busatan-root.html`, `mockup-busatan-text.html`.
>
> This document is the source of truth for implementing the Busatan design system into the live SvelteKit frontend. When implementing, read the mockup HTML files for exact CSS values — this spec captures intent, rationale, and component contracts.

---

## 1. Design Principles

- **Warm editorial, not SaaS.** Every decision should feel like a well-typeset study journal, not a dashboard.
- **Arabic text is primary.** Arabic script is always larger, more prominent, and given more line-height than Latin text at the same level of the hierarchy.
- **Three accent roles, each with a job.** Busatan avoids both monochrome and random color. Every accent color has a specific semantic role — see §2. This is what makes it feel lively without being chaotic.
- **Right panel for metadata.** Detail pages use a 2-column layout: wide left column for content, narrow right column for contextual metadata cards.
- **Mastery communicates at a glance.** Color-coded mastery strips, dot indicators, and gauge steps must be consistent across all surfaces where words appear.

---

## 2. Color Tokens

Replace the current `tokens.css` `:root` block with the following.

### Semantic roles — read before implementing

The palette has three accent families, each assigned to a specific UI role:

| Role | Color | Used for |
|------|-------|----------|
| **Structural / brand** | olive `#4A6228` | Nav active, hero transliteration, example left borders, root card bg, section label underlines, word-family card hover |
| **Action / progress** | coral `#C04830` | CTA buttons (Edit), mastery steps/gauge fill, difficulty/mastery level badges, word-family chip hover border |
| **External / information** | cerulean `#1E5898` | Pronunciation button, dictionary pill default style, dialect badges (MSA/Tunisian), external link hover |

Standard red `#C03030` stays for the Delete/destructive button — universal danger signal, intentionally different from all three accent families.

### CSS custom properties

```css
:root {
  /* ── Base ── */
  --background:        48 22% 96%;      /* #F9F8F1 — warm parchment */
  --foreground:        36 14% 9%;       /* #1A1810 */
  --card:              0 0% 100%;
  --card-foreground:   36 14% 9%;
  --border:            40 20% 80%;      /* #DDD5C0 */
  --input:             40 20% 80%;
  --ring:              102 40% 28%;     /* olive — focus ring */
  --radius:            0.625rem;

  /* ── Primary: Olive (structural) ── */
  --primary:           102 40% 28%;    /* #4A6228 */
  --primary-foreground: 0 0% 98%;

  /* ── Secondary ── */
  --secondary:         36 14% 22%;     /* #3C3428 — warm dark brown */
  --secondary-foreground: 0 0% 98%;

  /* ── Muted ── */
  --muted:             40 22% 88%;     /* #EEE8DC — bg-dark */
  --muted-foreground:  36 14% 40%;     /* #786848 */

  /* ── Accent ── */
  --accent:            40 18% 82%;     /* #E4DDD0 — bg-darker */
  --accent-foreground: 102 40% 28%;    /* olive */

  /* ── Destructive ── */
  --destructive:       0 72% 47%;      /* #C03030 */
  --destructive-foreground: 0 0% 98%;

  /* ── Sidebar — keep light ── */
  --sidebar:           40 22% 88%;     /* #EEE8DC */
  --sidebar-foreground: 36 14% 40%;
  --sidebar-primary:   102 40% 28%;
  --sidebar-primary-foreground: 0 0% 98%;
  --sidebar-accent:    48 22% 96%;
  --sidebar-accent-foreground: 102 40% 28%;
  --sidebar-border:    40 20% 80%;
  --sidebar-ring:      102 40% 28%;

  /* ── Busatan-specific tokens ── */
  --olive:             #4A6228;   /* structural primary */
  --olive-soft:        #5A7838;
  --olive-pale:        #E8F0D8;
  --coral:             #C04830;   /* action/progress */
  --coral-soft:        #D05A40;
  --coral-pale:        #FAE8E4;
  --cerulean:          #1E5898;   /* external/information */
  --cerulean-soft:     #2868B0;
  --cerulean-pale:     #E0EAF8;
  --sage:              #2A6840;   /* mastery positive (mastered/known) */
  --sage-pale:         #D8EEE0;
  --amber:             #B07820;   /* used only for mastery familiar strip */
  --amber-pale:        #F5EDD0;
  --bg:                #F9F8F1;   /* page background */
  --bg-dark:           #EEE8DC;   /* sidebar / example cards */
  --bg-darker:         #E4DDD0;
  --ink:               #1A1810;
  --ink-mid:           #3C3428;
  --ink-light:         #786848;
  --ink-ghost:         #B8A888;
}
```

**Remove** the `.dark` mode block — Busatan is light-only. Single-user app, no dark mode needed.

---

## 3. Typography

### Font stack

```css
@theme {
  --font-sans: 'DM Sans', ui-sans-serif, system-ui, sans-serif;
  --font-display: 'Lora', Georgia, serif;
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
| Transliterations | Lora | 0.8–0.875rem | 400 italic | color: var(--olive) |
| Metadata keys | DM Sans | 0.58–0.62rem | 500 | UPPERCASE, 0.15em spacing |

---

## 4. Layout System

### App shell

```
┌─────────────────────────────────────────────────────┐
│  Sidebar (220px, fixed)  │  Main content (flex: 1)  │
│  background: --bg-dark   │  margin-left: 220px       │
└─────────────────────────────────────────────────────┘
```

**Sidebar** (`--sidebar-width: 220px`):
- Background: `var(--bg-dark)` (#EEE8DC) — warm, not dark.
- Brand: Amiri قلم at 2.875rem in `--olive`, "Qalam" in Lora SC below.
- Nav section labels (Study / Practice): 0.58rem, uppercase, `--ink-ghost`.
- Nav links: `justify-content: space-between`; Latin label left, Arabic label right. Active state: `var(--olive-pale)` background, `--olive` text.
- Progress strip at bottom: vocabulary mastery bar with `linear-gradient(to right, var(--olive), var(--coral))`.

**Breadcrumb bar**: 46px tall, white background, 1px bottom border (`--border-thin`). Arabic current-page label uses `font-family: 'Amiri'`.

### Detail page grid (word, root, text)

```
┌──────────────────────────┬────────────────┐
│  Content  (1fr)          │  Right panel   │
│  padding: 2.5rem 3rem    │  260–288px     │
│  border-right: 1px solid │  padding:      │
│  var(--border)           │  2.5rem 1.75rem│
└──────────────────────────┴────────────────┘
```

Grid: `grid-template-columns: 1fr 272px` (word) / `1fr 260px` (text) / `1fr 288px` (root).

---

## 5. Component Library

### Buttons

```css
/* Base */
padding: 0.45rem 1.1rem;
border-radius: 6px;
font-family: 'DM Sans';
font-size: 0.78rem;
font-weight: 500;
border: 1px solid var(--border);
background: transparent;
color: var(--ink-mid);
transition: all 150ms;

/* Primary (Edit/Save) — coral */
background: var(--coral);
color: white;
border-color: var(--coral);
:hover { background: var(--coral-soft); }

/* Destructive (Delete) — red text, NOT coral */
color: #C03030;
border-color: rgba(192,48,48,0.22);
:hover { background: rgba(192,48,48,0.06); border-color: #C03030; }
```

### Chips / Badges

All chips: `border-radius: 999px`, `font-size: 0.7–0.72rem`, `font-weight: 500`, `border: 1px solid`.

Badge role assignments (apply consistently on all pages):

| Badge type | Variant | Background | Text |
|-----------|---------|-----------|------|
| Part of speech (Verb/Noun/Adj) | olive | `--olive-pale` | `--olive` |
| Dialect (MSA, Tunisian) | cerulean | `--cerulean-pale` | `--cerulean` |
| Difficulty (Intermediate…) | coral | `--coral-pale` | `--coral` |
| Mastery level chip | coral | `--coral-pale` | `--coral` |
| Genre tags, muted | muted | `--bg-dark` | `--ink-light` |

**Mastery color mapping** (apply consistently everywhere — strips, dots, bars, chips):

| Level | Strip / dot color | Chip background |
|-------|------------------|----------------|
| mastered | `--sage` `#2A6840` | `--sage-pale` |
| known | `#5A9870` | `#E0F0E4` |
| familiar | `--coral` `#C04830` | `--coral-pale` |
| learning | `--olive` `#4A6228` | `--olive-pale` |
| new | `--border` `#DDD5C0` | `--bg-dark` |

### Dictionary pills

Default state has color — not just on hover:
```css
.dict-pill {
  background: var(--cerulean-pale);
  border: 1px solid rgba(30,88,152,0.2);
  color: var(--cerulean);
  border-radius: 6px;
  padding: 0.28rem 0.875rem;
  font-size: 0.72rem;
}
.dict-pill:hover {
  background: var(--cerulean);
  color: white;
  border-color: var(--cerulean);
}
```

### Pronunciation / external link button

Cerulean (information role):
```css
background: var(--cerulean-pale);
border: 1px solid rgba(30,88,152,0.22);
color: var(--cerulean);
:hover { background: var(--cerulean); color: white; }
```

### Section label pattern

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
  padding: 1.125rem;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: white;
  box-shadow: 0 1px 4px rgba(26,24,16,0.04);
}
.meta-card-title {
  font-family: 'Lora', serif;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 0.875rem;
}
```

Meta rows inside cards: label (0.58rem, uppercase, `--ink-ghost`) + value (0.82rem, `--ink-mid`).

---

## 6. Page: Word Detail (`/words/[id]`)

### Hero section

```
┌──────────────────────────────────────────────────────┐
│  [ghost letter — first letter, 8rem, olive 5% opacity, right]  │
│                                                       │
│  كَتَبَ    ← Amiri, 7rem, ink, direction:rtl          │
│  kataba   ← Lora italic, 1.3rem, --olive              │
│                                                       │
│  [Verb]olive [MSA]cerulean [Intermediate]coral [Familiar]coral  [Edit]coral [Delete]red │
└──────────────────────────────────────────────────────┘
border-bottom: 1px solid --border; padding-bottom: 2.5rem; margin-bottom: 3rem
```

**Ghost letter**: `position: absolute`, `font-family: 'Amiri'`, `font-size: 8rem`, `color: var(--olive)`, `opacity: 0.05`, `right: -0.5rem`, `top: -1rem`, `user-select: none`.

### Content sections (left column)

Sections in order: Translation → Pronunciation → Examples → Dictionary sources → Annotations.

**Examples**: olive left-border cards.
```css
.example-item {
  padding: 1.125rem 1.25rem;
  border-left: 3px solid var(--olive);
  background: var(--bg-dark);
  border-radius: 0 8px 8px 0;
}
```
Arabic line: Noto Naskh Arabic 1.5rem, rtl. Transliteration: Lora italic 0.78rem `--olive-soft`. Translation: DM Sans 0.84rem `--ink-mid`.

### Right panel cards

1. **Root card** — olive-pale background, root Arabic large (`Amiri` 1.625rem), "View family →" in `--olive`.
2. **Mastery gauge** — 5-step indicator, filled steps in `--coral`, label in `--coral`.
3. **Details card** — POS, pattern, dialect, difficulty, date added.
4. **Word family preview** — 2×2 chip grid with mastery top strips.

---

## 7. Page: Root Detail (`/roots/[id]`)

### Hero section

```
← ب — ت — ك →  (RTL direction)
  b       t       k
[normalized: كَتَبَ — k-t-b]  [3 letters badge]      [Edit][Delete]
```

**Letter tile** (hover underline animation):
```css
.root-letter-tile { position: relative; }
.root-letter-tile::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%; right: 50%;
  height: 2px;
  background: var(--coral);   /* coral, not olive — action feel */
  border-radius: 1px;
  transition: left 200ms, right 200ms;
}
.root-letter-tile:hover::after { left: 0; right: 0; }
```

Arabic letter: `Amiri` 4.5rem. Transliteration below: `Lora` 0.72rem italic `--olive`.

**Ghost background**: root letters (space-separated) at ~10rem, `color: var(--olive)`, `opacity: 0.04`.

### Word family grid

```css
.word-family-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 0.75rem;
}
```

Each card: same pattern as word detail — white bg, 1px border, mastery top strip (3px), hover raises card with `border-color: var(--coral)`.

Card content (top → bottom): Arabic (`Amiri` 1.75rem) → transliteration (Lora italic 0.65rem `--olive`) → translation (DM Sans 0.68rem `--ink-light`) → POS chip → mastery chip.

### POS chip colors (word family + token cells)

| POS | Background | Text |
|-----|-----------|------|
| verb | `--coral-pale` | `--coral` |
| noun | `--cerulean-pale` | `--cerulean` |
| adjective | `--olive-pale` | `--olive` |
| particle/conjunction | `--bg-darker` | `--ink-light` |
| prep / prep+n | `--olive-pale` | `--olive` |

### Right panel cards

1. **Root info** — normalized form, letter count, word family count.
2. **Family mastery bar chart** — one row per mastery level: colored dot + label + horizontal bar + count.
3. **Key words** — 3 quick-access items; hover: `border-color: var(--olive)`, bg: `var(--olive-pale)`.
4. **Actions** — "Add word to family", "Practice this root".

---

## 8. Page: Text Detail (`/texts/[id]`)

### Header

Title in Lora 1.75rem. Chips: difficulty=coral, dialect=cerulean, genre tags=olive or muted. Description block: `font-style: italic`, `border-left: 2px solid var(--border)`, `padding-left: 1rem`.

### Interlinear section — layout per sentence

```
┌───┬────────────────────────────────────────────────┐
│ ١ │  Arabic text (Noto Naskh Arabic 1.75rem, rtl)  │
│   │  Transliteration (Lora italic, --olive)         │
│   │                                                 │
│   │  ┌ Token grid (RTL flex, bg-dark bg) ─────────┐ │
│   │  │  white cards: arabic / translit / POS / gloss │
│   │  └────────────────────────────────────────────┘ │
│   │                                                 │
│   │  ▏ Translation (Lora italic, olive border-left) │
│   │  Notes (italic, --ink-light)                    │
└───┴────────────────────────────────────────────────┘
```

**Sentence block** grid: `grid-template-columns: 2rem 1fr`, `gap: 0 1rem`, `padding: 1.75rem 0`, `border-bottom: 1px solid var(--border)`.

Sentence number: Lora italic 0.75rem, `--olive`, opacity 0.7.

**Token grid**:
```css
.token-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  direction: rtl;
  padding: 0.875rem;
  background: var(--bg-dark);
  border-radius: 8px;
  border: 1px solid var(--border);
}
```

**Token cell**:
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
  transition: border-color 120ms, box-shadow 120ms;
}
.token-cell:hover {
  border-color: var(--coral);
  box-shadow: 0 2px 8px rgba(192,72,48,0.1);
}
```

Token cell content (top → bottom):
1. Arabic — Noto Naskh Arabic 1.1rem, `--ink`
2. Transliteration — Lora 0.6rem italic, `--olive`
3. POS chip — 0.55rem, color-coded per §7 table
4. Gloss — DM Sans 0.62rem, `--ink-mid`, `max-width: 5rem`, ellipsis

**Translation block**:
```css
.sentence-translation {
  font-size: 0.875rem;
  color: var(--ink-mid);
  padding: 0.625rem 0.875rem;
  border-left: 2px solid var(--olive);
  background: rgba(74,98,40,0.03);
  border-radius: 0 4px 4px 0;
  font-family: 'Lora', serif;
  font-style: italic;
}
```

"Trans." label inside: DM Sans, 0.6rem, UPPERCASE, `--olive`, `font-style: normal`, `margin-right: 0.5rem`.

### Full text panel

Below the interlinear section, separated by `section-label`:
- Arabic body: Noto Naskh Arabic 1.5rem, rtl, line-height: 2.2
- Transliteration: Lora italic 0.9rem `--olive`, line-height: 1.9
- Translation: DM Sans 0.9rem `--ink-mid`, line-height: 1.75

### Right panel cards

1. **Text details** — dialect, difficulty, sentence count, token count, date added.
2. **Word mastery breakdown** — horizontal bar per level. Mastered/known = sage; familiar = coral; learning = olive.
3. **Tags** — chip pills. Genre tags use olive chip; other tags use muted.
4. **Actions** — "Add sentence", "Re-tokenize all", "Practice words →" (cerulean link button).

---

## 9. CSS File Reorganisation

Current structure:
```
src/styles/tokens.css       ← design tokens and HSL bridge
src/styles/base.css         ← base element defaults and prose
src/styles/arabic.css       ← Arabic and transliteration roles
src/styles/layout.css       ← app shell, drawer shell, breadcrumb, page layouts
src/styles/components.css   ← shared component primitives (buttons, chips, meta cards, toggles)
src/styles/utilities.css    ← spacing/layout helpers and cross-page utility classes
src/styles/word.css         ← word detail, dictionary pills, morphology, notes
src/styles/root.css         ← root detail and word-family visuals
src/styles/text.css         ← text detail presentation
src/styles/lists.css        ← list pages, search/filter bars, collection cards
src/styles/home.css         ← landing page composition
src/styles/forms.css        ← full-page forms and autocomplete flows
src/styles/training.css     ← analytics, training, flashcards, vocab lookup
src/styles/annotations.css  ← annotation UI and drawer form patterns
src/styles/editors.css      ← token/sentence editing surfaces
src/styles/ai.css           ← AI result cards and notices
src/styles/error.css        ← error states
src/styles/semantic.css     ← mastery/difficulty/dialect semantic color classes
src/styles/animations.css   ← animation utilities
src/app.css                 ← import manifest only; preserve import order
```

Reorganisation rules:
- `layout.css` is intentionally narrow. It owns only shell/layout primitives, not feature styling.
- Shared reusable visual primitives belong in `components.css` or `utilities.css`, not in feature partials.
- Feature-specific classes stay in their feature partial and keep clear prefixes where possible.
- When a concern grows beyond a few tightly-related selectors, split it into its own partial and register it in `src/app.css`.
- Avoid reusing the same class name for different concepts across features.

---

## 10. Migration Checklist

Work in this order to avoid regressions:

1. **Fonts** — add Google Fonts `<link>` in `app.html`, update `tokens.css` `@theme` block.
2. **Color tokens** — replace `:root` in `tokens.css`. Verify shadcn components still render (HSL variables).
3. **Sidebar** — update `layout.css` sidebar: light bg-dark, olive brand + active states.
4. **Breadcrumb** — update breadcrumb bar component.
5. **Buttons/chips/badges** — update `components.css` and `semantic.css`. Dict pills get cerulean default style in `word.css`.
6. **Word detail** — place word-specific rules in `word.css`. Update badge color assignments per §5.
7. **Root detail** — place root-specific rules in `root.css`; keep family-card naming distinct from annotation chips.
8. **Text detail** — place text presentation rules in `text.css` and editing surfaces in `editors.css`. POS chips → §7 table.
9. **Meta sidebar cards** — keep `.meta-card` in `components.css`, page grid/layout in `layout.css`.
10. **Home page** — update `.home-shell` to use `--bg` background and olive/coral accents.

---

## 11. What NOT to Change

- Arabic font family names (`--font-arabic`, `--font-arabic-display`, `--font-arabic-text`) — keep as-is.
- Arabic font sizes and line-heights in `tokens.css`.
- All backend, API types, store logic — purely a CSS/layout change.
- `semantic.css` annotation-type colors.
- The shadcn component files themselves — only override via CSS variables and wrapper classes.
