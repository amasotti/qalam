# Analytics Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A single `GET /api/v1/analytics/overview` endpoint returns word/text/root counts with distribution breakdowns (by dialect, difficulty, mastery, part-of-speech) plus training summary (accuracy trend, promotions); the frontend renders this as stat cards and horizontal CSS bars on a new `/analytics` page.

**Architecture:** New `analytics` domain — `AnalyticsRepository` interface, `ExposedAnalyticsRepository` (Kotlin-side groupBy matching the existing `getMasteryDistribution` pattern), and `AnalyticsService`. No changes to existing repositories. Single overview endpoint. Frontend: new `analytics.ts` store + one `+page.svelte`.

**Tech Stack:** Kotlin/Ktor, Exposed v1 DSL (`org.jetbrains.exposed.v1.*`), Arrow Either, Koin; SvelteKit Svelte 5 runes, `@tanstack/svelte-query`, OpenAPI-generated types.

---

## File Map

**Create (backend):**
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsDtos.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsRepository.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsService.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedAnalyticsRepository.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/AnalyticsRoutes.kt`
- `backend/src/test/kotlin/com/tonihacks/qalam/delivery/AnalyticsIntegrationTest.kt`

**Modify (backend):**
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/koin/AppModule.kt` — add `analyticsModule`
- `backend/src/main/kotlin/com/tonihacks/qalam/delivery/Routing.kt` — inject + mount `analyticsRoutes`
- `backend/src/main/resources/openapi/documentation.yaml` — add `analytics` tag + overview endpoint

**Create (frontend):**
- `frontend/src/lib/stores/analytics.ts`
- `frontend/src/routes/analytics/+page.svelte`

---

## Task 1: Analytics DTOs

**Files:**
- Create: `backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsDtos.kt`

- [ ] **Step 1: Create AnalyticsDtos.kt**

```kotlin
package com.tonihacks.qalam.domain.analytics

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsOverviewResponse(
    val words: WordStats,
    val texts: TextStats,
    val roots: RootStats,
    val training: TrainingAnalytics,
)

@Serializable
data class WordStats(
    val total: Int,
    val byDialect: Map<String, Int>,
    val byDifficulty: Map<String, Int>,
    val byMastery: Map<String, Int>,
    val byPartOfSpeech: Map<String, Int>,
)

@Serializable
data class TextStats(
    val total: Int,
    val byDialect: Map<String, Int>,
    val byDifficulty: Map<String, Int>,
)

@Serializable
data class RootStats(
    val total: Int,
)

@Serializable
data class TrainingAnalytics(
    val totalSessions: Int,
    val completedSessions: Int,
    val averageAccuracy: Double,
    val totalPromotions: Int,
    val recentSessions: List<SessionAccuracyPoint>,
)

@Serializable
data class SessionAccuracyPoint(
    val date: String,      // Instant.toString()
    val accuracy: Double,  // correct / (correct + incorrect), 0.0 when no answers
    val mode: String,      // TrainingMode.name
)
```

- [ ] **Step 2: Compile check**

```bash
cd backend && ./gradlew compileKotlin
```

Expected: no errors (these are pure data classes with no dependencies beyond `kotlinx.serialization`).

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsDtos.kt
git commit -m "feat: add analytics DTOs"
```

---

## Task 2: AnalyticsRepository Interface

**Files:**
- Create: `backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsRepository.kt`

- [ ] **Step 1: Create AnalyticsRepository.kt**

```kotlin
package com.tonihacks.qalam.domain.analytics

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError

interface AnalyticsRepository {
    suspend fun getWordStats(): Either<DomainError, WordStats>
    suspend fun getTextStats(): Either<DomainError, TextStats>
    suspend fun getRootCount(): Either<DomainError, Int>
    suspend fun getTrainingAnalytics(): Either<DomainError, TrainingAnalytics>
}
```

- [ ] **Step 2: Compile check**

```bash
cd backend && ./gradlew compileKotlin
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsRepository.kt
git commit -m "feat: add AnalyticsRepository interface"
```

---

## Task 3: ExposedAnalyticsRepository

**Files:**
- Create: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedAnalyticsRepository.kt`

