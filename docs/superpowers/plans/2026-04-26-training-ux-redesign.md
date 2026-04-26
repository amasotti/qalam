# Training UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the training flashcard with root/notes/examples/relations from the backend, and redesign the frontend card to a split-reveal layout with keyboard shortcuts (Space=reveal, 1/2/3=correct/wrong/skip).

**Architecture:** Backend extends `TrainingSessionWordResponse` with four new optional fields (`root`, `notes`, `examples[]`, `relations[]`) fetched eagerly in `findSessionWithWords` via a LEFT JOIN on roots and two batch sub-queries. Frontend rewrites `FlashCard.svelte` using Svelte 5 runes: question block always pinned, answer + context blocks expand below on reveal, keyboard handler registered in `$effect`.

**Tech Stack:** Kotlin 2.3/Ktor 3.4/Exposed v1 (backend); SvelteKit/Svelte 5 runes/Tailwind v4 (frontend); OpenAPI YAML as source of truth for frontend types.

---

## File Map

| File | Change |
|------|--------|
| `backend/.../domain/training/TrainingDomain.kt` | Add `TrainingWordExample`, `TrainingWordRelation` data classes; add 4 fields to `TrainingSessionWord` |
| `backend/.../domain/training/TrainingDtos.kt` | Add `TrainingWordExampleResponse`, `TrainingWordRelationResponse`; add 4 fields to `TrainingSessionWordResponse` |
| `backend/.../domain/training/TrainingService.kt` | Pass empty defaults for new fields when constructing `TrainingSessionWord` in `createSession` |
| `backend/.../infrastructure/exposed/ExposedTrainingRepository.kt` | LEFT JOIN RootsTable; batch-fetch examples + relations; update mapper |
| `backend/.../delivery/routes/TrainingRoutes.kt` | Map new fields in `toSessionResponse()` |
| `backend/.../resources/openapi/documentation.yaml` | Add `TrainingWordExampleResponse`, `TrainingWordRelationResponse` schemas; extend `TrainingSessionWordResponse` |
| `backend/.../delivery/TrainingIntegrationTest.kt` | Add test: session word response includes root/examples/relations |
| `frontend/src/lib/api/types.gen.ts` | Regenerated — do not edit manually |
| `frontend/src/lib/components/training/FlashCard.svelte` | Full rewrite |
| `frontend/src/routes/training/[id]/+page.svelte` | Pass `currentIndex`, `totalWords`, `mode` to FlashCard |

---

## Task 1: Add domain value types

**Files:**
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/training/TrainingDomain.kt`

- [ ] **Add two new data classes and extend `TrainingSessionWord`**

Replace the file content with:

```kotlin
package com.tonihacks.qalam.domain.training

import com.tonihacks.qalam.domain.word.WordId
import kotlin.time.Instant
import java.util.UUID

@JvmInline
value class TrainingSessionId(val value: UUID)

enum class TrainingMode { NEW, LEARNING, KNOWN, MIXED }
enum class FlashcardSide { ARABIC, TRANSLATION }
enum class TrainingResult { CORRECT, INCORRECT, SKIPPED }
enum class SessionStatus { ACTIVE, COMPLETED }

data class TrainingSession(
    val id: TrainingSessionId,
    val mode: TrainingMode,
    val status: SessionStatus,
    val totalWords: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val createdAt: Instant,
    val completedAt: Instant?,
)

data class TrainingWordExample(
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
)

data class TrainingWordRelation(
    val relatedWordId: String,
    val relatedWordArabic: String,
    val relatedWordTranslation: String?,
    val relationType: String,
)

data class TrainingSessionWord(
    val id: UUID,
    val sessionId: TrainingSessionId,
    val wordId: WordId,
    val position: Int,
    val frontSide: FlashcardSide,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val masteryLevel: String,
    val result: TrainingResult?,
    val masteryPromotedTo: String?,
    val answeredAt: Instant?,
    val root: String? = null,
    val notes: String? = null,
    val examples: List<TrainingWordExample> = emptyList(),
    val relations: List<TrainingWordRelation> = emptyList(),
)

data class MasteryPromotion(
    val wordId: WordId,
    val from: String,
    val to: String,
)
```

- [ ] **Compile to confirm no errors**

```bash
cd /Users/toni/halb-personal/qalam && ./gradlew :backend:compileKotlin --quiet
```

Expected: BUILD SUCCESSFUL

---

## Task 2: Add DTOs

**Files:**
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/training/TrainingDtos.kt`

