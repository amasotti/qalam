# Vocabulary Lookup Drawer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clicking any token in the interlinear view opens a right-side drawer that shows the word's vocabulary entry, or a compact AI-assisted quick-add form if the word isn't in vocabulary yet.

**Architecture:** A new `VocabLookupDrawer` component (mirroring the existing `AnnotationDrawer` pattern) opens on token click. It calls `GET /words/by-arabic?q=` (new exact-match endpoint) to look up the word. Found → show word card with mastery level + link. Not found → show `QuickAddWordForm`, which can call `POST /words/analyze` (new AI endpoint) to pre-fill fields. The drawer has a footer button to open the existing `AnnotationDrawer` for the same token — the two drawers are mutually exclusive. Token click callback signature changes from `(anchor: string)` to `(token: AlignmentTokenResponse)` to carry the full token through.

**Tech Stack:** Kotlin/Ktor + Exposed backend (two new routes, one new AI method). SvelteKit + Svelte 5 runes frontend (two new components, store hooks). `@tanstack/svelte-query` mutations. No new DB migrations required.

---

## File Map

| Action | Path |
|--------|------|
| Modify | `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordRepository.kt` |
| Modify | `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordRepository.kt` |
| Modify | `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordService.kt` |
| Modify | `backend/src/main/kotlin/com/tonihacks/qalam/delivery/dto/word/WordDtos.kt` |
| Modify | `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/ai/AiClient.kt` |
| Modify | `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/WordRoutes.kt` |
| Modify | `backend/src/test/kotlin/com/tonihacks/qalam/delivery/WordsIntegrationTest.kt` |
| Modify | `frontend/src/lib/stores/words.ts` |
| Modify | `frontend/src/lib/components/texts/TokenGrid.svelte` |
| Modify | `frontend/src/lib/components/texts/InterlinearSentence.svelte` |
| Modify | `frontend/src/routes/texts/[id]/+page.svelte` |
| Create | `frontend/src/lib/components/words/QuickAddWordForm.svelte` |
| Create | `frontend/src/lib/components/texts/VocabLookupDrawer.svelte` |

---

## Task 0: Backend — Exact-text word lookup (`GET /words/by-arabic`)

