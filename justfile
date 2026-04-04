set dotenv-load := false

# Start the database (detached)
start-db:
    docker compose up -d db
    docker compose exec db pg_isready -U qalam -d qalam

# Stop the database
stop-db:
    docker compose down

# Run the backend (requires DB running + Doppler)
backend: start-db
    doppler run -- ./backend/gradlew -p backend run

# Run backend tests (starts a Testcontainers-managed Postgres — no docker compose needed)
test:
    doppler run -- ./backend/gradlew -p backend test

# Start everything: DB + backend
run: start-db
    doppler run -- ./backend/gradlew -p backend run
