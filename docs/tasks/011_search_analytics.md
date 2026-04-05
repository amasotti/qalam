## Milestone 11 — Search + Analytics

Read-only features. Depend on all prior domains.

- [ ] 11.1 `[B]` Global search: `GET /api/v1/search?q=` — results grouped by type (`texts`, `words`, `annotations`); uses `pg_trgm` and `unaccent`
- [ ] 11.2 `[B]` Advanced text search: title, body free-text, dialect, difficulty, tag — combinable; pushed to SQL (no in-memory filtering)
- [ ] 11.3 `[B]` Vocabulary autocomplete: already done in 4.8 — confirm it covers token-to-vocabulary linking use case
- [ ] 11.4 `[B]` Analytics endpoint `GET /api/v1/analytics`: overview counts, content distribution, mastery breakdown, review queue size, 30-day activity, study streak, root statistics — single endpoint returning structured response
- [ ] 11.5 `[B]` Unit tests: streak calculation logic
- [ ] 11.6 `[B]` Integration tests: search result grouping, analytics counts match inserted data