**Files:**
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordRepository.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordRepository.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordService.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/WordRoutes.kt`
- Test: `backend/src/test/kotlin/com/tonihacks/qalam/delivery/WordsIntegrationTest.kt`

- [ ] **Step 1: Add `findByArabicText` to `WordRepository` interface**

  In `WordRepository.kt`, add after `autocomplete`:

  ```kotlin
  suspend fun findByArabicText(arabicText: String): Either<DomainError, Word?>
  ```

- [ ] **Step 2: Implement in `ExposedWordRepository`**

  In `ExposedWordRepository.kt`, add after `findById`:

  ```kotlin
  override suspend fun findByArabicText(arabicText: String): Either<DomainError, Word?> =
      suspendTransaction {
          val word = WordsTable
              .selectAll()
              .where { WordsTable.arabicText eq arabicText }
              .singleOrNull()
              ?.toWord()
          word.right()
      }
  ```

- [ ] **Step 3: Add service method in `WordService.kt`**

  Add after `getById`:

  ```kotlin
  suspend fun findByArabicText(arabicText: String): Either<DomainError, WordResponse?> =
      repo.findByArabicText(arabicText).map { it?.toResponse() }
  ```

- [ ] **Step 4: Add route in `WordRoutes.kt`**

  Add this `get` block inside `route("/words")`, after `get("/autocomplete")` and before `get("/{id}")` (order matters — specific paths before parameterised):

  ```kotlin
  get("/by-arabic") {
      val q = call.request.queryParameters["q"]
      if (q.isNullOrBlank()) {
          call.respondError(DomainError.InvalidInput("q parameter is required"))
          return@get
      }
      service.findByArabicText(q).fold(
          { call.respondError(it) },
          { word ->
              if (word != null) call.respond(HttpStatusCode.OK, word)
              else call.respondError(DomainError.NotFound("Word", q))
          },
      )
  }
  ```

- [ ] **Step 5: Write failing tests**

  In `WordsIntegrationTest.kt`, add a new describe block after the existing `"GET /api/v1/words/autocomplete"` block:

  ```kotlin
  "GET /api/v1/words/by-arabic" - {
      "returns 200 with word when exact match exists" {
          testApp { client ->
              client.post("/api/v1/words") {
                  contentType(ContentType.Application.Json)
                  setBody(katabJson)
              }
              val response = client.get("/api/v1/words/by-arabic?q=كَتَبَ")
              response.status shouldBe HttpStatusCode.OK
              response.bodyAsText() shouldContain "كَتَبَ"
              response.bodyAsText() shouldContain "to write"
          }
      }

      "returns 404 when no exact match" {
          testApp { client ->
              val response = client.get("/api/v1/words/by-arabic?q=غيرموجود")
              response.status shouldBe HttpStatusCode.NotFound
          }
      }

      "returns 422 when q is missing" {
          testApp { client ->
              val response = client.get("/api/v1/words/by-arabic")
              response.status shouldBe HttpStatusCode.UnprocessableEntity
          }
      }
  }
  ```

- [ ] **Step 6: Run tests, verify they fail**

  ```bash
  cd backend && ./gradlew test --tests "*.WordsIntegrationTest" 2>&1 | tail -20
  ```

  Expected: compilation error (method not found) or test failures.

- [ ] **Step 7: Run tests again after implementation, verify they pass**

  ```bash
  cd backend && ./gradlew test --tests "*.WordsIntegrationTest" 2>&1 | tail -20
  ```

  Expected: all `GET /api/v1/words/by-arabic` tests pass.

- [ ] **Step 8: Commit**

  ```bash
  git add backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordRepository.kt \
           backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordRepository.kt \
           backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordService.kt \
           backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/WordRoutes.kt \
           backend/src/test/kotlin/com/tonihacks/qalam/delivery/WordsIntegrationTest.kt
  git commit -m "feat: add GET /words/by-arabic exact-text lookup endpoint"
  ```

---

## Task 1: Backend — AI word analysis (`POST /words/analyze`)

**Files:**
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/dto/word/WordDtos.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/ai/AiClient.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordService.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/WordRoutes.kt`
- Test: `backend/src/test/kotlin/com/tonihacks/qalam/delivery/WordsIntegrationTest.kt`

- [ ] **Step 1: Add DTOs to `WordDtos.kt`**

  Append at end of `WordDtos.kt`:

  ```kotlin
  @Serializable
  data class AnalyzeWordRequest(val arabicText: String)

  @Serializable
  data class WordAnalysisResponse(
      val arabicText: String,
      val transliteration: String? = null,
      val translation: String? = null,
      val partOfSpeech: String? = null,
      val rootLetters: String? = null,
      val exampleSentence: AiExampleSentence? = null,
  )
  ```

- [ ] **Step 2: Add `analyzeWord` to `AiClient.kt`**

  Add this private data class alongside the other payload classes (near `ExamplesPayload`):

  ```kotlin
  @Serializable
  private data class WordAnalysisPayload(
      val transliteration: String? = null,
      val translation: String? = null,
      val partOfSpeech: String? = null,
      val rootLetters: String? = null,
      val exampleSentence: AiExampleSentence? = null,
  )
  ```

  Add this method to `AiClient` alongside `generateExamples`:

  ```kotlin
  suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> {
      if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

      val model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini"
      val prompt = """Analyze the Arabic word "$arabicText" and return a JSON object with:
- "transliteration": Latin transliteration using practical chat-alphabet style
- "translation": English translation or meaning
- "partOfSpeech": one of NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION, PARTICLE, INTERJECTION, CONJUNCTION, PRONOUN, UNKNOWN
- "rootLetters": Arabic trilateral/quadrilateral root consonants separated by dashes (e.g. "ك-ت-ب"), or null if not applicable
- "exampleSentence": object with "arabic", "transliteration", "translation" keys showing the word in a short sentence, or null"""

      return try {
          val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
              header(HttpHeaders.Authorization, "Bearer $apiKey")
              contentType(ContentType.Application.Json)
              setBody(OpenRouterRequest(
                  model = model,
                  messages = listOf(
                      Message("system", SYSTEM_PROMPT),
                      Message("user", prompt),
                  ),
                  responseFormat = ResponseFormat("json_object"),
              ))
          }

          val body = response.body<OpenRouterResponse>()
          val content = body.choices.firstOrNull()?.message?.content
              ?: return DomainError.InvalidInput("Empty AI response").left()

          val payload = jsonConfig.decodeFromString<WordAnalysisPayload>(content)
          WordAnalysisResponse(
              arabicText = arabicText,
              transliteration = payload.transliteration,
              translation = payload.translation,
              partOfSpeech = payload.partOfSpeech,
              rootLetters = payload.rootLetters,
              exampleSentence = payload.exampleSentence,
          ).right()
      } catch (_: Exception) {
          DomainError.InvalidInput("AI request failed").left()
      }
  }
  ```

  Note: `AiExampleSentence` is already imported from `com.tonihacks.qalam.delivery.dto.word` — no new import needed.

