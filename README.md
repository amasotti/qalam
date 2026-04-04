# قلم · Qalam

[![CI](https://github.com/amasotti/qalam/actions/workflows/ci.yml/badge.svg)](https://github.com/amasotti/qalam/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-3.x-087CFA?logo=ktor&logoColor=white)
![License](https://img.shields.io/github/license/amasotti/qalam)

*Qalam* (قلم — "pen") is the rewrite of [An-Na7wi](https://github.com/amasotti/an-na7wi), my personal Arabic learning
platform. The old stack (Quarkus + Nuxt) served me well but accumulated too many rough edges and given it was a learning project
in first place, why not keep learning with some other stack? :D This is the clean version: Ktor + SvelteKit, better data model and service / use-case layer, same goal.

**What it does**: manage Arabic texts with interlinear glosses, explore word roots and vocabulary in depth, annotate
passages, and drill with a spaced-repetition flashcard system. AI-assisted transliteration and example generation are
built in but entirely optional — the app works without an API key.

**Who it's for**: me. Single user, no auth, no multi-tenancy. If you're also learning Arabic and like building your own
tools, you might find it useful too.

---

## What you need

The dev environment is fully defined in `flake.nix`. If you have Nix with flakes enabled, that's the only thing you
need:

```bash
nix develop       # drops you into a shell with JDK 25, just, doppler, pnpm, psql
```

This provides:

| Tool    | Version | Purpose                         |
|---------|---------|---------------------------------|
| JDK     | 25      | Backend build and runtime       |
| just    | latest  | Task runner (replaces make)     |
| doppler | latest  | Secrets management              |
| Node.js | 24      | Frontend build and dev server   |
| pnpm    | latest  | Frontend package manager        |
| psql    | 17      | Database CLI for manual queries |

If you don't use Nix, install those tools manually and make sure `JAVA_HOME` points to JDK 25.
Doppler can be replaced by similar secretOps tools like Vault, Infisical etc.. or with a less secure .env file.

---

## Getting started

One-time setup:

```bash
doppler login
doppler setup    # project: qalam, config: dev
```

Then:

```bash
just run         # start Postgres + Ktor backend
```

Or in parts:

```bash
just start-db    # Postgres only (Docker Compose)
just backend     # Ktor backend (requires DB running)
just test        # backend tests (Testcontainers — no external DB needed)
just stop-db     # shut down Postgres
```

---

## What's in here

- **Texts** — add Arabic passages, attach transliteration and translation, tag by dialect and difficulty
- **Interlinear glosses** — break texts into sentences, align each token to a vocabulary word
- **Roots** — explore the trilateral root system; words link back to their root
- **Vocabulary** — full word entries with POS, dialect, mastery level, dictionary links, audio, and example sentences
- **Annotations** — highlight and annotate phrases within texts; link annotations to vocabulary words
- **SRS training** — session-based flashcard drills with mastery promotion
- **AI features** — transliteration, example generation, and auto-tokenization via OpenRouter (gracefully disabled if no
  key is set)

---

Full documentation lives in [`docs/`](docs/).

---

## Access points (once running)

- API: `http://localhost:8080/api/v1/`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui`
- Health: `http://localhost:8080/health`

---

## License

MIT — see [LICENSE](LICENSE).
