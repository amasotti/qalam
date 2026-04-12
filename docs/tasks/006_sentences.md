## Milestone 6 — Sentences + Alignment Tokens ✅

Interlinear view — the most behaviourally complex domain. Stale-token logic is the critical invariant.

- [x] 6.0 `[B]` Update `documentation.yaml`: add all new schemas (SentenceResponse, CreateSentenceRequest, UpdateSentenceRequest, AlignmentTokenResponse, ReplaceTokensRequest) and all new paths (`/api/v1/texts/{textId}/sentences` CRUD + tokens sub-resource + `transliterate` AI endpoint) before implementing routes
- [x] 6.1 `[B]` `V009__create_sentences.sql`: `sentences` table (UUID PK, text_id FK, position INT, arabic_text, transliteration, free_translation, notes, tokens_valid BOOLEAN DEFAULT true, created_at, updated_at)
- [x] 6.2 `[B]` `V010__create_alignment_tokens.sql`: `alignment_tokens` table (UUID PK, sentence_id FK, position INT, arabic, transliteration, translation, word_id FK nullable); index on `sentence_id`
- [x] 6.3 `[B]` Domain: `Sentence`, `SentenceId`, `AlignmentToken` data classes; `tokensValid: Boolean` on `Sentence`
- [x] 6.4 `[B]` Domain rule: updating `Sentence.arabicText` sets `tokensValid = false` and must be enforced in the service (not the DB trigger)
- [x] 6.5 `[B]` Infrastructure: `SentencesTable`, `AlignmentTokensTable` + `SentenceRepository` interface + impl; tokens always loaded with their sentence
- [x] 6.6 `[B]` Application: `SentenceService` — CRUD sentences (position reordering), manage tokens (replace all / clear / reorder), enforce stale-token invalidation on sentence text edit
- [x] 6.7 `[B]` Delivery: nested routes under `/api/v1/texts/{textId}/sentences` — `GET /`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/tokens` (replace all), `DELETE /{id}/tokens` (clear with confirmation flag), `POST /{id}/auto-tokenize`
- [x] 6.8 `[B]` Prompt template `sentence.transliterate`: `POST /api/v1/texts/{textId}/sentences/{id}/transliterate`
- [x] 6.9 `[B]` Unit tests: stale-token invalidation logic, position reordering, `SentenceService` (MockK)
- [x] 6.10 `[B]` Integration tests: sentence CRUD within a text, token replacement, stale-token scenario, auto-tokenize