- [ ] **Step 3: Add service method in `WordService.kt`**

  Add alongside `generateExamples`:

  ```kotlin
  suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> =
      aiClient.analyzeWord(arabicText)
  ```

  Add import at top if not already present:
  ```kotlin
  import com.tonihacks.qalam.delivery.dto.word.WordAnalysisResponse
  import com.tonihacks.qalam.delivery.dto.word.AnalyzeWordRequest
  ```

- [ ] **Step 4: Add route in `WordRoutes.kt`**

  Add inside `route("/words")`, alongside the existing `post("/{id}/examples/generate")`:

  ```kotlin
  // AI word analysis (ephemeral — does not persist)
  post("/analyze") {
      val req = call.receive<AnalyzeWordRequest>()
      service.analyzeWord(req.arabicText).fold(
          { call.respondError(it) },
          { call.respond(HttpStatusCode.OK, it) },
      )
  }
  ```

  Add import at top of `WordRoutes.kt`:
  ```kotlin
  import com.tonihacks.qalam.delivery.dto.word.AnalyzeWordRequest
  ```

  **Important:** Register `post("/analyze")` before `post` (the bare create endpoint) to avoid routing ambiguity. Actually Ktor routes by full path so order doesn't matter between `/analyze` and `/`, but place it near the other AI endpoint for clarity.

- [ ] **Step 5: Write failing test**

  In `WordsIntegrationTest.kt`, add:

  ```kotlin
  "POST /api/v1/words/analyze" - {
      "returns 503 when AI is not configured" {
          testApp { client ->
              val response = client.post("/api/v1/words/analyze") {
                  contentType(ContentType.Application.Json)
                  setBody("""{"arabicText":"كَتَبَ"}""")
              }
              response.status shouldBe HttpStatusCode.ServiceUnavailable
              response.bodyAsText() shouldContain "AI_NOT_CONFIGURED"
          }
      }
  }
  ```

  This test always passes (no API key in test env) — it verifies the graceful degradation path.

- [ ] **Step 6: Run full word tests**

  ```bash
  cd backend && ./gradlew test --tests "*.WordsIntegrationTest" 2>&1 | tail -20
  ```

  Expected: all tests green including the new analyze test.

- [ ] **Step 7: Commit**

  ```bash
  git add backend/src/main/kotlin/com/tonihacks/qalam/delivery/dto/word/WordDtos.kt \
           backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/ai/AiClient.kt \
           backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordService.kt \
           backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/WordRoutes.kt \
           backend/src/test/kotlin/com/tonihacks/qalam/delivery/WordsIntegrationTest.kt
  git commit -m "feat: add POST /words/analyze AI word analysis endpoint"
  ```

---

## Task 2: Frontend — Generate types and add store hooks

**Files:**
- Run: `pnpm generate:types` (regenerates `frontend/src/lib/api/sdk.gen.ts` and `types.gen.ts`)
- Modify: `frontend/src/lib/stores/words.ts`

- [ ] **Step 1: Start backend and generate types**

  The backend must be running to expose the updated OpenAPI spec:

  ```bash
  just backend &
  sleep 8
  cd frontend && pnpm generate:types
  ```

  Verify the generated SDK has the new functions — check `frontend/src/lib/api/sdk.gen.ts` for functions matching `by-arabic` and `analyze`. The function names depend on the generated operationId. Search:

  ```bash
  grep -n "byArabic\|analyze\|by_arabic" frontend/src/lib/api/sdk.gen.ts | head -10
  ```

  Note the exact function names — you'll need them in the next step. They'll be something like `getWordsByArabic` and `analyzeWord`.

  Also check `types.gen.ts` for `WordAnalysisResponse` and `AnalyzeWordRequest`:

  ```bash
  grep -n "WordAnalysis\|AnalyzeWord" frontend/src/lib/api/types.gen.ts
  ```

