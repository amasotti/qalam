# Training and practice — SRS and exercise design

> How word SRS and non-SRS practice sessions work. Only flashcards mutate word mastery.

---

## Mastery levels

Each word carries a `mastery_level` column (stored as VARCHAR in the `words` table):

```
NEW → LEARNING → KNOWN → MASTERED
```

All words start at `NEW` when created.

---

## Training session lifecycle

### 1. Create session

`POST /api/v1/training/sessions`

Parameters:
- `mode` — which mastery tier to draw words from: `NEW`, `LEARNING`, `KNOWN`, or `MIXED` (all tiers)
- `size` — how many words (clamped to 1–50)
- `wordListIds` — optional list IDs that restrict the word pool. Multiple lists use union semantics:
  words from any selected list are eligible. Empty or omitted means all vocabulary.

Backend queries `words` filtered by mastery level and optional word-list membership, shuffles, takes `size` words.
For each word a `front_side` is randomly assigned at creation time (`ARABIC` or `TRANSLATION`) — fixed for the life of that session.

### 2. Answer words

`POST /api/v1/training/sessions/{id}/results`

Body: `{ wordId, result }` where `result` is one of `CORRECT`, `INCORRECT`, `SKIPPED`.

On each answer the backend:
1. Loads the word's `word_progress` row.
2. Calls `computeProgressUpdate()` (pure function) to get the new progress snapshot and an optional promotion target.
3. Writes the updated progress back.
4. If a promotion was computed, updates `words.mastery_level` in the same transaction.
5. Records the result (and `mastery_promoted_to` if applicable) in `training_session_words`.

### 3. Complete session

`POST /api/v1/training/sessions/{id}/complete`

Marks the session `COMPLETED`, tallies correct/incorrect/skipped counts, computes accuracy (`correct / (correct + incorrect)`, skips excluded), and returns a summary including any promotions that occurred.

---

## Promotion algorithm

Implemented in `TrainingService.computeProgressUpdate()`.

| Current level | Trigger                   | Promoted to |
|---------------|---------------------------|-------------|
| NEW           | `consecutiveCorrect >= 3` | LEARNING    |
| LEARNING      | `totalCorrect >= 10`      | KNOWN       |
| KNOWN         | `totalCorrect >= 15`      | MASTERED    |
| MASTERED      | —                         | (terminal)  |

**Progress counters per result:**

| Result | `consecutiveCorrect` | `totalCorrect` | `totalAttempts` |
|---|---|---|---|
| CORRECT | +1 | +1 | +1 |
| INCORRECT | reset to 0 | unchanged | +1 |
| SKIPPED | unchanged | unchanged | +1 |

Key observations:
- `NEW → LEARNING` requires an unbroken streak of 3. One wrong answer resets the streak.
- After entering `LEARNING`, the gate switches to cumulative correct counts — the streak no longer matters.
- Skipping a word costs an attempt but never resets the streak and never blocks promotion.

---

## Data model

### `word_progress` (1:1 with `words`, created atomically)

| Column | Type | Purpose |
|---|---|---|
| `word_id` | UUID PK/FK | Links to `words` |
| `consecutive_correct` | integer | Current unbroken correct streak |
| `total_correct` | integer | All-time correct answers |
| `total_attempts` | integer | All-time answer events (not skips) |
| `last_reviewed_at` | timestamp? | When word was last answered |

### `training_sessions`

| Column | Purpose |
|---|---|
| `mode` | NEW / LEARNING / KNOWN / MIXED |
| `status` | ACTIVE → COMPLETED |
| `correct_count`, `incorrect_count`, `skipped_count` | Filled on complete |

### `training_session_words`

| Column | Purpose |
|---|---|
| `position` | Order in session |
| `front_side` | ARABIC or TRANSLATION (fixed at creation) |
| `result` | CORRECT / INCORRECT / SKIPPED (null until answered) |
| `mastery_promoted_to` | Set if this answer triggered a promotion |

---

## Frontend

| Route | Purpose |
|---|---|
| `/training` | Practice-strategy hub |
| `/training/flashcards` | Flashcard session setup (scope, focus, and batch size) |
| `/training/[id]` | Active session — one `FlashCard` at a time |
| `/training/exercises/multiple-choice` | Multiple-choice exercise setup and paginated exercise history |
| `/training/exercises/[id]` | Resumable multiple-choice exercise and completed review |
| `/training/exercises/conjugation` | Conjugation matching setup and paginated history |
| `/training/conjugation-exercises/[id]` | Resumable conjugation matching session and review |

`FlashCard.svelte` shows the question side, reveals the answer on Space/Enter, then accepts 1/2/3 (or numpad) for correct/incorrect/skip. After the last card the route renders `SessionSummary.svelte` with accuracy and any promotions.

Server state is managed via `@tanstack/svelte-query` hooks in `frontend/src/lib/stores/training.ts`.

### Multiple-choice exercises

The first exercise strategy is `MULTIPLE_CHOICE_MEANING`: Arabic is shown as the prompt and the learner selects one of four translations. The setup mirrors flashcards: all vocabulary or selected word lists, a mastery focus, and a question count.

Exercise answers lock immediately and show the correct option before the learner moves on. Refreshing an active exercise resumes at its first unanswered question. Completing it records unanswered questions as skipped; completed sessions remain reviewable from the paginated history. Exercise server state is managed in `frontend/src/lib/stores/exercises.ts`.

### Conjugation exercises

Conjugation practice is a separate session family built on the deterministic MSA conjugation
engine. It deliberately does **not** modify `word_progress` or word mastery: conjugated forms are
generated from a lemma rather than stored vocabulary entries.

The matching-session API, composer, history, and accessible click-to-pair runner are implemented.

The first mode is a four-form matching board. It presents four fully vocalised forms of one verb
and four person/number/gender labels; the learner matches every form to its morphology. The
follow-up `WRITE_FORM` mode prompts for a morphology description and requires typing the complete
Arabic form, including harakat. Sessions retain the verb, generated-form snapshot, submitted
mapping/text, and per-form correctness so mistakes can be reviewed. No per-form SRS/progression
table is planned.

See [conjugation.md](conjugation.md) and the
[exercise implementation plan](../plans/2026-07-19-conjugation-exercises-design.md).

---

## API endpoints

| Method | Path | Action |
|---|---|---|
| POST | `/api/v1/training/sessions` | Create session |
| GET | `/api/v1/training/sessions` | List sessions (paginated) |
| GET | `/api/v1/training/sessions/{id}` | Fetch session + all words |
| POST | `/api/v1/training/sessions/{id}/results` | Record one answer |
| POST | `/api/v1/training/sessions/{id}/complete` | Finalize session |
| GET | `/api/v1/training/stats` | Mastery distribution + recent sessions |
| POST | `/api/v1/exercise-sessions` | Create an exercise session |
| GET | `/api/v1/exercise-sessions` | List exercise sessions (paginated) |
| GET | `/api/v1/exercise-sessions/{id}` | Fetch an exercise session and its items |
| POST | `/api/v1/exercise-sessions/{id}/answers` | Submit a multiple-choice answer |
| POST | `/api/v1/exercise-sessions/{id}/complete` | Finalize an exercise session |
