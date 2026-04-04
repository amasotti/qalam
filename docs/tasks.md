# Development Plan — na7wi Rewrite: The Rise of Qalam

Qalam is the new code name for An-Na7wi v2.

Tasks are numbered `MILESTONE.TASK` for direct reference (e.g. "implement 3.4", "test 5.2").
Mark `[ ]` → `[x]` when done. Completed tasks stay in the file as history.

**Legend**: `[B]` = backend only · `[F]` = frontend only · `[I]` = infrastructure · `[BF]` = both

---

## Milestone 0 — Project Skeleton

Infrastructure foundation. Nothing compiles or runs correctly until this is complete.

- [x] 0.1 `[I]` Fix Gradle build: uncomment Kotlin plugin, correct alias `kotlinJvm`, add `gradlePluginPortal()` to `pluginManagement`
- [x] 0.2 `[I]` Add `src/main/kotlin` and `src/test/kotlin` source sets; add a placeholder `Application.kt` so the project has a main entry point
- [x] 0.3 `[I]` Wire full dependency set in `build.gradle.kts`: Ktor server (netty), Exposed, Flyway, Koin, kotlinx.serialization, Logback, Kotest + MockK + Testcontainers for tests
- [x] 0.4 `[I]` `docker-compose.yml` at repo root: `postgres:17-alpine` with health check + named volume; no backend or frontend container yet
- [x] 0.5 `[I]` `justfile` at repo root with `start-db`, `backend`, `test`, `run` recipes (Doppler-wrapped)
- [x] 0.6 `[I]` `doppler.yaml` committed at repo root (project config stub — no secrets in file)
- [x] 0.7 `[I]` `flake.nix` devShell: JDK 25, Gradle, just, doppler, docker, pnpm, Node
- [x] 0.8 `[I]` GitHub Actions `ci.yml`: run `./gradlew test` on push/PR (Doppler token injected)
- [x] 0.9 `[B]` `detekt.yml` config wired and passing on empty project (already in `backend/config/detekt/`)
- [x] 0.10 `[I]` Write `README.md` with project overview, setup instructions, and development workflow. Reference the docs/ folder. Readme is nice to read, enjoyable and clear. Has badges and is appealing. Details are in docs/ folder and sub readmes.

---

## Milestone 1 — Backend Foundation

The Ktor application starts, connects to Postgres, serves health + OpenAPI. No domain logic yet.

- [ ] 1.1 `[B]` Package structure: `delivery/`, `application/`, `domain/`, `infrastructure/` under `com.tonihacks.qalam`
- [ ] 1.2 `[B]` `Application.kt`: `embeddedServer(Netty)` with config loaded from environment (port, DB URL)
- [ ] 1.3 `[B]` Ktor plugins registered: `ContentNegotiation` (kotlinx.serialization), `StatusPages`, `RequestValidation`, `CORS`, `CallLogging`
- [ ] 1.4 `[B]` Database connection: Exposed + HikariCP pool; `Database.connect()` on startup
- [ ] 1.5 `[B]` Flyway integration: auto-run migrations on startup from `src/main/resources/db/migration/`
- [ ] 1.6 `[B]` `V001__create_extensions.sql`: enable `uuid-ossp`, `pg_trgm`, `unaccent`
- [ ] 1.7 `[B]` Koin module wiring: `startKoin {}` in `Application.kt`; empty modules for each domain layer to fill in later
- [ ] 1.8 `[B]` `GET /health` → `{ "status": "ok" }` + `GET /api/v1/openapi.json` + `GET /api/v1/swagger-ui` (Ktor OpenAPI plugin or hand-wired)
- [ ] 1.9 `[B]` Typed error framework: `DomainError` sealed class; `StatusPages` handler mapping domain errors to HTTP responses; `ErrorResponse` DTO `{ error, code }`
- [ ] 1.10 `[B]` Pagination framework: `PaginatedResponse<T>` DTO + `PageRequest` value class; shared across all list endpoints
- [ ] 1.11 `[B]` Integration test harness: base test class that starts Testcontainers Postgres + Ktor `testApplication {}` + Flyway migrations; one smoke test asserting `/health` returns 200

---

## Milestone 2 — Arabic Roots Domain

Smallest domain — good for validating the full layer stack before adding complexity.

