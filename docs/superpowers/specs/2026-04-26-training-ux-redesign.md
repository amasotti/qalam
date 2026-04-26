# Training UX Redesign

**Date:** 2026-04-26
**Status:** Approved

## Problem

The flashcard training session has several UX failures:
- Card occupies 100vh with a 3:4 aspect ratio — enormous, wastes space
- Revealing the answer replaces the question entirely — user forgets what they were testing themselves on
- No keyboard shortcuts — requires mouse for every action
- No visual progress indicator (text counter only)
- Mastery level, root, notes, examples, and related words are never surfaced — the word is shown as a bare arabic string with no context

## Design Decisions

### Layout: Split revealed (not flip)

Before reveal: question block only. After reveal: question block stays pinned at top (compact), answer expands below. The question never disappears.

Card is contained (max ~560px wide, not full screen height). Progress bar + counter float above the card.

### Information density

**Before reveal (question face):**
- Direction label (`Arabic → Translation` or `Translation → Arabic`)
- Arabic text (large) + transliteration (italic, muted)
- Root chip (e.g. `ك ل م`) — shown if root data is present
- Space / click to reveal

**After reveal:**
- Question row (compact): arabic + transliteration + root chip, pinned
- Answer: translation, large and bold
- Examples (≤ 3): arabic sentence + transliteration + translation
- Notes: amber-tinted block, shown only if non-empty
- Related words: chips showing arabic + translation, labelled by relation type (SYNONYM / ANTONYM / RELATED)
- Action buttons: Correct / Wrong / Skip with keyboard hints

### Keyboard shortcuts

| Key | Action |
|-----|--------|
| `Space` or `Enter` | Reveal (when not revealed) |
| `1` or `Numpad 1` | Correct (after reveal) |
| `2` or `Numpad 2` | Wrong (after reveal) |
| `3` or `Numpad 3` | Skip (after reveal) |

Keys 1/2/3 are ignored before reveal to prevent accidental skips.

### Progress

Thin bar (3px) spanning full card width, fills left-to-right. Counter `N / total` and mode label (`MIXED` etc.) above the bar, small and muted.

---

## Backend Changes

### Extend `TrainingSessionWordResponse`

Add optional fields to be populated from joins at session-load time (no extra API calls during a session):

```
root?         String    resolved from roots table via word.rootId
notes?        String    word.notes
examples[]    { arabic, transliteration?, translation? }   up to 3, from word_examples
relations[]   { relatedWordId, relatedWordArabic, relatedWordTranslation?, relationType }
```

All fields are optional — cards without examples/notes/relations still render correctly.

### Files to change

- `backend/src/main/kotlin/com/tonihacks/qalam/domain/training/TrainingDtos.kt` — add new fields to `TrainingSessionWordResponse` DTO
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/training/TrainingDomain.kt` — add fields to domain model
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedTrainingRepository.kt` — join roots table, fetch examples + relations per word
- `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/TrainingRoutes.kt` — serialisation (if needed)
- `backend/src/main/resources/openapi/documentation.yaml` — update `TrainingSessionWordResponse` schema (source of truth)
- Run `pnpm generate:types` in frontend after yaml is updated

---

## Frontend Changes

### Files to change

- `frontend/src/lib/components/training/FlashCard.svelte` — full rewrite
- `frontend/src/routes/training/[id]/+page.svelte` — add progress bar, pass enriched word data down
- `frontend/src/styles/layout.css` (or inline scoped) — card shell styles

### FlashCard component contract

Props stay the same (`word`, `isPending`, `onresult`). Internal state: `revealed` boolean.

Keyboard handler: `keydown` on `window`, active only while the card is mounted. No global listener leaks — registered and cleaned up inside a `$effect` (Svelte 5 runes, not `onDestroy`).

### Component structure (post-rewrite)

```
FlashCard
  ├── progress bar + counter (passed as props from page)
  ├── question-block (always visible after first render)
  │     arabic + transliteration + root chip
  ├── [if revealed]
  │     answer-block
  │       translation
  │     examples-block (if examples.length > 0)
  │     notes-block (if notes)
  │     relations-block (if relations.length > 0)
  │     action-buttons (Correct / Wrong / Skip + kbd hints)
  └── [if !revealed]
        reveal-button (Space hint)
```

Progress bar and counter move from the page into FlashCard props so the card is fully self-contained visually.

---

## Out of Scope

- Mastery badge on question face (deprioritised — root is more useful)
- Flip animation (adds complexity with no UX gain for keyboard-first flow)
- Lazy-loading word detail mid-session (all data embedded at session creation)
- Session Summary redesign (not requested)
- Any change to the training setup page