- [ ] **Step 2: Add store hooks to `words.ts`**

  At the top of `words.ts`, add the new SDK imports alongside the existing ones. Use the exact function names from the generated SDK (found in Step 1):

  ```typescript
  // Add these to the existing import from '$lib/api/sdk.gen':
  // getWordByArabic,   ← check exact name in sdk.gen.ts
  // analyzeWord,       ← check exact name in sdk.gen.ts
  ```

  Add these type imports alongside existing ones from `types.gen`:
  ```typescript
  // Add to existing import from '$lib/api/types.gen':
  // WordAnalysisResponse,
  // AnalyzeWordRequest,
  ```

  Append these two hooks at the end of `words.ts`:

  ```typescript
  export function useLookupWordByArabic() {
    return createMutation({
      mutationFn: async (arabicText: string): Promise<WordResponse | null> => {
        const { data, error, response } = await getWordByArabic({ query: { q: arabicText } });
        if (response.status === 404) return null;
        if (error) throw error;
        return requireData(data, 'getWordByArabic') as WordResponse;
      },
    });
  }

  export function useAnalyzeWord() {
    return createMutation({
      mutationFn: async (arabicText: string): Promise<WordAnalysisResponse> => {
        const { data, error } = await analyzeWord({ body: { arabicText } });
        if (error) throw error;
        return requireData(data, 'analyzeWord') as WordAnalysisResponse;
      },
    });
  }
  ```

  Replace `getWordByArabic` and `analyzeWord` with the actual function names from your generated SDK.

- [ ] **Step 3: Verify TypeScript compiles**

  ```bash
  cd frontend && pnpm check 2>&1 | tail -20
  ```

  Expected: no errors related to the new hooks.

- [ ] **Step 4: Commit**

  ```bash
  git add frontend/src/lib/api/sdk.gen.ts \
           frontend/src/lib/api/types.gen.ts \
           frontend/src/lib/stores/words.ts
  git commit -m "feat: add useLookupWordByArabic and useAnalyzeWord store hooks"
  ```

---

## Task 3: Frontend — Change token click callback to pass full token

The annotation drawer uses `anchor: string`. We widen the callback to pass the full `AlignmentTokenResponse` so the vocab drawer can use `token.wordId` and the annotation drawer continues to receive `token.arabic`.

**Files:**
- Modify: `frontend/src/lib/components/texts/TokenGrid.svelte`
- Modify: `frontend/src/lib/components/texts/InterlinearSentence.svelte`

- [ ] **Step 1: Update `TokenGrid.svelte`**

  Change the Props interface and all usages of `onTokenClick`:

  ```svelte
  <script lang="ts">
  import type { AlignmentTokenResponse, AnnotationResponse } from '$lib/api/types.gen';
  import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';

  interface Props {
    tokens: AlignmentTokenResponse[];
    annotations?: AnnotationResponse[];
    onTokenClick?: (token: AlignmentTokenResponse) => void;  // changed: was (anchor: string)
  }

  let { tokens, annotations = [], onTokenClick }: Props = $props();

  function badgesFor(arabicText: string): AnnotationResponse[] {
    return annotations.filter((a) => a.anchor === arabicText);
  }
  </script>
  ```

  In the template, change the two click handlers from:
  ```svelte
  onclick={() => onTokenClick?.(token.arabic)}
  onkeydown={(e) => e.key === 'Enter' && onTokenClick?.(token.arabic)}
  ```
  to:
  ```svelte
  onclick={() => onTokenClick?.(token)}
  onkeydown={(e) => e.key === 'Enter' && onTokenClick?.(token)}
  ```

