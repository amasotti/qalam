# Qalam — Roadmap

> Goal: reach daily usability and deprecate an-na7wi as fast as possible.
> Post-MVP features are built iteratively after real data is in the system.

## Status Legend
- ✅ Done
- 🔜 Next
- 🎯 MVP
- 📦 Post-MVP (backlog)

---

## ✅ Done

| Milestone | Summary |
|-----------|---------|
| M0 Skeleton | Repo, Docker, justfile, CI skeleton |
| M1 Backend foundation | Ktor, Exposed, Flyway, Koin, error handling, OpenAPI |
| M2 Frontend foundation | SvelteKit shell, sidebar, shadcn base, svelte-query, Arabic CSS |
| M3 Roots domain | Full backend CRUD + tests |
| M4 Vocabulary domain | Full backend CRUD + AI examples + tests |
| M5 Texts domain | Full backend CRUD + search/filter + tests |
| M6 Sentences + tokens | Full backend: sentences, alignment tokens, stale-token invariant, AI transliterate/auto-tokenize, tests |

---

## 🔜 Now — Complete the backend

### M7 — Annotations Domain
*Texts ↔ Words cross-reference. The reverse lookup (word → texts) is a first-class feature.*
→ `docs/tasks/007_annotations.md`

### M10 — Transliteration Service
*Copy character map from an-na7wi, wrap in a clean service. ~2h of work.*
→ `docs/tasks/010_transliteration.md`

---

## 🎯 MVP — Frontend + Go Live

### M13 — Frontend Foundation
*API type pipeline, brand theme (green/red/near-black), mastery semantic classes, Biome linting.*
*Must be done before any feature frontend work.*
→ `docs/tasks/013_frontend_foundation.md`

### M14 — Frontend: Roots
*Small scope. Validates full stack end-to-end before heavier pages.*
→ `docs/tasks/014_frontend_roots.md`

### M15 — Frontend: Vocabulary
*Words list + detail + create/edit. Migrated data lands here first.*
→ `docs/tasks/015_frontend_vocabulary.md`

### M16 — Frontend: Texts + Annotations
*The primary daily-use screen. Interlinear view, annotation panel, word linking.*
→ `docs/tasks/016_frontend_texts.md`

### M19 — Data Migration (Phase 1: Core data)
*Migrate roots → words → texts only. Enough to start daily use.*
*Sentences/annotations/training migrate in Phase 2 after M7 frontend is stable.*
→ `docs/tasks/019_migration.md`

### M12.4 — Backup
*Daily `pg_dump` via Ofelia cron before trusting the system with real data.*
*Single task extracted from M12 — do not wait for all of M12.*

---

## 📦 Post-MVP (build after daily use is established)

These are real features — not cancelled, just deferred until the app is live with real data.

| Milestone | Why deferred |
|-----------|-------------|
| M7 frontend (annotations UI) | Backend needed first; UI follows naturally after M16 |
| M8 Audio attachments | No audio in an-na7wi; zero existing data; purely additive |
| M9 Training / SRS backend | Can study from texts without flashcards; needs real data first |
| M11 Global search + analytics | Vocabulary search (M4) covers immediate needs |
| M12 Backend hardening (rest) | Cycle guard, request validation, CI release pipeline — not MVP blockers |
| M17 Frontend training | Blocked on M9 |
| M18 Frontend analytics | Blocked on M11 |
| M19 Migration Phase 2 | Sentences, annotations, training — after M7 backend stable |
| M20 E2E tests | Post-MVP polish |

---

## Migration quick-reference

**Phase 1 (do with M19):** roots → words → word_dictionary_links → word_progress → texts → text_tags

**Phase 2 (post-MVP):** sentences → alignment_tokens → annotations → annotation_words → training_sessions → training_session_words

**Key schema changes from an-na7wi:**
- `words.arabic` → `arabic_text`; `words.example` → `example_sentence`; `words.pronunciation_link` → `pronunciation_url`
- `texts.arabic_content` → `body`; `texts.tags` (JSONB) → `text_tags` table rows
- `interlinear_texts` + `texts` → unified `texts` table (merge by title where possible)
- `interlinear_sentences.translation` → `sentences.free_translation`; `.annotations` → `.notes`
- `word_alignments.arabic_tokens` → one `alignment_tokens` row per token (spot-check if multi-word tokens exist)
- `dictionary_links` (JSON column) → `word_dictionary_links` table rows

**Run as SQL script, not Kotlin.** Test against a copy first. Keep old DB until 20+ words and 5+ texts are spot-checked in the new UI.
