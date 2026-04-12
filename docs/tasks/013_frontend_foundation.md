## Milestone 13 — Frontend Foundation

Complete the frontend infrastructure started in M2. API type generation, base component library,
and the brand colour scheme — must be done before any feature frontend work (M14+).

### Design system / colour theme

The an-na7wi colour palette (green primary, red accent, near-black secondary) gives the app its
identity. Qalam carries it forward, refined: cleaner surfaces, better contrast, dark mode from the
start.

- [x] 13.1 `[F]` Brand colour palette in `tokens.css`: replace zinc shadcn defaults with Qalam colours
  - **primary** — forest green (`#16a34a` base, full 50–950 scale); interactive elements, active nav, positive states
  - **accent/destructive** — deep red (`#dc2626` base); delete actions, errors, Arabic diacritical emphasis
  - **secondary** — near-black (`#18181b` base, zinc-900 family); sidebar, dark surfaces, headings
  - Update all `--primary`, `--secondary`, `--destructive`, `--accent` HSL values in `:root` and `.dark`
- [x] 13.2 `[F]` Surface and body: subtle green-tinted background (`from-gray-50 to-green-50` gradient on body in light mode; near-black in dark mode); update `base.css`
- [x] 13.3 `[F]` Sidebar active state: `.sidebar-nav-link.active` class using primary green highlight; wire `page.url.pathname` in `+layout.svelte` to apply it
- [x] 13.4 `[F]` Mastery colour classes in `src/styles/semantic.css` (new file):
  - `.mastery-new` (gray), `.mastery-learning` (amber), `.mastery-familiar` (blue), `.mastery-known` (green), `.mastery-mastered` (emerald)
  - Used on badges, word cards, progress indicators throughout the app
- [x] 13.5 `[F]` Micro-animations in `src/styles/animations.css` (new file): `fade-in`, `slide-up`, `scale-in` keyframes; apply to page entry and card appearance

### Infrastructure

- [x] 13.6 `[F]` `pnpm generate:types` script: `openapi-typescript` fetching from `/api/v1/openapi.json` → `src/lib/api/types.gen.ts`; commit output
- [x] 13.7 `[F]` HTTP client module `src/lib/api/configure.ts`: typed fetch wrapper singleton using generated types; `getApiClient()` lazy-initialises with `PUBLIC_API_BASE_URL` env var
- [x] 13.8 `[F]` Base UI components audit: verify `Button`, `Input`, `Badge`, `Card` cover all required variants (primary/secondary/outline/danger/ghost); added `danger` alias to Button
- [x] 13.9 `[F]` Biome installed (`@biomejs/biome ^1.9.4`); `biome.json` configured; `lint`/`lint:fix`/`format` scripts added; `src/lib/api/**` excluded (generated code)
- [x] 13.10 `[F]` `CI - Frontend` GitHub Action: pnpm 10 + Node 24, `pnpm lint` (Biome) + `pnpm check` (svelte-check) on `frontend/**` path changes
