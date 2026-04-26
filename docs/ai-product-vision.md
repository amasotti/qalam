# AI Product Vision — What qalam Could Become

> Written 2026-04-26. State of the app: word enrichment, sentence insights, auto-tokenization,
> transliteration, and example generation are live or in flight. The gap below is what modern
> AI capabilities would unlock that is NOT yet on the roadmap.

---

## What's already there (baseline)

| Feature | Status |
|---|---|
| AI example generation for words | Done |
| AI auto-tokenization of sentences | Done |
| AI transliteration | Done |
| AI word analysis (POS, root, example) | Done |
| AI word enrichment (gender, verb pattern, plurals, relations) | In flight |
| AI contextual insight for words and sentences | Done |

All of the above are one-shot structured requests. The gap is: **active learning loop**, **content
intelligence**, and **semantic understanding**.

---

## Tier 1 — High impact, feasible now

### 1. AI-Assisted Text Ingestion Pipeline

**What:** Paste Arabic text → AI does everything in one shot: transliterate the body, detect
dialect and difficulty, segment into sentences, propose alignment tokens for each sentence, flag
unknown words not yet in vocabulary and suggest adding them.

**Why it matters:** Right now adding a text with full interlinear glosses is hours of manual work.
This collapses it to minutes. The user still reviews and corrects — AI provides the draft.

**Model:** Single structured prompt, JSON output. Already have all the primitives (`autoTokenize`,
`transliterate`, `analyzeWord`). Needs orchestration and a "review & accept" UI step.

---

### 2. Contextual Flashcards (SRS + Texts)

**What:** Instead of isolated `Arabic → translation` flashcards, show the word in the sentence
from the text where you first annotated it. "You encountered this word in: [sentence from text X]."

**Why it matters:** Memory research is unambiguous: context beats isolation. The data is already
there — annotations link words to sentences. This is mostly a frontend change.

**Variant:** "Cloze" mode — show the sentence with the target word blanked out, user types or
picks it. Pure frontend, no AI needed beyond what's already there.

---

### 3. AI Grammar Tables On Demand

**What:** Click a verb anywhere in a text → AI returns full conjugation table (past/present/future,
all persons). Click a noun → declension table (nominative/accusative/genitive, singular/dual/plural).
Displayed in a slide-in drawer, same pattern as the annotation drawer.

**Why it matters:** Arabic morphology is the hardest part for learners. Having the full paradigm
one click away, in context, is enormously useful. No existing tool integrates this at the
sentence level with your own corpus.

**Note:** Verb pattern (I–X) is already being stored via word enrichment. The conjugation table
is the natural next step.

---

### 4. Vocabulary Gap Detection on New Texts

**What:** When a text is added or opened, AI (or rule-based fuzzy match) scans it against the
existing vocabulary and highlights words not yet in the lexicon. User can add them in bulk with
one click, pre-filled via `analyzeWord`.

**Why it matters:** Right now vocabulary grows only when the user manually notices a word. This
makes it systematic. Gap detection can also flag words already in vocabulary and show mastery
level inline on the text, so you can see at a glance what you know vs. don't.

---

### 5. Diacritization Toggle (Tashkeel Mode)

**What:** For any Arabic text, AI diacritizes it on demand (adds tashkeel). A toggle in the text
view switches between diacritized and clean output. Stored separately; does not modify the base
content.

**Why it matters:** Reading without tashkeel is a skill threshold. Beginners need it; intermediate
learners should train without it. Having it available removes the friction of reaching for a
separate tool.

**Implementation:** Single API call, cached per text. Models handle this well.

---

## Tier 2 — Strong value, requires more work

### 6. Semantic / Embedding Search

**What:** Generate embeddings for all words and texts (via OpenAI / Cohere embedding API). Store
in `pgvector`. Add a "semantic search" mode alongside the existing trigram search: "find words
related to hospitality" returns أَهْلًا, ضَيْف, كَرَم even without exact text match.

**Why it matters:** Trigram search is great for exact or near-exact queries. It fails for
conceptual search. As the corpus grows (hundreds of words, dozens of texts), discoverability
becomes the bottleneck.

