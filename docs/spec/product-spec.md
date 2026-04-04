# Product Specification — qalam

> Technology-agnostic description of what the app is and does.
> This is the source of truth for story decomposition. If a feature isn't here, it doesn't exist.
>
> **Reference**: The previous implementation lives at `an-na7wi` (path:
> `/Users/antoniomasotti/toni/100_programming/190_frontend/an-na7wi`). Consult it for
> implementation details, data migrations, or UX patterns worth preserving.

---

## Vision

A personal Arabic learning tool built for long-term, daily use by a single user. The goal is to
build a private knowledge base of Arabic texts, vocabulary, and linguistic annotations that grows
over years — with AI as a first-class feature, not an afterthought.

**The heart of the app is texts and sentences.** You learn a language through exposure to real
language in context, not through isolated vocabulary lists. The vocabulary domain is the "deep
dive" layer — once you encounter a word, you can explore its root, synonyms, antonyms, related
forms, and nuances.

**What it is not**:
- A gamified language learning app (no leaderboards, no streaks for their own sake)
- A social platform
- A content delivery system (content is created by the user, not consumed from a catalog)

---

## User

One person. The developer is the user. There is no need for auth, user management, roles, or
permissions. All endpoints are implicitly authorized.

---

## Developer Experience Requirement

A single `just run` command starts the complete stack (database, backend, frontend) with no
further setup. Individual parts (backend only, frontend only, database only) must also be
startable in isolation for debugging. A developer returning to the project after months of
absence should be productive within 5 minutes.

---

## Feature Domains

### 1. Texts and Interlinear Glosses

Texts are the primary content. The app supports two complementary presentation modes for the
same material: a plain annotated text view and a sentence-level interlinear gloss view. These
are not separate content types — a text can have both representations.

#### Plain Text View

A text entry contains:
- Title (required)
- Arabic body content (plain text, not structured markup)
- Transliteration of the body (optional)
- Translation of the body (optional)
- Tags: freeform string array (e.g., `["poetry", "Tunisian", "Hafiz Ibrahim"]`)
- Difficulty (beginner, intermediate, advanced)
- Dialect (MSA, Tunisian, Moroccan, Egyptian, Gulf, Levantine, Iraqi)
- Comments (freetext notes about the text)
- Audio attachment (see Audio section)

The plain text view displays Arabic / transliteration / translation in aligned columns. The user
can select any word or phrase in the Arabic text with the mouse, which opens an annotation form.

**No version history.** Texts are edited in place. The complexity of snapshot versioning was
never justified in practice — it was never used to roll back.

#### Annotations

Annotations mark a selected span of text with a note. They are the bridge between texts and
vocabulary.

- Anchor: the selected text string (used to highlight the span in the UI)
- Type: GRAMMAR, VOCABULARY, CULTURAL, OTHER
- Content: freetext explanation
- Per-annotation mastery level (NEW / LEARNING / KNOWN / MASTERED) for SRS use
- Per-annotation review flag for items to revisit
- **Vocabulary links**: an annotation is linked to zero or more vocabulary words. This is the
  primary cross-reference mechanism — from a word entry, you can find all texts where it is
  annotated, and vice versa.

#### Interlinear / Gloss View

The interlinear view renders a text sentence by sentence in glossa format: each sentence is
broken into tokens, each token displayed as a vertical triplet.

```
Arabic:          أَهْلًا
Transliteration: ahlan
Translation:     hello / welcome
```

**Structure**: a text has an ordered list of sentences. Each sentence has:
- Arabic text (full sentence)
- Transliteration (full sentence)
- Free translation (full sentence, what linguists call the "free gloss")
- Freetext notes
- An ordered list of word-alignment tokens (the per-word gloss)

**Word-alignment tokens** are a property of the sentence, not a separate entity level. Each
token is a triplet (Arabic / transliteration / translation) with an optional link to a
vocabulary word. Tokens are reorderable within their sentence.

**The key UX problem from the previous version**: when a sentence's text was edited, the
existing word-alignment tokens were left stale. The new design must handle this explicitly:
editing a sentence's Arabic text invalidates its tokens and prompts the user to re-tokenize
or confirm that existing tokens are still valid.

Auto-tokenization: given a sentence, split it into token triplets automatically (best effort;
user corrects). Clear all tokens for a sentence (destructive, with confirmation).

Vocabulary linking: the user can search the vocabulary and link any token to a matching word.

**CRUD**: Full create, read, update, delete. Available via both UI and API.

---

### 2. Vocabulary

Vocabulary is the "deep dive" domain. When you encounter a word in a text, you can add it to
the vocabulary and explore it fully: its root family, derivation chain, synonyms/antonyms,
dictionary links, usage examples, and audio.

**A word entry contains**:
- Arabic text (required)
- Transliteration into Latin / chat alphabet (optional)
- Translation (optional)
- Example sentence (optional, freetext)
- Part of speech (noun, verb, adjective, adverb, preposition, particle, interjection,
  conjunction, pronoun, unknown)
- Dialect (same enum as texts)
- Difficulty (beginner, intermediate, advanced)
- Mastery level (NEW / LEARNING / KNOWN / MASTERED — driven by SRS training)
- Pronunciation link (URL to external audio, e.g., Forvo)
- Audio attachment (recorded or uploaded file — see Audio section)

**Root association**: optional link to an ArabicRoot entry.

**Derivation chain** (`derivedFrom`): a word can reference another word as its morphological
parent. This supports building family trees from a root. Note: this is a self-referential FK
and creates a directed graph, not a strict tree — handle with care in queries.

**External dictionary links**: multiple URLs per word (Almany, Living Arabic, Derja Ninja,
Reverso, Wiktionary, Arabic Student Dictionary, Langenscheidt, Custom).

