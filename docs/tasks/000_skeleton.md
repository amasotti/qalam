## Milestone 0 — Project Skeleton

Infrastructure foundation. Nothing compiles or runs correctly until this is complete.

- [x] 0.1 `[I]` Fix Gradle build: uncomment Kotlin plugin, correct alias `kotlinJvm`, add `gradlePluginPortal()` to `pluginManagement`
- [x] 0.2 `[I]` Add `src/main/kotlin` and `src/test/kotlin` source sets; add a placeholder `Application.kt` so the project has a main entry point
- [x] 0.3 `[I]` Wire full dependency set in `build.gradle.kts`: Ktor server (netty), Exposed, Flyway, Koin, kotlinx.serialization, Logback, Kotest + MockK + Testcontainers for tests
- [x] 0.4 `[I]` `docker-compose.yml` at repo root: `postgres:17-alpine` with health check + named volume; no backend or frontend container yet
- [x] 0.5 `[I]` `justfile` at repo root with `start-db`, `backend`, `test`, `run` recipes (Doppler-wrapped)
- [x] 0.6 `[I]` `doppler.yaml` committed at repo root (project config stub — no secrets in file)
- [x] 0.7 `[I]` Toolchain: JDK 25 via sdkman, Node 24 via system package, pnpm via corepack — no nix (removed)
- [x] 0.8 `[I]` GitHub Actions `ci.yml`: run `./gradlew test` on push/PR (Doppler token injected)
- [x] 0.9 `[B]` `detekt.yml` config wired and passing on empty project (already in `backend/config/detekt/`)
- [x] 0.10 `[I]` Write `README.md` with project overview, setup instructions, and development workflow. Reference the docs/ folder. Readme is nice to read, enjoyable and clear. Has badges and is appealing. Details are in docs/ folder and sub readmes.
