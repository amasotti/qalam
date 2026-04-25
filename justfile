set dotenv-load := false

# List available recipes
default:
    @just --list

# ── Docker compose (primary) ─────────────────────────────────────────────────

# Start all services detached
up:
    docker compose up -d

# Rebuild images and start all services
build:
    docker compose up -d --build

# Stop all services
down:
    docker compose down

# Stop all services and wipe volumes (full DB reset)
reset:
    docker compose down -v

# Service status
ps:
    docker compose ps

# Follow logs — all services, or a specific one: just logs backend
logs service='':
    docker compose logs -f {{service}}

# Rebuild and restart a single service: just rebuild backend
rebuild service:
    docker compose up -d --build {{service}}

# Restart a single service without rebuilding: just restart frontend
restart service:
    docker compose restart {{service}}

# Open a shell in a running container: just sh backend
sh service:
    docker compose exec {{service}} sh

# ── Database ─────────────────────────────────────────────────────────────────

# Start only the DB (useful when running backend locally)
db:
    docker compose up -d db
    docker compose exec -T db pg_isready -U qalam -d qalam

# On-demand pg_dump (custom format, compressed) — written to ./backups/
dump:
    docker exec qalam-db sh -c 'pg_dump -U qalam -Fc qalam > /backups/qalam_$(date +%Y%m%d_%H%M%S).dump'
    @echo "Dump written to ./backups/"

# List available dumps
dump-ls:
    @ls -lh backups/ 2>/dev/null || echo "No backups yet."

# Inspect a dump's table of contents without restoring: just dump-inspect qalam_20260419_120000.dump
dump-inspect file:
    docker exec qalam-db pg_restore --list /backups/{{file}}

# Restore from a dump file: just dump-restore qalam_20260419_120000.dump
dump-restore file:
    docker exec qalam-db pg_restore -U qalam -d qalam --clean --if-exists /backups/{{file}}

# ── Code quality & tooling ───────────────────────────────────────────────────

# Run backend tests (Testcontainers spins its own Postgres — no secrets needed)
test:
    ./backend/gradlew -p backend test --no-daemon

check:
    ./backend/gradlew -p backend check --no-daemon

# Lint the OpenAPI spec
lint-api:
    docker run --rm -v $PWD:/spec redocly/cli lint /spec/backend/src/main/resources/openapi/documentation.yaml

# Generate frontend TS types from the OpenAPI spec (backend must be running)
gtypes:
    pnpm --prefix frontend generate:types

# ── Local dev mode (no Docker — for rapid iteration) ─────────────────────────

# Run backend locally against the dockerised DB (requires Doppler)
dev-backend: db
    doppler run -- ./backend/gradlew -p backend run

# Run frontend dev server locally (proxy → localhost:8085)
dev-frontend:
    pnpm --prefix frontend dev

# Full local stack: DB (docker) + backend (bg) + frontend (fg)
# Ctrl-C stops the frontend; `just down` tears down the DB
dev: db
    just dev-backend &
    just dev-frontend
