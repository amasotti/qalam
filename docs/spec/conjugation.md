# Verb conjugation

> Deterministic MSA verb paradigms and the practice modes built from them.

## Scope and status

The conjugation feature uses a pure Kotlin rule engine. It needs no AI key and produces the same
table for the same input. The reference API, `/verbs/conjugation` page, and matching-session API
are implemented. The matching UI remains the next feature.

Current target scope is MSA, past and present tense, active/passive voice, and all 13 Arabic
person/number/gender slots. Subjunctive, jussive, masdar, participles, and Tunisian rules are out
of scope for now.

## Inputs and model

A saved verb needs:

- a linked root;
- `verb_details`: form I–X, weakness type, and Form-I past/present patterns where applicable.

The engine builds a stem, applies weakness rules, attaches person-specific affixes, and returns
each form as `PREFIX`, `ROOT`, `PATTERN_VOWEL`, and `SUFFIX` segments. Those segments are both a
teaching aid in the reference table and feedback in future exercises.

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/conjugation/{wordId}` | Generate a table for a saved verb |
| POST | `/api/v1/conjugation/compute` | Generate a table from ad-hoc root and verb metadata |
| GET | `/api/v1/words/{id}/verb-details` | Read persisted verb metadata |
| PUT | `/api/v1/words/{id}/verb-details` | Create or replace persisted verb metadata |
| DELETE | `/api/v1/words/{id}/verb-details` | Remove persisted verb metadata |
| POST | `/api/v1/conjugation-exercise-sessions` | Create a four-form matching session |
| GET | `/api/v1/conjugation-exercise-sessions/{id}` | Resume or review a matching session |
| POST | `/api/v1/conjugation-exercise-sessions/{id}/answers` | Submit one board's four mappings |
| POST | `/api/v1/conjugation-exercise-sessions/{id}/complete` | Complete a session without SRS promotion |
| GET | `/api/v1/conjugation-exercise-sessions` | List matching-session history |

The saved-word endpoint requires a VERB word, verb details, and a linked root. The ad-hoc endpoint
accepts the same root/form/pattern/weakness inputs directly. Both return the dialect, root,
metadata, and a table keyed by tense and voice. The OpenAPI document is the API source of truth.

The page can compute from an ad-hoc root or search saved `VERB` words. It renders all four
past/present × active/passive tables and labels each returned span with its segment type.

## AI enrichment

AI enrichment remains an explicit preview-and-accept flow. For verbs, it can suggest
`verbDetails` containing form, Form-I vowel patterns, and weakness type; accepting it persists
the data through the verb-details endpoint. `verbPattern` remains a deprecated response field for
older clients, but new clients must use `verbDetails`.

## Practice boundary

Vocabulary flashcards and meaning multiple-choice sessions can update word-level mastery.
Conjugation exercises cannot: their forms are computed, not words. They will persist sessions,
verbs, presented forms, submitted answers, and individual mistakes only. The first mode matches
four forms to four morphology labels; the second requires full Arabic typed recall with harakat.
Neither mode has promotion logic or a per-form progress table.

## Credits & Inspiration

The deterministic conjugation engine is heavily inspired by the [Wiktionary ar-verb lua Module](https://en.wiktionary.org/wiki/Module:ar-verb)
adapted to the structure of this project and rewritten entirely to fit an idiomatic Kotlin/Ktor project.
