## Milestone 7 — Annotations Domain

Texts ↔ Words cross-reference. The reverse lookup (word → texts) is a first-class feature.

- [ ] 7.0 `[B]` Update `documentation.yaml`: add all new schemas (AnnotationResponse, CreateAnnotationRequest, UpdateAnnotationRequest) and all new paths (`/api/v1/texts/{textId}/annotations` CRUD + word-link sub-resource + reverse lookup `GET /api/v1/words/{wordId}/annotations`) before implementing routes
- [ ] 7.1 `[B]` `V011__create_annotations.sql`: `annotations` table (UUID PK, text_id FK, anchor VARCHAR, type VARCHAR CHECK, content, mastery_level VARCHAR CHECK, review_flag BOOLEAN, created_at, updated_at)
- [ ] 7.2 `[B]` `V012__create_annotation_words.sql`: `annotation_words` join table (annotation_id FK, word_id FK; composite PK); index on both FKs
- [ ] 7.3 `[B]` Domain: `Annotation`, `AnnotationId` data classes; `linkedWordIds: List<WordId>`
- [ ] 7.4 `[B]` Infrastructure: `AnnotationsTable`, `AnnotationWordsTable` + `AnnotationRepository` interface + impl; words loaded with annotation
- [ ] 7.5 `[B]` Application: `AnnotationService` — CRUD, manage word links; reverse lookup `getTextsForWord(wordId)`
- [ ] 7.6 `[B]` Delivery: nested under `/api/v1/texts/{textId}/annotations` — full CRUD + `POST /{id}/words` + `DELETE /{id}/words/{wordId}`; plus `GET /api/v1/words/{wordId}/annotations` for reverse lookup
- [ ] 7.7 `[B]` Unit tests: `AnnotationService` (MockK), word-link management
- [ ] 7.8 `[B]` Integration tests: annotation CRUD, word linking/unlinking, reverse lookup, mastery/review flag updates