- [ ] **Step 2: Update `InterlinearSentence.svelte`**

  Change the prop type (line 11):

  ```svelte
  interface Props {
    sentence: SentenceResponse;
    annotations?: AnnotationResponse[];
    onRetokenize?: (sentence: SentenceResponse) => Promise<void>;
    onMarkValid?: (sentence: SentenceResponse) => Promise<void>;
    onTokenClick?: (token: AlignmentTokenResponse) => void;  // changed: was (anchor: string)
    isPending?: boolean;
  }
  ```

  Add `AlignmentTokenResponse` to the import if not already there:
  ```svelte
  import type { AlignmentTokenResponse, AnnotationResponse, SentenceResponse } from '$lib/api/types.gen';
  ```

  The prop is passed through to `TokenGrid` unchanged — no template changes needed in InterlinearSentence since it uses `{onTokenClick}` spread.

- [ ] **Step 3: Verify TypeScript compiles**

  ```bash
  cd frontend && pnpm check 2>&1 | grep -i error | head -20
  ```

  Expected: errors in `+page.svelte` (the caller still passes `(anchor: string)` — intentional, fixed in Task 6). No errors in TokenGrid or InterlinearSentence themselves.

- [ ] **Step 4: Commit**

  ```bash
  git add frontend/src/lib/components/texts/TokenGrid.svelte \
           frontend/src/lib/components/texts/InterlinearSentence.svelte
  git commit -m "refactor: widen token click callback to pass full AlignmentTokenResponse"
  ```

---

## Task 4: Frontend — `QuickAddWordForm` component

A compact create form pre-fillable via AI analysis. Not a replacement for `WordForm` — it only covers the fields needed for quick capture from a token.

**Files:**
- Create: `frontend/src/lib/components/words/QuickAddWordForm.svelte`

- [ ] **Step 1: Create the component**

  ```svelte
  <script lang="ts">
  import type { PartOfSpeech } from '$lib/api/types.gen';
  import { useAnalyzeWord, useCreateWord } from '$lib/stores/words';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';

  interface Props {
    arabicText: string;
    onCreated: (wordId: string) => void;
    onCancel: () => void;
  }

  let { arabicText, onCreated, onCancel }: Props = $props();

  const analyzeWord = useAnalyzeWord();
  const createWord = useCreateWord();

  let transliteration = $state('');
  let translation = $state('');
  let partOfSpeech = $state<string>('UNKNOWN');

  const posOptions: string[] = [
    'UNKNOWN', 'NOUN', 'VERB', 'ADJECTIVE', 'ADVERB',
    'PREPOSITION', 'PARTICLE', 'INTERJECTION', 'CONJUNCTION', 'PRONOUN',
  ];

  async function handleAnalyze() {
    const result = await analyzeWord.mutateAsync(arabicText);
    transliteration = result.transliteration ?? '';
    translation = result.translation ?? '';
    partOfSpeech = result.partOfSpeech ?? 'UNKNOWN';
  }

  async function handleSubmit(e: SubmitEvent) {
    e.preventDefault();
    const result = await createWord.mutateAsync({
      arabicText,
      transliteration: transliteration.trim() || null,
      translation: translation.trim() || null,
      partOfSpeech,
    });
    onCreated(result.id);
  }
  </script>

  <form class="quick-add-form" onsubmit={handleSubmit}>
    <div class="quick-add-arabic arabic-text">{arabicText}</div>

    <div class="quick-add-ai">
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={analyzeWord.isPending}
        onclick={handleAnalyze}
      >
        {analyzeWord.isPending ? 'Analyzing…' : 'Analyze with AI'}
      </Button>
      {#if analyzeWord.isError}
        <span class="quick-add-error">AI unavailable</span>
      {/if}
    </div>

    <div class="quick-add-field">
      <Label for="qa-translit">Transliteration</Label>
      <Input id="qa-translit" bind:value={transliteration} placeholder="e.g. kataba" />
    </div>

    <div class="quick-add-field">
      <Label for="qa-translation">Translation</Label>
      <Input id="qa-translation" bind:value={translation} placeholder="English meaning" />
    </div>

    <div class="quick-add-field">
      <Label for="qa-pos">Part of speech</Label>
      <select id="qa-pos" class="quick-add-select" bind:value={partOfSpeech}>
        {#each posOptions as pos}
          <option value={pos}>{pos.charAt(0) + pos.slice(1).toLowerCase()}</option>
        {/each}
      </select>
    </div>

    <div class="quick-add-actions">
      <Button type="submit" size="sm" disabled={createWord.isPending}>
        {createWord.isPending ? 'Saving…' : 'Add to vocabulary'}
      </Button>
      <Button type="button" variant="ghost" size="sm" onclick={onCancel}>Cancel</Button>
    </div>
  </form>

  <style>
  .quick-add-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .quick-add-arabic {
    font-size: 1.5rem;
    text-align: center;
    padding: 0.5rem 0;
    color: hsl(var(--foreground));
  }

  .quick-add-ai {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .quick-add-error {
    font-size: 0.75rem;
    color: hsl(var(--destructive));
  }

  .quick-add-field {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }

  .quick-add-select {
    width: 100%;
    padding: 0.375rem 0.75rem;
    border: 1px solid hsl(var(--border));
    border-radius: var(--radius);
    background: hsl(var(--background));
    color: hsl(var(--foreground));
    font-size: 0.875rem;
  }

  .quick-add-actions {
    display: flex;
    gap: 0.5rem;
    padding-top: 0.5rem;
  }
  </style>
  ```

