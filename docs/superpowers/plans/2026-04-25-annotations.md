# Annotations Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the annotations feature — clean up the backend schema, wire the frontend store and components, and make annotations visible as type-coded inline badges in the interlinear view with a slide-in drawer for create/edit/delete.

**Architecture:** All annotations for a text are fetched once at the text-detail page level and passed down to TokenGrid; the drawer filters client-side by anchor. This avoids per-token API calls. The backend receives a DB migration to drop vestigial columns and rename enum values before any frontend work starts.

**Tech Stack:** Kotlin/Ktor/Exposed/Flyway (backend), SvelteKit Svelte 5 runes / @tanstack/svelte-query / Tailwind v4 / bits-ui (frontend)

---

## File Map

### Backend — modified
| File | Change |
|---|---|
| `backend/src/main/resources/db/migration/V017__fix_annotation_schema.sql` | **CREATE** — drop vestigial columns, rename enum values |
| `backend/src/main/kotlin/…/domain/annotation/Annotation.kt` | Remove `masteryLevel`, `reviewFlag`; rename enum values |
| `backend/src/main/kotlin/…/infrastructure/exposed/AnnotationsTable.kt` | Remove `masteryLevel`, `reviewFlag` column definitions |
| `backend/src/main/kotlin/…/infrastructure/exposed/ExposedAnnotationRepository.kt` | Remove dropped field references from insert/update/toAnnotation |
| `backend/src/main/kotlin/…/domain/annotation/AnnotationService.kt` | Remove dropped params from `create()` and `update()` |
| `backend/src/main/kotlin/…/delivery/dto/annotation/AnnotationDtos.kt` | Strip dropped fields from all DTOs + `toResponse()` |
| `backend/src/main/kotlin/…/delivery/routes/AnnotationRoutes.kt` | Strip dropped field parsing from POST and PUT handlers |

### Frontend — created
| File | Purpose |
|---|---|
| `frontend/src/lib/stores/annotations.ts` | TanStack Query hooks for all annotation CRUD + word linking |
| `frontend/src/lib/components/annotations/AnnotationBadge.svelte` | Type-coded inline chip rendered below a token cell |
| `frontend/src/lib/components/annotations/WordSearchCombobox.svelte` | Autocomplete input → selected words as dismissible chips |
| `frontend/src/lib/components/annotations/AnnotationForm.svelte` | Create / edit form: type, content, linked words |
| `frontend/src/lib/components/annotations/AnnotationItem.svelte` | Single annotation display row in the drawer |
| `frontend/src/lib/components/annotations/AnnotationDrawer.svelte` | Slide-in panel wiring all annotation sub-components |

### Frontend — modified
| File | Change |
|---|---|
| `frontend/src/lib/api/types.gen.ts` | Regenerated — drops `masteryLevel`/`reviewFlag`, fixes type enum |
| `frontend/src/lib/components/texts/TokenGrid.svelte` | Add `annotations` prop + `onTokenClick` callback + badge rendering |
| `frontend/src/lib/components/texts/InterlinearSentence.svelte` | Thread `annotations` + `onTokenClick` through to TokenGrid |
| `frontend/src/routes/texts/[id]/+page.svelte` | Fetch annotations, manage drawer state, render `AnnotationDrawer` |
| `frontend/src/routes/words/[id]/+page.svelte` | Enhance annotation display section (show content + type badge) |

---

## Task 1: DB Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V017__fix_annotation_schema.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V017__fix_annotation_schema.sql

-- 1. Migrate existing type values to new enum names
UPDATE annotations SET type = 'VOCABULARY' WHERE type = 'VOCAB';
UPDATE annotations SET type = 'OTHER'      WHERE type = 'STRUCTURE';

-- 2. Drop vestigial columns (their CHECK/DEFAULT constraints drop automatically)
ALTER TABLE annotations DROP COLUMN mastery_level;
ALTER TABLE annotations DROP COLUMN review_flag;

-- 3. Replace the type CHECK constraint
ALTER TABLE annotations DROP CONSTRAINT annotations_type_check;
ALTER TABLE annotations ADD CONSTRAINT annotations_type_check
    CHECK (type IN ('VOCABULARY', 'GRAMMAR', 'CULTURAL', 'OTHER'));
```

- [ ] **Step 2: Start the backend and verify Flyway applies it cleanly**

```bash
just backend
# Watch logs — expect: "Successfully applied 1 migration to schema 'public'"
# No "Error" lines
```

- [ ] **Step 3: Confirm schema**

```bash
doppler run -- psql $DATABASE_URL -c "\d annotations"
# mastery_level and review_flag must NOT appear
# type column constraint must list VOCABULARY,GRAMMAR,CULTURAL,OTHER
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V017__fix_annotation_schema.sql
git commit -m "feat: drop annotation vestigial fields and fix type enum values"
```

---

## Task 2: Kotlin backend cleanup

**Files:**
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/annotation/Annotation.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/AnnotationsTable.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedAnnotationRepository.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/domain/annotation/AnnotationService.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/dto/annotation/AnnotationDtos.kt`
- Modify: `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/AnnotationRoutes.kt`

- [ ] **Step 1: Update `Annotation.kt`**

Replace the entire file:

```kotlin
package com.tonihacks.qalam.domain.annotation

import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@JvmInline
value class AnnotationId(val value: UUID) {
    override fun toString(): String = value.toString()
}

enum class AnnotationType { VOCABULARY, GRAMMAR, CULTURAL, OTHER }

@OptIn(ExperimentalTime::class)
data class Annotation(
    val id: AnnotationId,
    val textId: TextId,
    val anchor: String,
    val type: AnnotationType,
    val content: String?,
    val linkedWordIds: List<WordId>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

- [ ] **Step 2: Update `AnnotationsTable.kt`**

Replace the entire file:

```kotlin
package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object AnnotationsTable : Table("annotations") {
    @OptIn(ExperimentalUuidApi::class) val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class) val textId = uuid("text_id")
    val anchor = varchar("anchor", 1000)
    val type = varchar("type", 50)
    val content = text("content").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    @OptIn(ExperimentalUuidApi::class) override val primaryKey = PrimaryKey(id)
}

