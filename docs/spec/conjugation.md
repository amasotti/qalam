# Verb conjugation

> Deterministic MSA verb paradigms and the practice modes built from them.

## Scope and status

The conjugation feature uses a pure Kotlin rule engine. It needs no AI key and produces the same
table for the same input. Its reference API is implemented; the reference page is being completed.
The next planned feature is conjugation practice.

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

The saved-word endpoint requires a VERB word, verb details, and a linked root. The ad-hoc endpoint
accepts the same root/form/pattern/weakness inputs directly. Both return the dialect, root,
metadata, and a table keyed by tense and voice. The OpenAPI document is the API source of truth.

## Practice boundary

Vocabulary flashcards and meaning multiple-choice sessions can update word-level mastery.
Conjugation exercises cannot: their forms are computed, not words. They will persist sessions,
verbs, presented forms, submitted answers, and individual mistakes only. The first mode matches
four forms to four morphology labels; the second requires full Arabic typed recall with harakat.
Neither mode has promotion logic or a per-form progress table.

## Credits & Inspiration

The deterministic conjugation engine is heavily inspired by the [Wiktionary ar-verb lua Module](https://en.wiktionary.org/wiki/Module:ar-verb)
adapted to the structure of this project and rewritten entirely to fit in a idiomatic kotlin ktor project.