- [ ] **Step 2: Verify TypeScript**

  ```bash
  cd frontend && pnpm check 2>&1 | grep -i "QuickAdd\|quick-add" | head -10
  ```

  Expected: no errors for this file.

- [ ] **Step 3: Commit**

  ```bash
  git add frontend/src/lib/components/words/QuickAddWordForm.svelte
  git commit -m "feat: add QuickAddWordForm component with AI pre-fill"
  ```

---

## Task 5: Frontend — `VocabLookupDrawer` component

Right-side slide-in panel. Same visual pattern as `AnnotationDrawer`. On open: looks up word by exact Arabic text. Found → word card. Not found → `QuickAddWordForm`. Footer → jump to annotation drawer.

**Files:**
- Create: `frontend/src/lib/components/texts/VocabLookupDrawer.svelte`

- [ ] **Step 1: Create the component**

  ```svelte
  <script lang="ts">
  import type { AlignmentTokenResponse, WordResponse } from '$lib/api/types.gen';
  import { fly } from 'svelte/transition';
  import QuickAddWordForm from '$lib/components/words/QuickAddWordForm.svelte';
  import { useLookupWordByArabic } from '$lib/stores/words';

  interface Props {
    open: boolean;
    token: AlignmentTokenResponse | null;
    onclose: () => void;
    onannotate: (anchor: string) => void;
  }

  let { open, token, onclose, onannotate }: Props = $props();

  const lookup = useLookupWordByArabic();

  let found = $state<WordResponse | null>(null);
  let notFound = $state(false);
  let loading = $state(false);

  $effect(() => {
    if (!open || !token) return;
    found = null;
    notFound = false;
    loading = true;
    lookup.mutateAsync(token.arabic).then((word) => {
      found = word;
      notFound = word === null;
      loading = false;
    });
  });

  function handleAnnotate() {
    if (!token) return;
    onannotate(token.arabic);
    onclose();
  }

  function handleCreated(wordId: string) {
    // Re-fetch so the drawer flips to "found" state
    loading = true;
    notFound = false;
    lookup.mutateAsync(token!.arabic).then((word) => {
      found = word;
      loading = false;
    });
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') onclose();
  }
  </script>

  <svelte:window onkeydown={handleKeydown} />

  {#if open}
    <div
      class="vocab-backdrop"
      onclick={onclose}
      role="button"
      aria-label="Close"
      tabindex="-1"
    ></div>

    <aside class="vocab-drawer" transition:fly={{ x: 360, duration: 220, opacity: 1 }}>
      <header class="vocab-header">
        <span class="vocab-header-arabic arabic-text">{token?.arabic ?? ''}</span>
        <button class="vocab-close" onclick={onclose} aria-label="Close">×</button>
      </header>

      <div class="vocab-body">
        {#if loading}
          <p class="vocab-state-msg">Looking up…</p>
        {:else if found}
          <div class="vocab-card">
            <p class="vocab-card-arabic arabic-text">{found.arabicText}</p>
            {#if found.transliteration}
              <p class="vocab-card-translit transliteration">{found.transliteration}</p>
            {/if}
            {#if found.translation}
              <p class="vocab-card-translation">{found.translation}</p>
            {/if}
            <span class="vocab-mastery-badge vocab-mastery-{found.masteryLevel.toLowerCase()}">
              {found.masteryLevel}
            </span>
            <a href="/words/{found.id}" class="vocab-open-link">Open in vocabulary →</a>
          </div>
        {:else if notFound}
          <p class="vocab-state-msg vocab-not-found-msg">Not in vocabulary yet.</p>
          <QuickAddWordForm
            arabicText={token!.arabic}
            onCreated={handleCreated}
            onCancel={onclose}
          />
        {/if}
      </div>

      {#if found}
        <footer class="vocab-footer">
          <button class="vocab-annotate-btn" onclick={handleAnnotate}>
            View / Add Annotations →
          </button>
        </footer>
      {/if}
    </aside>
  {/if}

  <style>
  .vocab-backdrop {
    position: fixed;
    inset: 0;
    background: hsl(var(--foreground) / 0.2);
    z-index: 40;
    cursor: default;
  }

  .vocab-drawer {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    width: 340px;
    z-index: 50;
    background: hsl(var(--background));
    border-left: 1px solid hsl(var(--border));
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .vocab-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem;
    border-bottom: 1px solid hsl(var(--border));
    flex-shrink: 0;
  }

  .vocab-header-arabic {
    font-size: 1.5rem;
    line-height: 1.4;
  }

  .vocab-close {
    font-size: 1.25rem;
    line-height: 1;
    background: none;
    border: none;
    cursor: pointer;
    color: hsl(var(--foreground) / 0.6);
    padding: 0.25rem;
  }

  .vocab-body {
    flex: 1;
    overflow-y: auto;
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .vocab-state-msg {
    font-size: 0.875rem;
    color: hsl(var(--foreground) / 0.6);
    margin: 0;
  }

  .vocab-not-found-msg {
    margin-bottom: 0.5rem;
  }

  .vocab-card {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .vocab-card-arabic {
    font-size: 1.75rem;
    margin: 0;
    line-height: 1.6;
  }

  .vocab-card-translit {
    font-size: 0.875rem;
    margin: 0;
    color: hsl(var(--foreground) / 0.7);
  }

  .vocab-card-translation {
    font-size: 1rem;
    margin: 0;
    font-weight: 500;
  }

  .vocab-mastery-badge {
    display: inline-block;
    font-size: 0.6875rem;
    font-weight: 600;
    padding: 0.125rem 0.5rem;
    border-radius: 9999px;
    width: fit-content;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .vocab-mastery-new      { background: hsl(var(--muted));    color: hsl(var(--muted-foreground)); }
  .vocab-mastery-learning { background: hsl(40 90% 60% / 0.2); color: hsl(40 70% 40%); }
  .vocab-mastery-known    { background: hsl(210 80% 60% / 0.2); color: hsl(210 60% 40%); }
  .vocab-mastery-mastered { background: hsl(140 60% 40% / 0.15); color: hsl(140 50% 32%); }

  .vocab-open-link {
    font-size: 0.8125rem;
    color: hsl(var(--primary));
    text-decoration: none;
    margin-top: 0.25rem;
  }

  .vocab-open-link:hover { text-decoration: underline; }

  .vocab-footer {
    border-top: 1px solid hsl(var(--border));
    padding: 0.75rem 1.25rem;
    flex-shrink: 0;
  }

  .vocab-annotate-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 0.8125rem;
    color: hsl(var(--foreground) / 0.7);
    padding: 0;
  }

  .vocab-annotate-btn:hover { color: hsl(var(--foreground)); text-decoration: underline; }
  </style>
  ```

