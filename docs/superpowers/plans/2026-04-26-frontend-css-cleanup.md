# Frontend CSS Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all dead global CSS, fix two rendering bugs from broken/mismatched tokens, and extract repeated component-scoped patterns into the global design library.

**Architecture:** Three phases — (1) delete dead rules, (2) fix bugs where scoped CSS conflicts with or references missing globals, (3) add missing global patterns and update components to use them. Each task leaves the app in a working, visually correct state.

**Tech Stack:** SvelteKit 5 (Svelte runes), Tailwind v4, CSS custom properties (two coexisting token systems: old Busatan `var(--coral)` etc., and shadcn `hsl(var(--primary))` etc.). Global styles in `frontend/src/styles/`. Svelte scoped `<style>` blocks get a hash-based attribute selector — they win over unscoped global rules of the same specificity.

**Acceptance criteria:** All three of these must pass clean (zero errors, zero warnings) at the end:

```bash
just lint-frontend      # biome check src/ — lints JS/TS/Svelte
just format-frontend    # biome check --write + biome format --write — auto-fixes, run and re-check
just check-frontend     # svelte-kit sync && svelte-check — TypeScript type safety
```

**Verification protocol per task:**
- CSS-only tasks (0–4, 7–9, 15): no type risk; `just lint-frontend` is sufficient.
- `.svelte`-modifying tasks (5, 6, 10–14, 16): run all three checks after the visual verify step, before committing. If `format-frontend` modifies files, stage the formatting changes and include them in the commit.

---

## Phase 1 — Delete Dead CSS

### Task 0: Remove dark mode remnants (app is light-mode only)

**Files:**
- Modify: `frontend/src/styles/tokens.css`
- Modify: `frontend/src/lib/components/texts/StaleTokenBanner.svelte`
- Note: shadcn UI components (`badge.svelte`, `input.svelte`, `button.svelte`) contain `dark:` Tailwind classes from the original copy-paste. These are **harmless** (`.dark` is never applied to the document) — skip them unless a full shadcn refresh is planned.

- [ ] **Step 1: Verify `.dark` is never applied anywhere**

```bash
rg -rn 'class.*\bdark\b|classList.*dark|document.*dark|toggleDark\|darkMode' frontend/src/ --type svelte --type ts
```

Expected: zero results (confirming no JS ever adds `.dark` class to the document).

- [ ] **Step 2: Remove the dead dark variant from tokens.css**

In `frontend/src/styles/tokens.css`, delete line 6:

```css
@custom-variant dark (&:is(.dark *));
```

- [ ] **Step 3: Remove the dark override from StaleTokenBanner.svelte**

In `frontend/src/lib/components/texts/StaleTokenBanner.svelte`, delete the block:

```css
:global(.dark) .stale-banner {
	background: hsl(38 60% 12%);
	border-color: hsl(38 60% 28%);
	color: hsl(38 80% 70%);
}
```

- [ ] **Step 4: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/styles/tokens.css \
        frontend/src/lib/components/texts/StaleTokenBanner.svelte
git commit -m "style: remove dark mode variant and dark overrides (app is light-mode only)"
```

---

### Task 1: Purge dead animation keyframes and utility classes

**Files:**
- Modify: `frontend/src/styles/animations.css`

Dead items: `@keyframes verse-word-in`, `@keyframes scale-in`, `@keyframes slide-in-left`, `.animate-scale-in`, `.animate-slide-in-left`, `.animate-fade-in` (the class — the keyframe `fade-in` is kept as it's used in the `prefers-reduced-motion` block).

- [ ] **Step 1: Verify nothing uses the dead classes**

```bash
cd frontend
rg -rn 'animate-scale-in|animate-slide-in-left|animate-fade-in|verse-word-in' src/ --type svelte
```

Expected: zero results.

- [ ] **Step 2: Delete the dead content from animations.css**

Remove these blocks from `frontend/src/styles/animations.css`:

```css
@keyframes scale-in {
	from {
		opacity: 0;
		transform: scale(0.97);
	}
	to {
		opacity: 1;
		transform: scale(1);
	}
}

@keyframes slide-in-left {
	from {
		opacity: 0;
		transform: translateX(-8px);
	}
	to {
		opacity: 1;
		transform: translateX(0);
	}
}

