## Milestone 12 — Backend Hardening

Before full frontend feature work starts. Backend must be solid.

- [ ] 12.1 `[B]` Request validation: all POST/PUT routes validate input via `RequestValidation` plugin before hitting service layer; invalid → 422
- [ ] 12.2 `[B]` OpenAPI spec completeness pass: every route documented, all request/response shapes present, importable into Postman without modification — note: each milestone already adds its own docs in step X.0; this is a final audit, not the first time documenting
- [ ] 12.3 `[B]` `derivedFrom` cycle guard: `WordService` rejects circular `derivedFrom` references; depth-limited graph traversal
- [ ] 12.4 `[B]` Backup setup: Ofelia cron container in `docker-compose.yml`; hourly `pg_dump --format=custom` to named volume; `just restore F=<file>` recipe
- [ ] 12.5 `[B]` `./gradlew test` timing baseline: must complete in under 2 minutes on a warm Testcontainers image
- [ ] 12.6 `[I]` GitHub Actions `release.yml`: on `v*` tags — build Docker image, push to GHCR, create release with git-cliff changelog