object AnnotationWordsTable : Table("annotation_words") {
    @OptIn(ExperimentalUuidApi::class) val annotationId = uuid("annotation_id")
    @OptIn(ExperimentalUuidApi::class) val wordId = uuid("word_id")
    @OptIn(ExperimentalUuidApi::class) override val primaryKey = PrimaryKey(annotationId, wordId)
}
```

- [ ] **Step 3: Update `ExposedAnnotationRepository.kt`**

Remove three sections (the file mostly stays the same — remove these specific parts):

In `save()`, remove lines:
```kotlin
it[masteryLevel] = annotation.masteryLevel?.name
it[reviewFlag] = annotation.reviewFlag
```

In `update()`, remove lines:
```kotlin
it[masteryLevel] = annotation.masteryLevel?.name
it[reviewFlag] = annotation.reviewFlag
```

Replace the `toAnnotation` extension function at the bottom of the file:
```kotlin
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
private fun ResultRow.toAnnotation(wordIds: List<WordId>) = Annotation(
    id = AnnotationId(this[AnnotationsTable.id].toJavaUuid()),
    textId = TextId(this[AnnotationsTable.textId].toJavaUuid()),
    anchor = this[AnnotationsTable.anchor],
    type = AnnotationType.valueOf(this[AnnotationsTable.type]),
    content = this[AnnotationsTable.content],
    linkedWordIds = wordIds,
    createdAt = this[AnnotationsTable.createdAt],
    updatedAt = this[AnnotationsTable.updatedAt],
)
```

Remove the two imports at the top:
```kotlin
import com.tonihacks.qalam.domain.word.MasteryLevel
```

- [ ] **Step 4: Update `AnnotationService.kt`**

Replace the entire file:

```kotlin
package com.tonihacks.qalam.domain.annotation

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AnnotationService(private val repo: AnnotationRepository) {

    suspend fun listByText(textId: TextId): Either<DomainError, List<Annotation>> =
        repo.findAllByTextId(textId)

    suspend fun getById(id: AnnotationId): Either<DomainError, Annotation> =
        repo.findById(id)

    suspend fun create(
        textId: TextId,
        anchor: String,
        type: AnnotationType,
        content: String?,
        linkedWordIds: List<WordId>,
    ): Either<DomainError, Annotation> = either {
        if (anchor.isBlank()) raise(DomainError.ValidationError("anchor", "anchor must not be blank"))

        val now = Clock.System.now()
        val annotation = Annotation(
            id = AnnotationId(UUID.randomUUID()),
            textId = textId,
            anchor = anchor,
            type = type,
            content = content,
            linkedWordIds = linkedWordIds,
            createdAt = now,
            updatedAt = now,
        )
        repo.save(annotation).bind()
    }

    suspend fun update(
        id: AnnotationId,
        anchor: String? = null,
        type: AnnotationType? = null,
        content: String? = null,
    ): Either<DomainError, Annotation> = either {
        val existing = repo.findById(id).bind()

        if (anchor != null && anchor.isBlank()) raise(DomainError.ValidationError("anchor", "anchor must not be blank"))

        val updated = existing.copy(
            anchor = anchor ?: existing.anchor,
            type = type ?: existing.type,
            content = content ?: existing.content,
            updatedAt = Clock.System.now(),
        )
        repo.update(updated).bind()
    }

    suspend fun delete(id: AnnotationId): Either<DomainError, Unit> =
        repo.delete(id)

    suspend fun addWordLink(id: AnnotationId, wordId: WordId): Either<DomainError, Annotation> =
        repo.addWordLink(id, wordId)

    suspend fun removeWordLink(id: AnnotationId, wordId: WordId): Either<DomainError, Annotation> =
        repo.removeWordLink(id, wordId)

    suspend fun getAnnotationsForWord(wordId: WordId): Either<DomainError, List<Annotation>> =
        repo.findAllByWordId(wordId)
}
```

- [ ] **Step 5: Update `AnnotationDtos.kt`**

Replace the entire file:

```kotlin
package com.tonihacks.qalam.delivery.dto.annotation

import com.tonihacks.qalam.domain.annotation.Annotation
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class AnnotationResponse(
    val id: String,
    val textId: String,
    val anchor: String,
    val type: String,
    val content: String?,
    val linkedWordIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateAnnotationRequest(
    val anchor: String,
    val type: String,
    val content: String? = null,
    val linkedWordIds: List<String> = emptyList(),
)

@Serializable
data class UpdateAnnotationRequest(
    val anchor: String? = null,
    val type: String? = null,
    val content: String? = null,
)

@Serializable
data class AddWordLinkRequest(
    val wordId: String,
)

@OptIn(ExperimentalTime::class)
fun Annotation.toResponse() = AnnotationResponse(
    id = id.toString(),
    textId = textId.toString(),
    anchor = anchor,
    type = type.name,
    content = content,
    linkedWordIds = linkedWordIds.map { it.toString() },
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
```

- [ ] **Step 6: Update `AnnotationRoutes.kt`**

Replace the entire file:

```kotlin
package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.annotation.AddWordLinkRequest
import com.tonihacks.qalam.delivery.dto.annotation.CreateAnnotationRequest
import com.tonihacks.qalam.delivery.dto.annotation.UpdateAnnotationRequest
import com.tonihacks.qalam.delivery.dto.annotation.toResponse
import com.tonihacks.qalam.domain.annotation.AnnotationId
import com.tonihacks.qalam.domain.annotation.AnnotationService
import com.tonihacks.qalam.domain.annotation.AnnotationType
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun Route.annotationRoutes(service: AnnotationService) {
    route("/texts/{textId}/annotations") {

        get {
            val textId = call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            service.listByText(TextId(textId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.map { a -> a.toResponse() }) },
            )
        }

        post {
            val textId = call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val req = call.receive<CreateAnnotationRequest>()
            val type = AnnotationType.entries.firstOrNull { it.name == req.type }
                ?: return@post call.respondError(DomainError.ValidationError("type", "'${req.type}' is not a valid annotation type"))
            val linkedWordIds = req.linkedWordIds.map { raw ->
                val uuid = raw.toAnnotationUuidOrNull()
                    ?: return@post call.respondError(DomainError.InvalidInput("'$raw' is not a valid UUID for wordId"))
                WordId(uuid)
            }
            service.create(
                textId = TextId(textId),
                anchor = req.anchor,
                type = type,
                content = req.content,
                linkedWordIds = linkedWordIds,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it.toResponse()) },
            )
        }

        get("/{id}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))
            service.getById(AnnotationId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        put("/{id}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))
            val req = call.receive<UpdateAnnotationRequest>()
            val type = if (req.type != null) {
                AnnotationType.entries.firstOrNull { it.name == req.type }
                    ?: return@put call.respondError(DomainError.ValidationError("type", "'${req.type}' is not a valid annotation type"))
            } else null
            service.update(
                id = AnnotationId(id),
                anchor = req.anchor,
                type = type,
                content = req.content,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))
            service.delete(AnnotationId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        post("/{id}/words") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))
            val req = call.receive<AddWordLinkRequest>()
            val wordId = req.wordId.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${req.wordId}' is not a valid UUID"))
            service.addWordLink(AnnotationId(id), WordId(wordId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}/words/{wordId}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))
            val wordId = call.parameters["wordId"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["wordId"]}' is not a valid UUID"))
            service.removeWordLink(AnnotationId(id), WordId(wordId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }
    }
}

