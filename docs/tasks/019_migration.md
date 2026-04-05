## Milestone 19 — Data Migration from an-na7wi

Import existing data from the old Postgres instance into qalam.

- [ ] 19.1 `[I]` Map `an-na7wi` schema → qalam schema: document all field renames and structural changes
- [ ] 19.2 `[I]` Write migration script (SQL or Kotlin CLI): roots → words (root_id FK resolution) → texts (unified, drop InterlinearText split) → sentences → alignment_tokens → annotations → annotation_words → training_sessions → word_progress
- [ ] 19.3 `[I]` Handle renamed fields: `InterlinearSentence.translation` → `free_translation`, `InterlinearSentence.annotations` → `notes`
- [ ] 19.4 `[I]` Handle removed data: `text_versions` (drop), separate `interlinear_texts` (merge into `texts`)
- [ ] 19.5 `[I]` `just migrate-from-old F=<old_dump>` recipe: restore old dump to temp DB → run script → verify counts
- [ ] 19.6 `[I]` Validation queries: row counts match for each entity type; FK integrity checks pass; spot-check 10 random words/texts
