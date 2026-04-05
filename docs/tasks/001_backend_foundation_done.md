
## Milestone 0 — Project Skeleton

Infrastructure foundation. Nothing compiles or runs correctly until this is complete.

- [x] 0.1 `[I]` Fix Gradle build: uncomment Kotlin plugin, correct alias `kotlinJvm`, add `gradlePluginPortal()` to `pluginManagement`
- [x] 0.2 `[I]` Add `src/main/kotlin` and `src/test/kotlin` source sets; add a placeholder `Application.kt` so the project has a main entry point
- [x] 0.3 `[I]` Wire full dependency set in `build.gradle.kts`: Ktor server (netty), Exposed, Flyway, Koin, kotlinx.serialization, Logback, Kotest + MockK + Testcontainers for tests
- [x] 0.4 `[I]` `docker-compose.yml` at repo root: `postgres:17-alpine` with health check + named volume; no backend or frontend container yet
- [x] 0.5 `[I]` `justfile` at repo root with `start-db`, `backend`, `test`, `run` recipes (Doppler-wrapped)
- [x] 0.6 `[I]` `doppler.yaml` committed at repo root (project config stub — no secrets in file)
- [x] 0.7 `[I]` `flake.nix` devShell: JDK 25, Gradle, just, doppler, docker, pnpm, Node
- [x] 0.8 `[I]` GitHub Actions `ci.yml`: run `./gradlew test` on push/PR (Doppler token injected)
- [x] 0.9 `[B]` `detekt.yml` config wired and passing on empty project (already in `backend/config/detekt/`)
- [x] 0.10 `[I]` Write `README.md` with project overview, setup instructions, and development workflow. Reference the docs/ folder. Readme is nice to read, enjoyable and clear. Has badges and is appealing. Details are in docs/ folder and sub readmes.

---

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