fun Route.annotationWordRoutes(service: AnnotationService) {
    route("/words/{wordId}/annotations") {
        get {
            val wordId = call.parameters["wordId"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["wordId"]}' is not a valid UUID"))
            service.getAnnotationsForWord(WordId(wordId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.map { a -> a.toResponse() }) },
            )
        }
    }
}

private fun String.toAnnotationUuidOrNull(): UUID? =
    try { UUID.fromString(this) } catch (_: IllegalArgumentException) { null }
```

- [ ] **Step 7: Build and verify**

```bash
cd backend && ./gradlew classes
# Expected: BUILD SUCCESSFUL
```

- [ ] **Step 8: Smoke-test with curl**

With backend running (`just backend`):

```bash
# Set a valid TEXT_ID from your DB
TEXT_ID="<uuid-of-an-existing-text>"

# Create annotation — must use VOCABULARY not VOCAB
curl -s -X POST "http://localhost:8080/api/v1/texts/$TEXT_ID/annotations" \
  -H "Content-Type: application/json" \
  -d '{"anchor":"اللَّهِ","type":"VOCABULARY","content":"Genitive form of Allah"}' | jq .
# Expected: 201 response with id, no masteryLevel, no reviewFlag

# List annotations
curl -s "http://localhost:8080/api/v1/texts/$TEXT_ID/annotations" | jq .
# Expected: array, each item has: id, textId, anchor, type, content, linkedWordIds, createdAt, updatedAt
```

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/kotlin/
git commit -m "feat: remove annotation mastery/review fields and align enum values"
```

---

## Task 3: Regenerate frontend types

**Files:**
- Modify: `frontend/src/lib/api/types.gen.ts` (auto-generated)
- Modify: `frontend/src/lib/api/sdk.gen.ts` (auto-generated)

- [ ] **Step 1: Regenerate**

With backend running:
```bash
cd frontend && pnpm generate:types
```

- [ ] **Step 2: Verify the generated types**

```bash
grep -A 12 "^export type AnnotationResponse" frontend/src/lib/api/types.gen.ts
```

Expected output — no `masteryLevel`, no `reviewFlag`:
```
export type AnnotationResponse = {
    id: string;
    textId: string;
    anchor: string;
    type: string;
    content: string | null;
    linkedWordIds: string[];
    createdAt: string;
    updatedAt: string;
};
```

- [ ] **Step 3: Verify CreateAnnotationRequest**

```bash
grep -A 8 "^export type CreateAnnotationRequest" frontend/src/lib/api/types.gen.ts
```

Expected — no `masteryLevel`, no `reviewFlag`:
```
export type CreateAnnotationRequest = {
    anchor: string;
    type: string;
    content?: string | null;
    linkedWordIds?: string[];
};
```

- [ ] **Step 4: Commit**

```bash
cd frontend
git add src/lib/api/types.gen.ts src/lib/api/sdk.gen.ts
git commit -m "chore: regenerate api types after annotation schema cleanup"
```

---

## Task 4: Annotations store

**Files:**
- Create: `frontend/src/lib/stores/annotations.ts`

- [ ] **Step 1: Create the store**

```typescript
// frontend/src/lib/stores/annotations.ts
import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
    addWordLink,
    createAnnotation,
    deleteAnnotation,
    listAnnotations,
    removeWordLink,
    updateAnnotation,
} from '$lib/api/sdk.gen';
import type { AnnotationResponse, CreateAnnotationRequest, UpdateAnnotationRequest } from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
    if (data === undefined) throw new Error(`${label}: empty response`);
    return data;
}

export function useTextAnnotations(textId: () => string | undefined) {
    return createQuery(() => ({
        queryKey: ['annotations', textId()],
        queryFn: async () => {
            const id = textId();
            if (!id) throw new Error('Missing textId');
            const { data, error } = await listAnnotations({ path: { textId: id } });
            if (error) throw error;
            return requireData(data, 'listAnnotations') as AnnotationResponse[];
        },
        enabled: !!textId(),
    }));
}

export function useCreateAnnotation() {
    const qc = useQueryClient();
    return createMutation(() => ({
        mutationFn: async ({ textId, body }: { textId: string; body: CreateAnnotationRequest }) => {
            const { data, error } = await createAnnotation({ path: { textId }, body });
            if (error) throw error;
            return requireData(data, 'createAnnotation') as AnnotationResponse;
        },
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
        },
    }));
}

export function useUpdateAnnotation() {
    const qc = useQueryClient();
    return createMutation(() => ({
        mutationFn: async ({ textId, id, body }: { textId: string; id: string; body: UpdateAnnotationRequest }) => {
            const { data, error } = await updateAnnotation({ path: { textId, id }, body });
            if (error) throw error;
            return requireData(data, 'updateAnnotation') as AnnotationResponse;
        },
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
        },
    }));
}

export function useDeleteAnnotation() {
    const qc = useQueryClient();
    return createMutation(() => ({
        mutationFn: async ({ textId, id }: { textId: string; id: string }) => {
            const { error } = await deleteAnnotation({ path: { textId, id } });
            if (error) throw error;
        },
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
        },
    }));
}

export function useAddWordLink() {
    const qc = useQueryClient();
    return createMutation(() => ({
        mutationFn: async ({ textId, annotationId, wordId }: { textId: string; annotationId: string; wordId: string }) => {
            const { data, error } = await addWordLink({ path: { textId, id: annotationId }, body: { wordId } });
            if (error) throw error;
            return requireData(data, 'addWordLink') as AnnotationResponse;
        },
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
            qc.invalidateQueries({ queryKey: ['words', variables.wordId, 'annotations'] });
        },
    }));
}

export function useRemoveWordLink() {
    const qc = useQueryClient();
    return createMutation(() => ({
        mutationFn: async ({ textId, annotationId, wordId }: { textId: string; annotationId: string; wordId: string }) => {
            const { data, error } = await removeWordLink({ path: { textId, id: annotationId, wordId } });
            if (error) throw error;
            return requireData(data, 'removeWordLink') as AnnotationResponse;
        },
        onSuccess: (_data, variables) => {
            qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
            qc.invalidateQueries({ queryKey: ['words', variables.wordId, 'annotations'] });
        },
    }));
}
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd frontend && pnpm check
# Expected: no errors in annotations.ts
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/stores/annotations.ts
git commit -m "feat: add annotations store with full CRUD and word-link mutations"
```

