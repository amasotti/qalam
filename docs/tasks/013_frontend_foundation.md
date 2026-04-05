## Milestone 13 — Frontend Foundation

Complete the frontend infrastructure started in M2. API type generation, base component library,
and the brand colour scheme — must be done before any feature frontend work (M14+).

### Design system / colour theme

The an-na7wi colour palette (green primary, red accent, near-black secondary) gives the app its
identity. Qalam carries it forward, refined: cleaner surfaces, better contrast, dark mode from the
start.

- [ ] 13.1 `[F]` Brand colour palette in `tokens.css`: replace zinc shadcn defaults with Qalam colours
  - **primary** — forest green (`#16a34a` base, full 50–950 scale); interactive elements, active nav, positive states
  - **accent/destructive** — deep red (`#dc2626` base); delete actions, errors, Arabic diacritical emphasis
  - **secondary** — near-black (`#18181b` base, zinc-900 family); sidebar, dark surfaces, headings
  - Update all `--primary`, `--secondary`, `--destructive`, `--accent` HSL values in `:root` and `.dark`
- [ ] 13.2 `[F]` Surface and body: subtle green-tinted background (`from-gray-50 to-green-50` gradient on body in light mode; near-black in dark mode); update `base.css`
- [ ] 13.3 `[F]` Sidebar active state: `.sidebar-nav-link.active` class using primary green highlight; wire `page.url.pathname` in `+layout.svelte` to apply it
- [ ] 13.4 `[F]` Mastery colour classes in `src/styles/semantic.css` (new file):
  - `.mastery-new` (gray), `.mastery-learning` (amber), `.mastery-familiar` (blue), `.mastery-known` (green), `.mastery-mastered` (emerald)
  - Used on badges, word cards, progress indicators throughout the app
- [ ] 13.5 `[F]` Micro-animations in `src/styles/animations.css` (new file): `fade-in`, `slide-up`, `scale-in` keyframes; apply to page entry and card appearance

### Infrastructure

- [ ] 13.6 `[F]` `pnpm generate:types` script: `openapi-typescript` fetching from `/api/v1/openapi.json` → `src/lib/api/types.gen.ts`; commit output
- [ ] 13.7 `[F]` HTTP client module `src/lib/api/client.ts`: typed fetch wrapper using generated types
- [ ] 13.8 `[F]` Base UI components audit: verify `Button`, `Input`, `Badge`, `Card` cover all required variants (primary/secondary/outline/danger/ghost); add any missing