- [ ] **Step 2: Verify TypeScript**

  ```bash
  cd frontend && pnpm check 2>&1 | grep -i "VocabLookup\|vocab-lookup" | head -10
  ```

  Expected: no errors.

- [ ] **Step 3: Commit**

  ```bash
  git add frontend/src/lib/components/texts/VocabLookupDrawer.svelte
  git commit -m "feat: add VocabLookupDrawer component"
  ```

---

## Task 6: Frontend — Wire into text page and smoke test

**Files:**
- Modify: `frontend/src/routes/texts/[id]/+page.svelte`

- [ ] **Step 1: Add imports to `+page.svelte`**

  Add alongside the existing `AnnotationDrawer` import:

  ```svelte
  import VocabLookupDrawer from '$lib/components/texts/VocabLookupDrawer.svelte';
  import type { AlignmentTokenResponse } from '$lib/api/types.gen';
  ```

- [ ] **Step 2: Add vocab drawer state**

  Below the existing `let drawerOpen = $state(false)` / `let drawerAnchor` declarations, add:

  ```svelte
  let vocabOpen = $state(false);
  let vocabToken = $state<AlignmentTokenResponse | null>(null);
  ```

- [ ] **Step 3: Replace `openDrawer` with two handlers**

  Replace the existing:
  ```svelte
  function openDrawer(anchor: string) {
    drawerAnchor = anchor;
    drawerOpen = true;
  }
  ```

  With:
  ```svelte
  function openVocabLookup(token: AlignmentTokenResponse) {
    drawerOpen = false;        // close annotation drawer if open
    vocabToken = token;
    vocabOpen = true;
  }

  function openAnnotationDrawer(anchor: string) {
    vocabOpen = false;         // close vocab drawer if open
    drawerAnchor = anchor;
    drawerOpen = true;
  }
  ```