---

## Task 5: AnnotationBadge component

**Files:**
- Create: `frontend/src/lib/components/annotations/AnnotationBadge.svelte`

- [ ] **Step 1: Create the component**

```svelte
<!-- frontend/src/lib/components/annotations/AnnotationBadge.svelte -->
<script lang="ts">
const BADGE_CONFIG: Record<string, { label: string; cls: string }> = {
    VOCABULARY: { label: 'V', cls: 'badge-vocab' },
    GRAMMAR:    { label: 'G', cls: 'badge-grammar' },
    CULTURAL:   { label: 'C', cls: 'badge-cultural' },
    OTHER:      { label: 'O', cls: 'badge-other' },
};

interface Props {
    type: string;
}

let { type }: Props = $props();

const cfg = $derived(BADGE_CONFIG[type] ?? BADGE_CONFIG['OTHER']);
</script>

<span class="annotation-badge {cfg.cls}" title={type.toLowerCase()}>{cfg.label}</span>

<style>
.annotation-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1rem;
    height: 1rem;
    border-radius: 0.25rem;
    font-size: 0.6rem;
    font-weight: 700;
    line-height: 1;
    flex-shrink: 0;
}
.badge-vocab   { background: hsl(150 30% 20%); color: hsl(150 55% 60%); }
.badge-grammar { background: hsl(220 30% 22%); color: hsl(220 55% 65%); }
.badge-cultural{ background: hsl(38 35% 22%);  color: hsl(38  55% 62%); }
.badge-other   { background: hsl(var(--muted)); color: hsl(var(--muted-foreground)); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/components/annotations/AnnotationBadge.svelte
git commit -m "feat: add AnnotationBadge component with type-coded colors"
```

---

## Task 6: WordSearchCombobox component

**Files:**
- Create: `frontend/src/lib/components/annotations/WordSearchCombobox.svelte`

- [ ] **Step 1: Create the component**

Uses the existing `useWordAutocomplete` hook from `words.ts` (enabled when `q.length >= 2`).

```svelte
<!-- frontend/src/lib/components/annotations/WordSearchCombobox.svelte -->
<script lang="ts">
import { useWordAutocomplete } from '$lib/stores/words';
import type { WordAutocompleteResponse } from '$lib/api/types.gen';

interface Props {
    selectedWords: WordAutocompleteResponse[];
    onchange: (words: WordAutocompleteResponse[]) => void;
}

let { selectedWords, onchange }: Props = $props();

let q = $state('');
let open = $state(false);

const autocomplete = useWordAutocomplete(() => q);

function select(word: WordAutocompleteResponse) {
    if (!selectedWords.some((w) => w.id === word.id)) {
        onchange([...selectedWords, word]);
    }
    q = '';
    open = false;
}

function remove(wordId: string) {
    onchange(selectedWords.filter((w) => w.id !== wordId));
}

function handleInput(e: Event) {
    q = (e.target as HTMLInputElement).value;
    open = q.length >= 2;
}
</script>

<div class="word-search">
    {#if selectedWords.length > 0}
        <div class="word-chips">
            {#each selectedWords as word (word.id)}
                <span class="word-chip">
                    <span class="word-chip-arabic arabic-text">{word.arabicText}</span>
                    {#if word.translation}
                        <span class="word-chip-translation">{word.translation}</span>
                    {/if}
                    <button
                        type="button"
                        class="word-chip-remove"
                        onclick={() => remove(word.id)}
                        aria-label="Remove {word.arabicText}"
                    >×</button>
                </span>
            {/each}
        </div>
    {/if}

    <div class="word-search-wrap">
        <input
            type="text"
            value={q}
            oninput={handleInput}
            onfocus={() => { if (q.length >= 2) open = true; }}
            onblur={() => setTimeout(() => (open = false), 150)}
            placeholder="Search Arabic or translation…"
            class="word-search-input"
        />
        {#if open && (autocomplete.data ?? []).length > 0}
            <ul class="word-dropdown" role="listbox">
                {#each autocomplete.data ?? [] as word (word.id)}
                    <li role="option" aria-selected="false">
                        <button
                            type="button"
                            class="word-dropdown-option"
                            onmousedown={() => select(word)}
                        >
                            <span class="arabic-text">{word.arabicText}</span>
                            {#if word.translation}
                                <span class="word-dropdown-translation">{word.translation}</span>
                            {/if}
                        </button>
                    </li>
                {/each}
            </ul>
        {/if}
    </div>
</div>

<style>
.word-search { display: flex; flex-direction: column; gap: 0.5rem; }

.word-chips { display: flex; flex-wrap: wrap; gap: 0.375rem; }

.word-chip {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    padding: 0.125rem 0.5rem;
    border-radius: 0.375rem;
    background: hsl(var(--muted));
    font-size: 0.8125rem;
}

.word-chip-arabic { font-size: 0.9rem; }
.word-chip-translation { font-size: 0.7rem; color: hsl(var(--muted-foreground)); }

.word-chip-remove {
    background: none;
    border: none;
    cursor: pointer;
    color: hsl(var(--muted-foreground));
    padding: 0 0.125rem;
    line-height: 1;
    font-size: 1rem;
}
.word-chip-remove:hover { color: hsl(var(--destructive)); }

.word-search-wrap { position: relative; }

.word-search-input {
    width: 100%;
    padding: 0.375rem 0.625rem;
    background: hsl(var(--muted) / 0.5);
    border: 1px solid hsl(var(--border));
    border-radius: 0.375rem;
    font-size: 0.8125rem;
    color: hsl(var(--foreground));
    outline: none;
    box-sizing: border-box;
}
.word-search-input:focus { border-color: hsl(var(--primary) / 0.6); }

.word-dropdown {
    position: absolute;
    top: calc(100% + 2px);
    left: 0;
    right: 0;
    z-index: 50;
    background: hsl(var(--popover, var(--background)));
    border: 1px solid hsl(var(--border));
    border-radius: 0.375rem;
    padding: 0.25rem 0;
    margin: 0;
    list-style: none;
    box-shadow: 0 8px 24px hsl(0 0% 0% / 0.3);
    max-height: 200px;
    overflow-y: auto;
}

.word-dropdown-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
    width: 100%;
    padding: 0.375rem 0.75rem;
    background: none;
    border: none;
    cursor: pointer;
    text-align: right;
    font-size: 0.8125rem;
    color: hsl(var(--foreground));
}
.word-dropdown-option:hover { background: hsl(var(--muted)); }

.word-dropdown-translation { font-size: 0.7rem; color: hsl(var(--muted-foreground)); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/components/annotations/WordSearchCombobox.svelte
git commit -m "feat: add WordSearchCombobox with autocomplete and chip selection"
```

