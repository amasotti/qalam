---
description: Mandatory quality gates after every code change — task is not complete until all pass
---

After ANY edit to backend Kotlin, frontend Svelte/TS/CSS, or the OpenAPI spec, run these gates in order before declaring the task done:

```sh
just check          # backend: detekt + compile (Gradle check)
just gtypes         # regenerate TS types from OpenAPI spec
just check-frontend # frontend: svelte-check type safety (zero errors, zero warnings)
just test-frontend  # frontend: unit tests (zero errors, all passed)
```

## Rules

- **All three must pass with zero errors** — do not hand off or claim completion otherwise
- `just gtypes` requires the backend running (`just dev-backend` or `just up`); if it is not up, say so explicitly — never skip silently
- If `just check` surfaces new detekt violations, fix them in the same step — do not defer
- If `just check-frontend` has type errors after `just gtypes`, the OpenAPI spec and generated types are out of sync — fix the spec, regenerate, recheck
- Backend-only changes still require `just check`; frontend-only changes still require `just check-frontend`; any endpoint change requires all three
