## Milestone 6 — Sentences + Alignment Tokens

Interlinear view — the most behaviourally complex domain. Stale-token logic is the critical invariant.

- [ ] 6.1 `[B]` `V009__create_sentences.sql`: `sentences` table (UUID PK, text_id FK, position INT, arabic_text, transliteration, free_translation, notes, tokens_valid BOOLEAN DEFAULT true, created_at, updated_at)
- [ ] 6.2 `[B]` `V010__create_alignment_tokens.sql`: `alignment_tokens` table (UUID PK, sentence_id FK, position INT, arabic, transliteration, translation, word_id FK nullable); index on `sentence_id`
- [ ] 6.3 `[B]` Domain: `Sentence`, `SentenceId`, `AlignmentToken` data classes; `tokensValid: Boolean` on `Sentence`
- [ ] 6.4 `[B]` Domain rule: updating `Sentence.arabicText` sets `tokensValid = false` and must be enforced in the service (not the DB trigger)
- [ ] 6.5 `[B]` Infrastructure: `SentencesTable`, `AlignmentTokensTable` + `SentenceRepository` interface + impl; tokens always loaded with their sentence
- [ ] 6.6 `[B]` Application: `SentenceService` — CRUD sentences (position reordering), manage tokens (replace all / clear / reorder), enforce stale-token invalidation on sentence text edit
- [ ] 6.7 `[B]` Delivery: nested routes under `/api/v1/texts/{textId}/sentences` — `GET /`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/tokens` (replace all), `DELETE /{id}/tokens` (clear with confirmation flag), `POST /{id}/auto-tokenize`
- [ ] 6.8 `[B]` Prompt template `sentence.transliterate`: `POST /api/v1/texts/{textId}/sentences/{id}/transliterate`
- [ ] 6.9 `[B]` Unit tests: stale-token invalidation logic, position reordering, `SentenceService` (MockK)
- [ ] 6.10 `[B]` Integration tests: sentence CRUD within a text, token replacement, stale-token scenario, auto-tokenize