---

## Task 7: AnnotationForm component

**Files:**
- Create: `frontend/src/lib/components/annotations/AnnotationForm.svelte`

- [ ] **Step 1: Create the component**

```svelte
<!-- frontend/src/lib/components/annotations/AnnotationForm.svelte -->
<script lang="ts">
import type { AnnotationResponse, WordAutocompleteResponse } from '$lib/api/types.gen';
import { useCreateAnnotation, useUpdateAnnotation } from '$lib/stores/annotations';
import WordSearchCombobox from './WordSearchCombobox.svelte';

const TYPES = ['VOCABULARY', 'GRAMMAR', 'CULTURAL', 'OTHER'] as const;
type AnnotationType = (typeof TYPES)[number];

interface Props {
    textId: string;
    anchor: string;
    initial?: AnnotationResponse;
    onSuccess: () => void;
    onCancel: () => void;
}

let { textId, anchor, initial, onSuccess, onCancel }: Props = $props();

let type = $state<AnnotationType>((initial?.type as AnnotationType) ?? 'VOCABULARY');
let content = $state(initial?.content ?? '');
let linkedWords = $state<WordAutocompleteResponse[]>([]);

const createAnnotation = useCreateAnnotation();
const updateAnnotation = useUpdateAnnotation();

const isPending = $derived(createAnnotation.isPending || updateAnnotation.isPending);

async function handleSubmit(e: Event) {
    e.preventDefault();
    if (initial) {
        // UpdateAnnotationRequest only accepts type + content (no linkedWordIds)
        await updateAnnotation.mutateAsync({
            textId,
            id: initial.id,
            body: { type, content: content.trim() || null },
        });
    } else {
        await createAnnotation.mutateAsync({
            textId,
            body: {
                anchor,
                type,
                content: content.trim() || null,
                linkedWordIds: linkedWords.map((w) => w.id),
            },
        });
    }
    onSuccess();
}
</script>

<form class="annotation-form" onsubmit={handleSubmit}>
    <div class="form-field">
        <label class="form-label">Type</label>
        <div class="type-selector">
            {#each TYPES as t}
                <button
                    type="button"
                    class="type-btn"
                    class:type-btn-active={type === t}
                    onclick={() => (type = t)}
                >{t.charAt(0) + t.slice(1).toLowerCase()}</button>
            {/each}
        </div>
    </div>

    <div class="form-field">
        <label class="form-label" for="ann-content">Note</label>
        <textarea
            id="ann-content"
            class="form-textarea"
            rows="3"
            placeholder="Your observation…"
            bind:value={content}
        ></textarea>
    </div>

    {#if !initial}
        <div class="form-field">
            <label class="form-label">Linked words</label>
            <WordSearchCombobox
                selectedWords={linkedWords}
                onchange={(words) => (linkedWords = words)}
            />
        </div>
    {/if}

    <div class="form-actions">
        <button type="submit" class="btn-primary" disabled={isPending}>
            {initial ? 'Save' : 'Add'}
        </button>
        <button type="button" class="btn-ghost" onclick={onCancel} disabled={isPending}>
            Cancel
        </button>
    </div>
</form>

<style>
.annotation-form { display: flex; flex-direction: column; gap: 0.875rem; }

.form-field { display: flex; flex-direction: column; gap: 0.3rem; }

.form-label {
    font-size: 0.7rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: hsl(var(--muted-foreground));
}

.type-selector { display: flex; gap: 0.25rem; flex-wrap: wrap; }

.type-btn {
    padding: 0.25rem 0.625rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    border: 1px solid hsl(var(--border));
    background: transparent;
    color: hsl(var(--muted-foreground));
    cursor: pointer;
}
.type-btn:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }
.type-btn-active { background: hsl(var(--primary) / 0.15); border-color: hsl(var(--primary) / 0.5); color: hsl(var(--primary)); }

.form-textarea {
    padding: 0.375rem 0.625rem;
    background: hsl(var(--muted) / 0.5);
    border: 1px solid hsl(var(--border));
    border-radius: 0.375rem;
    font-size: 0.8125rem;
    color: hsl(var(--foreground));
    resize: vertical;
    outline: none;
    font-family: inherit;
    line-height: 1.5;
}
.form-textarea:focus { border-color: hsl(var(--primary) / 0.6); }

.form-actions { display: flex; gap: 0.5rem; justify-content: flex-end; }

.btn-primary {
    padding: 0.375rem 0.875rem;
    border-radius: 0.375rem;
    font-size: 0.8125rem;
    font-weight: 600;
    background: hsl(var(--primary));
    color: hsl(var(--primary-foreground));
    border: none;
    cursor: pointer;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-ghost {
    padding: 0.375rem 0.875rem;
    border-radius: 0.375rem;
    font-size: 0.8125rem;
    background: transparent;
    color: hsl(var(--muted-foreground));
    border: 1px solid hsl(var(--border));
    cursor: pointer;
}
.btn-ghost:hover { color: hsl(var(--foreground)); background: hsl(var(--muted)); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/components/annotations/AnnotationForm.svelte
git commit -m "feat: add AnnotationForm with type selector, note textarea, and word linking"
```

---

## Task 8: AnnotationItem component

**Files:**
- Create: `frontend/src/lib/components/annotations/AnnotationItem.svelte`

- [ ] **Step 1: Create the component**

