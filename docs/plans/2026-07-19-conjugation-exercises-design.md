---
title: Conjugation Exercises — Design & Implementation Plan
date: 2026-07-19
status: draft
depends_on: 2026-07-19-verb-conjugation-design.md
---

# Conjugation Exercises

## Goal

Turn generated conjugation tables into active recall practice. Start with a
four-form morphology matching exercise. Plan a fully vocalised typed-recall
variant on the same session family, after matching is stable.

This is a separate training family from vocabulary multiple choice. It records
what was practised and misguessed, but never changes lemma mastery or creates
per-form progress: `كَتَبْتُ` and `تَكْتُبْنَ` are generated forms, not stored
vocabulary records.

## Decision

| Decision | Choice |
|---|---|
| First exercise | Match 4 forms of one verb to 4 morphology labels |
| Interaction | Click/tap-to-pair first; optional pointer drag-and-drop enhancement |
| Initial scope | MSA, active past and present; all persons supported by engine |
| Board shape | One verb and one tense per board, 4 distinct persons |
| Session size | 5 boards by default; 4 pairs per board; configurable 3–10 boards, fixed 4 pairs in v1 |
| Eligible verbs | Saved VERB words with root and `verb_details`, whose engine table contains requested forms |
| Selection | Reuse word-list and word-mastery filters only to choose lemmas, not to score morphology |
| Persistence | Snapshot generated forms, labels, and submitted mappings at session creation |
| Progress | Session history only; no mutation of `word_progress` or form-progress table |
| Typed variant | Full Arabic typing with exact harakat checking, on same session infrastructure |

### Why matching first

It isolates morphology recognition from Arabic keyboard and diacritic-entry
friction. A same-tense board gives useful contrast: the learner identifies
person/number/gender from changes to prefix and suffix. Later boards can mix
past/present, then voice, without changing the data model.

The existing `/exercise-sessions` API is deliberately not extended. It stores
one target word plus selectable word options and its answer path applies
word-level SRS promotion. Conjugation answers are mappings between generated
forms and grammatical descriptions, therefore need different persisted state
and response shapes. They do not need a progression model.

## Exercise UX

Route: `/training/exercises/conjugation` creates a session; `/training/conjugation-exercises/{id}` runs it.

Each board shows:

- lemma, translation, form number, and selected tense/voice;
- four fully vocalised Arabic forms, shuffled;
- four shuffled labels, for example `1st singular`, `2nd plural feminine`,
  `3rd plural masculine`;
- a click/tap flow: choose a form, then a label. Paired rows can be changed
  before submission;
- `Check answers` after all four mappings exist; feedback reveals all
  correct pairings and uses the existing segment colours to explain each form.

Keyboard and accessibility requirements:

- form and label are real buttons; no drag-and-drop-only interaction;
- clear focus order, selected/paired state, and live feedback;
- Arabic strings use `lang="ar"` and RTL direction; morphology labels remain
  in the UI language;
- do not rely on colour alone for correct/incorrect feedback.

The composer keeps existing vocabulary scope and focus controls, renamed to
make clear that they select *verbs to practise*. It adds tense choices
(`past active`, `present active`, initially one required) and board count.
It must state how many eligible verbs are available and disable start when
none exist.

## Domain and API

Create `domain/conjugationexercise/`; keep dependencies inward:

```
Routes → ConjugationExerciseService → ConjugationExerciseRepository
                                  → ConjugationEngine + verb/root lookup ports
```

Use domain types, not strings, for `Tense`, `Voice`, and `Person`; reuse the
conjugation engine's enums and `PersonConjugation` where appropriate.

### Endpoints

```
POST /api/v1/conjugation-exercise-sessions
GET  /api/v1/conjugation-exercise-sessions/{id}
POST /api/v1/conjugation-exercise-sessions/{id}/answers
POST /api/v1/conjugation-exercise-sessions/{id}/complete
GET  /api/v1/conjugation-exercise-sessions?page=&size=
```

`CreateConjugationExerciseSessionRequest`:

```json
{
  "mode": "MIXED",
  "size": 5,
  "wordListIds": [],
  "tense": "PRESENT",
  "voice": "ACTIVE"
}
```

`size` means boards, not individual pairings. Server clamps it to `3..10`.
The server selects eligible verbs, generates the full table once, chooses four
distinct persons, shuffles form and label presentation independently, then
persists that exact board. If fewer than four usable forms or no eligible verb
exists, return a specific `NotEnoughConjugatableVerbs` domain error rather
than silently degrading to vocabulary.

The session response exposes only independent collections:

```json
{
  "itemId": "...",
  "verb": { "wordId": "...", "lemma": "أَرَادَ", "translation": "to want", "verbForm": "IV" },
  "tense": "PRESENT",
  "voice": "ACTIVE",
  "forms": [{ "formId": "...", "arabic": "أُرِيدُ", "segments": [] }],
  "labels": [{ "labelId": "...", "person": "1S", "display": "1st singular" }],
  "result": null
}
```

It must not expose the form-to-label association before the answer. The answer
body contains exactly four unique `{ formId, labelId }` mappings. Server
validates that all IDs belong to the unanswered item and that each form and
label occurs once. Its response contains overall result, submitted mappings,
and correct mappings for feedback. Repeated answers and completed sessions
return the existing conflict semantics.

Add every endpoint, enum, request/response, and error response to
`backend/src/main/resources/openapi/documentation.yaml`, then regenerate
frontend types. The Android client must be checked before publishing this new
API surface; it is additive, so it does not break existing callers.