- [ ] 2.1 `[B]` `V002__create_roots.sql`: `arabic_roots` table (UUID PK, letters array, normalized_form, display_form, letter_count, meaning, analysis, created_at, updated_at)
- [ ] 2.2 `[B]` Domain: `ArabicRoot` data class, `RootId` value class, `CreateRootRequest` / `UpdateRootRequest` DTOs
- [ ] 2.3 `[B]` Domain: `RootNormalizer` — accepts `"ر ح ب"` / `"رحب"` / `"ر-ح-ب"` / `"ر,ح,ب"` → canonical form; unit-tested
- [ ] 2.4 `[B]` Infrastructure: `RootsTable` (Exposed object) + `RootRepository` interface + `ExposedRootRepository` impl
- [ ] 2.5 `[B]` Application: `RootService` — CRUD + `normalize(input)` delegating to `RootNormalizer`; returns `Either<DomainError, T>`
- [ ] 2.6 `[B]` Delivery: routes under `/api/v1/roots` — `GET /` (paginated, filter by letter count), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `POST /normalize`
- [ ] 2.7 `[B]` Koin: wire `RootRepository` → `ExposedRootRepository`, inject into `RootService`
- [ ] 2.8 `[B]` Unit tests: `RootNormalizer` (all input variants), `RootService` (MockK repository)
- [ ] 2.9 `[B]` Integration tests: all 6 routes, happy path + 404 + 422 validation errors

---

## Milestone 3 — Vocabulary / Words Domain

Largest domain. Depends on Roots (FK). Includes AI integration and pg_trgm search.

- [ ] 3.1 `[B]` `V003__create_words.sql`: `words` table — all fields from product-spec (UUID PK, arabic_text, transliteration, translation, example_sentence, part_of_speech VARCHAR CHECK, dialect VARCHAR CHECK, difficulty VARCHAR CHECK, mastery_level VARCHAR CHECK, pronunciation_url, root_id FK nullable, derived_from FK nullable self-ref)
- [ ] 3.2 `[B]` `V004__create_word_dictionary_links.sql`: `word_dictionary_links` table (id, word_id FK, source VARCHAR CHECK, url)
- [ ] 3.3 `[B]` `V005__create_word_progress.sql`: `word_progress` table 1:1 with words (consecutive_correct, total_attempts, total_correct, last_reviewed_at); insert trigger or application-managed on word creation
- [ ] 3.4 `[B]` `V006__create_word_indexes.sql`: `pg_trgm` GIN index on `arabic_text` + `translation`; standard B-tree on `mastery_level`, `dialect`, `difficulty`, `root_id`
- [ ] 3.5 `[B]` Domain: `Word`, `WordId`, `DictionaryLink`, `WordProgress` data classes; all enum types as Kotlin enums mirroring product-spec values exactly
- [ ] 3.6 `[B]` Infrastructure: `WordsTable`, `WordDictionaryLinksTable`, `WordProgressTable` (Exposed objects) + `WordRepository` interface + `ExposedWordRepository` impl including search + autocomplete queries
- [ ] 3.7 `[B]` Application: `WordService` — CRUD, search/filter, autocomplete, manage dictionary links; `Either<DomainError, T>` throughout
- [ ] 3.8 `[B]` Delivery: routes under `/api/v1/words` — `GET /` (paginated + all filters), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `GET /autocomplete?q=`, `GET /{id}/dictionary-links`, `POST /{id}/dictionary-links`, `DELETE /{id}/dictionary-links/{linkId}`
- [ ] 3.9 `[B]` `AiClient`: single wrapper around OpenRouter HTTP (Ktor client); reads `OPENROUTER_API_KEY` from env; returns `503 AI_NOT_CONFIGURED` if absent; structured output enforced
- [ ] 3.10 `[B]` Prompt template `word.examples`: given a word, returns 2 example sentences `{ arabic, transliteration, translation }`; `POST /api/v1/words/{id}/examples`
- [ ] 3.11 `[B]` Unit tests: `WordService` (MockK), enum validation, `derivedFrom` cycle guard
- [ ] 3.12 `[B]` Integration tests: CRUD routes, search/filter combinations, autocomplete, dictionary links CRUD, AI endpoint (mocked OpenRouter)