```svelte
<!-- frontend/src/lib/components/annotations/AnnotationItem.svelte -->
<script lang="ts">
import { Pencil, Trash2 } from 'lucide-svelte';
import type { AnnotationResponse } from '$lib/api/types.gen';
import AnnotationBadge from './AnnotationBadge.svelte';

interface Props {
    annotation: AnnotationResponse;
    textId: string;
    onEdit: (annotation: AnnotationResponse) => void;
    onDelete: (id: string) => void;
}

let { annotation, textId, onEdit, onDelete }: Props = $props();

let deleteConfirm = $state(false);

function handleDelete() {
    if (!deleteConfirm) {
        deleteConfirm = true;
        setTimeout(() => (deleteConfirm = false), 3000);
        return;
    }
    onDelete(annotation.id);
}
</script>

<div class="annotation-item">
    <div class="item-header">
        <AnnotationBadge type={annotation.type} />
        <span class="item-type">{annotation.type.charAt(0) + annotation.type.slice(1).toLowerCase()}</span>
        <div class="item-actions">
            <button class="item-btn" onclick={() => onEdit(annotation)} aria-label="Edit">
                <Pencil size={11} />
            </button>
            <button
                class="item-btn"
                class:item-btn-danger={deleteConfirm}
                onclick={handleDelete}
                aria-label="Delete"
            >
                <Trash2 size={11} />
            </button>
        </div>
    </div>

    {#if annotation.content}
        <p class="item-content">{annotation.content}</p>
    {/if}

    {#if annotation.linkedWordIds.length > 0}
        <div class="item-words">
            {#each annotation.linkedWordIds as wordId (wordId)}
                <a href="/words/{wordId}" class="item-word-link">→ word</a>
            {/each}
        </div>
    {/if}
</div>

<style>
.annotation-item {
    padding: 0.625rem 0;
    border-bottom: 1px solid hsl(var(--border) / 0.4);
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
}
.annotation-item:last-of-type { border-bottom: none; }

.item-header {
    display: flex;
    align-items: center;
    gap: 0.375rem;
}

.item-type {
    font-size: 0.7rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: hsl(var(--muted-foreground));
    flex: 1;
}

.item-actions { display: flex; gap: 0.125rem; margin-left: auto; }

.item-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1.375rem;
    height: 1.375rem;
    border-radius: 0.25rem;
    background: none;
    border: none;
    cursor: pointer;
    color: hsl(var(--muted-foreground));
}
.item-btn:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }
.item-btn-danger { color: hsl(var(--destructive)); }

.item-content {
    font-size: 0.8125rem;
    color: hsl(var(--foreground) / 0.9);
    line-height: 1.5;
    margin: 0;
}

.item-words { display: flex; flex-wrap: wrap; gap: 0.25rem; }

.item-word-link {
    font-size: 0.7rem;
    color: hsl(var(--primary));
    text-decoration: none;
    padding: 0.125rem 0.375rem;
    border-radius: 0.25rem;
    background: hsl(var(--primary) / 0.08);
}
.item-word-link:hover { background: hsl(var(--primary) / 0.15); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/components/annotations/AnnotationItem.svelte
git commit -m "feat: add AnnotationItem display component with edit/delete actions"
```

---

## Task 9: AnnotationDrawer component

**Files:**
- Create: `frontend/src/lib/components/annotations/AnnotationDrawer.svelte`

- [ ] **Step 1: Create the component**

```svelte
<!-- frontend/src/lib/components/annotations/AnnotationDrawer.svelte -->
<script lang="ts">
import { fly } from 'svelte/transition';
import type { AnnotationResponse } from '$lib/api/types.gen';
import { useDeleteAnnotation } from '$lib/stores/annotations';
import AnnotationForm from './AnnotationForm.svelte';
import AnnotationItem from './AnnotationItem.svelte';

interface Props {
    open: boolean;
    anchor: string;
    textId: string;
    annotations: AnnotationResponse[];
    onclose: () => void;
}

let { open, anchor, textId, annotations, onclose }: Props = $props();

const anchorAnnotations = $derived(annotations.filter((a) => a.anchor === anchor));
let editingId = $state<string | null>(null);
let showForm = $state(false);

const deleteAnnotation = useDeleteAnnotation();

$effect(() => {
    if (open) {
        editingId = null;
        showForm = anchorAnnotations.length === 0;
    }
});

function handleFormSuccess() {
    editingId = null;
    showForm = false;
}

async function handleDelete(id: string) {
    await deleteAnnotation.mutateAsync({ textId, id });
    if (anchorAnnotations.length <= 1) onclose();
}
</script>

{#if open}
    <div
        class="drawer-backdrop"
        role="button"
        tabindex="-1"
        aria-label="Close"
        onclick={onclose}
        onkeydown={(e) => e.key === 'Escape' && onclose()}
    ></div>

    <aside
        class="annotation-drawer"
        transition:fly={{ x: 360, duration: 220, opacity: 1 }}
        aria-label="Annotations"
    >
        <div class="drawer-header">
            <span class="drawer-anchor arabic-text">{anchor}</span>
            <button class="drawer-close" onclick={onclose} aria-label="Close">×</button>
        </div>

        <div class="drawer-body">
            {#each anchorAnnotations as ann (ann.id)}
                {#if editingId === ann.id}
                    <AnnotationForm
                        {textId}
                        {anchor}
                        initial={ann}
                        onSuccess={handleFormSuccess}
                        onCancel={() => (editingId = null)}
                    />
                {:else}
                    <AnnotationItem
                        annotation={ann}
                        {textId}
                        onEdit={(a) => { editingId = a.id; showForm = false; }}
                        onDelete={handleDelete}
                    />
                {/if}
            {/each}

            {#if showForm && editingId === null}
                <div class="drawer-form-section">
                    <AnnotationForm
                        {textId}
                        {anchor}
                        onSuccess={handleFormSuccess}
                        onCancel={anchorAnnotations.length > 0 ? () => (showForm = false) : onclose}
                    />
                </div>
            {:else if editingId === null}
                <button class="drawer-add-btn" onclick={() => (showForm = true)}>
                    + Add annotation
                </button>
            {/if}
        </div>
    </aside>
{/if}

<style>
.drawer-backdrop {
    position: fixed;
    inset: 0;
    background: hsl(0 0% 0% / 0.35);
    z-index: 40;
    cursor: default;
}

.annotation-drawer {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    width: 360px;
    background: hsl(var(--background));
    border-left: 1px solid hsl(var(--border));
    z-index: 50;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.drawer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem 0.875rem;
    border-bottom: 1px solid hsl(var(--border) / 0.6);
    gap: 0.75rem;
    flex-shrink: 0;
}

.drawer-anchor {
    font-size: 1.5rem;
    line-height: 1.6;
    color: hsl(var(--foreground));
}

.drawer-close {
    flex-shrink: 0;
    width: 1.75rem;
    height: 1.75rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.375rem;
    background: none;
    border: none;
    cursor: pointer;
    font-size: 1.25rem;
    line-height: 1;
    color: hsl(var(--muted-foreground));
}
.drawer-close:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }

.drawer-body {
    flex: 1;
    overflow-y: auto;
    padding: 0.875rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0;
}

.drawer-form-section {
    padding-top: 0.5rem;
    border-top: 1px solid hsl(var(--border) / 0.4);
    margin-top: 0.5rem;
}

.drawer-add-btn {
    margin-top: 0.75rem;
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    font-size: 0.8125rem;
    color: hsl(var(--primary));
    background: none;
    border: 1px dashed hsl(var(--primary) / 0.4);
    border-radius: 0.375rem;
    padding: 0.375rem 0.75rem;
    cursor: pointer;
    width: 100%;
    justify-content: center;
}
.drawer-add-btn:hover { background: hsl(var(--primary) / 0.06); }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/components/annotations/AnnotationDrawer.svelte
git commit -m "feat: add AnnotationDrawer slide-in panel component"
```

