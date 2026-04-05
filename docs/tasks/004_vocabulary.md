## Milestone 4 — Vocabulary / Words Domain

Largest domain. Depends on Roots (FK). Includes AI integration and pg_trgm search.

- [ ] 4.1 `[B]` `V003__create_words.sql`: `words` table — all fields from product-spec (UUID PK, arabic_text, transliteration, translation, example_sentence, part_of_speech VARCHAR CHECK, dialect VARCHAR CHECK, difficulty VARCHAR CHECK, mastery_level VARCHAR CHECK, pronunciation_url, root_id FK nullable, derived_from FK nullable self-ref)
- [ ] 4.2 `[B]` `V004__create_word_dictionary_links.sql`: `word_dictionary_links` table (id, word_id FK, source VARCHAR CHECK, url)
- [ ] 4.3 `[B]` `V005__create_word_progress.sql`: `word_progress` table 1:1 with words (consecutive_correct, total_attempts, total_correct, last_reviewed_at); insert trigger or application-managed on word creation
- [ ] 4.4 `[B]` `V006__create_word_indexes.sql`: `pg_trgm` GIN index on `arabic_text` + `translation`; standard B-tree on `mastery_level`, `dialect`, `difficulty`, `root_id`
- [ ] 4.5 `[B]` Domain: `Word`, `WordId`, `DictionaryLink`, `WordProgress` data classes; all enum types as Kotlin enums mirroring product-spec values exactly
- [ ] 4.6 `[B]` Infrastructure: `WordsTable`, `WordDictionaryLinksTable`, `WordProgressTable` (Exposed objects) + `WordRepository` interface + `ExposedWordRepository` impl including search + autocomplete queries
- [ ] 4.7 `[B]` Application: `WordService` — CRUD, search/filter, autocomplete, manage dictionary links; `Either<DomainError, T>` throughout
- [ ] 4.8 `[B]` Delivery: routes under `/api/v1/words` — `GET /` (paginated + all filters), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `GET /autocomplete?q=`, `GET /{id}/dictionary-links`, `POST /{id}/dictionary-links`, `DELETE /{id}/dictionary-links/{linkId}`
- [ ] 4.9 `[B]` `AiClient`: single wrapper around OpenRouter HTTP (Ktor client); reads `OPENROUTER_API_KEY` from env; returns `503 AI_NOT_CONFIGURED` if absent; structured output enforced
- [ ] 4.10 `[B]` Prompt template `word.examples`: given a word, returns 2 example sentences `{ arabic, transliteration, translation }`; `POST /api/v1/words/{id}/examples`
- [ ] 4.11 `[B]` Unit tests: `WordService` (MockK), enum validation, `derivedFrom` cycle guard
- [ ] 4.12 `[B]` Integration tests: CRUD routes, search/filter combinations, autocomplete, dictionary links CRUD, AI endpoint (mocked OpenRouter)
