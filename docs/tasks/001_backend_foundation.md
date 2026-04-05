## Milestone 1 — Backend Foundation

The Ktor application starts, connects to Postgres, serves health + OpenAPI. No domain logic yet.

- [x] 1.1 `[B]` Package structure: `delivery/`, `application/`, `domain/`, `infrastructure/` under `com.tonihacks.qalam`
- [x] 1.2 `[B]` `Application.kt`: `EngineMain` (reads `application.conf`) with config loaded from environment (port, DB URL)
- [x] 1.3 `[B]` Ktor plugins registered: `ContentNegotiation` (kotlinx.serialization), `StatusPages`, `RequestValidation`, `CORS`, `CallLogging`
- [x] 1.4 `[B]` Database connection: Exposed + HikariCP pool; `Database.connect()` on startup
- [x] 1.5 `[B]` Flyway integration: auto-run migrations on startup from `src/main/resources/db/migration/`
- [x] 1.6 `[B]` `V001__create_extensions.sql`: enable `uuid-ossp`, `pg_trgm`, `unaccent`
- [x] 1.7 `[B]` Koin module wiring: `startKoin {}` in `Application.kt`; empty modules for each domain layer to fill in later
- [x] 1.8 `[B]` `GET /health` → `{ "status": "ok" }` + `GET /api/v1/openapi.json` + `GET /api/v1/swagger-ui` (Ktor OpenAPI plugin or hand-wired)
- [x] 1.9 `[B]` Typed error framework: `DomainError` sealed class; `StatusPages` handler mapping domain errors to HTTP responses; `ErrorResponse` DTO `{ error, code }`
- [x] 1.10 `[B]` Pagination framework: `PaginatedResponse<T>` DTO + `PageRequest` value class; shared across all list endpoints
- [x] 1.11 `[B]` Integration test harness: base test class that starts Testcontainers Postgres + Ktor `testApplication {}` + Flyway migrations; one smoke test asserting `/health` returns 200