**Context:** Uses `suspendTransaction` from `org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction` (matches all other `Exposed*Repository` files). Table objects are in the same package — `WordsTable`, `TextsTable`, `RootsTable`, `TrainingSessionsTable`, `TrainingSessionWordsTable`. Column names:
- `WordsTable.dialect`, `.difficulty`, `.masteryLevel`, `.partOfSpeech`
- `TextsTable.dialect`, `.difficulty`
- `RootsTable` — no distribution needed, count only
- `TrainingSessionsTable.status`, `.mode`, `.correctCount`, `.incorrectCount`, `.createdAt`
- `TrainingSessionWordsTable.masteryPromotedTo` (nullable varchar)

- [ ] **Step 1: Write ExposedAnalyticsRepository.kt**

```kotlin
package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.analytics.AnalyticsRepository
import com.tonihacks.qalam.domain.analytics.SessionAccuracyPoint
import com.tonihacks.qalam.domain.analytics.TextStats
import com.tonihacks.qalam.domain.analytics.TrainingAnalytics
import com.tonihacks.qalam.domain.analytics.WordStats
import com.tonihacks.qalam.domain.error.DomainError
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class ExposedAnalyticsRepository : AnalyticsRepository {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getWordStats(): Either<DomainError, WordStats> =
        suspendTransaction {
            try {
                val rows = WordsTable.selectAll().toList()
                WordStats(
                    total = rows.size,
                    byDialect = rows.groupBy { it[WordsTable.dialect] }.mapValues { it.value.size },
                    byDifficulty = rows.groupBy { it[WordsTable.difficulty] }.mapValues { it.value.size },
                    byMastery = rows.groupBy { it[WordsTable.masteryLevel] }.mapValues { it.value.size },
                    byPartOfSpeech = rows.groupBy { it[WordsTable.partOfSpeech] }.mapValues { it.value.size },
                ).right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getTextStats(): Either<DomainError, TextStats> =
        suspendTransaction {
            try {
                val rows = TextsTable.selectAll().toList()
                TextStats(
                    total = rows.size,
                    byDialect = rows.groupBy { it[TextsTable.dialect] }.mapValues { it.value.size },
                    byDifficulty = rows.groupBy { it[TextsTable.difficulty] }.mapValues { it.value.size },
                ).right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getRootCount(): Either<DomainError, Int> =
        suspendTransaction {
            try {
                RootsTable.selectAll().count().toInt().right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getTrainingAnalytics(): Either<DomainError, TrainingAnalytics> =
        suspendTransaction {
            try {
                val total = TrainingSessionsTable.selectAll().count().toInt()
                val completed = TrainingSessionsTable
                    .selectAll()
                    .where { TrainingSessionsTable.status eq "COMPLETED" }
                    .toList()
                val avgAccuracy = completed
                    .mapNotNull { row ->
                        val c = row[TrainingSessionsTable.correctCount]
                        val answered = c + row[TrainingSessionsTable.incorrectCount]
                        if (answered > 0) c.toDouble() / answered else null
                    }
                    .average()
                    .let { if (it.isNaN()) 0.0 else it }
                val promotions = TrainingSessionWordsTable
                    .selectAll()
                    .where { TrainingSessionWordsTable.masteryPromotedTo.isNotNull() }
                    .count().toInt()
                val recent = completed
                    .sortedByDescending { it[TrainingSessionsTable.createdAt] }
                    .take(20)
                    .map { row ->
                        val c = row[TrainingSessionsTable.correctCount]
                        val answered = c + row[TrainingSessionsTable.incorrectCount]
                        SessionAccuracyPoint(
                            date = row[TrainingSessionsTable.createdAt].toString(),
                            accuracy = if (answered > 0) c.toDouble() / answered else 0.0,
                            mode = row[TrainingSessionsTable.mode],
                        )
                    }
                TrainingAnalytics(
                    totalSessions = total,
                    completedSessions = completed.size,
                    averageAccuracy = avgAccuracy,
                    totalPromotions = promotions,
                    recentSessions = recent,
                ).right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }
}
```