**AI example generation**: the user can request AI-generated example sentences for any word.
Returns structured output (2 examples, each with Arabic / transliteration / translation). Uses
preconfigured prompts via OpenRouter. Output is a suggestion — the user decides whether to
accept it.

**Filtering and search**:
- Filter by difficulty, dialect, part of speech, mastery level
- Search by Arabic text (exact and fuzzy with pg_trgm)
- Search by translation (free text)
- Autocomplete across Arabic + translation fields

**CRUD**: Full create, read, update, delete. Available via both UI and API.

---

### 3. Arabic Roots

A root is a sequence of 2–6 Arabic consonants from which a family of words is derived.

**A root entry contains**:
- Letters: the individual consonants as an ordered array (e.g., `["ر", "ح", "ب"]`)
- Normalized form: letters concatenated (e.g., `"رحب"`)
- Display form: letters separated by dashes (e.g., `"ر-ح-ب"`)
- Letter count: auto-computed
- Meaning: short description of the semantic field
- Analysis: longer freetext analysis

**Root normalization**: input can arrive as `"ر ح ب"`, `"رحب"`, `"ر-ح-ب"`, `"ر,ح,ب"` — the
normalization function canonicalizes all of these. Available as an API endpoint.

**Word family browser**: from any root, browse all vocabulary words linked to it, including
the derivation chains (`derivedFrom` links). Useful for building mermaid-style root diagrams.

**Statistics**: root count, words per root (avg/max), letter-count distribution.

**CRUD**: Full create, read, update, delete. Available via both UI and API.

---

### 4. Training (Spaced Repetition Flashcards)

A simplified SRS model for vocabulary review.

**Session setup**: review mode (NEW / LEARNING / KNOWN / MIXED) + session length (default 15,
max 50).

**Session flow**:
1. Backend selects words by mode, shuffles
2. For each word: show front (Arabic or translation, randomly chosen), user reveals back
3. User marks result: CORRECT / INCORRECT / SKIPPED
4. Session ends → results summary

**Per-word progress tracking**: `consecutive_correct`, `total_attempts`, `total_correct`,
`last_reviewed_at`.

**Mastery promotion**:
- NEW → LEARNING: 3 consecutive correct
- LEARNING → KNOWN: 10 total correct
- KNOWN → MASTERED: 15 total correct
- Incorrect resets streak but does not demote level

**Training statistics**: accuracy by mode, session history, mastery distribution.

**Session cleanup**: purge oldest N sessions.

---

### 5. Analytics

Read-only dashboard.

- **Overview**: total texts, sentences, words, roots, annotations
- **Content distribution**: words by difficulty / dialect / POS; annotations by type
- **Learning progress**: mastery breakdown, review queue size
- **Activity metrics**: 30-day rolling window, study streak
- **Root analytics**: letter-count distribution, top roots by word count

---

## Cross-Cutting Features

### Audio

Words and texts can have audio attachments:
- Recorded directly in the browser (where browser supports it)
- Or uploaded as a file (MP3, WAV, M4A)
- Stored server-side, served via API
- Pronunciation links (external URLs, e.g., Forvo) remain for words as a lightweight alternative

### Search

- **Global search**: across texts, words, annotations — results grouped by type
- **Advanced text search**: free text, title, dialect, difficulty, tag (combinable)
- **Vocabulary search**: Arabic text, translation, dialect, difficulty, POS, mastery level
- **Root search**: normalized form, meaning
- **Autocomplete**: quick search for linking operations (annotation → word, token → vocabulary)

### Transliteration

Rule-based, deterministic Arabic → Latin/chat-alphabet conversion. Not AI. Used when adding
texts or sentences without a transliteration.

### AI Integration (OpenRouter)

All AI features use a set of **preconfigured prompt templates** that enforce **structured
output** (JSON schema). The user triggers a prompt by name; the AI returns a validated
structured response. No freeform prompting from the UI.

Preconfigured prompts (v1):
- `word.examples`: generate 2 example sentences for a vocabulary word
- `sentence.transliterate`: suggest a transliteration for an Arabic sentence
- `text.summarize`: generate a short description of an Arabic text

The model is configurable via OpenRouter. All AI features degrade gracefully if
`OPENROUTER_API_KEY` is absent.

### REST API

All features accessible via REST API under `/api/v1/`. The user scripts against this directly
(bulk imports, data population). Consistent pagination everywhere. OpenAPI spec auto-generated
and served. Health and metrics endpoints included.

---

## Data Model Overview

```
ArabicRoot ──< Word >── DictionaryLink
               │
               └── derivedFrom (self-ref, optional)

Text ──< Sentence ──< AlignmentToken >── Word (optional)
Text ──< Annotation ──< AnnotationWord >── Word

TrainingSession ──< TrainingSessionWord >── Word
TrainingSession ──< TrainingSessionResult >── Word
Word ──── WordProgressTracking (1:1)
```

Notes:
- `Text` is unified — both plain view and interlinear view are properties of the same text
- `Sentence` replaces the old `InterlinearSentence` (it belongs to a text, not a separate container)
- `AlignmentToken` is a property of a sentence, stored as an ordered list — not a separate API entity with independent CRUD
- `TextVersion` is removed
- The old separate `InterlinearText` container is merged into `Text`

---

## Non-Goals (explicit exclusions)

- User authentication and authorization
- Multi-user support
- Offline-first PWA
- Real-time collaboration
- Gamification (badges, points)
- Content import from external sources
- Full SM-2 spaced repetition algorithm (the mastery thresholds are a simplified approximation)
- Text-to-speech generation (audio is recorded/uploaded by the user, not synthesized)
