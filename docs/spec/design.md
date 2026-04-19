# Technical Design — na7wi Rewrite

> The target architecture for the rewritten application.
> Concrete technology choices, architectural principles, and ops setup.
> Folder structure and implementation details are decided while coding, not here.
>
> **Reference**: The previous implementation lives at `an-na7wi` (path:
> `/Users/antoniomasotti/toni/100_programming/190_frontend/an-na7wi`). Consult it for
> data model details, migration history (22 SQL files), API contracts, and CSS patterns.

The new name for this project will be `Qalam` (successor of an-na7wi).
---

## Stack

| Layer                      | Technology                                                                                                                                                                                                      |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Backend language           | Kotlin 2.3.20, JDK 25 (or latest LTS compatible with tooling)                                                                                                                                                   |
| Backend framework          | Ktor 3.4.x (embeddedServer, DSL routing)                                                                                                                                                                        |
| Database ORM               | Exposed 1.x (SQL DSL — not DAO pattern). **Note**: Exposed 1.x moved to `org.jetbrains.exposed.v1.*` — all imports use this package, not the `org.jetbrains.exposed.sql.*` you'll find in older docs/tutorials. |
| Migrations                 | Flyway (SQL files)                                                                                                                                                                                              |
| Database                   | PostgreSQL 17                                                                                                                                                                                                   |
| Serialization              | kotlinx.serialization                                                                                                                                                                                           |
| HTTP client (backend)      | Ktor client (for OpenRouter calls)                                                                                                                                                                              |
| DI                         | Koin (when dependency wiring justifies it)                                                                                                                                                                      |
| Frontend framework         | SvelteKit (Svelte 5, runes API)                                                                                                                                                                                 |
| Component library          | shadcn-svelte (copy-paste, owned components)                                                                                                                                                                    |
| Server state               | @tanstack/svelte-query                                                                                                                                                                                          |
| Forms                      | sveltekit-superforms + Zod                                                                                                                                                                                      |
| Styling                    | Tailwind CSS v4                                                                                                                                                                                                 |
| API types (frontend)       | openapi-typescript (generated from backend OpenAPI spec)                                                                                                                                                        |
| AI provider                | OpenRouter (OpenAI-compatible)                                                                                                                                                                                  |
| Build tool (frontend)      | Vite (via SvelteKit)                                                                                                                                                                                            |
| Package manager (frontend) | pnpm                                                                                                                                                                                                            |
| Container runtime          | Docker + Docker Compose                                                                                                                                                                                         |
| Secret management          | Doppler                                                                                                                                                                                                         |
| Task runner                | just (justfile)                                                                                                                                                                                                 |
| CI                         | GitHub Actions                                                                                                                                                                                                  |

---

## Backend Architecture

The backend follows **Clean Architecture / Onion Architecture** principles. Dependencies point
inward — outer layers know about inner layers, never the reverse.

```
┌─────────────────────────────────────┐
│         Delivery (Ktor routes)      │  HTTP in/out, DTOs, serialization
├─────────────────────────────────────┤
│         Application (Services)      │  Use cases, business rules
├─────────────────────────────────────┤
│         Domain                      │  Entities, value objects, enums
├─────────────────────────────────────┤
│         Infrastructure              │  DB (Exposed), external APIs, file storage
└─────────────────────────────────────┘
```

**Rules**:
- Domain has zero dependencies on any framework, ORM, or HTTP library.
- Application services depend on domain and on infrastructure *interfaces* — not on
  infrastructure implementations directly. This makes services testable with MockK.
- Ktor routes (delivery) call application services. They never call repositories or the DB.
- Exposed tables and SQL queries live in infrastructure. They never leak into domain.
- Koin wires implementations to interfaces at startup. Services receive their dependencies
  via constructor injection — no service locator pattern, no `KoinComponent` everywhere.

### Error Handling

Services return `Either<DomainError, T>` (Arrow). They never throw. Routes handle the left case
explicitly via `fold`:

```kotlin
service.doThing().fold(
    { error -> call.respondError(it) },   // delivery/ErrorMapping.kt extension
    { result -> call.respond(result) },
)
```

`DomainError` is a sealed class with no framework imports (domain stays pure). The HTTP status
mapping lives in `delivery/ErrorMapping.kt` as an extension function. `StatusPages` is only a
catch-all for truly unexpected `Throwable`s, not for domain errors.

---

## Frontend Architecture

```
routes/ (SvelteKit pages)         ← thin: compose components, load data via stores
  └── components/                 ← UI only: no API calls, no business logic
        └── lib/stores/           ← server data state, powered by svelte-query
              └── lib/api/        ← typed HTTP client (openapi-typescript)
```

**Rules**:
- Components call stores or accept props. Components never call the API directly.
- UI state (modal open/closed, hover, selection, multi-select) lives in the component that
  owns it — never in stores.
- Stores hold server data and expose typed actions. They do not hold modal state.
- Route files (`+page.svelte`) are thin: they read stores, render components. No logic.

### Design System

The frontend has a **single, consistent design system**. Every interactive element (button,
input, badge, modal, dropdown, pagination, card) is implemented once as a component and reused
everywhere. No one-off inline styles for things that have a component equivalent.

- shadcn-svelte provides the base layer: components are copy-pasted into `lib/components/ui/`
  and fully owned. No runtime npm dependency on shadcn-svelte.