---

## Milestone 4 — Texts Domain (Plain View)

Unified `Text` entity — plain view only in this milestone. Interlinear (sentences + tokens) added in Milestone 5.

- [ ] 4.1 `[B]` `V007__create_texts.sql`: `texts` table (UUID PK, title, body, transliteration, translation, difficulty, dialect, comments, created_at, updated_at)
- [ ] 4.2 `[B]` `V008__create_text_tags.sql`: `text_tags` table (text_id FK, tag VARCHAR) — simple join table, no PK needed beyond composite
- [ ] 4.3 `[B]` Domain: `Text`, `TextId` data classes; tags as `List<String>`
- [ ] 4.4 `[B]` Infrastructure: `TextsTable`, `TextTagsTable` + `TextRepository` interface + `ExposedTextRepository` impl (tags loaded with text in one query)
- [ ] 4.5 `[B]` Application: `TextService` — CRUD + advanced search (title, body free text, dialect, difficulty, tag); `Either<DomainError, T>`
- [ ] 4.6 `[B]` Delivery: routes under `/api/v1/texts` — `GET /` (paginated + filters), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`
- [ ] 4.7 `[B]` Prompt template `text.summarize`: `POST /api/v1/texts/{id}/summarize`
- [ ] 4.8 `[B]` Unit tests: `TextService` (MockK), tag handling
- [ ] 4.9 `[B]` Integration tests: CRUD, search/filter combinations, tag filtering

---

## Milestone 5 — Sentences + Alignment Tokens

Interlinear view — the most behaviourally complex domain. Stale-token logic is the critical invariant.

- [ ] 5.1 `[B]` `V009__create_sentences.sql`: `sentences` table (UUID PK, text_id FK, position INT, arabic_text, transliteration, free_translation, notes, tokens_valid BOOLEAN DEFAULT true, created_at, updated_at)
- [ ] 5.2 `[B]` `V010__create_alignment_tokens.sql`: `alignment_tokens` table (UUID PK, sentence_id FK, position INT, arabic, transliteration, translation, word_id FK nullable); index on `sentence_id`
- [ ] 5.3 `[B]` Domain: `Sentence`, `SentenceId`, `AlignmentToken` data classes; `tokensValid: Boolean` on `Sentence`
- [ ] 5.4 `[B]` Domain rule: updating `Sentence.arabicText` sets `tokensValid = false` and must be enforced in the service (not the DB trigger)
- [ ] 5.5 `[B]` Infrastructure: `SentencesTable`, `AlignmentTokensTable` + `SentenceRepository` interface + impl; tokens always loaded with their sentence
- [ ] 5.6 `[B]` Application: `SentenceService` — CRUD sentences (position reordering), manage tokens (replace all / clear / reorder), enforce stale-token invalidation on sentence text edit
- [ ] 5.7 `[B]` Delivery: nested routes under `/api/v1/texts/{textId}/sentences` — `GET /`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/tokens` (replace all), `DELETE /{id}/tokens` (clear with confirmation flag), `POST /{id}/auto-tokenize`
- [ ] 5.8 `[B]` Prompt template `sentence.transliterate`: `POST /api/v1/texts/{textId}/sentences/{id}/transliterate`
- [ ] 5.9 `[B]` Unit tests: stale-token invalidation logic, position reordering, `SentenceService` (MockK)
- [ ] 5.10 `[B]` Integration tests: sentence CRUD within a text, token replacement, stale-token scenario, auto-tokenize

---

## Milestone 6 — Annotations Domain

Texts ↔ Words cross-reference. The reverse lookup (word → texts) is a first-class feature.

