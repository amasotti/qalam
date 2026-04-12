set dotenv-load := false

# Start the database (detached)
start-db:
    docker compose up -d db
    docker compose exec -T db pg_isready -U qalam -d qalam

# Stop the database
stop-db:
    docker compose down

# Run the backend (requires DB running + Doppler)
backend: start-db
    doppler run -- ./backend/gradlew -p backend run

# Run the frontend dev server
frontend:
    pnpm --prefix frontend dev

# Run backend tests (Testcontainers manages its own Postgres — no secrets needed)
test:
    ./backend/gradlew -p backend test

lint-api:
    docker run --rm -v $PWD:/spec redocly/cli lint /spec/backend/src/main/resources/openapi/documentation.yaml

generate-types:
    pnpm --prefix frontend generate:types

# Start everything: DB + backend (background) + frontend (foreground)
# Ctrl+C stops the frontend; run `just stop-db` to tear down the database
run:
    just backend &
    just frontend