- [ ] **Add two new serializable DTOs and extend `TrainingSessionWordResponse`**

Add the following two classes before `TrainingSessionWordResponse`:

```kotlin
@Serializable
data class TrainingWordExampleResponse(
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
)

@Serializable
data class TrainingWordRelationResponse(
    val relatedWordId: String,
    val relatedWordArabic: String,
    val relatedWordTranslation: String?,
    val relationType: String,
)
```

Replace `TrainingSessionWordResponse` with:

```kotlin
@Serializable
data class TrainingSessionWordResponse(
    val wordId: String,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val frontSide: String,
    val position: Int,
    val result: String?,
    val masteryLevel: String,
    val root: String? = null,
    val notes: String? = null,
    val examples: List<TrainingWordExampleResponse> = emptyList(),
    val relations: List<TrainingWordRelationResponse> = emptyList(),
)
```

- [ ] **Compile**

```bash
cd /Users/toni/halb-personal/qalam && ./gradlew :backend:compileKotlin --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/domain/training/TrainingDomain.kt \
        backend/src/main/kotlin/com/tonihacks/qalam/domain/training/TrainingDtos.kt
git commit -m "feat(training): add domain types and DTOs for root/notes/examples/relations"
```

---

## Task 3: Update repository to fetch enriched data

**Files:**
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedTrainingRepository.kt`

- [ ] **Add imports for new tables and Exposed operations**

Add to the import block (after existing imports):

```kotlin
import com.tonihacks.qalam.domain.training.TrainingWordExample
import com.tonihacks.qalam.domain.training.TrainingWordRelation
import org.jetbrains.exposed.v1.core.inList
```

- [ ] **Replace `findSessionWithWords` with the enriched version**

The method must: (1) LEFT JOIN `RootsTable` in the main word query to pick up `displayForm`; (2) collect word UUIDs; (3) batch-fetch up to 2 examples per word; (4) batch-fetch all relations with related word text; (5) pass examples and relations into the mapper.

```kotlin
override suspend fun findSessionWithWords(
    id: TrainingSessionId,
): Either<DomainError, Pair<TrainingSession, List<TrainingSessionWord>>> =
    suspendTransaction {
        try {
            val sessionRow = TrainingSessionsTable
                .selectAll()
                .where { TrainingSessionsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?: return@suspendTransaction DomainError.NotFound("TrainingSession", id.value.toString()).left()

            val session = sessionRow.toTrainingSession()

            val wordRows = TrainingSessionWordsTable
                .join(WordsTable, JoinType.INNER, additionalConstraint = { TrainingSessionWordsTable.wordId eq WordsTable.id })
                .join(RootsTable, JoinType.LEFT, additionalConstraint = { WordsTable.rootId eq RootsTable.id })
                .selectAll()
                .where { TrainingSessionWordsTable.sessionId eq id.value.toKotlinUuid() }
                .orderBy(TrainingSessionWordsTable.position)
                .toList()

            val wordUuids = wordRows.map { it[TrainingSessionWordsTable.wordId] }

            // Batch-fetch examples (max 2 per word, ordered by createdAt)
            val examplesByWordId: Map<kotlin.uuid.Uuid, List<TrainingWordExample>> =
                if (wordUuids.isEmpty()) emptyMap()
                else WordExamplesTable
                    .selectAll()
                    .where { WordExamplesTable.wordId inList wordUuids }
                    .orderBy(WordExamplesTable.createdAt)
                    .groupBy { it[WordExamplesTable.wordId] }
                    .mapValues { (_, rows) ->
                        rows.take(2).map { row ->
                            TrainingWordExample(
                                arabic          = row[WordExamplesTable.arabic],
                                transliteration = row[WordExamplesTable.transliteration],
                                translation     = row[WordExamplesTable.translation],
                            )
                        }
                    }

            // Batch-fetch relations — two queries to avoid a self-join alias on WordsTable
            val relationsByWordId: Map<kotlin.uuid.Uuid, List<TrainingWordRelation>> =
                if (wordUuids.isEmpty()) emptyMap()
                else {
                    val allRelationRows = WordRelationsTable
                        .selectAll()
                        .where { WordRelationsTable.wordId inList wordUuids }
                        .toList()

                    val relatedWordIds = allRelationRows
                        .map { it[WordRelationsTable.relatedWordId] }
                        .distinct()

                    val relatedWordsMap: Map<kotlin.uuid.Uuid, Pair<String, String?>> =
                        if (relatedWordIds.isEmpty()) emptyMap()
                        else WordsTable
                            .selectAll()
                            .where { WordsTable.id inList relatedWordIds }
                            .associate { row ->
                                row[WordsTable.id] to (row[WordsTable.arabicText] to row[WordsTable.translation])
                            }

                    allRelationRows
                        .groupBy { it[WordRelationsTable.wordId] }
                        .mapValues { (_, rows) ->
                            rows.mapNotNull { row ->
                                val rwId = row[WordRelationsTable.relatedWordId]
                                val (arabic, translation) = relatedWordsMap[rwId] ?: return@mapNotNull null
                                TrainingWordRelation(
                                    relatedWordId          = rwId.toJavaUuid().toString(),
                                    relatedWordArabic      = arabic,
                                    relatedWordTranslation = translation,
                                    relationType           = row[WordRelationsTable.relationType],
                                )
                            }
                        }
                }

            val words = wordRows.map { row ->
                val wid = row[TrainingSessionWordsTable.wordId]
                row.toTrainingSessionWord(
                    examples  = examplesByWordId[wid] ?: emptyList(),
                    relations = relationsByWordId[wid] ?: emptyList(),
                )
            }

            (session to words).right()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            DomainError.DatabaseError.left()
        }
    }
```

- [ ] **Update the `toTrainingSessionWord` mapper to accept examples and relations, and read new columns**

Replace the existing private `toTrainingSessionWord()` extension with:

```kotlin
private fun ResultRow.toTrainingSessionWord(
    examples: List<TrainingWordExample>,
    relations: List<TrainingWordRelation>,
) = TrainingSessionWord(
    id                = this[TrainingSessionWordsTable.id].toJavaUuid(),
    sessionId         = TrainingSessionId(this[TrainingSessionWordsTable.sessionId].toJavaUuid()),
    wordId            = WordId(this[TrainingSessionWordsTable.wordId].toJavaUuid()),
    position          = this[TrainingSessionWordsTable.position],
    frontSide         = FlashcardSide.valueOf(this[TrainingSessionWordsTable.frontSide]),
    arabicText        = this[WordsTable.arabicText],
    transliteration   = this[WordsTable.transliteration],
    translation       = this[WordsTable.translation],
    masteryLevel      = this[WordsTable.masteryLevel],
    result            = this[TrainingSessionWordsTable.result]?.let { TrainingResult.valueOf(it) },
    masteryPromotedTo = this[TrainingSessionWordsTable.masteryPromotedTo],
    answeredAt        = this[TrainingSessionWordsTable.answeredAt],
    root              = this.getOrNull(RootsTable.displayForm),
    notes             = this[WordsTable.notes],
    examples          = examples,
    relations         = relations,
)
```

- [ ] **Update `toSessionResponse` in `TrainingRoutes.kt` to map new fields**

In `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/TrainingRoutes.kt`, replace the `words` mapping inside `toSessionResponse`:

```kotlin
words = words.map { w ->
    TrainingSessionWordResponse(
        wordId          = w.wordId.value.toString(),
        arabicText      = w.arabicText,
        transliteration = w.transliteration,
        translation     = w.translation,
        frontSide       = w.frontSide.name,
        position        = w.position,
        result          = w.result?.name,
        masteryLevel    = w.masteryLevel,
        root            = w.root,
        notes           = w.notes,
        examples        = w.examples.map { ex ->
            TrainingWordExampleResponse(
                arabic          = ex.arabic,
                transliteration = ex.transliteration,
                translation     = ex.translation,
            )
        },
        relations       = w.relations.map { rel ->
            TrainingWordRelationResponse(
                relatedWordId          = rel.relatedWordId,
                relatedWordArabic      = rel.relatedWordArabic,
                relatedWordTranslation = rel.relatedWordTranslation,
                relationType           = rel.relationType,
            )
        },
    )
},
```

Also add the missing imports at the top of `TrainingRoutes.kt`:

```kotlin
import com.tonihacks.qalam.domain.training.TrainingWordExampleResponse
import com.tonihacks.qalam.domain.training.TrainingWordRelationResponse
```

- [ ] **Compile**

```bash
cd /Users/toni/halb-personal/qalam && ./gradlew :backend:compileKotlin --quiet
```

Expected: BUILD SUCCESSFUL

- [ ] **Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedTrainingRepository.kt \
        backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/TrainingRoutes.kt
git commit -m "feat(training): enrich session word response with root, notes, examples, relations"
```

---

## Task 4: Write integration test for enriched response

**Files:**
- Modify: `backend/src/test/kotlin/com/tonihacks/qalam/delivery/TrainingIntegrationTest.kt`

- [ ] **Add test: session word includes root, examples, and relations**

Add the following test group inside the `init` block, after the existing groups:

```kotlin
// ── Group N: enriched word data in session response ─────────────────────────

"GET /api/v1/training/sessions/{id} returns enriched word data" - {
    "word response includes root displayForm, examples, and relations" {
        testApp { client ->
            // Create a root
            val rootResp = client.post("/api/v1/roots") {
                contentType(ContentType.Application.Json)
                setBody("""{"letters":["ك","ت","ب"],"normalizedForm":"كتب","displayForm":"ك ت ب"}""")
            }
            rootResp.status shouldBe HttpStatusCode.Created
            val rootId = Regex(""""id":"([^"]+)"""").find(rootResp.bodyAsText())!!.groupValues[1]

            // Create two words (need both for a relation)
            val word1Id = createWord(
                client,
                """{"arabicText":"كَتَبَ","translation":"to write","dialect":"MSA","rootId":"$rootId","notes":"common verb"}""",
            )
            val word2Id = createWord(
                client,
                """{"arabicText":"كِتَابٌ","translation":"book","dialect":"MSA"}""",
            )

            // Add an example to word1
            client.post("/api/v1/words/$word1Id/examples") {
                contentType(ContentType.Application.Json)
                setBody("""{"arabic":"كَتَبَ الطَّالِبُ","translation":"The student wrote"}""")
            }.status shouldBe HttpStatusCode.Created

            // Add a relation: word1 RELATED word2
            client.post("/api/v1/words/$word1Id/relations") {
                contentType(ContentType.Application.Json)
                setBody("""{"relatedWordId":"$word2Id","relationType":"RELATED"}""")
            }.status shouldBe HttpStatusCode.Created

            // Create a training session (only word1 has mastery NEW, word2 too)
            val sessionResp = client.post("/api/v1/training/sessions") {
                contentType(ContentType.Application.Json)
                setBody("""{"mode":"MIXED","size":10}""")
            }
            sessionResp.status shouldBe HttpStatusCode.Created
            val sessionId = Json.parseToJsonElement(sessionResp.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content

            // Fetch session and assert enriched data on word1's entry
            val getResp = client.get("/api/v1/training/sessions/$sessionId")
            getResp.status shouldBe HttpStatusCode.OK
            val body = getResp.bodyAsText()

            // root
            body shouldContain "ك ت ب"
            // notes
            body shouldContain "common verb"
            // example
            body shouldContain "كَتَبَ الطَّالِبُ"
            body shouldContain "The student wrote"
            // relation
            body shouldContain "كِتَابٌ"
            body shouldContain "RELATED"
        }
    }
}
```

- [ ] **Run the new test**

```bash
cd /Users/toni/halb-personal/qalam && ./gradlew :backend:test --tests "com.tonihacks.qalam.delivery.TrainingIntegrationTest" --quiet
```

Expected: all tests pass including the new one.

- [ ] **Commit**

```bash
git add backend/src/test/kotlin/com/tonihacks/qalam/delivery/TrainingIntegrationTest.kt
git commit -m "test(training): assert enriched word data in session response"
```

---

## Task 5: Update OpenAPI schema

**Files:**
- Modify: `backend/src/main/resources/openapi/documentation.yaml`

- [ ] **Add two new schemas before `TrainingSessionWordResponse`**

Find the line `TrainingSessionWordResponse:` (around line 2759) and insert before it:

```yaml
    TrainingWordExampleResponse:
      type: object
      required: [arabic]
      properties:
        arabic:
          type: string
        transliteration:
          type: string
          nullable: true
        translation:
          type: string
          nullable: true

    TrainingWordRelationResponse:
      type: object
      required: [relatedWordId, relatedWordArabic, relationType]
      properties:
        relatedWordId:
          type: string
          format: uuid
        relatedWordArabic:
          type: string
        relatedWordTranslation:
          type: string
          nullable: true
        relationType:
          type: string
          enum: [SYNONYM, ANTONYM, RELATED]

```

- [ ] **Extend `TrainingSessionWordResponse` with the four new properties**

After the existing `masteryLevel` property in `TrainingSessionWordResponse`, add:

```yaml
        root:
          type: string
          nullable: true
          description: Root display form (e.g. "ك ت ب"), null if word has no root
        notes:
          type: string
          nullable: true
          description: Free-text notes on the word
        examples:
          type: array
          items:
            $ref: "#/components/schemas/TrainingWordExampleResponse"
          description: Up to 2 example sentences
        relations:
          type: array
          items:
            $ref: "#/components/schemas/TrainingWordRelationResponse"
          description: Related words (synonyms, antonyms, related)
```

- [ ] **Commit**

```bash
git add backend/src/main/resources/openapi/documentation.yaml
git commit -m "docs(openapi): extend TrainingSessionWordResponse with root/notes/examples/relations"
```

---

## Task 6: Regenerate frontend types

**Files:**
- Modify (auto-generated): `frontend/src/lib/api/types.gen.ts`

- [ ] **Start backend (needs to be running to serve the OpenAPI spec)**

```bash
cd /Users/toni/halb-personal/qalam && just backend &
```

Wait ~15 seconds for it to start, then verify:

```bash
curl -s http://localhost:8080/api/v1/openapi.json | grep -c "TrainingWordExampleResponse"
```

Expected: output `1` (or more).

- [ ] **Regenerate types**

```bash
cd /Users/toni/halb-personal/qalam/frontend && pnpm generate:types
```

- [ ] **Verify new types are present**

```bash
grep -n "TrainingWordExampleResponse\|TrainingWordRelationResponse\|root.*null\|examples.*Array" frontend/src/lib/api/types.gen.ts | head -10
```

Expected: lines showing both new types and new fields on `TrainingSessionWordResponse`.

- [ ] **Kill the background backend**

```bash
pkill -f "qalam.*backend" 2>/dev/null || true
```

- [ ] **Commit**

```bash
git add frontend/src/lib/api/types.gen.ts
git commit -m "chore: regenerate frontend types with enriched training word response"
```

---

## Task 7: Rewrite FlashCard.svelte

**Files:**
- Modify: `frontend/src/lib/components/training/FlashCard.svelte`

- [ ] **Replace the entire file**

```svelte
<script lang="ts">
import type { TrainingSessionWordResponse } from '$lib/api/types.gen';

interface Props {
	word: TrainingSessionWordResponse;
	isPending?: boolean;
	currentIndex: number;
	totalWords: number;
	mode: string;
	onresult: (result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') => void;
}

let { word, isPending = false, currentIndex, totalWords, mode, onresult }: Props = $props();

let revealed = $state(false);

const progressPct = $derived(Math.round((currentIndex / totalWords) * 100));

const front = $derived(
	word.frontSide === 'ARABIC' ? word.arabicText : (word.translation ?? word.arabicText)
);
const back = $derived(word.frontSide === 'ARABIC' ? (word.translation ?? '') : word.arabicText);
const backIsArabic = $derived(word.frontSide === 'TRANSLATION');
const directionLabel = $derived(
	word.frontSide === 'ARABIC' ? 'Arabic → Translation' : 'Translation → Arabic'
);

function handleResult(result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') {
	revealed = false;
	onresult(result);
}

$effect(() => {
	function handleKey(e: KeyboardEvent) {
		if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
		if (!revealed && (e.key === ' ' || e.key === 'Enter')) {
			e.preventDefault();
			revealed = true;
		} else if (revealed && !isPending) {
			if (e.key === '1' || e.code === 'Numpad1') handleResult('CORRECT');
			else if (e.key === '2' || e.code === 'Numpad2') handleResult('INCORRECT');
			else if (e.key === '3' || e.code === 'Numpad3') handleResult('SKIPPED');
		}
	}
	window.addEventListener('keydown', handleKey);
	return () => window.removeEventListener('keydown', handleKey);
});
</script>

<div class="fc-shell">
	<div class="fc-progress-bar">
		<div class="fc-progress-fill" style="width: {progressPct}%"></div>
	</div>
	<div class="fc-meta">
		<span>{currentIndex + 1} / {totalWords}</span>
		<span>{mode}</span>
	</div>

	<div class="fc-card">
		<!-- Question block — always visible -->
		<div class="fc-question" class:fc-question--compact={revealed}>
			<span class="fc-direction">{directionLabel}</span>

			{#if word.frontSide === 'ARABIC'}
				<div class="arabic arabic-display fc-arabic">{front}</div>
				{#if word.transliteration}
					<div class="fc-translit">{word.transliteration}</div>
				{/if}
			{:else}
				<div class="fc-translation-front">{front}</div>
			{/if}

			{#if word.root}
				<span class="fc-root-chip">{word.root}</span>
			{/if}
		</div>

		{#if !revealed}
			<button class="fc-reveal-btn" onclick={() => (revealed = true)} disabled={isPending}>
				▾ Reveal
				<kbd>Space</kbd>
			</button>
		{:else}
			<!-- Answer -->
			<div class="fc-answer-block">
				<span class="fc-section-label">Answer</span>
				{#if backIsArabic}
					<div class="arabic arabic-display fc-answer-arabic">{back}</div>
					{#if word.transliteration}
						<div class="fc-translit">{word.transliteration}</div>
					{/if}
				{:else}
					<div class="fc-answer-text">{back}</div>
				{/if}
			</div>

			<!-- Examples -->
			{#if word.examples && word.examples.length > 0}
				<div class="fc-examples-block">
					<span class="fc-section-label">Examples</span>
					{#each word.examples as ex (ex.arabic)}
						<div class="fc-example">
							<div class="arabic-text fc-example-arabic">{ex.arabic}</div>
							{#if ex.transliteration}
								<div class="fc-translit">{ex.transliteration}</div>
							{/if}
							{#if ex.translation}
								<div class="fc-example-translation">{ex.translation}</div>
							{/if}
						</div>
					{/each}
				</div>
			{/if}

			<!-- Notes -->
			{#if word.notes}
				<div class="fc-notes-block">
					<span class="fc-section-label fc-notes-label">Note</span>
					<p class="fc-notes-text">{word.notes}</p>
				</div>
			{/if}

			<!-- Relations -->
			{#if word.relations && word.relations.length > 0}
				<div class="fc-relations-block">
					<span class="fc-section-label">Related</span>
					<div class="fc-relations-chips">
						{#each word.relations as rel (rel.relatedWordId + rel.relationType)}
							<span class="fc-relation-chip">
								<span class="arabic">{rel.relatedWordArabic}</span>
								{#if rel.relatedWordTranslation}
									<span class="fc-relation-translation">{rel.relatedWordTranslation}</span>
								{/if}
							</span>
						{/each}
					</div>
				</div>
			{/if}

			<!-- Actions -->
			<div class="fc-actions">
				<button
					class="fc-btn fc-btn--correct"
					onclick={() => handleResult('CORRECT')}
					disabled={isPending}
				>
					✓ Correct <kbd>1</kbd>
				</button>
				<button
					class="fc-btn fc-btn--wrong"
					onclick={() => handleResult('INCORRECT')}
					disabled={isPending}
				>
					✗ Wrong <kbd>2</kbd>
				</button>
				<button
					class="fc-btn fc-btn--skip"
					onclick={() => handleResult('SKIPPED')}
					disabled={isPending}
				>
					→ Skip <kbd>3</kbd>
				</button>
			</div>
		{/if}
	</div>
</div>

<style>
	.fc-shell {
		width: 100%;
		max-width: 560px;
		margin: 2rem auto;
		padding: 0 1rem;
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
	}

	.fc-progress-bar {
		height: 3px;
		background: hsl(var(--muted));
		border-radius: 9999px;
		overflow: hidden;
	}

	.fc-progress-fill {
		height: 100%;
		background: hsl(var(--foreground));
		border-radius: 9999px;
		transition: width 0.3s ease;
	}

	.fc-meta {
		display: flex;
		justify-content: space-between;
		font-size: 0.75rem;
		color: hsl(var(--muted-foreground));
		margin-bottom: 0.5rem;
	}

	.fc-card {
		background: hsl(var(--background));
		border: 1px solid hsl(var(--border));
		border-radius: 10px;
		overflow: hidden;
		display: flex;
		flex-direction: column;
	}

	.fc-question {
		padding: 1.5rem 1.5rem 1.25rem;
		border-bottom: 1px solid hsl(var(--border));
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		transition: padding 0.2s ease;
	}

	.fc-question--compact {
		padding: 0.75rem 1.5rem;
		background: hsl(var(--muted) / 0.4);
		flex-direction: row;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.5rem 1rem;
	}

	.fc-direction {
		font-size: 0.7rem;
		text-transform: uppercase;
		letter-spacing: 0.06em;
		color: hsl(var(--muted-foreground));
		width: 100%;
		text-align: center;
	}

	.fc-question--compact .fc-direction {
		display: none;
	}

	.fc-arabic {
		font-size: 2rem;
		line-height: 1.4;
		text-align: center;
	}

	.fc-question--compact .fc-arabic {
		font-size: 1.25rem;
	}

	.fc-translit {
		font-style: italic;
		font-size: 0.85rem;
		color: hsl(var(--muted-foreground));
	}

	.fc-translation-front {
		font-size: 1.5rem;
		font-weight: 500;
		text-align: center;
	}

	.fc-question--compact .fc-translation-front {
		font-size: 1rem;
	}

	.fc-root-chip {
		font-size: 0.75rem;
		background: hsl(var(--muted));
		color: hsl(var(--muted-foreground));
		padding: 0.15rem 0.5rem;
		border-radius: 4px;
		direction: rtl;
		display: inline-block;
	}

	.fc-reveal-btn {
		margin: 1.25rem auto;
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0.6rem 1.5rem;
		background: hsl(var(--muted));
		border: none;
		border-radius: 6px;
		font-size: 0.875rem;
		color: hsl(var(--foreground));
		cursor: pointer;
		transition: background 0.15s;
	}

	.fc-reveal-btn:hover:not(:disabled) {
		background: hsl(var(--muted) / 0.7);
	}

	.fc-section-label {
		font-size: 0.65rem;
		text-transform: uppercase;
		letter-spacing: 0.08em;
		color: hsl(var(--muted-foreground));
		display: block;
		margin-bottom: 0.35rem;
	}

	.fc-answer-block {
		padding: 1rem 1.5rem;
		border-bottom: 1px solid hsl(var(--border));
	}

	.fc-answer-text {
		font-size: 1.375rem;
		font-weight: 600;
		color: hsl(var(--foreground));
	}

	.fc-answer-arabic {
		font-size: 1.5rem;
	}

	.fc-examples-block {
		padding: 0.75rem 1.5rem;
		border-bottom: 1px solid hsl(var(--border) / 0.6);
	}

	.fc-example + .fc-example {
		margin-top: 0.75rem;
		padding-top: 0.75rem;
		border-top: 1px solid hsl(var(--border) / 0.4);
	}

	.fc-example-arabic {
		font-size: 1rem;
		line-height: 1.5;
	}

	.fc-example-translation {
		font-size: 0.8rem;
		color: hsl(var(--muted-foreground));
		margin-top: 0.1rem;
	}

	.fc-notes-block {
		padding: 0.625rem 1.5rem;
		background: hsl(45 100% 96%);
		border-bottom: 1px solid hsl(var(--border) / 0.6);
	}

	.fc-notes-label {
		color: hsl(35 80% 40%);
	}

	.fc-notes-text {
		font-size: 0.8rem;
		color: hsl(35 60% 25%);
		margin: 0;
		line-height: 1.5;
	}

	.fc-relations-block {
		padding: 0.625rem 1.5rem;
		border-bottom: 1px solid hsl(var(--border) / 0.6);
	}

	.fc-relations-chips {
		display: flex;
		flex-wrap: wrap;
		gap: 0.4rem;
	}

	.fc-relation-chip {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		background: hsl(var(--muted));
		border-radius: 4px;
		padding: 0.2rem 0.5rem;
		font-size: 0.8rem;
	}

	.fc-relation-translation {
		font-size: 0.7rem;
		color: hsl(var(--muted-foreground));
	}

	.fc-actions {
		display: flex;
		gap: 0.5rem;
		padding: 0.875rem 1.25rem;
	}

	.fc-btn {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.4rem;
		padding: 0.55rem 0;
		border-radius: 6px;
		font-size: 0.8rem;
		cursor: pointer;
		border: 1px solid hsl(var(--border));
		background: hsl(var(--background));
		color: hsl(var(--foreground));
		transition: background 0.12s;
	}

	.fc-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.fc-btn--correct {
		background: hsl(var(--foreground));
		color: hsl(var(--background));
		border-color: hsl(var(--foreground));
	}

	.fc-btn--correct:hover:not(:disabled) {
		opacity: 0.85;
	}

	.fc-btn--wrong:hover:not(:disabled),
	.fc-btn--skip:hover:not(:disabled) {
		background: hsl(var(--muted));
	}

	kbd {
		font-size: 0.65rem;
		background: hsl(var(--muted));
		color: hsl(var(--muted-foreground));
		padding: 0.1rem 0.3rem;
		border-radius: 3px;
		border: 1px solid hsl(var(--border));
		font-family: inherit;
	}

	.fc-btn--correct kbd {
		background: hsl(var(--background) / 0.15);
		color: hsl(var(--background) / 0.7);
		border-color: hsl(var(--background) / 0.2);
	}
</style>
```

- [ ] **Run frontend checks**

```bash
cd /Users/toni/halb-personal/qalam && just lint-frontend && just format-frontend && just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Commit**

```bash
git add frontend/src/lib/components/training/FlashCard.svelte
git commit -m "feat(training): rewrite FlashCard — split reveal, keyboard shortcuts, enriched metadata"
```

---

## Task 8: Update session page to pass new props

**Files:**
- Modify: `frontend/src/routes/training/[id]/+page.svelte`

- [ ] **Pass `currentIndex`, `totalWords`, and `mode` to FlashCard**

Replace the existing `<FlashCard ... />` usage and remove the old `.session-header` block. The full updated file:

```svelte
<script lang="ts">
import { page } from '$app/state';
import type { SessionSummaryResponse, TrainingSessionWordResponse } from '$lib/api/types.gen';
import FlashCard from '$lib/components/training/FlashCard.svelte';
import SessionSummary from '$lib/components/training/SessionSummary.svelte';
import { useCompleteSession, useRecordResult, useSession } from '$lib/stores/training';

const sessionId = $derived(page.params.id);

const session = useSession(() => sessionId);
const record = useRecordResult();
const complete = useCompleteSession();

let summary = $state<SessionSummaryResponse | null>(null);
let currentIndex = $state(0);
let isPending = $state(false);

// Snapshot unanswered words once on load — never re-derive from server state mid-session.
// Re-deriving causes the array to shrink on each background refetch, which breaks currentIndex.
let localWords = $state<TrainingSessionWordResponse[]>([]);

$effect(() => {
	if (session.data && localWords.length === 0) {
		localWords = (session.data.words ?? []).filter(
			(w) => w.result === null || w.result === undefined
		);
	}
});

const currentWord = $derived(localWords[currentIndex] ?? null);
const isFinished = $derived(currentWord === null && localWords.length > 0);

async function handleResult(result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') {
	const sid = sessionId;
	if (!currentWord || !sid) return;
	isPending = true;
	try {
		await record.mutateAsync({
			sessionId: sid,
			body: { wordId: currentWord.wordId, result },
		});
		if (currentIndex + 1 >= localWords.length) {
			summary = await complete.mutateAsync(sid);
		} else {
			currentIndex += 1;
		}
	} finally {
		isPending = false;
	}
}
</script>

{#if session.isLoading}
  <p style="text-align:center;padding:2rem;color:hsl(var(--muted-foreground))">Loading…</p>
{:else if session.isError}
  <p style="text-align:center;padding:2rem;color:hsl(var(--destructive))">Error loading session.</p>
{:else if summary}
  <SessionSummary {summary} />
{:else if currentWord}
  <FlashCard
    word={currentWord}
    isPending={isPending || record.isPending || complete.isPending}
    {currentIndex}
    totalWords={localWords.length}
    mode={session.data?.mode ?? ''}
    onresult={handleResult}
  />
{:else if isFinished}
  <p style="text-align:center;padding:2rem;color:hsl(var(--muted-foreground))">All words answered. Completing session…</p>
{/if}
```

- [ ] **Run frontend checks**

```bash
cd /Users/toni/halb-personal/qalam && just lint-frontend && just format-frontend && just check-frontend
```

Expected: zero errors, zero warnings.

- [ ] **Commit**

```bash
git add frontend/src/routes/training/[id]/+page.svelte
git commit -m "feat(training): pass progress props to FlashCard, remove redundant session-header"
```

---

## Task 9: Full stack smoke test

- [ ] **Start everything**

```bash
cd /Users/toni/halb-personal/qalam && just run
```

- [ ] **Open training in browser**

Navigate to `http://localhost:5173/training`. Create a session and step through cards:

- Card should be compact (not full-screen)
- Progress bar fills left-to-right as you answer
- Before reveal: question visible, root chip if word has a root
- After reveal: question pinned at top, answer large and bold, examples/notes/relations sections visible if data exists
- Press Space to reveal, then 1/2/3 to answer — confirm keyboard works
- Complete session — summary screen appears normally

- [ ] **Stop services**

```bash
just stop 2>/dev/null || pkill -f "qalam" 2>/dev/null || true
```