- [ ] 6.1 `[B]` `V011__create_annotations.sql`: `annotations` table (UUID PK, text_id FK, anchor VARCHAR, type VARCHAR CHECK, content, mastery_level VARCHAR CHECK, review_flag BOOLEAN, created_at, updated_at)
- [ ] 6.2 `[B]` `V012__create_annotation_words.sql`: `annotation_words` join table (annotation_id FK, word_id FK; composite PK); index on both FKs
- [ ] 6.3 `[B]` Domain: `Annotation`, `AnnotationId` data classes; `linkedWordIds: List<WordId>`
- [ ] 6.4 `[B]` Infrastructure: `AnnotationsTable`, `AnnotationWordsTable` + `AnnotationRepository` interface + impl; words loaded with annotation
- [ ] 6.5 `[B]` Application: `AnnotationService` — CRUD, manage word links; reverse lookup `getTextsForWord(wordId)`
- [ ] 6.6 `[B]` Delivery: nested under `/api/v1/texts/{textId}/annotations` — full CRUD + `POST /{id}/words` + `DELETE /{id}/words/{wordId}`; plus `GET /api/v1/words/{wordId}/annotations` for reverse lookup
- [ ] 6.7 `[B]` Unit tests: `AnnotationService` (MockK), word-link management
- [ ] 6.8 `[B]` Integration tests: annotation CRUD, word linking/unlinking, reverse lookup, mastery/review flag updates

---

## Milestone 7 — Audio Attachments

Words and texts can have a single audio file each.

- [ ] 7.1 `[B]` `V013__create_audio.sql`: `audio_files` table (UUID PK, entity_type VARCHAR CHECK, entity_id UUID, filename, content_type, size_bytes, created_at); unique on `(entity_type, entity_id)`
- [ ] 7.2 `[B]` Storage strategy: local filesystem under a configurable `AUDIO_STORAGE_PATH`; served as static files (or via endpoint)
- [ ] 7.3 `[B]` `AudioRepository` + `AudioService` — store, retrieve, delete file; validation: MP3/WAV/M4A only, max 50 MB
- [ ] 7.4 `[B]` Routes: `POST /api/v1/words/{id}/audio` (multipart upload), `GET /api/v1/words/{id}/audio`, `DELETE /api/v1/words/{id}/audio`; same for `/texts/{id}/audio`
- [ ] 7.5 `[B]` Integration tests: upload, serve, delete, format rejection, duplicate replacement

---

## Milestone 8 — Training / SRS Domain

Session-based SRS. Mastery promotion is the core invariant.

- [ ] 8.1 `[B]` `V014__create_training.sql`: `training_sessions` table (UUID PK, mode VARCHAR CHECK, status VARCHAR CHECK, created_at, completed_at); `training_session_words` (session_id FK, word_id FK, position, result VARCHAR CHECK nullable)
- [ ] 8.2 `[B]` Domain: `TrainingSession`, `TrainingSessionWord`; enums `SessionMode`, `TrainingResult`; mastery promotion thresholds as named constants
- [ ] 8.3 `[B]` Application: `TrainingService` — create session (select + shuffle words by mode), record result per word, complete session (promote mastery via `WordProgress`); purge oldest N sessions
- [ ] 8.4 `[B]` Delivery: `POST /api/v1/training/sessions` (create), `GET /api/v1/training/sessions/{id}`, `POST /api/v1/training/sessions/{id}/words/{wordId}/result`, `POST /api/v1/training/sessions/{id}/complete`, `DELETE /api/v1/training/sessions` (purge, `?keep=N`)
- [ ] 8.5 `[B]` Unit tests: mastery promotion logic (all thresholds, streak reset on incorrect), session word selection by mode
- [ ] 8.6 `[B]` Integration tests: full session lifecycle, mastery progression over multiple sessions, purge

---

## Milestone 9 — Transliteration Service

Copy character map from `an-na7wi`, wrap in a clean service, expose as API endpoint.

- [ ] 9.1 `[B]` `TransliterationService`: copy Arabic→Latin/chat-alphabet map from `an-na7wi/backend/.../TransliterationService.kt`; pure function, no framework deps
- [ ] 9.2 `[B]` `POST /api/v1/transliterate` — `{ arabic: String }` → `{ transliteration: String }`
- [ ] 9.3 `[B]` Unit tests: all major character mappings, edge cases (empty string, mixed text)

---

## Milestone 10 — Search + Analytics

Read-only features. Depend on all prior domains.

