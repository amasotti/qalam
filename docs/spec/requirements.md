# Engineering Requirements — Qalam

> The rules. Every architectural decision defers to this document.

---

## Core Principles

### 1. YAGNI and KISS, ruthlessly

- Build what the app needs today. Not what it might need in six months.
- No abstractions for a single use case. Two similar functions are fine; a premature base class
  is not.
- No feature flags, plugin systems, event buses, or multi-tenancy scaffolding.
- No configuration for behavior that never varies.
- A 30-line function beats a 5-class hierarchy.
- If you are writing an interface that will have exactly one implementation, delete the interface.

### 2. Clean layering without ceremony

Backend layers follow Clean / Onion Architecture (see design.md). The rule is simple:
dependencies point inward only.

- Routes call services. Routes do not call repositories directly.
- Services call repositories (via interfaces). Services do not construct HTTP responses.
- Repositories execute SQL. Repositories do not contain business rules.
- Domain classes have zero framework dependencies.
- **Koin** is the DI framework when wiring justifies it. Used properly: constructor injection,
  no service locator, no `KoinComponent` sprinkled on every class.

Frontend layers (see design.md):
- Components call stores or accept props. Components never call the API directly.
- UI state lives in components. Server data lives in stores.
- Route files are thin.

### 3. Types everywhere, runtime validation at boundaries

**Backend**:
- Kotlin sealed classes and enums for domain states. Exhaustive `when` enforced by the compiler.
- kotlinx.serialization for all JSON. No `Any`, no `Map<String, Any>`.
- Request DTOs validated at the route layer before the service is called. Invalid input → 422
  before reaching business logic.

**Frontend**:
- `openapi-typescript` generates types from the backend spec. Never hand-write types that can
  be generated.
- Zod schemas validate all form input at submission time.
- No `any` in TypeScript — enforced by the linter.
- The OpenAPI spec is the contract. If the backend changes a response shape, the frontend
  build fails at the type-check step, not at runtime.

### 4. Explicit over magic

- Ktor: explicit routing DSL, explicit plugin registration, explicit transaction blocks.
- Exposed: SQL DSL — you write what executes. No lazy loading, no proxy objects.
- Svelte runes: `$state`, `$derived` are explicit. Reactivity does not happen by accident.
- Flyway: SQL files. The schema is exactly what the SQL says.
- No annotation-driven behavior in the backend.

### 5. AI is optional infrastructure

- App is fully functional without `OPENROUTER_API_KEY`. AI routes return 503 with
  `{ "code": "AI_NOT_CONFIGURED" }` when the key is absent.
- All AI calls go through a single `AiClient` wrapper. The rest of the codebase does not know
  which model or provider is behind it.
- Prompts are defined as named templates in configuration/code — not typed freeform in the UI.
- Structured output (JSON schema-constrained responses) is enforced on all AI calls.

### 6. Design system discipline

- Every UI primitive (button, input, modal, badge, etc.) is implemented once and reused.
- No one-off inline styles for elements that have a component equivalent.
- Variants are added intentionally, not to accommodate minor differences.
- The Arabic font stack is fixed (see design.md) and not changed without a documented reason.

---

## Repository Structure

**Decision: Monorepo**

Single Git repository. `backend/` and `frontend/` at the root. Shared `justfile`, Doppler
config, `docker-compose.yml`, and CI at the root.

**Why**: solo developer, no team coordination overhead, breaking API changes visible in a single
commit, shared task recipes.

---

## Testing Strategy

### Backend

**Unit tests** — service layer, utility functions, root normalization, transliteration:
- JUnit 5 + MockK
- No containers, no framework startup. Run in seconds.

**Integration tests** — repositories, route handlers:
- JUnit 5 + Testcontainers (PostgreSQL 17)
- One test class per route group
- Full stack: HTTP request → route → service → repository → real database
- No mocking of the database. Testcontainers exists for this reason.
- `ktor-server-test-host` for in-process HTTP testing

**Coverage target**: 70% on service and route packages.

**`./gradlew test` must complete in under 2 minutes.** No native image. Ever.

### Frontend

**Unit tests** — components, stores, utilities:
- Vitest + @testing-library/svelte
- MSW for API mocking
- No snapshot tests (fragile, low signal)
- Tests exercise behavior, not implementation

**E2E tests** — critical paths:
- Playwright
- Three paths at minimum: flashcard session, annotation creation, interlinear alignment editing
- Run against full stack in CI (`pnpm test:e2e`)
- Not included in default `pnpm test`

**Coverage target**: 60% line coverage.

---

## Database Handling

### Migrations

- Flyway, SQL files only
- Naming: `V001__create_extensions.sql`, `V002__create_words.sql` (three-digit, double
  underscore, descriptive)
- One logical change per file. Schema and data migrations never in the same file.
- Never edit a committed migration. Corrections are new migrations.
- `flyway.cleanDisabled=true` in all non-development environments.
- Fresh start from V001 — no backward compatibility with legacy migrations.

### Schema Rules

- UUID PKs generated in application code
- Enums as VARCHAR with CHECK constraints (not PostgreSQL ENUM types)
- All FK columns have explicit `ON DELETE` behavior
- Indexes defined in migrations for all FK columns and hot filter/sort columns
- No auto-DDL

### Backup

Automated via Ofelia cron container (see design.md). Manual recipe: `just restore F=<file>`.
Full schema + data via `pg_dump --format=custom`.

---

## API Design Rules

- Plural nouns: `/words`, `/texts`, `/roots`
- Nested for hierarchy: `/texts/{id}/annotations`, `/texts/{id}/sentences/{sid}/tokens`
- Verbs only for explicit actions: `/roots/normalize`, `/training/sessions/{id}/complete`
- One `page` + `size` pair everywhere — no `pageSize`, no aliases
- Pagination response always: `{ items, total, page, size }`
- Error response always: `{ error, code }`
- HTTP status codes: 200, 201, 204, 400, 404, 422, 503, 500 — nothing exotic

---

## Dependency Management

- Gradle version catalog (`gradle/libs.versions.toml`), all versions pinned
- pnpm with committed lockfile, exact versions for direct dependencies
- shadcn-svelte: copy-paste into `src/lib/components/ui/` — not a runtime dep
- `openapi-typescript` output committed to the repo so API changes are code-reviewable

---

## Non-Negotiables

1. **No auth** — single-user tool, forever.
2. **No native image** — `./gradlew test` under 2 minutes, `./gradlew run` under 10 seconds.
3. **The API is primary** — every feature reachable via `curl`. The UI is a convenience layer.
4. **AI features are optional** — fully functional without `OPENROUTER_API_KEY`.
5. **No `any` in TypeScript** — enforced by linter.
6. **`just run` starts everything** — zero additional setup after `doppler login`.
