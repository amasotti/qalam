## Milestone 2 — Frontend Bootstrap

SvelteKit scaffold, design system baseline, Arabic layout, hello-world home page. Self-contained — no backend domain work required. After this milestone `just run` starts the full stack and the browser shows a real page.

- [x] 2.1 `[F]` `pnpm create svelte@latest frontend` at repo root — Svelte 5, TypeScript strict, no framework (SPA-friendly); commit generated scaffold
- [x] 2.2 `[F]` Install Tailwind v4 + shadcn-svelte; copy base components into `src/lib/components/ui/`
- [x] 2.3 `[F]` Design tokens in `app.css`: colors, spacing, typography as CSS custom properties; Arabic font stack (Noto Naskh Arabic + Lateef + Markazi Text) via CDN or `@font-face`
- [x] 2.4 `[F]` `.arabic` and `.transliteration` CSS classes; a placeholder Arabic string in the home page confirms RTL renders correctly
- [x] 2.5 `[F]` Root layout (`+layout.svelte`): app shell with sidebar nav — Home, Words, Roots, Texts, Training, Analytics links (structure only, no routing logic yet)
- [x] 2.5a `[F]` Split `app.css` into `src/styles/` partials (tokens, base, arabic, layout); `app.css` becomes the import manifest — single source of truth maintained
- [x] 2.6 `[F]` Home page (`/`): fetches `GET /health`, displays "Backend connected" or "Backend unreachable" — proves the stack talks end-to-end
- [x] 2.7 `[F]` `@tanstack/svelte-query` provider wired in root layout
- [x] 2.8 `[F]` `justfile`: add `frontend` recipe (`pnpm --prefix frontend dev`); update `run` to start DB + backend + frontend together
