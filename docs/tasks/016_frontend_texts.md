## Milestone 16 — Frontend: Texts (Interlinear View)

**Scope decision**: Only interlinear view is built. No plain 3-column view. No annotations (deferred to backlog). Display + edit mode toggle on `/texts/[id]`.

- [x] 16.0 `[F]` Re-run `pnpm generate:types` — types confirmed current
- [x] 16.1 `[F]` `TextsStore`: list (all filters), single text, sentences with tokens, CRUD texts, CRUD sentences, token ops (replace, clear, auto-tokenize, transliterate, mark-valid)
- [x] 16.2 `[F]` `/texts` page: paginated list with filter bar (dialect, difficulty, tag), free-text search
- [x] 16.3 `[F]` `/texts/new` — create text form (title, dialect, difficulty, body, transliteration, translation, tags, comments)
- [x] 16.4 `[F]` `/texts/[id]` display mode: interlinear sentence list (token triplet grid + free translation + notes per sentence), full Arabic+transliteration panel at bottom
- [x] 16.5 `[F]` `/texts/[id]` edit mode: text metadata editing, sentence add/delete/reorder, per-sentence edit (Arabic triggers stale-token), token editor (manual triplets), AI auto-tokenize, AI transliterate
- [x] 16.6 `[F]` Stale-token UX: banner when `tokensValid=false` with "Re-tokenize" and "Mark as valid" actions; mark-valid uses replaceTokens with existing tokens (sets tokensValid=true server-side)
- [x] 16.7 `[F]` Type-check pass: `pnpm check` — 0 errors, 0 warnings
