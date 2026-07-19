---
title: Verb Conjugation Feature — Design & Implementation Plan
date: 2026-07-19
status: draft
---

# Verb Conjugation Feature

## Goal

Reference & learning tool for Arabic verb conjugation tables. User picks a verb (from DB or ad-hoc), sees full paradigm with morphological color-coding of prefix / root / pattern vowels / suffix.

## Reference

Inspired by the Wiktionary Lua module: https://en.wiktionary.org/wiki/Module:ar-verb — adapted for our subset (past/present/imperative, active/passive, no subjunctive/jussive/masdar).

## Decisions (from brainstorming)

| Decision           | Choice                                                                                |
|--------------------|---------------------------------------------------------------------------------------|
| Scope              | Past + present tense, active + passive, imperative, all 13 person/number/gender slots |
| No                 | Subjunctive, jussive, masdar, participles                                             |
| Dialect            | MSA first; API accepts dialect param for future Tunisian engine                       |
| Engine             | Pure rule-based in Kotlin. Deterministic, testable, zero-latency                      |
| Persistence        | Verb metadata persisted, conjugated forms computed on-the-fly                         |
| Frontend placement | `/verbs/conjugation` — reference/learning tool                                        |
| Affix coloring     | 4-color: prefix / root letters / pattern vowels / suffix                              |
| Form I vowels      | Stored as full pattern strings (e.g. `fa3ala`, `yaf3ulu`)                             |
| Weakness type      | Stored explicitly per verb, not derived from root                                     |
| Integration        | Later: embed in word detail page when POS=VERB                                        |

## Data Model

### New table: `verb_details`

Replaces `verb_pattern` column currently in `word_morphology`. Dedicated to verb-specific metadata.

```sql
CREATE TABLE verb_details (
    word_id       UUID PRIMARY KEY REFERENCES words(id) ON DELETE CASCADE,
    verb_form     VARCHAR(5)  NOT NULL,  -- I, II, III, ... X
    past_pattern  VARCHAR(20),           -- e.g. 'fa3ala', 'fa3ila', 'fa3ula' (Form I only)
    present_pattern VARCHAR(20),         -- e.g. 'yaf3ulu', 'yaf3ilu', 'yaf3alu' (Form I only)
    weakness_type VARCHAR(20) NOT NULL DEFAULT 'SOUND',
    -- SOUND, ASSIMILATED, HOLLOW, GEMINATE, DEFECTIVE, DOUBLY_WEAK
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_verb_details_form ON verb_details(verb_form);
CREATE INDEX idx_verb_details_weakness ON verb_details(weakness_type);
```

- `past_pattern` / `present_pattern`: nullable — only needed for Form I. Forms II-X derive patterns from the form number.
- `weakness_type`: explicit enum-as-varchar. AI-enrich can populate.

### Migration of `verb_pattern` from `word_morphology`

- New Flyway migration: move existing `verb_pattern` values into `verb_details`, drop column from `word_morphology`.
- `word_morphology` retains only `gender` (relevant for nouns too).

### Domain enums (new)

```kotlin
enum class WeaknessType {
    SOUND, ASSIMILATED, HOLLOW, GEMINATE, DEFECTIVE, DOUBLY_WEAK
}
```

### Domain model (new)

```kotlin
data class VerbDetails(
    val wordId: WordId,
    val verbForm: VerbPattern,      // reuse existing enum
    val pastPattern: String?,       // Form I only
    val presentPattern: String?,    // Form I only
    val weaknessType: WeaknessType,
)
```

## API Design

### Verb Details CRUD

```
GET    /api/v1/words/{id}/verb-details     → VerbDetailsResponse
PUT    /api/v1/words/{id}/verb-details     → VerbDetailsResponse  (upsert)
DELETE /api/v1/words/{id}/verb-details     → 204
```

### Conjugation Endpoint

```
GET /api/v1/conjugation/{wordId}?dialect=MSA
```

Response:

```json
{
  "word": { "id": "...", "arabicText": "كتب", "translation": "to write" },
  "verbDetails": { "verbForm": "I", "pastPattern": "fa3ala", "presentPattern": "yaf3ulu", "weaknessType": "SOUND" },
  "root": { "letters": ["ك", "ت", "ب"] },
  "dialect": "MSA",
  "conjugations": {
    "past_active": [
      {
        "person": "1S",
        "arabic": "كَتَبْتُ",
        "segments": [
          { "text": "كَ", "type": "ROOT" },
          { "text": "تَ", "type": "ROOT" },
          { "text": "بْ", "type": "ROOT" },
          { "text": "تُ", "type": "SUFFIX" }
        ]
      }
      // ... 12 more persons
    ],
    "past_passive": [ ... ],
    "present_active": [ ... ],
    "present_passive": [ ... ]
  }
}
```