**Note on `.isNotNull()`:** If the compiler cannot resolve `isNotNull()` on `TrainingSessionWordsTable.masteryPromotedTo`, add the import `import org.jetbrains.exposed.v1.core.isNotNull`.

- [ ] **Step 2: Compile check**

```bash
cd backend && ./gradlew compileKotlin
```

Expected: no errors. If `isNotNull` is unresolved, add the import noted above.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedAnalyticsRepository.kt
git commit -m "feat: implement ExposedAnalyticsRepository"
```

---

## Task 4: AnalyticsService

**Files:**
- Create: `backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsService.kt`

- [ ] **Step 1: Create AnalyticsService.kt**

```kotlin
package com.tonihacks.qalam.domain.analytics

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.error.DomainError

class AnalyticsService(private val repo: AnalyticsRepository) {

    suspend fun getOverview(): Either<DomainError, AnalyticsOverviewResponse> = either {
        val words    = repo.getWordStats().bind()
        val texts    = repo.getTextStats().bind()
        val rootCount = repo.getRootCount().bind()
        val training = repo.getTrainingAnalytics().bind()
        AnalyticsOverviewResponse(
            words    = words,
            texts    = texts,
            roots    = RootStats(total = rootCount),
            training = training,
        )
    }
}
```

- [ ] **Step 2: Compile check**

```bash
cd backend && ./gradlew compileKotlin
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/domain/analytics/AnalyticsService.kt
git commit -m "feat: implement AnalyticsService"
```

---

## Task 5: AnalyticsRoutes + DI + Routing wire-up

**Files:**
- Create: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/AnalyticsRoutes.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/koin/AppModule.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/Routing.kt`

- [ ] **Step 1: Create AnalyticsRoutes.kt**

```kotlin
package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.analytics.AnalyticsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.analyticsRoutes(service: AnalyticsService) {
    route("/analytics") {
        get("/overview") {
            service.getOverview().fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}
```

- [ ] **Step 2: Add analyticsModule to AppModule.kt**

Open `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/koin/AppModule.kt`. Add at the bottom, before the `appModule` definition:

```kotlin
val analyticsModule = module {
    single<AnalyticsRepository> { ExposedAnalyticsRepository() }
    single { AnalyticsService(get()) }
}
```

Then extend the `appModule` includes list by adding `analyticsModule`:

```kotlin
val appModule = module {
    includes(rootsModules, wordsModules, textsModule, sentencesModule, transliterationModule, annotationsModule, trainingModule, analyticsModule)
}
```

Also add the missing imports:
```kotlin
import com.tonihacks.qalam.domain.analytics.AnalyticsRepository
import com.tonihacks.qalam.domain.analytics.AnalyticsService
import com.tonihacks.qalam.infrastructure.exposed.ExposedAnalyticsRepository
```

- [ ] **Step 3: Wire into Routing.kt**

Open `backend/src/main/kotlin/com/tonihacks/qalam/delivery/Routing.kt`. Inside `configureRouting()`, add the service injection after the `trainingService` line:

```kotlin
val analyticsService by inject<AnalyticsService>()
```

Add the import:
```kotlin
import com.tonihacks.qalam.domain.analytics.AnalyticsService
import com.tonihacks.qalam.delivery.routes.analyticsRoutes
```

Inside `route("/api/v1") { }`, add after `trainingRoutes(trainingService)`:

```kotlin
analyticsRoutes(analyticsService)
```

- [ ] **Step 4: Compile check**