---

## Task 10: TokenGrid — add annotations and click

**Files:**
- Modify: `frontend/src/lib/components/texts/TokenGrid.svelte`

- [ ] **Step 1: Replace the file**

```svelte
<!-- frontend/src/lib/components/texts/TokenGrid.svelte -->
<script lang="ts">
import type { AlignmentTokenResponse, AnnotationResponse } from '$lib/api/types.gen';
import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';

interface Props {
    tokens: AlignmentTokenResponse[];
    annotations?: AnnotationResponse[];
    onTokenClick?: (anchor: string) => void;
}

let { tokens, annotations = [], onTokenClick }: Props = $props();

function badgesFor(arabicText: string): AnnotationResponse[] {
    return annotations.filter((a) => a.anchor === arabicText);
}
</script>

{#if tokens.length > 0}
    <div class="token-grid">
        <div class="token-row">
            {#each tokens as token (token.id)}
                {@const tokenBadges = badgesFor(token.arabic)}
                <dl
                    class="token-cell"
                    class:token-cell-clickable={!!onTokenClick}
                    class:token-cell-annotated={tokenBadges.length > 0}
                    role={onTokenClick ? 'button' : undefined}
                    tabindex={onTokenClick ? 0 : undefined}
                    onclick={() => onTokenClick?.(token.arabic)}
                    onkeydown={(e) => e.key === 'Enter' && onTokenClick?.(token.arabic)}
                >
                    <dt class="token-arabic arabic-text">{token.arabic}</dt>
                    {#if token.transliteration}
                        <dd class="token-translit transliteration">{token.transliteration}</dd>
                    {/if}
                    {#if token.translation}
                        <dd class="token-translation">{token.translation}</dd>
                    {/if}
                    {#if tokenBadges.length > 0}
                        <dd class="token-badges">
                            {#each tokenBadges as ann (ann.id)}
                                <AnnotationBadge type={ann.type} />
                            {/each}
                        </dd>
                    {/if}
                </dl>
            {/each}
        </div>
    </div>
{/if}

<style>
.token-grid { padding: 0.25rem 0; }

.token-row {
    display: flex;
    flex-wrap: wrap;
    direction: rtl;
    gap: 0;
}

.token-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0.25rem 0.625rem;
    border-inline-end: 1px solid hsl(var(--border) / 0.5);
    min-width: 4rem;
    margin: 0;
}

.token-cell-clickable { cursor: pointer; }
.token-cell-clickable:hover { background: hsl(var(--muted) / 0.5); }
.token-cell-clickable:focus-visible { outline: 2px solid hsl(var(--primary) / 0.6); outline-offset: -2px; }

.token-cell-annotated { position: relative; }

.token-arabic { font-size: 1.1rem; line-height: 1.8; }
.token-translit { font-size: 0.8125rem; margin: 0; }
.token-translation { font-size: 0.75rem; color: hsl(var(--foreground) / 0.7); text-align: center; margin: 0; }

.token-badges {
    display: flex;
    gap: 0.125rem;
    margin: 0.125rem 0 0;
    justify-content: center;
}
</style>
```

- [ ] **Step 2: Verify TypeScript**

```bash
cd frontend && pnpm check
# Expected: no errors
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/components/texts/TokenGrid.svelte
git commit -m "feat: add annotation badges and click handler to TokenGrid"
```

---

## Task 11: Thread annotations through InterlinearSentence

**Files:**
- Modify: `frontend/src/lib/components/texts/InterlinearSentence.svelte`

- [ ] **Step 1: Update InterlinearSentence.svelte**

Add `annotations` and `onTokenClick` to props and pass through to TokenGrid:

```svelte
<!-- frontend/src/lib/components/texts/InterlinearSentence.svelte -->
<script lang="ts">
import type { AnnotationResponse, SentenceResponse } from '$lib/api/types.gen';
import StaleTokenBanner from './StaleTokenBanner.svelte';
import TokenGrid from './TokenGrid.svelte';

interface Props {
    sentence: SentenceResponse;
    annotations?: AnnotationResponse[];
    onRetokenize?: (sentence: SentenceResponse) => Promise<void>;
    onMarkValid?: (sentence: SentenceResponse) => Promise<void>;
    onTokenClick?: (anchor: string) => void;
    isPending?: boolean;
}

let { sentence, annotations = [], onRetokenize, onMarkValid, onTokenClick, isPending = false }: Props = $props();

const showStaleBanner = $derived(!sentence.tokensValid && sentence.tokens.length > 0);
</script>

<div class="interlinear-sentence">
    <div class="sentence-number">{sentence.position}</div>

    <div class="sentence-body">
        <div class="sentence-arabic arabic-text">{sentence.arabicText}</div>

        {#if sentence.transliteration}
            <div class="sentence-transliteration transliteration">{sentence.transliteration}</div>
        {/if}

        {#if showStaleBanner && onRetokenize && onMarkValid}
            <StaleTokenBanner
                onRetokenize={() => onRetokenize!(sentence)}
                onMarkValid={() => onMarkValid!(sentence)}
                {isPending}
            />
        {/if}

        {#if sentence.tokens.length > 0}
            <div class="sentence-tokens">
                <TokenGrid tokens={sentence.tokens} {annotations} {onTokenClick} />
            </div>
        {/if}

        {#if sentence.freeTranslation}
            <div class="sentence-free-translation">
                <span class="sentence-free-label">Trans.</span>
                {sentence.freeTranslation}
            </div>
        {/if}

        {#if sentence.notes}
            <div class="sentence-notes">
                <span class="sentence-notes-label">Notes.</span>
                {sentence.notes}
            </div>
        {/if}
    </div>
</div>

<style>
.interlinear-sentence {
    display: flex;
    gap: 0.75rem;
    padding: 1rem 0;
    border-bottom: 1px solid hsl(var(--border) / 0.5);
}
.interlinear-sentence:last-child { border-bottom: none; }

.sentence-number {
    flex-shrink: 0;
    width: 1.5rem;
    font-size: 0.75rem;
    color: hsl(var(--muted-foreground));
    padding-top: 0.25rem;
    text-align: right;
}

.sentence-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.sentence-arabic { font-size: 1.25rem; line-height: 1.8; }
.sentence-transliteration { font-size: 0.9375rem; }

.sentence-tokens {
    border: 1px solid hsl(var(--border) / 0.6);
    border-radius: 0.375rem;
    overflow: hidden;
    background: hsl(var(--muted) / 0.3);
}

.sentence-free-translation {
    font-size: 0.875rem;
    color: hsl(var(--foreground) / 0.85);
    line-height: 1.6;
}

.sentence-free-label,
.sentence-notes-label {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: hsl(var(--muted-foreground));
    margin-right: 0.375rem;
}

.sentence-notes {
    font-size: 0.8125rem;
    color: hsl(var(--muted-foreground));
    line-height: 1.6;
    font-style: italic;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/components/texts/InterlinearSentence.svelte
git commit -m "feat: thread annotations and onTokenClick through InterlinearSentence"
```