## Persistence

Use dedicated tables, never overloaded `exercise_session_*` tables:

```sql
conjugation_exercise_sessions
  id, mode, status, total_items, correct_count, incorrect_count, skipped_count,
  tense, voice, created_at, completed_at

conjugation_exercise_items
  id, session_id, word_id, position,
  lemma_snapshot, translation_snapshot, verb_form_snapshot,
  result, answered_at

conjugation_exercise_pairs
  id, item_id, position,
  form_id, label_id, arabic, segments_json,
  tense, voice, person

conjugation_exercise_answers
  item_id, form_id, selected_label_id, submitted_text, is_correct
```

All UUIDs are application-generated. Define explicit FKs with `ON DELETE
CASCADE`; index session IDs, item IDs, word IDs, and answer item IDs. Pair rows
hold canonical morphology plus display snapshot. `form_id` and `label_id` are
different opaque UUIDs, preventing the client from deriving the solution.
`segments_json` is the response snapshot, so a later engine correction cannot
rewrite completed-session history. An answer row records every submitted
mapping or text answer and its per-form correctness. This gives useful history
of verbs used and forms misguessed without modelling durable learner progress.

Add an Exposed repository interface and implementation; database-specific
selection belongs in a focused query/repository method such as
`findConjugatableForTraining`, not in route code. It filters POS, root,
`verb_details`, requested word lists, and lemma mastery mode. The service still
asks the engine whether a candidate supplies four usable forms.

## Typed Recall Variant: second phase

After matching has shipped, add `WRITE_FORM` item mode to the same *new*
conjugation-exercise domain. Prompt: lemma plus `present active · 2nd plural
feminine`; answer: the fully vocalised generated form. Arabic keyboards on
desktop and mobile are the expected input method; no transliteration fallback
or answer auto-completion.

Do not implement it in the first slice. It needs a deliberate input policy:

- normalise both strings with Unicode NFC and canonical combining-mark order;
- strip only presentation characters such as tatweel and surrounding
  whitespace;
- then require exact equality, including every expected haraka, shadda,
  sukūn, and letter/hamza choice;
- missing, extra, or wrong diacritics are incorrect; feedback shows expected
  form and the learner's submission;
- provide an Arabic input and diacritic toolbar, with keyboard-friendly
  insertion, but never auto-complete the answer.

This maintains the requested full-harakat standard while accepting equivalent
Unicode mark ordering. Add focused normalization tests from real Arabic input
variants before enabling production scoring.

## Implementation slices

### Slice 1: Preconditions and eligibility

- Require completed conjugation engine/API slices and establish engine tests
  for all v1 selectable active forms.
- Add repository query/port for eligible saved verbs, including word-list and
  word-mastery filters.
- Define person-label formatter and candidate form selection with deterministic
  unit tests: exactly four distinct persons; no missing table keys; no leaked
  answer association.

**Verify:** backend unit tests and `./backend/gradlew -p backend detekt check`.

### Slice 2: Session domain and storage

- Add Flyway migration and Exposed tables for conjugation exercise sessions,
  items, pairs, and answers.
- Add domain models, repository interface/implementation, service creation,
  loading, answering, completion, and history listing.
- Snapshot generated segments and all presentation text at creation.
- Persist each submitted mapping and whether it was correct, so history can
  show the verb and individual forms misguessed.
- Do not call `wordRepo.updateProgress`, mutate word mastery, or add a
  `verb_form_progress` table.

**Verify:** repository/integration tests for FK cascade, shuffled IDs,
idempotency/conflicts, incomplete-session skips, and immutable snapshots.

### Slice 3: HTTP contract

- Add routes and serializable DTOs under `/api/v1/conjugation-exercise-sessions`.
- Update OpenAPI source of truth and generate TypeScript types.
- Add integration tests for create/get/answer/complete/list, malformed or
duplicate mappings, no eligible verbs, and hidden correct pairings before
submission.
- Verify additive API compatibility with qalam-app before release.

**Verify:** `just check`, curl every endpoint, `just gtypes`.

### Slice 4: Composer and runner

- Add a conjugation exercise card on `/training`, composer route, stores, and
  session history component.
- Build reusable `ConjugationMatchBoard.svelte` with click/tap pairing first.
- Add optional pointer drag/drop only after the accessible path works.
- Use `ConjugatedForm.svelte` from the conjugation feature for feedback,
including segment colouring.
- Put shared rules in `training.css`; put conjugation-specific rules in the
narrowest existing/new partial and import it from `app.css`.

**Verify:** `just lint-frontend`, `just format-frontend`,
`just check-frontend`, keyboard screen-reader smoke test, and browser visual
check at desktop and narrow widths.

### Slice 5: Typed production follow-up

- Add `WRITE_FORM` persistence/DTO discriminator, expected-form snapshot, and
  submitted-text answer field without changing matching responses.
- Implement canonical Arabic comparison and test cases for NFC/NFD ordering,
tatweel/whitespace, missing marks, extra marks, shadda, sukūn, and hamza.
- Build input/diacritic toolbar and answer feedback.
- Store typed-answer correctness only in session history; do not add promotion
  or persistent per-form progress logic.

**Verify:** all prior gates plus production normalization unit and integration
tests.

## Documentation

When shipping each behaviour/API/data-model slice, update this plan's status,
the OpenAPI document, and relevant training/conjugation architecture docs.
Keep the reference table and exercise implementation independently usable:
neither needs an AI key, and exercise generation uses persisted verb metadata
plus the deterministic engine only.
