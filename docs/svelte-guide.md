# SvelteKit — Qalam Frontend Guide

A practical reference for navigating this codebase. Read the [official docs](https://svelte.dev/docs/kit) for depth. 
This covers the concepts you need day-to-day and how they map to what's already here.

---

## Mental model

A `.svelte` file is a component. SvelteKit wraps Svelte with a **filesystem router** and a **build pipeline** — it knows
how to turn a directory of components into pages, handle data loading, and produce a deployable output.

This project uses the **static adapter** (`adapter-static`). That means everything compiles to static HTML/JS/CSS — no
Node server at runtime. Data fetching happens client-side (svelte-query calling the Ktor backend).

```
Svelte   = component framework (reactive UI)
SvelteKit = Svelte + router + data loading + build pipeline
```

---

## Filesystem routing

Everything under `src/routes/` becomes a URL. The file name determines the route type:

| File             | Purpose                                                                   |
|------------------|---------------------------------------------------------------------------|
| `+page.svelte`   | The visible page component                                                |
| `+layout.svelte` | Wraps child routes (persistent shell, nav)                                |
| `+page.ts`       | Runs before the page renders; exports a `load()` function                 |
| `+layout.ts`     | Same as above but for the layout                                          |
| `+server.ts`     | API endpoint (GET/POST/etc.) — not used in this project (backend is Ktor) |

**Route shapes:**

```
src/routes/
  +page.svelte              → /
  +layout.svelte            → wraps everything
  texts/
    +page.svelte            → /texts
    [id]/
      +page.svelte          → /texts/abc123
  (app)/                    → route group — no URL segment, just organizes files
    vocabulary/
      +page.svelte          → /vocabulary
```

- `[id]` — dynamic segment, available as `params.id` in the load function
- `(group)` — parentheses group = folder that doesn't affect the URL (use it to share layouts)
- `[[optional]]` — optional segment

**Layouts nest.** A `+layout.svelte` in a subfolder wraps only that subtree, inheriting from parent layouts.

---

## Svelte 5 runes

This project uses **runes mode** (configured in `svelte.config.js`). Runes are compile-time signals written as
`$rune()`. 

**Props example — how shadcn components do it:**

```svelte
<script lang="ts">
  import type { HTMLButtonAttributes } from 'svelte/elements';

  let { children, class: className, disabled, ...rest }: HTMLButtonAttributes = $props();
</script>

<button class={cn('base-styles', className)} {disabled} {...rest}>
  {@render children?.()}
</button>
```

**State example:**

```svelte
<script lang="ts">
  let open = $state(false);
  let filtered = $derived(items.filter(i => i.active));

  $effect(() => {
    // runs whenever `open` changes, after DOM update
    console.log('dialog is now', open);
  });
</script>
```

---

## Component anatomy

```svelte
<script lang="ts">
  // imports, props, state, logic — all here
  import { cn } from '$lib/utils';
  let { class: className } = $props();
</script>

<!-- Template: plain HTML + Svelte directives -->
<div class={cn('my-component', className)}>
  <slot />   <!-- Svelte 4 style, still works but prefer {@render} -->
</div>

<style>
  /* Scoped to this component — class names are hashed at build time */
  /* Avoid this in favour of app.css classes per the design system rule */
</style>
```

**The `cn()` pattern** (from `$lib/utils.ts`):

```typescript
// Merges Tailwind classes, resolving conflicts intelligently
cn('px-4 py-2', isLarge && 'px-8', className)
// → 'py-2 px-8' (last px wins) + whatever className adds
```

**shadcn-svelte components** (`$lib/components/ui/`) are copy-pasted source — they're yours to edit. They use
`tailwind-variants` for variant-based class composition. When you `pnpm dlx shadcn-svelte@latest add dialog`, it drops
the files into `$lib/components/ui/dialog/`.

---

## State management layers

Pick the simplest layer that works:

| Layer             | When                                                               | How                                       |
|-------------------|--------------------------------------------------------------------|-------------------------------------------|
| `$state()`        | Local UI state (open/closed, selected item)                        | Inside component                          |
| Props + callbacks | Parent owns state, child reports events                            | `$props()` + `$bindable()`                |
| Svelte store      | Shared UI state across components (current route context, sidebar) | `import { writable } from 'svelte/store'` |
| svelte-query      | **Server data** (texts, words, roots from the API)                 | `createQuery`, `createMutation`           |

**svelte-query** is the right tool for anything that comes from the backend. It handles caching, refetching,
loading/error states. Pattern:

```typescript
// $lib/queries/texts.ts  ← future home for these
import {createQuery} from '@tanstack/svelte-query';

export function useTexts() {
    // Options must be a function — required by @tanstack/svelte-query v6 (Svelte 5)
    return createQuery(() => ({
        queryKey: ['texts'],
        queryFn: () => fetch('/api/v1/texts').then(r => r.json())
    }));
}
```

```svelte
<script lang="ts">
  import { useTexts } from '$lib/queries/texts';
  const query = useTexts();
</script>

<!-- v6 returns a reactive object, NOT a Svelte store — no $ prefix -->
{#if query.isPending}  Loading...
{:else if query.isError}  Error: {query.error.message}
{:else}
  {#each query.data.items as text}
    <p>{text.title}</p>
  {/each}
{/if}
```

---

## API integration

The backend is Ktor at (locally) `http://localhost:8080`. The frontend never hand-writes types — they're generated from
the OpenAPI spec:

```bash
pnpm generate:types   # hits /api/v1/openapi.json → writes src/lib/api/types.ts
```

The planned API client (`$lib/api/client.ts`) will be a thin wrapper over `fetch`. Components never call `fetch`
directly — they call query/mutation hooks from `$lib/queries/`.

**For static pages with data that can be preloaded at build time**, you can also use a `+page.ts` load function:

```typescript
// src/routes/texts/[id]/+page.ts
import type {PageLoad} from './$types';

export const load: PageLoad = async ({params, fetch}) => {
    const text = await fetch(`/api/v1/texts/${params.id}`).then(r => r.json());
    return {text};
};
```

```svelte
<!-- +page.svelte receives it as a prop -->
<script lang="ts">
  let { data } = $props();  // data.text is typed
</script>
```

---

## Project lib structure

```
src/lib/
  components/
    ui/               ← shadcn-svelte components (owned, edit freely)
      button/
      card/
      input/
      badge/
  queries/            ← (to create) svelte-query hooks, one file per domain
  api/                ← (to create) typed fetch client + generated types
  stores/             ← (to create) shared UI stores (if needed)
  utils.ts            ← cn() utility + bits-ui re-exports
  index.ts            ← re-export barrel (currently empty)
```

Import via the `$lib` alias from anywhere in `src/`:

```typescript
import {cn} from '$lib/utils';
import {Button} from '$lib/components/ui/button';
```

---

## Arabic / RTL in templates

Apply the `.arabic` class to any element with Arabic content. Font and direction come from `app.css`:

```svelte
<!-- Body text (word cards, labels) -->
<span class="arabic">{word.arabic}</span>

<!-- Reading passage -->
<p class="arabic-text">{sentence.arabic}</p>

<!-- Display heading -->
<h1 class="arabic-display">{text.title}</h1>

<!-- Explicit direction when needed -->
<div dir="rtl" class="arabic">...</div>
```

**Tailwind RTL variants** flip layout properties automatically when `dir="rtl"` is on a parent:

```svelte
<div dir="rtl">
  <span class="ml-2 rtl:ml-0 rtl:mr-2">adapts to direction</span>
</div>
```

Three font families (from `app.css` — don't change the names):

| Class                                      | Font                       | Use for                              |
|--------------------------------------------|----------------------------|--------------------------------------|
| `arabic` (`--font-arabic`)                 | Noto Naskh Arabic + Lateef | UI labels, word cards, short strings |
| `arabic-display` (`--font-arabic-display`) | Lateef + Noto Naskh        | Headings, titles                     |
| `arabic-text` (`--font-arabic-text`)       | Markazi Text + Noto Naskh  | Reading passages, interlinear text   |

---

## Dev workflow

```bash
# Start dev server (from project root — wraps with doppler)
just dev-frontend         # or: cd frontend && pnpm dev

# Type-check
pnpm check
pnpm check:watch          # watch mode

# Add a shadcn component
pnpm dlx shadcn-svelte@latest add dialog
# → drops files into src/lib/components/ui/dialog/

# Generate API types from backend OpenAPI spec
pnpm generate:types       # backend must be running
```

New route checklist:

1. Create `src/routes/your-path/+page.svelte`
2. Add `+page.ts` if you need data on load
3. Import components from `$lib/components/ui/`
4. Use svelte-query hooks from `$lib/queries/` for server data
5. Apply `arabic` / `arabic-text` / `arabic-display` classes to Arabic content
