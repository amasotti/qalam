## Milestone 9 — Training / SRS Domain

Session-based SRS. Mastery promotion is the core invariant.

- [ ] 9.0 `[B]` Update `documentation.yaml`: add all new schemas (enums: SessionMode, TrainingResult; bodies: TrainingSessionResponse, CreateSessionRequest, RecordResultRequest) and all new paths (`/api/v1/training/sessions` CRUD + result + complete + purge) before implementing routes
- [ ] 9.1 `[B]` `V014__create_training.sql`: `training_sessions` table (UUID PK, mode VARCHAR CHECK, status VARCHAR CHECK, created_at, completed_at); `training_session_words` (session_id FK, word_id FK, position, result VARCHAR CHECK nullable)
- [ ] 9.2 `[B]` Domain: `TrainingSession`, `TrainingSessionWord`; enums `SessionMode`, `TrainingResult`; mastery promotion thresholds as named constants
- [ ] 9.3 `[B]` Application: `TrainingService` — create session (select + shuffle words by mode), record result per word, complete session (promote mastery via `WordProgress`); purge oldest N sessions
- [ ] 9.4 `[B]` Delivery: `POST /api/v1/training/sessions` (create), `GET /api/v1/training/sessions/{id}`, `POST /api/v1/training/sessions/{id}/words/{wordId}/result`, `POST /api/v1/training/sessions/{id}/complete`, `DELETE /api/v1/training/sessions` (purge, `?keep=N`)
- [ ] 9.5 `[B]` Unit tests: mastery promotion logic (all thresholds, streak reset on incorrect), session word selection by mode
- [ ] 9.6 `[B]` Integration tests: full session lifecycle, mastery progression over multiple sessions, purge