---

## Task 12: Wire up the text detail page

**Files:**
- Modify: `frontend/src/routes/texts/[id]/+page.svelte`

- [ ] **Step 1: Add imports and state**

Add to the `<script>` block (after the existing imports):

```svelte
import AnnotationDrawer from '$lib/components/annotations/AnnotationDrawer.svelte';
import { useTextAnnotations } from '$lib/stores/annotations';

const annotations = useTextAnnotations(() => id);

let drawerOpen = $state(false);
let drawerAnchor = $state('');

function openDrawer(anchor: string) {
    drawerAnchor = anchor;
    drawerOpen = true;
}
```

- [ ] **Step 2: Update InterlinearSentence usages**

In the template, change each `<InterlinearSentence>` call to pass annotations and the click handler:

```svelte
{#each sentences.data ?? [] as sentence (sentence.id)}
    <InterlinearSentence
        {sentence}
        annotations={annotations.data ?? []}
        onTokenClick={openDrawer}
        isPending={autoTokenize.isPending || markValid.isPending}
        onRetokenize={async (s) => { await autoTokenize.mutateAsync({ textId: id, id: s.id }); }}
        onMarkValid={async (s) => { await markValid.mutateAsync({ textId: id, id: s.id, currentTokens: s.tokens }); }}
    />
{/each}
```

- [ ] **Step 3: Add the drawer to the template**

Add before the closing `</div>` of `page-text-detail`:

```svelte
<AnnotationDrawer
    open={drawerOpen}
    anchor={drawerAnchor}
    textId={id}
    annotations={annotations.data ?? []}
    onclose={() => (drawerOpen = false)}
/>
```

- [ ] **Step 4: Verify TypeScript**

```bash
cd frontend && pnpm check
# Expected: no errors
```

- [ ] **Step 5: Smoke-test in browser**

```bash
just run
# Open http://localhost:5173/texts/<any-text-with-tokens>
# Click any token cell → drawer opens from the right
# If token has no annotations: create form shown immediately
# Fill type + content → click Add → drawer shows new AnnotationItem
# Click again on same token → drawer shows the annotation
# Click × or backdrop → drawer closes
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/routes/texts/[id]/+page.svelte
git commit -m "feat: wire annotation drawer into text detail page"
```

---

## Task 13: Enhance word detail annotation display

**Files:**
- Modify: `frontend/src/routes/words/[id]/+page.svelte`

The annotations section already exists and queries data. Enhance the list items to show content and use `AnnotationBadge`.

- [ ] **Step 1: Add AnnotationBadge import**

In the `<script>` block, add:
```svelte
import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';
```

- [ ] **Step 2: Replace the annotation list markup**

Find and replace the `{#each annotations.data ?? [] as annotation}` block:

```svelte
<ul class="word-annotations-list">
    {#each annotations.data ?? [] as annotation (annotation.id)}
        <li class="word-annotation-item">
            <div class="word-annotation-header">
                <AnnotationBadge type={annotation.type} />
                <span class="word-annotation-anchor arabic-text">{annotation.anchor}</span>
                <a class="word-annotation-link" href="/texts/{annotation.textId}">
                    View text →
                </a>
            </div>
            {#if annotation.content}
                <p class="word-annotation-content">{annotation.content}</p>
            {/if}
        </li>
    {/each}
</ul>
```

- [ ] **Step 3: Add CSS for the new elements** (in `<style>` or inline — whichever the file uses)

The existing `word-annotation-*` classes are already in `layout.css`. Add only what's new:

```css
.word-annotation-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.word-annotation-content {
    font-size: 0.8125rem;
    color: hsl(var(--foreground) / 0.85);
    line-height: 1.5;
    margin: 0.25rem 0 0 1.5rem;
}
```

(Add inside a `<style>` block at the bottom of the file.)

- [ ] **Step 4: Verify TypeScript and test**

```bash
cd frontend && pnpm check
# Navigate to /words/<id> for a word that has annotations linked
# Annotations section shows: badge + anchor + "View text →" + content if present
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/routes/words/[id]/+page.svelte
git commit -m "feat: enhance word detail annotation display with badge and content"
```

---

## Verification Checklist

End-to-end test sequence:

- [ ] `just run` starts cleanly, Flyway applies V017 with no error
- [ ] `GET /api/v1/texts/:id/annotations` returns items without `masteryLevel`/`reviewFlag`
- [ ] `POST /api/v1/texts/:id/annotations` with `type: "VOCAB"` returns 400 (old value rejected)
- [ ] `POST /api/v1/texts/:id/annotations` with `type: "VOCABULARY"` returns 201
- [ ] Navigate to a text with tokens — token cells show hover cursor
- [ ] Click an unannotated token → drawer opens with empty create form
- [ ] Create an annotation → drawer shows the new `AnnotationItem`, token now shows a badge
- [ ] Click the same token again → drawer shows existing annotation + "Add annotation" button
- [ ] Edit annotation → form pre-fills, save updates the item
- [ ] Delete the only annotation on a token → drawer closes automatically
- [ ] Navigate to `/words/:id` for a linked word → Annotations section shows the annotation with badge + content
- [ ] `pnpm check` passes with zero TypeScript errors