- [ ] 10.1 `[B]` Global search: `GET /api/v1/search?q=` — results grouped by type (`texts`, `words`, `annotations`); uses `pg_trgm` and `unaccent`
- [ ] 10.2 `[B]` Advanced text search: title, body free-text, dialect, difficulty, tag — combinable; pushed to SQL (no in-memory filtering)
- [ ] 10.3 `[B]` Vocabulary autocomplete: already done in 3.8 — confirm it covers token-to-vocabulary linking use case
- [ ] 10.4 `[B]` Analytics endpoint `GET /api/v1/analytics`: overview counts, content distribution, mastery breakdown, review queue size, 30-day activity, study streak, root statistics — single endpoint returning structured response
- [ ] 10.5 `[B]` Unit tests: streak calculation logic
- [ ] 10.6 `[B]` Integration tests: search result grouping, analytics counts match inserted data

---

## Milestone 11 — Backend Hardening

Before frontend starts. Backend must be solid.

- [ ] 11.1 `[B]` Request validation: all POST/PUT routes validate input via `RequestValidation` plugin before hitting service layer; invalid → 422
- [ ] 11.2 `[B]` OpenAPI spec completeness pass: every route documented, all request/response shapes present, importable into Postman without modification
- [ ] 11.3 `[B]` `derivedFrom` cycle guard: `WordService` rejects circular `derivedFrom` references; depth-limited graph traversal
- [ ] 11.4 `[B]` Backup setup: Ofelia cron container in `docker-compose.yml`; daily `pg_dump --format=custom` to named volume; `just restore F=<file>` recipe
- [ ] 11.5 `[B]` `./gradlew test` timing baseline: must complete in under 2 minutes on a warm Testcontainers image
- [ ] 11.6 `[I]` GitHub Actions `release.yml`: on `v*` tags — build Docker image, push to GHCR, create release with git-cliff changelog

---

## Milestone 12 — Frontend Foundation

SvelteKit scaffold, design system baseline, API type generation.

- [ ] 12.1 `[F]` `pnpm create svelte frontend` at repo root; Svelte 5, TypeScript strict, Tailwind v4
- [ ] 12.2 `[F]` Install and configure shadcn-svelte; copy base components into `src/lib/components/ui/`
- [ ] 12.3 `[F]` Design tokens: colors, spacing, typography as CSS custom properties in `app.css`; Arabic font stack wired (Noto Naskh Arabic, Lateef, Markazi Text)
- [ ] 12.4 `[F]` `.arabic` / `.transliteration` CSS classes; verify RTL layout with a placeholder component
- [ ] 12.5 `[F]` `pnpm generate:types` script: `openapi-typescript` fetching from `/api/v1/openapi.json` → `src/lib/api/types.gen.ts`; commit output
- [ ] 12.6 `[F]` HTTP client module `src/lib/api/client.ts`: typed fetch wrapper using generated types
- [ ] 12.7 `[F]` `@tanstack/svelte-query` provider wired in root layout
- [ ] 12.8 `[F]` Base UI components: `Button` (primary/secondary/outline/danger), `Input`, `Badge`, `Modal`, `Pagination`, `Card` — all variants, nothing one-off
- [ ] 12.9 `[F]` `just frontend` starts dev server; `just run` starts full stack

---

## Milestone 13 — Frontend: Roots

- [ ] 13.1 `[F]` `RootsStore`: svelte-query queries for list + single root
- [ ] 13.2 `[F]` `/roots` page: paginated list, letter-count filter, search by normalized form
- [ ] 13.3 `[F]` Root detail page `/roots/[id]`: display root + word family browser (all linked words + derivation chains)
- [ ] 13.4 `[F]` Create/edit root form: normalization preview (calls `/api/v1/roots/normalize` live)
- [ ] 13.5 `[F]` Component tests: root form validation, normalization preview

---

## Milestone 14 — Frontend: Vocabulary

- [ ] 14.1 `[F]` `WordsStore`: list (all filters), single word, autocomplete
- [ ] 14.2 `[F]` `/words` page: paginated list with filter bar (dialect, difficulty, POS, mastery), search box (Arabic + translation)
- [ ] 14.3 `[F]` Word detail page `/words/[id]`: all fields, dictionary links, audio player/recorder, derivation chain, linked annotations (reverse lookup)
- [ ] 14.4 `[F]` Create/edit word form: root autocomplete, derivedFrom autocomplete, dictionary link management
- [ ] 14.5 `[F]` AI example generation: trigger button → show structured suggestion → accept/discard
- [ ] 14.6 `[F]` Audio: record in-browser or upload file; play/delete existing
- [ ] 14.7 `[F]` Component tests: word form, filter combinations, audio component