**Cost:** pgvector extension needed (it's already common in PG17 distributions), embedding
generation on write, ~1500ms per word for first-time indexing. Incremental.

---

### 7. Weak Pattern Analysis

**What:** After N training sessions, AI reads the session history and produces a short diagnostic:
"You consistently miss broken plural forms", "Verb pattern VII trips you up", "Words from
Tunisian dialect have 60% accuracy vs. 80% for MSA words." Surfaces as a dashboard card.

**Why it matters:** Generic flashcard apps show aggregate accuracy. This one has structured
metadata (POS, verb pattern, dialect, root) that makes pattern analysis possible. That's a
genuine competitive edge for self-directed learners.

**Implementation:** Aggregate session data per metadata axis → send to AI for narrative synthesis.
Could be rule-based for the analysis part; AI just writes the human-readable summary.

---

### 8. AI Tandem Chat (Conversation Mode)

**What:** A dedicated chat interface. User writes Arabic (Tunisian or MSA). AI responds in Arabic,
corrects mistakes inline using `[[original → corrected]]` markup, and explains the correction in
English. Can optionally constrain vocabulary to words in the user's corpus.

**Why it matters:** Production in reading/writing is what separates passive recognizers from
active speakers. No existing feature in qalam supports output practice. This is the missing loop.

**Implementation:** Streaming response (Ktor SSE or WebSocket). Conversation history in-session,
not persisted (no schema change needed). System prompt scoped to user's vocab list if desired.

---

### 9. Root Family Tree Generator

**What:** For any root, AI generates the full word family with Arabic patterns (أَوْزَان): فَاعِل,
مَفْعُول, فِعَال, etc. — with examples for each pattern. Rendered as a visual tree or table in the
root detail view. User can selectively add derived words to their vocabulary with one click.

**Why it matters:** Arabic root-based morphology is the single most powerful leverage point for
vocabulary acquisition. The root domain exists; the derivation chains exist. Making the full
pattern space visible is the natural evolution.

---

### 10. Text Difficulty Scoring (Per Sentence + Per Text)

**What:** AI estimates difficulty per sentence based on vocabulary complexity, grammatical
structures, and dialect. Aggregate to text level. Display as a bar alongside the existing
difficulty enum. Also flag "hardest sentence" in a text so the learner knows where to focus.

**Why it matters:** The current `difficulty` field is user-assigned and coarse. AI can provide a
more granular, consistent signal — especially useful when adding new material from external sources.

---

## Tier 3 — Ambitious, worth planting the seed

### 11. Image OCR + Text Extraction

**What:** Upload a photo (textbook page, sign, menu, screenshot). AI extracts the Arabic text,
pre-fills a new text entry, and triggers the ingestion pipeline from idea #1.

**Why it matters:** A huge amount of Arabic learning material lives in physical books or images.
This makes qalam the destination for all of it, not just what you can type.

**Implementation:** Multimodal models (GPT-4o, Claude 3.x) handle this natively. Single API call.

---

### 12. Audio TTS for Words and Sentences (Where No Recording Exists)

**What:** For words or sentences without a recorded audio file, offer AI-generated TTS as a
fallback (OpenAI TTS API or similar). Displayed with a visual marker distinguishing AI audio from
human recordings.

**Why it matters:** Audio is in the roadmap (M8) but deferred because no existing audio data
exists. AI TTS fills the gap immediately with no recording effort. User can always replace with
a real recording later.

---

### 13. Personalized Study Plan

**What:** AI reads your corpus state (texts, vocabulary, mastery levels, session history) and
proposes a study plan: "Texts to work through this week", "Roots to drill", "Words in LEARNING
for 30 days — prioritize these". A weekly suggestion, not a lock-in.

**Why it matters:** The app collects rich learning data. Today that data informs SRS but nothing
synthesizes it into forward-looking recommendations. This closes the loop from data → action.

---

## What the existing AI design gets right

- Structured output everywhere (JSON schema) — no hallucination risk on fields
- All features degrade to 503 without API key — no hidden dependency
- `InsightContext` with `InsightMode.HOMEWORK` vs `READING` — good model for context-aware prompts
- Model swappable via env var — works with any OpenRouter model

## What needs architectural attention before scaling AI

- **Prompt templates are inline strings** — as AI features grow, these should move to a template
  registry with versioning. Changing a prompt today means a code deploy.
- **No caching** — word enrichment, grammar tables, and other deterministic outputs should be
  cached in the DB. Right now every request hits the model.
- **No cost tracking** — once usage grows, it's useful to know which features burn tokens.
  Add a simple `ai_requests` log table (feature, model, approx_tokens, timestamp).
- **No retry / circuit breaker** — currently a network hiccup returns a 500. Fine for solo use,
  but worth adding a simple retry with exponential backoff.