```bash
cd backend && ./gradlew compileKotlin
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/AnalyticsRoutes.kt \
        backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/koin/AppModule.kt \
        backend/src/main/kotlin/com/tonihacks/qalam/delivery/Routing.kt
git commit -m "feat: add analytics routes, DI, and routing wire-up"
```

---

## Task 6: Backend integration test

**Files:**
- Create: `backend/src/test/kotlin/com/tonihacks/qalam/delivery/AnalyticsIntegrationTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class AnalyticsIntegrationTest : BaseIntegrationTest() {

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement().execute("TRUNCATE TABLE words CASCADE")
                conn.createStatement().execute("TRUNCATE TABLE texts CASCADE")
                conn.createStatement().execute("TRUNCATE TABLE arabic_roots CASCADE")
                conn.createStatement().execute("TRUNCATE TABLE training_sessions CASCADE")
            }
        }

        "GET /api/v1/analytics/overview" - {
            "returns 200 with zero counts on empty DB" {
                testApp { client ->
                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain """"total":0"""
                    body shouldContain """"totalSessions":0"""
                }
            }

            "counts a word and reflects it in words.total and byDialect" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"كَتَبَ","dialect":"MSA","difficulty":"BEGINNER"}""")
                    }

                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain """"MSA":1"""
                    body shouldContain """"BEGINNER":1"""
                    body shouldContain """"NEW":1"""
                }
            }

            "counts a text and reflects it in texts.total and byDialect" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"Test","body":"نص","dialect":"MSA","difficulty":"BEGINNER"}""")
                    }

                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    // texts.total = 1, texts.byDialect.MSA = 1
                    body shouldContain """"MSA":1"""
                    body shouldContain """"BEGINNER":1"""
                }
            }

            "training.completedSessions reflects completed training sessions" {
                testApp { client ->
                    // Create a word so we can start a session
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"كَتَبَ","dialect":"MSA","difficulty":"BEGINNER"}""")
                    }

                    // Create + complete a session
                    val sessionBody = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":1}""")
                    }.bodyAsText()

                    val sessionId = Regex(""""id":"([^"]+)"""").find(sessionBody)!!.groupValues[1]
                    val wordId = Regex(""""wordId":"([^"]+)"""").find(sessionBody)!!.groupValues[1]

                    client.post("/api/v1/training/sessions/$sessionId/results") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId","result":"CORRECT"}""")
                    }
                    client.post("/api/v1/training/sessions/$sessionId/complete")

                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain """"completedSessions":1"""
                    response.bodyAsText() shouldContain """"totalSessions":1"""
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run the test (should fail — nothing implemented yet if you ran this first, or pass if Task 5 was done first)**

```bash
cd backend && ./gradlew test --tests "com.tonihacks.qalam.delivery.AnalyticsIntegrationTest"
```

Expected: all 4 tests PASS (Tasks 1–5 already done).

- [ ] **Step 3: Run full backend test suite**

```bash
cd backend && ./gradlew test
```

Expected: `BUILD SUCCESSFUL` — all existing tests still pass.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/kotlin/com/tonihacks/qalam/delivery/AnalyticsIntegrationTest.kt
git commit -m "test: analytics overview integration tests"
```

---

## Task 7: OpenAPI documentation

**Files:**
- Modify: `backend/src/main/resources/openapi/documentation.yaml`

- [ ] **Step 1: Add analytics tag**

Find the `tags:` section near the top of `documentation.yaml`. Add after the `training` entry:

```yaml
  - name: analytics
    description: Aggregated statistics across all domains
```

- [ ] **Step 2: Add the overview path**

Find the `paths:` section. Add after the last training path:

```yaml
  /api/v1/analytics/overview:
    get:
      tags:
        - analytics
      summary: Get aggregated analytics overview
      operationId: getAnalyticsOverview
      responses:
        '200':
          description: Analytics overview
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AnalyticsOverviewResponse'
```

- [ ] **Step 3: Add schemas**

