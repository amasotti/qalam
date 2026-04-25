# Annotations Feature Design

**Date:** 2026-04-25
**Status:** Approved

## Problem

The backend has a complete Annotation domain (model, repository, service, routes, migrations) but the frontend has zero annotation UI. Users cannot add, view, or manage annotations on texts. Additionally, the existing implementation carries two vestigial fields (`mastery_level`, `review_flag`) and an inconsistent type enum (`VOCAB`/`STRUCTURE`) that need to be fixed before any UI is built.

## Decisions Made

| Question | Decision |
|---|---|
| Interaction model | Inline type-coded badges on tokens + slide-in drawer |
| Create trigger | Click any token (with or without existing annotation) |
| Span | Single-token only; sentence `notes` field covers multi-token observations |
| Type enum | `VOCABULARY`, `GRAMMAR`, `CULTURAL`, `OTHER` |
| Extra fields | Drop `mastery_level` and `review_flag` entirely |
| Word linking | Search in drawer; annotation surfaces on word detail page |

## Data Model (after changes)

```
annotations
  id            UUID PK
  text_id       UUID FK → texts (cascade delete)
  anchor        VARCHAR  -- the token's Arabic text
  type          VARCHAR CHECK (type IN ('VOCABULARY','GRAMMAR','CULTURAL','OTHER'))
  content       TEXT nullable

annotation_words
  annotation_id UUID FK → annotations (cascade delete)
  word_id       UUID FK → words (cascade delete)
  PRIMARY KEY (annotation_id, word_id)
```

`mastery_level` and `review_flag` are dropped.

## Backend Changes

**New migration** (`V017__fix_annotation_schema.sql`):
- Drop column `mastery_level`
- Drop column `review_flag`
- Drop and recreate `CHECK` constraint with corrected type values

**Code updates:**
- `AnnotationType` enum: `VOCAB → VOCABULARY`, `STRUCTURE → OTHER`
- `Annotation` domain class: remove `masteryLevel`, `reviewFlag`
- `AnnotationService`: remove those params from `create()` and `update()`
- `ExposedAnnotationRepository`: remove column mappings for dropped fields
- `AnnotationRoutes`: update request/response DTOs

OpenAPI spec regenerates automatically at startup → run `pnpm generate:types` after.

## Frontend Architecture

### Store: `src/lib/stores/annotations.ts`

Follows the existing pattern in `words.ts`. Exports:
- `useTextAnnotations(textId)` — TanStack Query, key `['annotations', textId]`
- `useCreateAnnotation()` — mutation, invalidates `['annotations', textId]`
- `useUpdateAnnotation()` — mutation, invalidates `['annotations', textId]`
- `useDeleteAnnotation()` — mutation, invalidates `['annotations', textId]`
- `useAddWordLink()` / `useRemoveWordLink()` — mutations, invalidate annotation + word queries

### New Components (`src/lib/components/annotations/`)

| Component | Purpose |
|---|---|
| `AnnotationBadge.svelte` | Small type-coded chip (G/V/C/O) rendered below a token cell |
| `AnnotationDrawer.svelte` | Slide-in panel from the right; receives `anchor` + `textId` |
| `AnnotationItem.svelte` | Single annotation row inside the drawer (type badge, content, linked words, edit/delete) |
| `AnnotationForm.svelte` | Create/edit form: type selector, content textarea, word search |
| `WordSearchCombobox.svelte` | Search dictionary by Arabic/transliteration, returns selected words as chips |

### TokenGrid changes (`src/lib/components/texts/TokenGrid.svelte`)

New props:
- `annotations: AnnotationResponse[]` — all annotations for this text
- `onTokenClick: (anchor: string) => void`

Behaviour: for each token, check if any annotation's `anchor === token.arabic`. If yes, render one `AnnotationBadge` per annotation type present. On cell click, call `onTokenClick(token.arabic)`.

### InterlinearSentence / Text detail page

Text detail page (`src/routes/texts/[id]/+page.svelte`):
- Fetch `useTextAnnotations(textId)`
- Hold drawer state: `{ open: boolean, anchor: string | null }`
- Pass `annotations` + `onTokenClick` down to `InterlinearSentence` → `TokenGrid`
- Render `AnnotationDrawer` as a page-level overlay (not per-sentence)

### Word detail page

Existing `useWordAnnotations(wordId)` hook already in `words.ts`. Add a section that lists linked annotations using `AnnotationItem` (read-only view).

## Drawer UX Flow

```
User clicks token
  ↓
Drawer opens, showing anchor Arabic text as header
  ↓
  ├─ No annotations yet → form shown immediately (type selector + content + word search)
  └─ Annotations exist → list of AnnotationItems + "Add another" button
        ↓
        Click "Add another" or "Edit" → inline form replaces / appends
```

Form fields:
1. **Type** — segmented control or select: VOCABULARY / GRAMMAR / CULTURAL / OTHER
2. **Content** — textarea (freetext note)
3. **Linked words** — `WordSearchCombobox`: type-ahead search, selected words shown as dismissible chips

## Badge Color Coding

| Type | Badge label | Color |
|---|---|---|
| VOCABULARY | V | green (`#4caf82`) |
| GRAMMAR | G | blue (`#6b9bdc`) |
| CULTURAL | C | amber (`#d4a84b`) |
| OTHER | O | muted (`#888`) |

## Out of Scope

- Multi-token span selection (use sentence `notes` field)
- Mastery level / SRS review of annotations
- Annotation export or bulk operations
- Annotation history / versioning