Segment types: `PREFIX`, `ROOT`, `PATTERN_VOWEL`, `SUFFIX`

Person codes: `1S`, `2SM`, `2SF`, `3SM`, `3SF`, `2D`, `3DM`, `3DF`, `1P`, `2PM`, `2PF`, `3PM`, `3PF`

### Ad-hoc Conjugation (no saved word)

```
POST /api/v1/conjugation/compute
{
  "rootLetters": ["ك", "ت", "ب"],
  "verbForm": "I",
  "pastPattern": "fa3ala",
  "presentPattern": "yaf3ulu",
  "weaknessType": "SOUND",
  "dialect": "MSA"
}
```

Same response shape minus the `word` field.

## Conjugation Engine Architecture

```
domain/conjugation/
├── ConjugationEngine.kt          -- interface: conjugate(root, form, patterns, weakness, dialect) → ConjugationTable
├── MsaConjugationEngine.kt       -- MSA implementation
├── model/
│   ├── ConjugationTable.kt       -- data class: map of tense+voice → list of PersonConjugation
│   ├── PersonConjugation.kt      -- person code + arabic string + segments
│   ├── Segment.kt                -- text + SegmentType enum
│   ├── Tense.kt                  -- PAST, PRESENT
│   ├── Voice.kt                  -- ACTIVE, PASSIVE
│   └── Person.kt                 -- enum of 13 person/number/gender combos
├── rules/
│   ├── AffixTable.kt             -- past suffixes, present prefixes/suffixes per person
│   ├── FormStemBuilder.kt        -- builds stem from root + form number (II-X patterns)
│   └── WeakVerbRules.kt          -- weakness-specific stem modifications
```

Strategy pattern: `ConjugationEngine` interface, `MsaConjugationEngine` as first impl. Koin binds by dialect. Tunisian engine plugs in later.

### Engine Logic (simplified)

1. **Build stem** from root letters + verb form + vowel pattern
   - Form I: use provided past/present patterns with root letter substitution
   - Forms II-X: deterministic stem from form rules (e.g. Form II = fa33ala)
2. **Apply weakness rules** — modify stem for hollow/defective/geminate/assimilated
3. **Attach affixes** per person/number/gender from affix table
4. **Split into segments** — tag each character span as PREFIX/ROOT/PATTERN_VOWEL/SUFFIX
5. Return `ConjugationTable`

## Frontend

### Route: `/verbs/conjugation`

**Components:**

```
routes/verbs/conjugation/
├── +page.svelte              -- page shell, verb picker, results
├── +page.ts                  -- load function (optional: preload verb if query param)

lib/components/conjugation/
├── VerbPicker.svelte         -- search DB verbs (filtered to POS=VERB) or ad-hoc input
├── ConjugationTable.svelte   -- renders one tense+voice table (past active, etc.)
├── ConjugatedForm.svelte     -- single form with 4-color segment rendering
├── AdHocVerbForm.svelte      -- input form for root + form + patterns + weakness
```

**Color scheme** (CSS classes):

| Segment       | Class         | Color intent              |
|---------------|---------------|---------------------------|
| PREFIX        | `.seg-prefix` | Blue                      |
| ROOT          | `.seg-root`   | Default/bold (the anchor) |
| PATTERN_VOWEL | `.seg-vowel`  | Amber/gold                |
| SUFFIX        | `.seg-suffix` | Green                     |

### Table layout

4 tables on page: past active, past passive, present active, present passive. Each table has 13 rows (one per person). Columns: person label (Arabic grammar term + transliteration), conjugated form with colored segments.

### Integration with word detail (future)

When word detail page detects POS=VERB and verb_details exist: show "View Conjugation" link → navigates to `/verbs/conjugation?wordId={id}`.

---

## Implementation Plan — Slices

### Slice 1: Database & Domain Model

- New Flyway migration: create `verb_details` table
- New Flyway migration: migrate `verb_pattern` data from `word_morphology` → `verb_details`, drop `verb_pattern` column
- New domain types: `VerbDetails`, `WeaknessType` enum
- Update `WordMorphology` domain model: remove `verbPattern` field
- New `VerbDetailsTable` (Exposed)
- New `VerbDetailsRepository` interface + Exposed implementation
- New `VerbDetailsService` (CRUD)
- Update `WordMorphologyRepository` and service: remove verbPattern handling
- Update existing tests

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 2: Verb Details API