Find the `components: schemas:` section. Add all six new schemas. Follow the exact style of existing schemas in the file (check whether existing schemas use `nullable: true` or `type: [string, 'null']` — match whatever is already there):

```yaml
    AnalyticsOverviewResponse:
      type: object
      required: [words, texts, roots, training]
      properties:
        words:
          $ref: '#/components/schemas/WordStats'
        texts:
          $ref: '#/components/schemas/TextStats'
        roots:
          $ref: '#/components/schemas/RootStats'
        training:
          $ref: '#/components/schemas/TrainingAnalytics'

    WordStats:
      type: object
      required: [total, byDialect, byDifficulty, byMastery, byPartOfSpeech]
      properties:
        total:
          type: integer
        byDialect:
          type: object
          additionalProperties:
            type: integer
        byDifficulty:
          type: object
          additionalProperties:
            type: integer
        byMastery:
          type: object
          additionalProperties:
            type: integer
        byPartOfSpeech:
          type: object
          additionalProperties:
            type: integer

    TextStats:
      type: object
      required: [total, byDialect, byDifficulty]
      properties:
        total:
          type: integer
        byDialect:
          type: object
          additionalProperties:
            type: integer
        byDifficulty:
          type: object
          additionalProperties:
            type: integer

    RootStats:
      type: object
      required: [total]
      properties:
        total:
          type: integer

    TrainingAnalytics:
      type: object
      required: [totalSessions, completedSessions, averageAccuracy, totalPromotions, recentSessions]
      properties:
        totalSessions:
          type: integer
        completedSessions:
          type: integer
        averageAccuracy:
          type: number
          format: double
        totalPromotions:
          type: integer
        recentSessions:
          type: array
          items:
            $ref: '#/components/schemas/SessionAccuracyPoint'

    SessionAccuracyPoint:
      type: object
      required: [date, accuracy, mode]
      properties:
        date:
          type: string
        accuracy:
          type: number
          format: double
        mode:
          type: string
```

- [ ] **Step 4: Start backend and verify spec is served**

```bash
just backend   # in another terminal, or: cd backend && ./gradlew run
curl http://localhost:8080/api/v1/openapi.json | python3 -m json.tool | grep -A3 "analytics"
```

Expected: the `analytics` tag and `/api/v1/analytics/overview` path appear in the JSON output.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/openapi/documentation.yaml
git commit -m "docs: add analytics overview endpoint to OpenAPI spec"
```

---

## Task 8: Regenerate frontend API types

**Files:**
- Modify: `frontend/src/lib/api/types.gen.ts` (auto-generated)
- Modify: `frontend/src/lib/api/sdk.gen.ts` (auto-generated)

- [ ] **Step 1: Backend must be running, then regenerate**

```bash
# Terminal 1 (if not already running):
just backend

# Terminal 2:
cd frontend && pnpm generate:types
```

Expected output: `types.gen.ts` updated with new types:
- `AnalyticsOverviewResponse`
- `WordStats`
- `TextStats`
- `RootStats`
- `TrainingAnalytics`
- `SessionAccuracyPoint`

Expected output: `sdk.gen.ts` updated with new SDK function:
- `getAnalyticsOverview`

Verify:
```bash
grep "getAnalyticsOverview\|AnalyticsOverviewResponse\|WordStats\|TrainingAnalytics" \
    frontend/src/lib/api/sdk.gen.ts frontend/src/lib/api/types.gen.ts
```

Expected: all six names appear.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/api/types.gen.ts frontend/src/lib/api/sdk.gen.ts
git commit -m "chore: regenerate API types with analytics endpoint"
```

---

## Task 9: Analytics Store

**Files:**
- Create: `frontend/src/lib/stores/analytics.ts`

- [ ] **Step 1: Create analytics.ts**

