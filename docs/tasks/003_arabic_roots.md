## Milestone 3 — Arabic Roots Domain

Smallest domain — good for validating the full layer stack before adding complexity.

- [x] 3.1 `[B]` `V002__create_roots.sql`: `arabic_roots` table (UUID PK, letters array, normalized_form, display_form, letter_count, meaning, analysis, created_at, updated_at)
- [x] 3.2 `[B]` Domain: `ArabicRoot` data class, `RootId` value class, `CreateRootRequest` / `UpdateRootRequest` DTOs
- [x] 3.3 `[B]` Domain: `RootNormalizer` — accepts `"ر ح ب"` / `"رحب"` / `"ر-ح-ب"` / `"ر,ح,ب"` → canonical form; unit-tested
- [x] 3.4 `[B]` Infrastructure: `RootsTable` (Exposed object) + `RootRepository` interface + `ExposedRootRepository` impl
- [x] 3.5 `[B]` Application: `RootService` — CRUD + `normalize(input)` delegating to `RootNormalizer`; returns `Either<DomainError, T>`
- [x] 3.6 `[B]` Delivery: routes under `/api/v1/roots` — `GET /` (paginated, filter by letter count), `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`, `POST /normalize`
- [x] 3.7 `[B]` Koin: wire `RootRepository` → `ExposedRootRepository`, inject into `RootService`
- [x] 3.8 `[B]` Unit tests: `RootNormalizer` (all input variants), `RootService` (MockK repository)
- [x] 3.9 `[B]` Integration tests: all 6 routes, happy path + 404 + 422 validation errors
