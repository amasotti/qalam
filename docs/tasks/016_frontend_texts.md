## Milestone 16 — Frontend: Texts + Annotations

- [ ] 16.0 `[F]` Re-run `pnpm generate:types` if backend `documentation.yaml` has changed since last type generation; commit updated `types.gen.ts` before writing any typed store/component code
- [ ] 16.1 `[F]` `TextsStore`: list (all filters), single text with sentences and annotations
- [ ] 16.2 `[F]` `/texts` page: paginated list with filter bar (dialect, difficulty, tag), free-text search
- [ ] 16.3 `[F]` Text plain view `/texts/[id]`: Arabic / transliteration / translation in aligned columns; click-to-annotate (word/phrase selection → annotation form)
- [ ] 16.4 `[F]` Annotation panel: list annotations for a text, show type/mastery/review flag, link/unlink vocabulary words (autocomplete)
- [ ] 16.5 `[F]` Text interlinear view toggle: switch between plain and gloss view on the same text
- [ ] 16.6 `[F]` Interlinear editor: sentence list, reorder sentences; per-sentence: edit Arabic text (shows stale-token warning if tokens exist), manage tokens (manual + AI auto-tokenize), link tokens to vocabulary words
- [ ] 16.7 `[F]` Stale-token UX: when sentence text edited and `tokensValid = false`, show banner with "Re-tokenize" and "Mark as valid" actions
- [ ] 16.8 `[F]` Create/edit text form: all fields, tag input (freeform array)
- [ ] 16.9 `[F]` Component tests: annotation creation flow, stale-token warning, interlinear editor