```typescript
import { createQuery } from '@tanstack/svelte-query';
import { getAnalyticsOverview } from '$lib/api/sdk.gen';
import type { AnalyticsOverviewResponse } from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
    if (data === undefined) throw new Error(`${label}: empty response`);
    return data;
}

export function useAnalytics() {
    return createQuery(() => ({
        queryKey: ['analytics', 'overview'],
        queryFn: async () => {
            const { data, error } = await getAnalyticsOverview({});
            if (error) throw error;
            return requireData(data, 'getAnalyticsOverview') as AnalyticsOverviewResponse;
        },
        staleTime: 60_000,
    }));
}
```

**Note:** `getAnalyticsOverview` may not take `{}` — check the generated `sdk.gen.ts` signature and adjust if needed (some endpoints take no arguments at all).

- [ ] **Step 2: Type-check**

```bash
cd frontend && pnpm check
```

Expected: 0 errors in `analytics.ts`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/stores/analytics.ts
git commit -m "feat: add analytics store"
```

---

## Task 10: Analytics Page

**Files:**
- Create: `frontend/src/routes/analytics/+page.svelte`

- [ ] **Step 1: Create +page.svelte**

```svelte
<script lang="ts">
  import { useAnalytics } from '$lib/stores/analytics';

  const analytics = useAnalytics();
</script>