- [ ] **Step 4: Update `InterlinearSentence` usage in template**

  Find the `<InterlinearSentence>` usages (there's one per sentence in `{#each sentences.data.items as sentence}`). Change:
  ```svelte
  onTokenClick={(anchor) => openDrawer(anchor)}
  ```
  to:
  ```svelte
  onTokenClick={(token) => openVocabLookup(token)}
  ```

- [ ] **Step 5: Update `AnnotationDrawer` usage**

  The `AnnotationDrawer` component stays unchanged. Its `open` and `anchor` bindings remain. The `onclose` handler also stays unchanged. No changes needed to the AnnotationDrawer element itself.

- [ ] **Step 6: Add `VocabLookupDrawer` to template**

  Add alongside `<AnnotationDrawer>` (at the end of the template, outside the `{#if text.data}` block or inside it next to the annotation drawer):

  ```svelte
  <VocabLookupDrawer
    open={vocabOpen}
    token={vocabToken}
    onclose={() => (vocabOpen = false)}
    onannotate={openAnnotationDrawer}
  />
  ```

- [ ] **Step 7: Verify TypeScript compiles clean**

  ```bash
  cd frontend && pnpm check 2>&1 | tail -20
  ```

  Expected: zero errors.

- [ ] **Step 8: Smoke test in browser**

  ```bash
  just run
  ```

  Open a text that has sentences with tokens. Click a token:
  - Vocab drawer slides in from the right.
  - If word is in vocabulary: word card appears with mastery badge and "Open in vocabulary →" link.
  - If word is NOT in vocabulary: "Not in vocabulary yet." + `QuickAddWordForm` appears.
  - Click "Analyze with AI" (requires `OPENROUTER_API_KEY`): fields pre-fill with transliteration, translation, POS.
  - Submit the form: word is created, drawer flips to "found" state.
  - Footer "View / Add Annotations →": annotation drawer opens, vocab drawer closes.
  - Escape key closes the vocab drawer.
  - Clicking backdrop closes the vocab drawer.

- [ ] **Step 9: Commit**

  ```bash
  git add frontend/src/routes/texts/[id]/+page.svelte
  git commit -m "feat: wire VocabLookupDrawer into text page — click token to look up or add word"
  ```

---

## Self-Review

**Spec coverage:**
- ✅ Click token → drawer opens
- ✅ Word in vocab → word card (translation, transliteration, mastery)
- ✅ Word not in vocab → quick-add form
- ✅ AI pre-fill via `POST /words/analyze`
- ✅ AI degrades to 503 `AI_NOT_CONFIGURED` without key
- ✅ "Open in vocabulary →" link
- ✅ Footer → jump to annotation drawer
- ✅ No end-of-session summary (explicitly excluded)

**Placeholder scan:** None found.

**Type consistency:**
- `AlignmentTokenResponse` used for `onTokenClick` callback in Task 3 — matches `VocabLookupDrawer.token` prop in Task 5 and `openVocabLookup` in Task 6. ✅
- `useLookupWordByArabic` returns `WordResponse | null` — matches `found` state type in VocabLookupDrawer. ✅
- `handleCreated(wordId: string)` in VocabLookupDrawer matches `onCreated: (wordId: string) => void` prop in QuickAddWordForm. ✅
- `onannotate: (anchor: string) => void` in VocabLookupDrawer matches `openAnnotationDrawer(anchor: string)` in page. ✅
