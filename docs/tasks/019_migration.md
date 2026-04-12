## Milestone 19 — Data Migration from an-na7wi

Import existing data from the old Postgres instance into qalam.
Executed in two phases: Phase 1 is MVP (get data in to start daily use), Phase 2 is post-MVP.

---

### Phase 1 — MVP: Core data (roots, vocabulary, texts)

Run after M15 (vocabulary frontend) is live. Enough to replace an-na7wi for daily vocabulary review.

- [ ] 19.1 `[I]` Restore an-na7wi dump to a temp DB; document all field renames and structural changes (see ROADMAP.md quick-reference)
- [ ] 19.2 `[I]` Write SQL migration script — Phase 1 only:
  - `arabic_roots` (no deps)
  - `words` (resolve root_id FK from step above)
  - `word_dictionary_links` (expand JSON column → rows)
  - `word_progress` (rename table, drop `mastery_level_updated_at`)
  - `texts` (unified: merge `interlinear_texts` by title match; `arabic_content` → `body`; drop version columns)
  - `text_tags` (expand JSONB array → rows)
- [ ] 19.3 `[I]` `just migrate-from-old F=<old_dump>` recipe: restore old dump to temp DB → run script → verify counts
- [ ] 19.4 `[I]` Validation queries: row counts match; FK integrity checks pass; spot-check 20 random words and 5 texts in the new UI

### Phase 2 — Post-MVP: Sentences, annotations, training

Run after M7 (annotations backend) and M16 (texts frontend) are stable.

- [ ] 19.5 `[I]` Extend migration script:
  - `sentences` (from `interlinear_sentences`: `sentence_order`→`position`, `translation`→`free_translation`, `annotations`→`notes`)
  - `alignment_tokens` (from `word_alignments`: spot-check multi-word token rows before splitting)
  - `annotations` + `annotation_words`
  - `training_sessions` + `training_session_words`
- [ ] 19.6 `[I]` Full validation pass: all FK integrity checks, spot-check interlinear view for 3 migrated texts

### Key schema changes reference

| an-na7wi | qalam | Notes |
|----------|-------|-------|
| `words.arabic` | `arabic_text` | Rename |
| `words.example` | `example_sentence` | Rename |
| `words.pronunciation_link` | `pronunciation_url` | Rename |
| `words.dictionary_links` (JSON) | `word_dictionary_links` table | Denormalize → rows |
| `texts.arabic_content` | `body` | Rename |
| `texts.tags` (JSONB array) | `text_tags` rows | Expand array |
| `texts` + `interlinear_texts` | unified `texts` | Merge by title; drop version columns |
| `interlinear_sentences.translation` | `sentences.free_translation` | Rename (bug fix) |
| `interlinear_sentences.annotations` | `sentences.notes` | Rename (bug fix) |
| `word_alignments` | `alignment_tokens` | Rename table |
| `word_progress_tracking` | `word_progress` | Rename table |
