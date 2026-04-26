# =============================================================================
# Qalam Frontend
# =============================================================================

# ---------- Stage 1: install deps ----------------------------------------
# docker-compose mounts source over /app and runs `pnpm dev --host` against
# this stage for hot-reload. Node modules live in a named volume so they
# survive the bind mount.
FROM node:24-alpine AS deps

RUN corepack enable && corepack prepare pnpm@10 --activate

WORKDIR /app

COPY package.json pnpm-lock.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile


# ---------- Stage 2: build static assets ---------------------------------
FROM deps AS builder

COPY . .
RUN pnpm build


# ---------- Stage 3: production (Caddy serves static assets) -------------
# Traefik handles ingress routing; Caddy only serves files + SPA fallback.
FROM caddy:2-alpine AS production

COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=builder /app/build /srv

EXPOSE 80