---

## Milestone 15 — Frontend: Texts + Annotations

- [ ] 15.1 `[F]` `TextsStore`: list (all filters), single text with sentences and annotations
- [ ] 15.2 `[F]` `/texts` page: paginated list with filter bar (dialect, difficulty, tag), free-text search
- [ ] 15.3 `[F]` Text plain view `/texts/[id]`: Arabic / transliteration / translation in aligned columns; click-to-annotate (word/phrase selection → annotation form)
- [ ] 15.4 `[F]` Annotation panel: list annotations for a text, show type/mastery/review flag, link/unlink vocabulary words (autocomplete)
- [ ] 15.5 `[F]` Text interlinear view toggle: switch between plain and gloss view on the same text
- [ ] 15.6 `[F]` Interlinear editor: sentence list, reorder sentences; per-sentence: edit Arabic text (shows stale-token warning if tokens exist), manage tokens (manual + AI auto-tokenize), link tokens to vocabulary words
- [ ] 15.7 `[F]` Stale-token UX: when sentence text edited and `tokensValid = false`, show banner with "Re-tokenize" and "Mark as valid" actions
- [ ] 15.8 `[F]` Create/edit text form: all fields, tag input (freeform array)
- [ ] 15.9 `[F]` Component tests: annotation creation flow, stale-token warning, interlinear editor

---

## Milestone 16 — Frontend: Training

- [ ] 16.1 `[F]` `TrainingStore`: create session, record result, complete session
- [ ] 16.2 `[F]` `/training` page: session setup (mode selector + length slider)
- [ ] 16.3 `[F]` Flashcard view: show front (Arabic or translation, randomised), reveal back, CORRECT / INCORRECT / SKIP buttons
- [ ] 16.4 `[F]` Session summary: accuracy by mode, mastery promotions, session word list
- [ ] 16.5 `[F]` Component tests: flashcard flip, result recording, summary display

---

## Milestone 17 — Frontend: Analytics

- [ ] 17.1 `[F]` `/analytics` page: overview counts, mastery distribution chart, content distribution (POS, dialect), 30-day activity calendar, study streak, root statistics
- [ ] 17.2 `[F]` Component tests: streak display, empty state

---

## Milestone 18 — Data Migration from an-na7wi

Import existing data from the old Postgres instance into na7wi.

- [ ] 18.1 `[I]` Map `an-na7wi` schema → na7wi schema: document all field renames and structural changes
- [ ] 18.2 `[I]` Write migration script (SQL or Kotlin CLI): roots → words (root_id FK resolution) → texts (unified, drop InterlinearText split) → sentences → alignment_tokens → annotations → annotation_words → training_sessions → word_progress
- [ ] 18.3 `[I]` Handle renamed fields: `InterlinearSentence.translation` → `free_translation`, `InterlinearSentence.annotations` → `notes`
- [ ] 18.4 `[I]` Handle removed data: `text_versions` (drop), separate `interlinear_texts` (merge into `texts`)
- [ ] 18.5 `[I]` `just migrate-from-old F=<old_dump>` recipe: restore old dump to temp DB → run script → verify counts
- [ ] 18.6 `[I]` Validation queries: row counts match for each entity type; FK integrity checks pass; spot-check 10 random words/texts

---

## Milestone 19 — E2E Tests + Final Hardening

- [ ] 19.1 `[F]` Playwright setup: `pnpm test:e2e` against full stack
- [ ] 19.2 `[F]` E2E: flashcard session (create → complete → verify mastery change)
- [ ] 19.3 `[F]` E2E: annotation creation on a text + word link
- [ ] 19.4 `[F]` E2E: interlinear alignment editing with stale-token flow
- [ ] 19.5 `[I]` CI: add `pnpm test:e2e` to `ci.yml` (runs against docker-compose stack)
- [ ] 19.6 `[I]` Final README: what it is, `just run`, link to `docs/spec/`

---

## Backlog (deferred — not in scope for initial migration)

- Monitoring stack (Prometheus + Grafana + cAdvisor + postgres-exporter via Docker Compose profiles)
- Backblaze B2 / S3 sync for backups via rclone
- `github-release.yml` with git-cliff changelog
- Mobile-optimised flashcard view
