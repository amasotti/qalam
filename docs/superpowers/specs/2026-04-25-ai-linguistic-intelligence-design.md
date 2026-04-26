# AI Linguistic Intelligence Design

**Date:** 2026-04-25
**Status:** Approved

## Problem

The existing AI layer generates examples and tokenizes sentences but offers no linguistic intelligence: no semantic
disambiguation, no awareness of near-synonyms and their nuances, no corrections for learner mistakes. When studying a
word or reviewing a sentence you wrote, there is no way to ask "what's the difference between this and that?", "is my
phrasing natural?", or "what should I watch out for?". This fills that gap.

## Scope

Two surfaces, one backend endpoint:

1. **Word insight** — on the word detail page, an on-demand panel gives linguistic analysis of the word: semantic
   disambiguation from near-synonyms (primary focus), synonyms/antonyms, common learner mistakes, register/dialect
   notes.
2. **Sentence insight** — on the interlinear text view, each sentence gets an on-demand button for insights. The AI receives
   the full text as context to evaluate the target sentence and suggest corrections or alternatives or a simple evaluation of the
   sentence. 

**Not in scope:** persistence (insights are ephemeral), background/auto-load, audio, new data model tables.

## Decisions Made

| Question             | Decision                                                         |
|----------------------|------------------------------------------------------------------|
| Trigger              | On-demand only — explicit button click on both surfaces          |
| Persistence          | Ephemeral — generated on request, discarded on close             |
| Backend surface      | Single endpoint `POST /api/v1/ai/insight`                        |
| Context enrichment   | Backend loads entity from DB before calling AI                   |
| Sentence context     | Full text (all sentences) sent, not just neighbours              |
| Sentence mode flag   | Optional `mode: HOMEWORK \| READING` — hints prompt focus        |
| Word panel position  | After `word-related`, before `DictionaryLinks`                   |
| Sentence panel       | Inline below each `InterlinearSentence` row, per-sentence toggle |
| Markdown rendering   | `marked` (already in `package.json`)                             |
| Graceful degradation | 503 `AI_NOT_CONFIGURED` hides the button; no error shown         |
| System prompt        | Adapted from `skills/et-tounsi-persona.md`                       |

## API

### Request

```
POST /api/v1/ai/insight
Content-Type: application/json

{
  "entityType": "WORD" | "SENTENCE",
  "entityId":   "<uuid>",
  "mode":       "HOMEWORK" | "READING"   // optional, SENTENCE only
}
```

### Response

```json
{
  "insight": "<markdown string>"
}
```

### Error cases

- `503 AI_NOT_CONFIGURED` — no `OPENROUTER_API_KEY` set (existing pattern, frontend hides button)
- `404 NOT_FOUND` — entityId does not exist
- `400 INVALID_INPUT` — unknown entityType

## Backend Architecture

### New files

```
delivery/routes/AiInsightRoutes.kt          POST /api/v1/ai/insight
delivery/dto/ai/AiInsightDtos.kt            InsightRequest, InsightResponse
domain/ai/AiInsightService.kt               DB enrichment → AiClient
domain/ai/InsightContext.kt                 Sealed class (WordInsight | SentenceInsight)
domain/ai/InsightMode.kt                    Enum: HOMEWORK, READING
```

### Modified files

```
infrastructure/ai/AiClient.kt               +generateInsight(context: InsightContext)
infrastructure/koin/AppModule.kt            +AiInsightService binding
delivery/Routing.kt                         +aiInsightRoutes()
domain/error/DomainError.kt                 (no change — AiNotConfigured already exists)
```

### InsightContext sealed class

```kotlin
sealed class InsightContext {

    data class WordInsight(
        val arabicText: String,
        val translation: String?,
        val partOfSpeech: String,
        val dialect: String,
        val rootLetters: String?,   // e.g. "ج-ر-ب", null if no root linked
        val rootMeaning: String?,
        val examples: List<String>, // up to 3, Arabic text only
    ) : InsightContext()

    data class SentenceInsight(
        val targetArabic: String,
        val targetTranslation: String?,
        val targetIndex: Int,                        // 1-based position in text
        val dialect: String,
        val textTitle: String?,
        val allSentences: List<Pair<String, String?>>, // arabic + freeTranslation, ordered
        val mode: InsightMode,
    ) : InsightContext()
}
```