/* Verse word materialise — softer blur-to-clear + small rise */
@keyframes verse-word-in {
	from {
		opacity: 0;
		transform: translateY(5px);
		filter: blur(2px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
		filter: blur(0);
	}
}
```

Also remove these utility classes (lines ~76–91 in the current file):

```css
.animate-fade-in {
	animation: fade-in 200ms ease-out both;
}

.animate-scale-in {
	animation: scale-in 220ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.animate-slide-in-left {
	animation: slide-in-left 240ms cubic-bezier(0.16, 1, 0.3, 1) both;
}
```

Also update the `prefers-reduced-motion` block — remove `.animate-fade-in`, `.animate-scale-in`, `.animate-slide-in-left` from the selector list. Final block:

```css
@media (prefers-reduced-motion: reduce) {
	.animate-slide-up,
	.stagger-children > *,
	.page-enter {
		animation: fade-in 100ms ease-out both;
	}
}
```

- [ ] **Step 3: Verify `animations.css` still contains the live rules**

```bash
rg 'slide-up|stagger-children|page-enter' frontend/src/styles/animations.css
```

Expected: matches for `.animate-slide-up`, `.stagger-children`, `.page-enter`.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/animations.css
git commit -m "style: remove dead animation keyframes and utility classes"
```

---

### Task 2: Delete the legacy layout block from layout.css

**Files:**
- Modify: `frontend/src/styles/layout.css`

The file has a large `LEGACY — preserved for backward compat during page migration` section. Every class in it is dead — components use the Busatan redesign classes instead.

- [ ] **Step 1: Locate the legacy block boundaries**

```bash
grep -n 'LEGACY\|app-shell-legacy\|sidebar-legacy\|page-roots\|page-root-detail\|page-words\|page-create-root\|page-create-word\|page-home\b' frontend/src/styles/layout.css
```

Find the line where the legacy block starts (the `/* LEGACY` comment) and the line where it ends (before the next non-legacy section).

- [ ] **Step 2: Verify none of the legacy classes are used**

```bash
rg -rn 'app-shell-legacy|sidebar-legacy|sidebar-nav-link|page-roots\b|roots-page-header|roots-toolbar|page-root-detail|root-detail-back|root-info\b|root-word-family\b|page-create-root|page-words\b|words-page-header|words-toolbar|page-create-word|page-home\b|page-home-header|word-detail-back|word-info\b|word-related\b|word-annotations\b|word-example-item' frontend/src/ --type svelte
```

Expected: zero results.

- [ ] **Step 3: Delete the entire legacy block**

Delete from the `/* LEGACY` comment header down through the end of the legacy section. Do not delete any Busatan section content that follows.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/layout.css
git commit -m "style: remove legacy layout CSS block (all classes unused since Busatan redesign)"
```

---

### Task 3: Delete dead Busatan classes from layout.css

**Files:**
- Modify: `frontend/src/styles/layout.css`

Dead items in the active Busatan section:

- `.mastery-bars`, `.mastery-bar-row`, `.mastery-bar-label`, `.mastery-bar-track`, `.mastery-bar-fill`, `.mbf-mastered`, `.mbf-known`, `.mbf-familiar`, `.mbf-learning`, `.mastery-bar-count` (entire "Text side panel - mastery bars" block)
- `.word-fam-grid`, `.word-fam-chip`, `.wfc-mastered`, `.wfc-known`, `.wfc-familiar`, `.wfc-learning`, `.wfc-ar`, `.wfc-en`
- `.pronunciation-badge` (and `:hover`)
- `.dict-links-manual-toggle`
- `.dict-links-header`, `.dict-links-title`, `.dict-links-header-actions`, `.dict-badges`, `.dict-badge`, `.dict-badge-link`, `.dict-badge-delete`, all `.dict-badge-*` variant classes, `.dict-links-empty`
- `.c-sage`, `.c-outline`
- `.root-letter-tile`, `.root-letter-tile::after`, `.root-letter-tile:hover::after`, `.root-letter-tr`, `.root-sep`, `.root-letters`
- `.word-hero-chips`

- [ ] **Step 1: Verify each group is unused**

```bash
rg -rn 'mastery-bars|mastery-bar-row|mastery-bar-label|mastery-bar-track|mastery-bar-fill|mbf-' frontend/src/ --type svelte
rg -rn 'word-fam-grid|word-fam-chip|wfc-' frontend/src/ --type svelte
rg -rn 'pronunciation-badge|dict-links-manual-toggle|dict-badge\b|dict-badge-|dict-links-header|dict-badges\b|dict-links-empty' frontend/src/ --type svelte
rg -rn '\"c-sage\"|\"c-outline\"' frontend/src/ --type svelte
rg -rn 'root-letter-tile|root-letter-tr|root-sep\b|root-letters\b' frontend/src/ --type svelte
rg -rn 'word-hero-chips' frontend/src/ --type svelte
```

Expected: zero results for all.

- [ ] **Step 2: Delete each dead block from layout.css**

Search for and delete each block. Use `grep -n '<classname>' frontend/src/styles/layout.css` to locate line numbers, then delete the block.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/styles/layout.css
git commit -m "style: remove dead Busatan layout classes (mastery bars, dict-badge system, word-fam-chip, etc.)"
```

---

### Task 4: Delete dead semantic.css classes

**Files:**
- Modify: `frontend/src/styles/semantic.css`

Dead: `.mastery-new`, `.mastery-learning`, `.mastery-familiar`, `.mastery-known`, `.mastery-mastered`, all `.difficulty-*`, all `.dialect-*`.

Note: `.btn-outline-danger` and its `:hover` are **live** — `TokenEditor.svelte` and `SentenceEditor.svelte` use them. Do not delete.

The mastery classes will be reintroduced with correct values in Task 9 (they currently use undefined `var(--bg-dark)` etc. tokens). Delete them now so the dead definitions are gone before the correct ones are added.

- [ ] **Step 1: Verify the dead classes are unused**

```bash
rg -rn 'mastery-new|mastery-learning|mastery-familiar|mastery-known|mastery-mastered' frontend/src/ --type svelte
rg -rn 'difficulty-beginner|difficulty-intermediate|difficulty-advanced' frontend/src/ --type svelte
rg -rn 'dialect-msa|dialect-tunisian|dialect-moroccan|dialect-egyptian|dialect-gulf|dialect-levantine|dialect-iraqi' frontend/src/ --type svelte
```

Expected: zero results.

- [ ] **Step 2: Delete from semantic.css**

Delete the `/* ── Mastery levels ── */` block (`.mastery-new` through `.mastery-mastered`), the `/* ── Difficulty ── */` block, and the `/* ── Dialect ── */` block. Keep `/* ── Buttons ── */` and `.btn-outline-danger`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/styles/semantic.css
git commit -m "style: remove dead mastery/difficulty/dialect classes from semantic.css"
```

---

## Phase 2 — Bug Fixes

### Task 5: Fix SessionSummary broken CSS custom properties

**Files:**
- Modify: `frontend/src/lib/components/training/SessionSummary.svelte`

The component scoped styles reference `var(--color-bg-secondary)`, `var(--color-text-secondary)`, `var(--color-bg-tertiary)` — these variables **do not exist** in `tokens.css`. Stat cards and promotion items render with no background color.

- [ ] **Step 1: Reproduce the bug**

Start the dev server (`just frontend`), navigate to a completed training session. The stat cards (correct/incorrect/accuracy) and the promotions section should have no visible background.

- [ ] **Step 2: Fix the CSS variables**

In `frontend/src/lib/components/training/SessionSummary.svelte`, update the `<style>` block:

```css
.stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    border-radius: 0.5rem;
    background-color: hsl(var(--muted));   /* was: var(--color-bg-secondary) */
}

.stat-label {
    font-size: 0.875rem;
    color: hsl(var(--muted-foreground));   /* was: var(--color-text-secondary) */
    text-align: center;
}

.promotions-section {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 1rem;
    border-radius: 0.5rem;
    background-color: hsl(var(--muted));   /* was: var(--color-bg-secondary) */
}

.promotion-item {
    padding: 0.75rem;
    background-color: hsl(var(--muted) / 0.5);   /* was: var(--color-bg-tertiary) */
    border-radius: 0.375rem;
    font-size: 0.95rem;
}
```

Keep all other rules unchanged.

- [ ] **Step 3: Verify visually**

Reload the session summary page. Stat cards should now have a visible muted background. Promotion items should show a lighter muted background.

- [ ] **Step 4: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib/components/training/SessionSummary.svelte
git commit -m "fix: replace undefined CSS vars in SessionSummary with valid shadcn tokens"
```

---

### Task 6: Fix FullTextPanel scoped/global CSS naming conflict

**Files:**
- Modify: `frontend/src/lib/components/texts/FullTextPanel.svelte`
- Modify: `frontend/src/styles/layout.css`

**Problem:** `FullTextPanel.svelte` has a `<style>` block with class names that diverge from global `layout.css` equivalents. The Svelte scoped `.full-text-panel` overrides the global one (different `margin-top`). The content classes `.full-text-arabic`/`.full-text-transliteration`/`.full-text-translation` don't exist globally — global has `.full-text-ar`/`.full-text-tr`/`.full-text-en`. The heading class `.full-text-heading` has no global equivalent.

**Fix:** Delete the scoped style block. Rename HTML classes to match globals. Add `.full-text-heading` to the global `layout.css` full-text block.

- [ ] **Step 1: Add `.full-text-heading` to layout.css**

In `frontend/src/styles/layout.css`, find the `/* Full text panel */` section (around line 1159). After the `.full-text-en` block, add:

```css
.full-text-heading {
	font-size: 0.8125rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.06em;
	color: var(--ink-ghost);
	margin-bottom: 1rem;
}
```

- [ ] **Step 2: Update FullTextPanel.svelte HTML**

In `frontend/src/lib/components/texts/FullTextPanel.svelte`, replace the template section:

```svelte
{#if text.body || text.transliteration}
	<div class="full-text-panel">
		<h2 class="full-text-heading">Full text</h2>

		{#if text.body}
			<div class="full-text-ar arabic-text">{text.body}</div>
		{/if}

		{#if text.transliteration}
			<div class="full-text-tr transliteration">{text.transliteration}</div>
		{/if}

		{#if text.translation}
			<div class="full-text-en">{text.translation}</div>
		{/if}
	</div>
{/if}
```

Changes: `full-text-arabic` → `full-text-ar`, `full-text-transliteration` → `full-text-tr`, `full-text-translation` → `full-text-en`.

- [ ] **Step 3: Delete the entire `<style>` block from FullTextPanel.svelte**

Remove everything from `<style>` through `</style>` at the end of the file.

- [ ] **Step 4: Verify visually**

Navigate to a text detail page that has body/transliteration/translation. The full-text panel should render correctly with Arabic text right-aligned in Noto Naskh and transliteration in Spectral italic.

- [ ] **Step 5: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/lib/components/texts/FullTextPanel.svelte frontend/src/styles/layout.css
git commit -m "fix: align FullTextPanel to global CSS classes, remove scoped style override"
```

---

## Phase 3 — Add Missing Global Patterns

### Task 7: Add `.btn-ghost`, `.toggle-group`/`.toggle-btn`, and `.form-error` (shadcn flavor) to layout.css

**Files:**
- Modify: `frontend/src/styles/layout.css`

The existing `.btn` system in layout.css uses old Busatan tokens (`var(--coral)`, `var(--ink-mid)`). These new additions use shadcn tokens (`hsl(var(--primary))`, `hsl(var(--muted))`) to serve components in the shadcn context. Both can coexist.

- [ ] **Step 1: Add the new classes to layout.css**

Find the `.btn-full` block (around line 291) in layout.css. After it, insert:

```css
/* Ghost variant (shadcn-token context) */
.btn-ghost {
	padding: 0.375rem 0.875rem;
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	background: transparent;
	color: hsl(var(--muted-foreground));
	border: 1px solid hsl(var(--border));
	cursor: pointer;
	font-family: inherit;
}
.btn-ghost:hover { color: hsl(var(--foreground)); background: hsl(var(--muted)); }

/* Toggle / segmented control */
.toggle-group { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.toggle-btn {
	padding: 0.25rem 0.625rem;
	border-radius: 0.375rem;
	font-size: 0.75rem;
	border: 1px solid hsl(var(--border));
	background: transparent;
	color: hsl(var(--muted-foreground));
	cursor: pointer;
	font-family: inherit;
}
.toggle-btn:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }
.toggle-btn-active {
	background: hsl(var(--primary) / 0.15);
	border-color: hsl(var(--primary) / 0.5);
	color: hsl(var(--primary));
}

/* shadcn-token form error (supplements the old .form-error which uses var(--danger)) */
.form-error-msg {
	font-size: 0.875rem;
	color: hsl(var(--destructive));
}
```

Note: We call the new error class `.form-error-msg` to avoid colliding with the existing `.form-error` which uses `var(--danger)`.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/styles/layout.css
git commit -m "style: add btn-ghost, toggle-group/toggle-btn, form-error-msg to global layout"
```

---

### Task 8: Add drawer shell to layout.css

**Files:**
- Modify: `frontend/src/styles/layout.css`

Both `AnnotationDrawer.svelte` and `VocabLookupDrawer.svelte` implement the same fixed right-panel pattern. Extract the shell to globals; components keep only their unique structural details.

- [ ] **Step 1: Add global drawer shell**

Find a logical section in layout.css (e.g., after the sidebar block or at end of Busatan section). Add:

```css
/* ── Drawer shell ── */
.drawer-backdrop {
	position: fixed;
	inset: 0;
	background: hsl(0 0% 0% / 0.35);
	z-index: 40;
	cursor: default;
}
.drawer {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	width: 360px;
	background: hsl(var(--background));
	border-left: 1px solid hsl(var(--border));
	z-index: 50;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}
.drawer-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 1rem 1.25rem 0.875rem;
	border-bottom: 1px solid hsl(var(--border) / 0.6);
	gap: 0.75rem;
	flex-shrink: 0;
}
.drawer-body {
	flex: 1;
	overflow-y: auto;
	padding: 0.875rem 1.25rem;
	display: flex;
	flex-direction: column;
}
.drawer-footer {
	border-top: 1px solid hsl(var(--border));
	padding: 0.75rem 1.25rem;
	flex-shrink: 0;
}
.drawer-close {
	flex-shrink: 0;
	width: 1.75rem;
	height: 1.75rem;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 0.375rem;
	background: none;
	border: none;
	cursor: pointer;
	font-size: 1.25rem;
	line-height: 1;
	color: hsl(var(--muted-foreground));
}
.drawer-close:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/styles/layout.css
git commit -m "style: add global drawer shell pattern to layout.css"
```

---

### Task 9: Add `.banner-warning` and mastery classes to semantic.css

**Files:**
- Modify: `frontend/src/styles/semantic.css`

- [ ] **Step 1: Add banner-warning and mastery classes**

Append to `frontend/src/styles/semantic.css`:

```css
/* ── Mastery levels ──
   Apply to any badge/pill alongside a border-radius class.
   Requires a 1px border so border-color is visible. */

.mastery-new {
	background: hsl(var(--muted));
	color: hsl(var(--muted-foreground));
	border-color: hsl(var(--border));
}
.mastery-learning {
	background: hsl(40 90% 60% / 0.2);
	color: hsl(40 70% 40%);
	border-color: hsl(40 70% 60% / 0.4);
}
.mastery-known {
	background: hsl(210 80% 60% / 0.2);
	color: hsl(210 60% 40%);
	border-color: hsl(210 60% 55% / 0.4);
}
.mastery-mastered {
	background: hsl(140 60% 40% / 0.15);
	color: hsl(140 50% 32%);
	border-color: hsl(140 50% 45% / 0.4);
}

/* ── Warning banner ── */
.banner-warning {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	padding: 0.5rem 0.75rem;
	background: hsl(38 92% 95%);
	border: 1px solid hsl(38 80% 80%);
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	color: hsl(38 80% 28%);
	flex-wrap: wrap;
}
```

App is **light mode only** — no dark mode variant needed. Do not add any `.dark` selector.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/styles/semantic.css
git commit -m "style: add mastery level classes and banner-warning to semantic.css"
```

---

## Phase 4 — Update Components to Use Globals

### Task 10: Update AnnotationForm.svelte to use global classes

**Files:**
- Modify: `frontend/src/lib/components/annotations/AnnotationForm.svelte`

The component's `<style>` block defines `.form-field`, `.form-label`, `.type-btn`/`.type-btn-active`, `.btn-primary`, `.btn-ghost` — all now in globals.

- [ ] **Step 1: Read the component's current HTML**

```bash
awk '!/<style/,/<\/style>/{print}' frontend/src/lib/components/annotations/AnnotationForm.svelte | head -100
```

Identify: where `.type-btn`/`.type-btn-active` classes appear (they will become `.toggle-btn`/`.toggle-btn-active`), where `.form-field`/`.form-label` appear, where `.btn-ghost`/`.btn-primary` appear.

- [ ] **Step 2: Update class names in the template**

In the HTML template:
- `.type-selector` → `.toggle-group`
- `.type-btn` → `.toggle-btn`
- `.type-btn-active` → `.toggle-btn-active` (this is dynamically applied, verify the conditional expression)
- `.form-field` — already matches global name (no change to HTML needed)
- `.form-label` — already matches global name (no change needed)
- `.btn-ghost` — already matches new global name (no change needed)
- `.btn-primary` — the global `.btn-primary` uses `var(--coral)` (old tokens). The component's local version uses `hsl(var(--primary))`. Keep the scoped `.btn-primary` in the style block OR rename to `.btn-primary-sh` in both HTML and style.

Recommendation: rename local `.btn-primary` to `.btn-submit` (since it's only the submit button of this form) to avoid collision.

- [ ] **Step 3: Delete now-redundant rules from the `<style>` block**

Remove:
- `.form-field { ... }` — now global
- `.form-label { ... }` — now global
- `.type-selector { ... }` — was renamed to `.toggle-group`, now global
- `.type-btn { ... }` and `.type-btn:hover { ... }` — now `.toggle-btn` in global
- `.type-btn-active { ... }` — now `.toggle-btn-active` in global
- `.btn-ghost { ... }` and `.btn-ghost:hover { ... }` — now global
- `.annotation-form { ... }` — check if this is used; if only used in this component's root, keep it

Keep: `.form-textarea { ... }`, `.form-actions { ... }`, `.btn-submit { ... }` (renamed from btn-primary, keep local).

- [ ] **Step 4: Verify no visual regression**

Open a text detail page, click on a word annotation area. Open the annotation drawer/form. Confirm: type selector buttons highlight correctly on click, form labels render, ghost cancel button renders, submit button renders.

- [ ] **Step 5: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/lib/components/annotations/AnnotationForm.svelte
git commit -m "refactor: AnnotationForm uses global toggle-group, form-field, btn-ghost classes"
```

---

### Task 11: Update AnnotationDrawer + VocabLookupDrawer to use global drawer shell

**Files:**
- Modify: `frontend/src/lib/components/annotations/AnnotationDrawer.svelte`
- Modify: `frontend/src/lib/components/texts/VocabLookupDrawer.svelte`

**AnnotationDrawer** — current classes map to globals:
- `.drawer-backdrop` → `.drawer-backdrop` (global, delete local)
- `.annotation-drawer` → `.drawer` (global, rename in HTML + delete local rule)
- `.drawer-header` → `.drawer-header` (global, delete local)
- `.drawer-close` → `.drawer-close` (global, delete local)
- `.drawer-body` → `.drawer-body` (global, delete local — note: global has `gap: 0`, local has `gap: 0`, OK)
- Keep local: `.drawer-anchor`, `.drawer-form-section`, `.drawer-add-btn` (component-specific)

**VocabLookupDrawer** — map:
- `.vocab-backdrop` → `.drawer-backdrop` (global)
- `.vocab-drawer` → `.drawer drawer--narrow` — the vocab drawer is 340px vs global 360px. Two options: (a) add modifier `.drawer--narrow { width: 340px; }` to layout.css, (b) keep a single local override rule `width: 340px`. **Use option (b) for simplicity** — add one local rule `width: 340px` on the element, delete the full `.vocab-drawer` rule.
- `.vocab-header` → `.drawer-header` (global, rename HTML + delete local)
- `.vocab-body` → `.drawer-body` (global, rename HTML + delete local)
- `.vocab-footer` → `.drawer-footer` (global, rename HTML + delete local)
- `.vocab-close` → `.drawer-close` (global, rename HTML + delete local)
- Keep local: `.vocab-header-input`, `.vocab-state-msg`, `.vocab-not-found-msg`, `.vocab-card-*`, `.vocab-mastery-badge`, `.vocab-mastery-*`, `.vocab-open-link`, `.vocab-annotate-btn` (component-specific — mastery colors will be replaced in Task 12)

- [ ] **Step 1: Update AnnotationDrawer.svelte**

In the HTML, rename `class="annotation-drawer"` to `class="drawer"`. All other class names already match globals.

Delete from `<style>`: `.drawer-backdrop`, `.annotation-drawer`, `.drawer-header`, `.drawer-close`, `.drawer-body` blocks. Keep: `.drawer-anchor`, `.drawer-form-section`, `.drawer-add-btn`.

- [ ] **Step 2: Update VocabLookupDrawer.svelte**

In the HTML:
- `class="vocab-backdrop"` → `class="drawer-backdrop"`
- `class="vocab-drawer"` → `class="drawer"` (then add inline `style="width:340px"` OR keep a minimal local rule)
- `class="vocab-header"` → `class="drawer-header"`
- `class="vocab-body"` → `class="drawer-body"`
- `class="vocab-footer"` → `class="drawer-footer"`
- `class="vocab-close"` → `class="drawer-close"`

Delete the corresponding rule blocks from `<style>`. Keep all `vocab-card-*`, `vocab-state-msg`, `vocab-mastery-*`, etc.

For the 340px width difference, add a single-line local rule:

```css
/* vocab drawer is narrower than the default 360px shell */
:global(.drawer).vocab-override { width: 340px; }
```

Or simpler: just add a `style="width:340px"` attribute to the drawer div. Use whichever the executor finds cleaner.

- [ ] **Step 3: Visual check**

Open annotation drawer (click annotate on a word). Open vocab lookup (click a token). Both should slide in from the right correctly.

- [ ] **Step 4: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib/components/annotations/AnnotationDrawer.svelte \
        frontend/src/lib/components/texts/VocabLookupDrawer.svelte
git commit -m "refactor: use global drawer shell in AnnotationDrawer and VocabLookupDrawer"
```

---

### Task 12: Update VocabLookupDrawer mastery badges to use global classes

**Files:**
- Modify: `frontend/src/lib/components/texts/VocabLookupDrawer.svelte`

The component has local `.vocab-mastery-new/learning/known/mastered` that duplicate what `semantic.css` now defines as `.mastery-new/learning/known/mastered`.

- [ ] **Step 1: Update the HTML mastery badge expression**

Find the line that dynamically applies `vocab-mastery-{level}` (it will be something like `class="vocab-mastery-badge vocab-mastery-{word.masteryLevel}"`). Change to:

```svelte
class="vocab-mastery-badge mastery-{word.masteryLevel}"
```

Verify the mastery level values from the API match the suffix names: `new`, `learning`, `known`, `mastered`. Check `frontend/src/lib/api/types.gen.ts` for the `MasteryLevel` enum.

- [ ] **Step 2: Delete the scoped mastery color rules**

Delete from `<style>` in VocabLookupDrawer.svelte:
```css
.vocab-mastery-new      { ... }
.vocab-mastery-learning { ... }
.vocab-mastery-known    { ... }
.vocab-mastery-mastered { ... }
```

Keep `.vocab-mastery-badge { ... }` (the base pill styling — display, padding, border-radius, font-size).

- [ ] **Step 3: Verify**

Open vocab lookup on a word with a known mastery level. The badge should still render with the correct color.

- [ ] **Step 4: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib/components/texts/VocabLookupDrawer.svelte
git commit -m "refactor: VocabLookupDrawer uses global mastery level classes"
```

---

### Task 13: Update StaleTokenBanner to use .banner-warning

**Files:**
- Modify: `frontend/src/lib/components/texts/StaleTokenBanner.svelte`

- [ ] **Step 1: Update the root element class**

In `StaleTokenBanner.svelte`, the root `<div>` has `class="stale-banner"`. Change to `class="banner-warning"`.

- [ ] **Step 2: Update child class names in HTML**

`class="stale-banner-text"` → `class="banner-text"` (or keep as-is and just delete `.stale-banner-text` from style if it's purely flex: 1 min-width: 0)
`class="stale-banner-actions"` → `class="banner-actions"` (or keep as-is)

Check if `.stale-banner-text` and `.stale-banner-actions` have enough uniqueness to warrant keeping as local named classes. They do (flex layout glue) — rename them to `banner-text` and `banner-actions` in both HTML and style.

- [ ] **Step 3: Delete the scoped root styles**

Delete from `<style>`:
```css
.stale-banner { ... }
:global(.dark) .stale-banner { ... }
```

Keep the renamed `.banner-text` and `.banner-actions` rules (they are structural glue).

The `<style>` block after cleanup:
```css
.banner-text {
	flex: 1;
	min-width: 0;
}
.banner-actions {
	display: flex;
	gap: 0.25rem;
}
```

- [ ] **Step 4: Verify**

Open a text detail that has stale tokens (or temporarily trigger the condition). The amber warning banner should display correctly.

- [ ] **Step 5: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/lib/components/texts/StaleTokenBanner.svelte
git commit -m "refactor: StaleTokenBanner uses global banner-warning class"
```

---

### Task 14: Update training/+page.svelte to use global patterns

**Files:**
- Modify: `frontend/src/routes/training/+page.svelte`

Dead/duplicate scoped patterns in this page: `.dist-badge` (= `.chip c-muted`), `.field`/`label` (= `.form-field`/`form-label`), `.mode-buttons` (= `.toggle-group`), `.error` (= `.form-error-msg`).

- [ ] **Step 1: Read the template to understand which elements use each class**

```bash
awk '!/<style/,/<\/style>/{print}' frontend/src/routes/training/+page.svelte
```

- [ ] **Step 2: Update class names in the HTML template**

- `.dist-badge` → `chip c-muted` (global chip system, no local rule needed)
- Each `.field` wrapper div → `form-field`
- `label` and `.field-label` → `form-label` (check if it's a bare `<label>` or a `<div>`)
- `.mode-buttons` container → `toggle-group`; each mode button → `toggle-btn` + conditionally `toggle-btn-active`
- `.error` span → `form-error-msg`

Note: The mode buttons are likely shadcn `<Button>` components. If so, check how `class=` is applied — shadcn buttons merge classes. If they're plain `<button>` elements, the above applies directly.

- [ ] **Step 3: Delete the now-redundant scoped rules**

Delete from the `<style>` block:
```css
.dist-badge { ... }
.field { ... }
label, .field-label { ... }
.mode-buttons { ... }
.error { ... }
```

Keep: `.training-setup { ... }`, `.distribution { ... }`, `.setup-form { ... }`, `input[type='range'] { ... }`, `.range-hints { ... }`.

- [ ] **Step 4: Verify**

Navigate to `/training`. Check: mastery distribution badges render (muted pills), form fields have labels, mode toggle buttons highlight when selected, error message (if any) appears in destructive color.

- [ ] **Step 5: Run checks**

```bash
just lint-frontend
just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/routes/training/+page.svelte
git commit -m "refactor: training setup page uses global chip, form-field, toggle-group classes"
```

---

### Task 15: Add `.morph-select`, `.chip-delete`, `.input-ar-compact`, and `.spinner` to globals

**Files:**
- Modify: `frontend/src/styles/layout.css`
- Modify: `frontend/src/styles/animations.css`

These four patterns appear in 2–3 of the new `word/` components with **identical definitions** — they must be extracted before the components are updated.

**Context on existing `.form-input.ar-input`:** layout.css already has an Arabic input modifier (uses Amiri at 1.25rem — a large display font). The new components need a compact Arabic input (Noto Naskh at 1rem for inline forms). Add a separate `.input-ar` class rather than reusing `.form-input.ar-input`.

- [ ] **Step 1: Add to layout.css**

Find the btn/chip section in layout.css. After `.btn-full`, add the compact select and chip-delete. After the form input section (around `.form-input.ar-input`), add the compact Arabic input.

```css
/* Compact inline select — for inline editing strips */
.select-compact {
	font-size: 0.8rem;
	padding: 0.25rem 0.5rem;
	border: 1px solid var(--border, #e2e8f0);
	border-radius: 6px;
	background: var(--white, #fff);
	color: var(--ink, #1a1a1a);
	height: 2rem;
	font-family: inherit;
}

/* Delete button inside a chip/pill */
.chip-delete {
	background: none;
	border: none;
	cursor: pointer;
	color: var(--coral);
	font-size: 0.9rem;
	line-height: 1;
	padding: 0 0.125rem;
}
.chip-delete:disabled { opacity: 0.5; cursor: not-allowed; }

/* Compact Arabic text input for inline forms */
.input-ar {
	font-family: 'Noto Naskh Arabic', serif;
	font-size: 1rem;
	direction: rtl;
	text-align: right;
}
```

Note: The class is `.select-compact` (not `.morph-select`) — name it for its role, not the component it came from.

- [ ] **Step 2: Add spinner to animations.css**

At the end of `frontend/src/styles/animations.css`, add:

```css
@keyframes spin {
	to { transform: rotate(360deg); }
}

.spinner {
	width: 1.75rem;
	height: 1.75rem;
	border: 3px solid var(--border);
	border-top-color: var(--olive);
	border-radius: 50%;
	animation: spin 0.7s linear infinite;
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/styles/layout.css frontend/src/styles/animations.css
git commit -m "style: add select-compact, chip-delete, input-ar, spinner to global design system"
```

---

### Task 16: Update word/* components to use global patterns

**Files:**
- Modify: `frontend/src/lib/components/word/WordEnrichDrawer.svelte`
- Modify: `frontend/src/lib/components/word/WordMorphologyStrip.svelte`
- Modify: `frontend/src/lib/components/word/WordPluralChips.svelte`
- Modify: `frontend/src/lib/components/word/WordRelationsPanel.svelte`

**Depends on:** Task 8 (drawer shell in layout.css), Task 7 (form-field/form-error-msg), Task 15 (select-compact, chip-delete, input-ar, spinner).

#### WordEnrichDrawer.svelte

This component re-implements the full drawer shell. Map to globals:

| Local class | Global |
|---|---|
| `.drawer-backdrop` | `.drawer-backdrop` (global) |
| `.drawer-panel` | `.drawer` (global) + local `style="width:26rem; max-width:95vw"` or local override rule |
| `.drawer-header` | `.drawer-header` (global) |
| `.drawer-close` | `.drawer-close` (global) |
| `.drawer-body` | `.drawer-body` (global) |
| `.drawer-field` | `.form-field` (global, Task 7) |
| `.drawer-field-label` | `.form-label` — but check: this uses a `<label>` with checkbox inside it; the global `.form-label` is a plain label element. If visual conflicts exist, keep scoped. |
| `.drawer-title` | keep scoped (component-specific heading) |
| `.spinner` | `.spinner` (global, Task 15) — delete local `@keyframes spin` and `.spinner` rule |
| `.drawer-loading` | keep scoped (component-specific layout) |
| `.drawer-notice/error/success` | keep scoped for now (semantic status boxes — candidate for a future semantic.css addition) |
| `.drawer-plural-ar`, `.drawer-relation-ar` | delete; add `arabic-text` class to those elements in HTML |
| `.drawer-textarea` | keep scoped (component-specific textarea styling) |
| `.drawer-actions` | keep scoped (component-specific action row) |

- [ ] **Step 1: Update WordEnrichDrawer.svelte HTML**

In the template:
- `class="drawer-backdrop"` → `class="drawer-backdrop"` (no HTML change, class already matches global)
- `class="drawer-panel"` → `class="drawer"`, add `style="width:26rem;max-width:95vw"` (or a local `.drawer-wide` override)
- `class="drawer-header"` → `class="drawer-header"` (no HTML change)
- `class="drawer-close"` → `class="drawer-close"` (no HTML change)
- `class="drawer-body"` → `class="drawer-body"` (no HTML change)
- Each `class="drawer-field"` → `class="form-field"`
- On the `<span>` elements with Arabic text inside `.drawer-plural-row` and `.drawer-relation-row`: add `class="arabic-text"` to the Arabic spans, remove `.drawer-plural-ar` and `.drawer-relation-ar` from HTML

- [ ] **Step 2: Update WordEnrichDrawer.svelte `<style>` block**

Delete: `.drawer-backdrop`, `.drawer-panel`, `.drawer-header`, `.drawer-close`, `.drawer-body`, `.drawer-field`, `.drawer-plural-ar`, `.drawer-relation-ar`, `.spinner`, `@keyframes spin`

Keep: `.drawer-title`, `.drawer-loading`, `.drawer-notice`, `.drawer-error`, `.drawer-success`, `.drawer-field-label`, `.drawer-textarea`, `.drawer-plural-row`, `.drawer-plural-type`, `.drawer-relation-row`, `.drawer-relation-type`, `.drawer-actions`

Replace inline error styles `style="color:var(--coral);font-size:0.8rem;"` with `class="form-error-msg"` on the error `<p>` or `<span>` elements.

- [ ] **Step 3: Update WordMorphologyStrip.svelte**

In HTML: change `class="morph-select"` → `class="select-compact"` (both `<select>` elements).

In `<style>`: delete `.morph-select { ... }` entirely. Keep all `.morph-strip*` and `.morph-edit-btn` rules.

- [ ] **Step 4: Update WordPluralChips.svelte**

In HTML:
- `class="morph-select"` → `class="select-compact"`
- `class="chip-delete"` → `class="chip-delete"` (no HTML change — class name matches global)
- `class="example-input-ar"` → `class="form-input input-ar"` (compose global form input + arabic modifier), remove the `width: 10rem` from the scoped rule or keep it as a local override
- `.plural-chip-ar` span: add `arabic-text` to class list; the font/size will come from global. Keep `.plural-chip-ar` only if you need to preserve its specific `font-size: 1rem; line-height: 1.4` (check whether `.arabic-text` sets these — if so, delete `.plural-chip-ar` entirely)

In `<style>`: delete `.morph-select`, `.chip-delete`, `.example-input-ar`. Keep `.plurals-section`, `.plurals-chips`, `.plural-chip`, `.plural-chip-ar` (if still needed for line-height), `.plural-chip-type`, `.plural-add-form`.

Replace inline error `style="font-size:0.75rem;color:var(--coral);"` with `class="form-error-msg"`.

- [ ] **Step 5: Update WordRelationsPanel.svelte**

In HTML:
- `class="morph-select"` → `class="select-compact"`
- `class="chip-delete"` → `class="chip-delete"` (no HTML change)
- `class="relations-group-label"` → `class="sect-label"` (already global in layout.css)
- `.relation-chip-ar` span: add `arabic-text` to class list; delete `.relation-chip-ar` from style if `.arabic-text` covers the same properties

In `<style>`: delete `.morph-select`, `.chip-delete`, `.relations-group-label`. Keep `.relations-panel`, `.relations-group`, `.relations-chips`, `.relation-chip`, `.relation-chip-ar` (if needed), `.relation-chip-tr`, `.relation-add-form`, `.relation-id-input`.

Replace inline error `style="font-size:0.75rem;color:var(--coral);"` with `class="form-error-msg"`.

- [ ] **Step 6: Visual verification**

Navigate to a word detail page. Check:
- WordMorphologyStrip renders gender/verb pattern chips correctly, edit mode shows select dropdowns
- WordPluralChips shows plural chips with Arabic text, add form works
- WordRelationsPanel shows grouped relations, group labels render as small uppercase labels
- WordEnrichDrawer opens from the AI enrichment button, drawer slides in, spinner shows while loading, suggestions render with Arabic text

- [ ] **Step 7: Commit**

```bash
git add frontend/src/lib/components/word/
git commit -m "refactor: word/* components use global drawer shell, select-compact, chip-delete, arabic-text classes"
```

---

## Self-Review Checklist

- [x] All dead CSS tasks verified with `rg` before deletion
- [x] Bug fixes (SessionSummary, FullTextPanel) are independent of phase 3/4
- [x] Global additions (Tasks 7–9, 15) committed before component updates that depend on them
- [x] Mastery classes deleted (Task 4) before re-added correctly (Task 9) — correct order
- [x] `.btn-outline-danger` explicitly preserved (not deleted in Task 4)
- [x] AnnotationDrawer and VocabLookupDrawer done in same task since they share the drawer shell pattern
- [x] VocabLookupDrawer mastery migration depends on Task 9 — Task 12 comes after
- [x] Task 15 (new globals) must run before Task 16 (word/* component updates)
- [x] `.morph-select` renamed to `.select-compact` in global — name reflects role not origin component
- [x] No placeholders — every step has explicit commands or code
