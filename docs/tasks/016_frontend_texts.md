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

## Post-implementation fixes (code review pass)

- [x] `[B]` `reorder` endpoint `PUT /texts/{textId}/sentences/reorder` — two-phase position update avoids `UNIQUE (text_id, position)` constraint violation on swap
- [x] `[B]` `maxPosition` bug: `maxByOrNull { SentencesTable.position }` was comparing Column objects (all equal → first row returned). Fixed to `{ it[SentencesTable.position] }`
- [x] `[B]` Nullable field clearing in `SentenceService` and `TextService`: empty string = set null, null = keep existing (`clearable()` helper). Previously clearing transliteration/notes/comments was silently ignored.
- [x] `[F]` `useReorderSentences` hook — replaces broken move-up/down (which sent single-position updates, causing constraint violations)
- [x] `[F]` `useUpdateText` invalidation: added `exact: true` to avoid collateral sentence query invalidations
- [x] `[F]` `TokenEditor` draft sync: `$effect` re-syncs drafts when external token update changes the ID set (e.g., auto-tokenize fires while editor is open)
- [x] `[F]` `InterlinearSentence` / `StaleTokenBanner`: `isPending` prop threaded from page to prevent double-fire on stale banner actions
- [x] `[F]` `TokenGrid`: replaced fake `role="table"` ARIA with `<dl>/<dt>/<dd>` semantic markup per token; `.arabic` → `.arabic-text`
- [x] `[F]` `SentenceEditor`: delete error handling added (try/catch + error display); order buttons wired to `reorderSentences.isPending`
- [x] OpenAPI spec (`documentation.yaml`) updated with `reorderSentences` path and `ReorderSentencesRequest` schema; types regenerated