### AiInsightService responsibilities

**WORD:** load `Word` by id → load `ArabicRoot` if `rootId` set → load up to 3 `WordExample` records (most recently
created) → build `WordInsight`.

**SENTENCE:** load `Sentence` by id → load parent `Text` (for title + dialect) → load all `Sentence` records for that
text ordered by position → build `SentenceInsight`. When `mode` is absent from the request, default to `HOMEWORK`.

### Prompt design

**System prompt** (both types):

```
You are an experienced Arabic language teacher and tandem partner,
specialising in Tunisian Arabic and MSA.

Rules:
- Respond in English unless Arabic script is needed for examples
- Never add vocalization (tashkeel) to Arabic script
- Be concise and academic in tone — no motivational filler
- The user is between beginner and B1 level
- Do not break down every word unless it is the focus of the insight
- Return plain text with minimal markdown — bold for key terms, no headers
```

**WORD user prompt:**

```
Analyse the Arabic word: [arabicText] ("[translation]").
Part of speech: [partOfSpeech]. Dialect: [dialect].
[IF root] Root: [rootLetters] — meaning: "[rootMeaning]".
[IF examples] Example usage: "[ex1]" / "[ex2]"

Give concise linguistic insights. Lead with semantic disambiguation
if this word is commonly confused with a near-synonym (different nuance,
register, or dialect). Then cover relevant synonyms/antonyms, common
learner mistakes, or register notes — only what is genuinely interesting
for this specific word. Skip empty sections.
```

**SENTENCE user prompt:**

```
Analyse sentence [targetIndex] from the text "[textTitle]" ([dialect]):

Full text:
  1. [arabic] — [translation]
  2. [arabic] — [translation]
→ [N]. [TARGET arabic] — [translation]    ← target
  ...

[IF mode=HOMEWORK]
This is a student-authored sentence. Prioritise corrections and
natural alternatives over analysis.
[IF mode=READING]
This is a native-authored sentence. Focus on nuance, notable
constructions, and vocabulary choices.

Be concise.
```

## Frontend Architecture

### New files

```
src/lib/stores/insight.ts                   useInsight() mutation
src/lib/components/ai/AiInsightPanel.svelte reusable panel component
```

### Modified files

```
src/routes/words/[id]/+page.svelte          +<AiInsightPanel entityType="WORD" entityId={id} />
src/lib/components/texts/InterlinearSentence.svelte  +insight toggle + <AiInsightPanel>
src/lib/api/sdk.gen.ts                      regenerated after endpoint added
src/lib/api/types.gen.ts                    regenerated after endpoint added
```

### AiInsightPanel props

```typescript
interface Props {
    entityType: 'WORD' | 'SENTENCE';
    entityId: string;
    mode?: 'HOMEWORK' | 'READING'; // SENTENCE only
}
```

States: `idle` → button visible → `loading` → spinner → `result` → markdown rendered + refresh/close buttons.

Button is hidden entirely when the backend returns 503 (AI not configured).

### Word detail placement

```
word-hero
word-info        (translation, saved examples, pronunciation)
word-related     (root link, derivedFrom link)
AiInsightPanel   ← HERE
DictionaryLinks
AiExamples
Annotations
```

### Sentence placement

Per-sentence toggle button in `InterlinearSentence`. When active, an inline panel expands below the sentence row. Only
one sentence panel open at a time per text is not enforced — each is independent state.

## Prompt context budget

WORD: small — arabicText + root + 3 short examples. Well within any model's context.

SENTENCE: potentially large for long texts. `allSentences` is the full ordered list. For texts with > 30 sentences,
truncate to the 10 sentences nearest the target (5 before, 5 after) and note the truncation in the prompt. This avoids
token bloat while preserving enough context.

## Out of scope

- Persisting insights to the database
- A dedicated "AI chat" or multi-turn interface
- Insights on roots, annotations, or training words
- Auto-loading insights without user action
- Any new Flyway migration (no schema changes)
