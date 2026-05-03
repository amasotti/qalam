# Technical Design — Qalam

> The target architecture for the application.
> Concrete technology choices, architectural principles, and ops setup.
> Folder structure and implementation details are decided while coding, not here.
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

The frontend has a **single, consistent design system**: **Busatan**. Every interactive element
(button, input, badge, modal, dropdown, pagination, card) is expressed through Busatan tokens,
semantics, and shared visual rules. No one-off inline styles for things that have a Busatan
equivalent.

- Busatan is the only app-level design system. Routes and feature components must depend on
  Busatan primitives and semantic classes, not on raw shadcn theme semantics or Tailwind utility
  styling for app look and feel.
- Global CSS in `frontend/src/styles/*` is the source of truth for tokens, typography, layout,
  semantic roles, and shared visual patterns.
- Low-level controls in `lib/components/ui/` may use shadcn/Tailwind internally, but their public
  API and visual semantics must be Busatan-only. No runtime npm dependency on shadcn-svelte.
- Each component has clear variants (e.g., Button: primary / secondary / outline / danger). A
  new variant is only added when there is a genuine design reason — not for minor spacing
  differences.
- Inline `style=` is allowed only for dynamic values that cannot be expressed cleanly through
  classes, such as computed widths, CSS variables, or animation delays.
- Component-scoped `<style>` is allowed only for structure unique to a single component and only
  after checking existing global classes first.

### Arabic / RTL

Arabic font stack follows Busatan:
- **Body Arabic**: Noto Naskh Arabic, Amiri
- **Display Arabic**: Amiri, Noto Naskh Arabic
- **Text Arabic**: Noto Naskh Arabic, Amiri

These fonts are part of Busatan and are not swapped ad hoc per page or component.

```css
.arabic {
    font-family: 'Noto Naskh Arabic', 'Amiri', serif;
    direction: rtl;
    text-align: right;
}
.transliteration {
    direction: ltr;
    font-style: italic;
    color: var(--olive);
}
```

---

## Database Design

### PostgreSQL Extensions

`uuid-ossp`, `pg_trgm` (trigram search), `unaccent`. Enabled in V001 migration.

### Key Decisions

- **Enums as VARCHAR** with CHECK constraints. Never PostgreSQL ENUM types (ALTER TYPE requires
  table lock and is painful to manage).
- **UUID primary keys** generated in application code, not in the database.
- **No auto-DDL**. Schema is defined entirely in Flyway SQL migrations.
- **No lazy loading**. Exposed's SQL DSL has no session/proxy concept — all joins are explicit.
  The entire class of `LazyInitializationException` from the old Panache/Hibernate stack does
  not exist here.
- **Circular FK removed**. The `Text ↔ TextVersion` circular foreign key is eliminated by
  removing version history entirely (see product-spec).
- **`Text` is unified**. The old split between `Text` and `InterlinearText` is merged into a
  single `Text` entity with optional sentence/alignment data.

---

## Ops & Infrastructure

### Local Development

```
doppler run -- just up # full Docker stack: postgres + backend + frontend
just db           # postgres only
just dev-backend  # Ktor backend against Docker DB (requires Doppler)
just dev-frontend # SvelteKit dev server
just dev          # DB + local backend + local frontend
just test         # backend tests
```

No native image. No 30-minute builds. Ktor on JVM starts in under 5 seconds.

### Secret Management (Doppler)

Secrets are managed with **Doppler**, not `.env` files. Doppler injects environment variables into the process at runtime.

```
doppler run -- just up # full Docker stack; passes DOPPLER_TOKEN to compose
just dev-backend  # runs: doppler run -- ./backend/gradlew -p backend run
```

`.env` files are not committed and not expected to be present. The `doppler.yaml` project
config file is committed. The backend Docker service expects `DOPPLER_TOKEN` in the shell
environment; `doppler run -- just up` supplies it for local full-stack runs. A `DOPPLER_TOKEN`
is the only secret needed in CI.

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