- Each component has clear variants (e.g., Button: primary / secondary / outline / danger).
  A new variant is only added when there is a genuine design reason — not for minor spacing
  differences.
- Tailwind utility classes are used for layout and spacing. Domain-specific visual rules
  (Arabic text direction, font choices) are encapsulated in named CSS classes, not repeated
  inline.

### Arabic / RTL

The Arabic font stack from `an-na7wi` is kept exactly:
- **Body Arabic**: Noto Naskh Arabic, Lateef (serif fallback)
- **Display Arabic**: Lateef, Noto Naskh Arabic
- **Text Arabic**: Markazi Text, Noto Naskh Arabic

These fonts were chosen with care for readability at body size and are not changed.

```css
.arabic {
    font-family: 'Noto Naskh Arabic', 'Lateef', serif;
    direction: rtl;
    text-align: right;
}
.transliteration {
    direction: ltr;
    font-style: italic;
}
```

Tailwind `rtl:` variants handle directional layout adjustments per component.

---

## Database Design

### PostgreSQL Extensions

`uuid-ossp`, `pg_trgm` (trigram search), `unaccent`. Enabled in V001 migration.

### Key Decisions

- **Enums as VARCHAR** with CHECK constraints. Never PostgreSQL ENUM types (ALTER TYPE requires
  table lock and is painful to manage). Inherited from `an-na7wi` V15 migration.
- **UUID primary keys** generated in application code, not in the database.
- **No auto-DDL**. Schema is defined entirely in Flyway SQL migrations.
- **No lazy loading**. Exposed's SQL DSL has no session/proxy concept — all joins are explicit.
  The entire class of `LazyInitializationException` from the old Panache/Hibernate stack does
  not exist here.
- **Circular FK removed**. The `Text ↔ TextVersion` circular foreign key from `an-na7wi` is
  eliminated by removing version history entirely (see product-spec).
- **`Text` is unified**. The old split between `Text` and `InterlinearText` is merged into a
  single `Text` entity with optional sentence/alignment data.

---

## Ops & Infrastructure

### Local Development

```
just run          # full stack: postgres + backend + frontend
just start-db     # postgres only
just backend      # Ktor with hot reload
just frontend     # SvelteKit dev server
just test         # all tests (backend + frontend)
```

No native image. No 30-minute builds. Ktor on JVM starts in under 5 seconds.

### Secret Management (Doppler)

Secrets are managed with **Doppler**, not `.env` files. The `.env` pattern from `an-na7wi` is
replaced. Doppler injects environment variables into the process at runtime.

```
just run          # runs: doppler run -- docker compose up
just backend      # runs: doppler run -- ./gradlew run
```

`.env` files are not committed and not expected to be present. The `doppler.yaml` project
config file is committed. A `DOPPLER_TOKEN` is the only secret needed in CI.

### Docker Compose

`docker-compose.yml` runs the development stack:
- `postgres` (postgres:17-alpine, health check, named volume)
- `backend` (Ktor JVM image, waits for postgres)
- `frontend` (Node + SvelteKit)

### Automated Database Backup

Backup is fully automated via **Ofelia** (a Docker-native cron job runner) as a container in
the compose stack. No manual shell scripts, no cron tab on the host.

Ofelia schedules `pg_dump` on a configurable interval (default: hourly). Dumps are written to a
named volume. An optional rclone container syncs dumps to Backblaze B2 or S3 on the same schedule.

Restore is a `just restore F=<dump-file>` recipe that:
1. Stops the backend container
2. Runs `pg_restore`
3. Restarts the backend container

No lines commented out. All steps are enforced in the recipe.

### Monitoring

Monitoring (Prometheus, Grafana, cAdvisor, postgres-exporter) is **optional and deferred**.
It will be designed and added as a separate implementation story after the core app is working.

When added, it will use Docker Compose profiles (`--profile monitoring`) — not a separate
copy-paste compose file.

### CI/CD (GitHub Actions)

- **ci.yml**: backend tests + frontend tests in parallel, on every push/PR
- **release.yml**: on `v*` tags — builds + pushes Docker images to GHCR, creates GitHub
  release with changelog (git-cliff)
- Secrets injected via `DOPPLER_TOKEN` in CI (not hardcoded)

---

## API Contract

### Base URL
`/api/v1/`

### Pagination
All list endpoints: `page` (1-based, default 1) + `size` (default 20, max 100).

Response shape:
```json
{ "items": [...], "total": 142, "page": 1, "size": 20 }
```

### Error Response
```json
{ "error": "Human-readable message", "code": "DOMAIN_ERROR_CODE" }
```

### OpenAPI

The spec is **hand-written** in `backend/src/main/resources/openapi/documentation.yaml`.
Two Ktor plugins serve it at startup — they are not redundant, they do different things:

| Plugin                | Endpoint               | Purpose                                                         |
|-----------------------|------------------------|-----------------------------------------------------------------|
| `ktor-server-openapi` | `/api/v1/openapi.json` | Serves the raw spec (used by Postman and `pnpm generate:types`) |
| `ktor-server-swagger` | `/api/v1/swagger-ui`   | Serves the interactive Swagger UI                               |

**Maintenance rule**: whenever a route is added or changed, update `documentation.yaml` in the
same commit. The YAML is the API contract — keeping it stale defeats its purpose.

`pnpm generate:types` (frontend) fetches `/api/v1/openapi.json` from a running backend to
regenerate the typed client. Run it after any API change.
