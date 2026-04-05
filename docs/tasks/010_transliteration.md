## Milestone 10 — Transliteration Service

Copy character map from `an-na7wi`, wrap in a clean service, expose as API endpoint.

- [ ] 10.1 `[B]` `TransliterationService`: copy Arabic→Latin/chat-alphabet map from `an-na7wi/backend/.../TransliterationService.kt`; pure function, no framework deps
- [ ] 10.2 `[B]` `POST /api/v1/transliterate` — `{ arabic: String }` → `{ transliteration: String }`
- [ ] 10.3 `[B]` Unit tests: all major character mappings, edge cases (empty string, mixed text)
