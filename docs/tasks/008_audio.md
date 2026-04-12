## Milestone 8 — Audio Attachments

Words and texts can have a single audio file each.

- [ ] 8.0 `[B]` Update `documentation.yaml`: add all new schemas (AudioFileResponse) and all new paths (upload/download/delete for `GET|POST|DELETE /api/v1/words/{id}/audio` and `/api/v1/texts/{id}/audio`) before implementing routes
- [ ] 8.1 `[B]` `V013__create_audio.sql`: `audio_files` table (UUID PK, entity_type VARCHAR CHECK, entity_id UUID, filename, content_type, size_bytes, created_at); unique on `(entity_type, entity_id)`
- [ ] 8.2 `[B]` Storage strategy: local filesystem under a configurable `AUDIO_STORAGE_PATH`; served as static files (or via endpoint)
- [ ] 8.3 `[B]` `AudioRepository` + `AudioService` — store, retrieve, delete file; validation: MP3/WAV/M4A only, max 50 MB
- [ ] 8.4 `[B]` Routes: `POST /api/v1/words/{id}/audio` (multipart upload), `GET /api/v1/words/{id}/audio`, `DELETE /api/v1/words/{id}/audio`; same for `/texts/{id}/audio`
- [ ] 8.5 `[B]` Integration tests: upload, serve, delete, format rejection, duplicate replacement
