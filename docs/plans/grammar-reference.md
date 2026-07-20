# Grammar Reference — Implementation Plan

## Decision

Add a static, version-controlled grammar reference library to the frontend. It is
reference material, not user-created learning data: it has no backend API, database
schema, store, OpenAPI type, or CRUD flow.

The sidebar gets a new top-level **Reference** section, separate from **Study** and
**Practice**. Its first link is **Grammar / قواعد**.

## User experience

- `/grammar` lists all grammar sheets as ordered, grouped cards.
- `/grammar/[slug]` renders one sheet in a readable reference layout.
- Slugs are stable filenames: `broken-plurals.md` becomes `/grammar/broken-plurals`.
- Each sheet has a return link to the index and displays its title, category, and
  short summary.
- An unknown slug shows a local not-found state rather than making a backend request.

V1 deliberately excludes full-text search, filters, a generated table of contents,
and links to words, roots, or annotations. Revisit those once the number and shape
of sheets make a navigation aid worthwhile.

## Content contract

Store sheets under `frontend/src/content/grammar/`. They are bundled into the static
frontend build and committed with code.

Each Markdown file begins with frontmatter:

```md
---
title: Broken plurals
summary: Common patterns, recognition clues, and practical examples.
category: Nouns
order: 10
---

## What is a broken plural?
```

Rules:

- Filename: lowercase kebab-case; filename is the public slug.
- `title`, `summary`, `category`, and numeric `order` are required.
- Sort index by `category`, then `order`, then `title` for deterministic output.
- Body begins at `##`; page owns the visible `h1` from `title`.
- Use standard Markdown only in V1: headings, paragraphs, lists, emphasis, links,
  blockquotes, code, and tables. Do not use raw HTML.
- Arabic examples use the existing Arabic typography classes only when a presentation
  need cannot be expressed by normal Markdown. Add a dedicated supported Markdown
  convention before introducing raw HTML.

The current renderer passes `marked` output to `{@html}`. Keeping committed sheets
free of raw HTML makes the content model clear and avoids extending that trust boundary.
If external or editable content is ever introduced, sanitize rendered HTML before it
reaches `{@html}`.

## Implementation slices

### 1. Content registry

Create a frontend-only grammar-content module.

- Use Vite `import.meta.glob` with raw Markdown imports to discover every
  `frontend/src/content/grammar/*.md` file at build time.
- Parse and validate the frontmatter into a typed `GrammarSheet` descriptor:
  `slug`, `title`, `summary`, `category`, `order`, and `body`.
- Fail clearly in development when metadata is missing, malformed, duplicated, or a
  filename is not valid kebab-case.
- Expose `listGrammarSheets()` and `getGrammarSheet(slug)`; routes must not know file
  paths or parse Markdown themselves.

Decision during implementation: use a small, typed parser if frontmatter stays flat;
add a library only if nested metadata becomes necessary. Do not introduce a content
management framework for this static collection.

### 2. Routes and navigation

- Add `frontend/src/routes/grammar/+page.svelte` for grouped reference cards.
- Add `frontend/src/routes/grammar/[slug]/+page.svelte` for individual sheets.
- Add a `Reference` section and `Grammar / قواعد` link to `+layout.svelte`.
- Ensure `isActive('/grammar')` highlights Grammar for both index and reader URLs.
- Keep routes client-side, consistent with global `ssr = false` and adapter-static
  fallback configuration.

### 3. Presentation

- Reuse `Markdown.svelte` and global `.prose` styles for sheet bodies.
- Add `frontend/src/styles/grammar.css` for grammar index and reader-specific layout;
  import it through `frontend/src/app.css`.
- Use the existing page-width, card, section-label, and Arabic typography primitives.
- Keep a comfortable measure for reading and make wide Markdown tables horizontally
  scrollable without clipping Arabic examples.

### 4. Initial content and test coverage

- Add two representative sheets: `broken-plurals.md` and `idafa-construction.md`.
- Include headings, lists, Arabic examples, and a table across those fixtures so the
  rendering contract is exercised.
- Unit-test frontmatter parsing, ordering, and slug lookup, including invalid metadata
  and an unknown slug.
- Component-test index links and the reader's not-found state. Extend the existing
  `Markdown.svelte` tests only if renderer behaviour changes.

## Verification

After implementation, check the .claude/rules/quality-gates.md and execute those commands.

## Documentation follow-up

When implementation starts, add the final user-facing grammar-reference behaviour to
`docs/spec/product-spec.md` and its visual/navigation rules to
`docs/spec/visual-design.md`. Keep this file as implementation history and decision
record; remove superseded options rather than letting it become a second product spec.