{#if analytics.isPending}
  <p class="loading">Loading…</p>
{:else if analytics.isError}
  <p class="error">Failed to load analytics.</p>
{:else if analytics.data}
  {@const d = analytics.data}

  <div class="analytics-page">
    <h1>Analytics</h1>

    <div class="stat-cards">
      <div class="stat-card">
        <span class="stat-value">{d.words.total}</span>
        <span class="stat-label">Words</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{d.texts.total}</span>
        <span class="stat-label">Texts</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{d.roots.total}</span>
        <span class="stat-label">Roots</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{d.training.completedSessions}</span>
        <span class="stat-label">Sessions</span>
      </div>
    </div>

    <section class="analytics-section">
      <h2>Words</h2>
      <div class="distributions">
        {#each [
          { label: 'Mastery', dist: d.words.byMastery, total: d.words.total },
          { label: 'Dialect', dist: d.words.byDialect, total: d.words.total },
          { label: 'Difficulty', dist: d.words.byDifficulty, total: d.words.total },
          { label: 'Part of Speech', dist: d.words.byPartOfSpeech, total: d.words.total },
        ] as group}
          <div class="dist-group">
            <h3>{group.label}</h3>
            {#each Object.entries(group.dist).sort((a, b) => b[1] - a[1]) as [key, count]}
              <div class="bar-row">
                <span class="bar-label">{key}</span>
                <div class="bar-track">
                  <div
                    class="bar-fill"
                    style="width: {group.total > 0 ? ((count / group.total) * 100).toFixed(1) : 0}%"
                  ></div>
                </div>
                <span class="bar-count">{count}</span>
              </div>
            {/each}
          </div>
        {/each}
      </div>
    </section>

    <section class="analytics-section">
      <h2>Texts</h2>
      <div class="distributions">
        {#each [
          { label: 'Dialect', dist: d.texts.byDialect, total: d.texts.total },
          { label: 'Difficulty', dist: d.texts.byDifficulty, total: d.texts.total },
        ] as group}
          <div class="dist-group">
            <h3>{group.label}</h3>
            {#each Object.entries(group.dist).sort((a, b) => b[1] - a[1]) as [key, count]}
              <div class="bar-row">
                <span class="bar-label">{key}</span>
                <div class="bar-track">
                  <div
                    class="bar-fill"
                    style="width: {group.total > 0 ? ((count / group.total) * 100).toFixed(1) : 0}%"
                  ></div>
                </div>
                <span class="bar-count">{count}</span>
              </div>
            {/each}
          </div>
        {/each}
      </div>
    </section>

    <section class="analytics-section">
      <h2>Training</h2>
      <div class="stat-cards">
        <div class="stat-card">
          <span class="stat-value">{Math.round(d.training.averageAccuracy * 100)}%</span>
          <span class="stat-label">Avg Accuracy</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{d.training.totalPromotions}</span>
          <span class="stat-label">Promotions</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{d.training.totalSessions}</span>
          <span class="stat-label">Total Sessions</span>
        </div>
      </div>

      {#if d.training.recentSessions.length > 0}
        <div class="dist-group" style="margin-top: 1.5rem;">
          <h3>Recent sessions (last {d.training.recentSessions.length})</h3>
          {#each d.training.recentSessions as s}
            <div class="bar-row">
              <span class="bar-label">
                {s.mode} · {new Date(s.date).toLocaleDateString()}
              </span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  style="width: {(s.accuracy * 100).toFixed(1)}%"
                ></div>
              </div>
              <span class="bar-count">{(s.accuracy * 100).toFixed(0)}%</span>
            </div>
          {/each}
        </div>
      {/if}
    </section>
  </div>
{/if}

<style>
  .analytics-page {
    max-width: 52rem;
    margin: 2rem auto;
    padding: 0 1rem;
    display: flex;
    flex-direction: column;
    gap: 2.5rem;
  }

  h1 { font-size: 1.5rem; font-weight: 700; margin: 0; }
  h2 { font-size: 1.1rem; font-weight: 600; margin: 0 0 1rem; }
  h3 { font-size: 0.8rem; font-weight: 600; text-transform: uppercase;
       letter-spacing: 0.05em; color: hsl(var(--muted-foreground)); margin: 0 0 0.5rem; }

  .stat-cards {
    display: flex;
    gap: 1rem;
    flex-wrap: wrap;
  }

  .stat-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.25rem;
    padding: 1rem 1.5rem;
    border: 1px solid hsl(var(--border));
    border-radius: 0.5rem;
    min-width: 6rem;
  }

  .stat-value { font-size: 1.75rem; font-weight: 700; line-height: 1; }
  .stat-label { font-size: 0.75rem; color: hsl(var(--muted-foreground)); text-transform: uppercase;
                letter-spacing: 0.04em; }

  .analytics-section {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .distributions {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr));
    gap: 1.5rem;
  }

  .dist-group {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
  }

  .bar-row {
    display: grid;
    grid-template-columns: 10rem 1fr 3rem;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.8rem;
  }

  .bar-label {
    color: hsl(var(--foreground));
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .bar-track {
    height: 0.5rem;
    background: hsl(var(--muted));
    border-radius: 9999px;
    overflow: hidden;
  }

  .bar-fill {
    height: 100%;
    background: hsl(var(--primary));
    border-radius: 9999px;
    transition: width 0.3s ease;
    min-width: 2px;
  }

  .bar-count {
    text-align: right;
    color: hsl(var(--muted-foreground));
    font-variant-numeric: tabular-nums;
  }

  .loading, .error {
    max-width: 52rem;
    margin: 2rem auto;
    padding: 0 1rem;
  }

  .error { color: hsl(var(--destructive)); }
</style>
```

- [ ] **Step 2: Type-check**

```bash
cd frontend && pnpm check
```

Expected: 0 errors, 0 warnings.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/routes/analytics/+page.svelte
git commit -m "feat: add analytics dashboard page"
```

---

## Verification

End-to-end after all tasks:

```bash
# 1. Full backend test suite
cd backend && ./gradlew test
# Expected: BUILD SUCCESSFUL

# 2. Frontend type check
cd frontend && pnpm check
# Expected: 0 errors

# 3. Manual flow
just run   # starts full stack

# 4. Open http://localhost:5173/analytics
# Verify:
#   - Stat cards show word/text/root/session counts
#   - Horizontal bars render for each distribution group
#   - Training section shows avg accuracy and recent session bars
#   - All bars scale proportionally to the totals
#   - Page works on empty DB (all zeros, no crash)

# 5. curl smoke test
curl -s http://localhost:8080/api/v1/analytics/overview | python3 -m json.tool
# Expected: JSON with keys words, texts, roots, training
```