- DTOs: `VerbDetailsResponse`, `UpsertVerbDetailsRequest`
- Routes: GET/PUT/DELETE `/api/v1/words/{id}/verb-details`
- Update OpenAPI spec (`documentation.yaml`)
- Regenerate frontend types (`just gtypes`)
- Wire Koin module

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 3: Conjugation Engine (Core)

- `ConjugationEngine` interface
- `MsaConjugationEngine` — Form I sound verbs only (past + present, active only)
- Affix tables (13 persons × past suffixes, present prefixes+suffixes)
- Form I stem builder (root + vowel pattern → stem)
- Segment splitting logic
- Unit tests: known verb (كتب Form I) → verify all 13 past + 13 present forms

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 4: Engine — Forms II-X + Passive

- `FormStemBuilder` for Forms II-X (deterministic patterns)
- Passive voice stem rules (vowel substitution)
- Unit tests for Form II (e.g. دَرَّسَ), Form V, Form X
- Unit tests for passive forms

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 5: Engine — Weak Verbs

- `WeakVerbRules`: hollow, defective, assimilated, geminate modifications
- Handle each weakness type for past + present, active + passive
- Unit tests: قال (hollow), مشى (defective), وصل (assimilated)

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 6: Conjugation API

- Routes: `GET /api/v1/conjugation/{wordId}?dialect=MSA`
- Routes: `POST /api/v1/conjugation/compute` (ad-hoc)
- `ConjugationService` — orchestrates: load word + verb details + root → call engine → return response
- DTOs: `ConjugationResponse`, `AdHocConjugationRequest`
- Update OpenAPI spec
- Regenerate frontend types

**Verify:** `just check`, curl both endpoints, `just gtypes`, `just check-frontend`

### Slice 7: Frontend — Conjugation Page

- New route: `/verbs/conjugation/+page.svelte` + `+page.ts`
- `VerbPicker` component — search verbs from DB (POS=VERB filter)
- `ConjugationTable` component — renders 13-row table for one tense+voice
- `ConjugatedForm` component — renders segments with 4-color CSS
- CSS classes in `frontend/src/styles/conjugation.css` (new partial)
- Page layout: verb picker top, 4 tables below (2×2 grid: past/present × active/passive)
- API integration via svelte-query

**Verify:** quality gates (`.claude/rules/quality-gates.md`) + visual check in browser

### Slice 8: Frontend — Ad-hoc Input + Polish

- `AdHocVerbForm` component — root letters input, form selector, pattern dropdowns, weakness selector
- Toggle between "pick from DB" and "ad-hoc" mode
- Add navigation link to sidebar/nav
- Add `conjugation.css` import to `app.css`
- Responsive layout adjustments for tables

**Verify:** quality gates (`.claude/rules/quality-gates.md`) + visual check in browser

### Slice 9: AI Enrich + Verb Details on Word Page

- Update AI-enrich prompts to populate `verb_details` (form, patterns, weakness) when POS=VERB
- Show verb details strip on word detail page (similar to morphology strip)
- "View Conjugation" link on word detail → `/verbs/conjugation?wordId={id}`
- Update word detail page to use new verb details endpoint instead of morphology.verbPattern

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 10: Update OpenAPI + Docs

- Final OpenAPI spec review — ensure all new endpoints documented
- Update `docs/` with architecture notes on conjugation engine
- Update any affected docs (word model changes)

**Verify:** quality gates (`.claude/rules/quality-gates.md`)

### Slice 11: Consolidate word morphology → noun_details

Now that verb-specific data lives in `verb_details`, `word_morphology` holds only `gender` (one nullable column). Clean this up:

- Create `noun_details` table: `word_id`, `gender`, link plurals here instead of directly to words
- Migrate `word_morphology.gender` → `noun_details` for NOUN and ADJECTIVE words
- Drop `word_morphology` table entirely
- Move plural FK from `word_plurals.word_id → words(id)` to `word_plurals.word_id → noun_details(word_id)`
- Update morphology endpoints → noun-details endpoints (GET/PUT/DELETE `/api/v1/words/{id}/noun-details`)
- Update DTOs, services, repositories, Koin bindings
- Update frontend: `WordMorphologyStrip` → `NounDetailsStrip`, remove old morphology API calls
- Update AI-enrich to populate `noun_details` instead of `word_morphology`
- Update OpenAPI spec, regenerate types

**Verify:** quality gates (`.claude/rules/quality-gates.md`)
