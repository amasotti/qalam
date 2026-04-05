## Milestone 5 — Texts Domain (Plain View)

Unified `Text` entity — plain view only in this milestone. Interlinear (sentences + tokens) added in Milestone 6.

- [ ] 5.1 `[B]` `V007__create_texts.sql`: `texts` table (UUID PK, title, body, transliteration, translation, difficulty, dialect, comments, created_at, updated_at)
- [ ] 5.2 `[B]` `V008__create_text_tags.sql`: `text_tags` table (text_id FK, tag VARCHAR) — simple join table, no PK needed beyond composite
- [ ] 5.3 `[B]` Domain: `Text`, `TextId` data classes; tags as `List<String>`
- [ ] 5.4 `[B]` Infrastructure: `TextsTable`, `TextTagsTable` + `TextRepository` interface + `ExposedTextRepository` impl (tags loaded with text in one query)
- [ ] 5.5 `[B]` Application: `TextService` — CRUD + advanced search (title, body free text, dialect, difficulty, tag); `Either<DomainError, T>`
- [ ] 5.6 `[B]` Delivery: routes under `/api/v1/texts` — `GET /` (paginated + filters), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`
- [ ] 5.7 `[B]` Prompt template `text.summarize`: `POST /api/v1/texts/{id}/summarize`
- [ ] 5.8 `[B]` Unit tests: `TextService` (MockK), tag handling
- [ ] 5.9 `[B]` Integration tests: CRUD, search/filter combinations, tag filtering
