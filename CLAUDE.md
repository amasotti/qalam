# CLAUDE.md — qalam

## What this project is

Personal Arabic learning tool — texts, vocabulary, roots, SRS training, annotations, interlinear glosses. Single user (Toni), no auth ever.

## Repo layout

```
backend/      Kotlin + Ktor
frontend/     SvelteKit (bootstrapped, M2 complete)
docs/spec/    Source of truth — product-spec.md, design.md, requirements.md
docs/tasks/   Living development plan — one file per milestone (000–020 + backlog)
justfile      dev commands: just run / just test / just backend / just frontend
docker-compose.yml  full dev stack (postgres, backend, frontend)
```

## Stack (locked — don't deviate without asking)

|             |                                                                                                                               |
|-------------|-------------------------------------------------------------------------------------------------------------------------------|
| Backend     | Kotlin 2.3.20, Ktor 3.4.x, Exposed (SQL DSL), Flyway, Koin, kotlinx.serialization                                             |
| DB          | PostgreSQL 17, extensions: uuid-ossp, pg_trgm, unaccent                                                                       |
| Frontend    | SvelteKit (Svelte 5 runes), shadcn-svelte (copy-paste owned), @tanstack/svelte-query, sveltekit-superforms + Zod, Tailwind v4 |
| AI          | OpenRouter (OpenAI-compatible). Single `AiClient` wrapper. App works fully without it.                                        |
| Secrets     | Doppler only — no .env files, ever                                                                                            |
| Task runner | just                                                                                                                          |
| CI          | GitHub Actions                                                                                                                |

## Architecture rules

**Backend** — Clean/Onion, deps inward only:
- Routes → Services → Repositories (interfaces) → Exposed/DB
- Domain has zero framework deps
- Koin for DI, constructor injection only, no `KoinComponent` sprinkled everywhere
- Sealed classes for domain errors, `Either<DomainError, A>` across service boundaries
- No throwing across service boundaries

**Frontend** — thin routes, component stores, no direct API calls from components:
- Components call stores or accept props
- Stores hold server state (svelte-query), not UI state
- UI state (modals, hover, selection) lives in the component that owns it

**Frontend CSS** — global-first, scoped as last resort:
- Before adding a scoped `<style>` rule, check `frontend/src/styles/` — use the existing global class if one fits
- Global files by concern: `layout.css` (structure, buttons, chips, drawers), `semantic.css` (mastery, banners), `animations.css`, `tokens.css`
- Scoped styles are for component-unique structure only (e.g. a specific grid layout not shared anywhere)
- Two token systems coexist — prefer shadcn tokens (`hsl(var(--primary))` etc.) for new code; old Busatan tokens (`var(--coral)` etc.) only when touching existing Busatan-era classes
- App is light-mode only — never add `.dark` variants

**API**:
- All under `/api/v1/`
- OpenAPI generated at startup → `/api/v1/openapi.json` + `/api/v1/swagger-ui`
- Pagination: `page` + `size`, response: `{ items, total, page, size }`
- Error envelope: `{ error, code }`
- Frontend types generated via `pnpm generate:types` from the spec — never hand-write types

## Database rules

- Flyway SQL migrations only — naming: `V001__create_extensions.sql`
- UUID PKs generated in application code
- Enums as VARCHAR + CHECK constraints (not PG ENUM types)
- All FK columns have explicit `ON DELETE`; all FK/filter columns indexed
- No auto-DDL ever

## Key design decisions (don't re-litigate)

- `Text` is unified — plain view and interlinear gloss are properties of the same entity, no separate `InterlinearText`
- Alignment tokens are a property of the sentence, not independent API entities — they're invalidated when sentence text changes
- No version history (TextVersion removed — was never used)
- `derivedFrom` on Word is a self-referential FK creating a directed graph — depth-limit all queries over it
- Field name conventions on Sentence: `freeTranslation` (not `translation`) and `notes` (not `annotations`)

## Non-negotiables

1. No auth — single-user forever
2. No native image — `./gradlew test` under 2 min
3. Every feature reachable via `curl`
4. AI features degrade gracefully to 503 `AI_NOT_CONFIGURED` without `OPENROUTER_API_KEY`
5. No `any` in TypeScript
6. `just run` starts everything after `doppler login`
7. Frontend checks must always pass before committing any `.svelte` or CSS file:
   - `just lint-frontend` — Biome lint (zero errors, zero warnings)
   - `just format-frontend` — Biome format (run, stage any auto-fixes, re-lint)
   - `just check-frontend` — svelte-check type safety (zero errors, zero warnings)
8. the `superpowers` skill MUST be used and loaded in every new session
 8b. the `agentsys` plugins can be used if helpful

## Working style with Tony

- One task at a time, stop and share after each logical unit
- Sketch approach in words before writing real code for non-trivial things
- Pause at design forks: present options briefly, let Tony choose
- Tick tasks in the relevant `docs/tasks/0NN_*.md` file when done
- Update `docs/` when behaviour/API/data model changes — proactively, not when asked
- **Any change to a backend endpoint (new param, changed response, new route) MUST update `backend/src/main/resources/openapi/documentation.yaml` in the same step — it is the source of truth for frontend types**
- Tony is an experienced developer — don't over-explain, don't pad responses

## Context Budget
- Read only files directly needed for the current task
- Do not proactively explore the codebase unless asked or really needed for the task at hand
