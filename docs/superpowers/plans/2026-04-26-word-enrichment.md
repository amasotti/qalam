# Word Enrichment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (obligatory) or superpowers:executing-plans to implement this plan task-by-task. 
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the Word entity with morphological properties (gender, verb pattern, plural forms) and semantic
relations (synonyms, antonyms, related words) plus free-text notes. An AI enrichment endpoint suggests all fields on
demand; the user confirms before saving. Frontend word detail page displays the new data in a clean editorial layout.

**Architecture:**

- Three new DB tables: `word_morphology` (1:0..1), `word_plurals` (1:many), `word_relations` (self-join)
- `notes TEXT` added to `words`
- New domain classes: `WordMorphology`, `WordPlural`, `WordRelation`
- `WordRepository` gains morphology/plural/relation CRUD methods
- `ExposedWordRepository` implements them; new Exposed table objects
- New DTOs and routes under `/api/v1/words/{id}/`
- `AiClient` gains `enrichWord()` returning structured suggestions (no auto-save)
- Frontend: word detail page gets morphology strip, plural chips, relations panel, notes block, AI Enrich button/drawer

**Tech Stack:** Kotlin 2.x, Ktor 3.4.x, Exposed v1 DSL (`org.jetbrains.exposed.v1.*`), Arrow Either, Koin; SvelteKit
Svelte 5 runes, `@tanstack/svelte-query`, OpenAPI-generated types, Tailwind v4.

---

## File Map

**Create (backend):**

- `backend/src/main/resources/db/migration/V018__add_word_notes.sql`
- `backend/src/main/resources/db/migration/V019__create_word_morphology.sql`
- `backend/src/main/resources/db/migration/V020__create_word_plurals.sql`
- `backend/src/main/resources/db/migration/V021__create_word_relations.sql`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/WordMorphologyTable.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/WordPluralsTable.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/WordRelationsTable.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordMorphologyRepository.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordPluralsRepository.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordRelationsRepository.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordMorphology.kt`
- `backend/src/main/kotlin/com/tonihacks/qalam/delivery/dto/word/WordEnrichmentDtos.kt`
- `backend/src/test/kotlin/com/tonihacks/qalam/delivery/WordEnrichmentIntegrationTest.kt`

**Modify (backend):**

- `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/Word.kt` — add `WordMorphology`, `WordPlural`, `WordRelation`
  domain classes + enums
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordEnums.kt` — add `Gender`, `VerbPattern`, `RelationType`,
  `PluralType`
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordRepository.kt` — add morphology/plural/relation methods
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/exposed/ExposedWordRepository.kt` — add `notes` field
  handling
- `backend/src/main/kotlin/com/tonihacks/qalam/domain/word/WordService.kt` — add enrichment service methods
- `backend/src/main/kotlin/com/tonihacks/qalam/delivery/dto/word/WordDtos.kt` — add `notes` to `Word*Request/Response`
- `backend/src/main/kotlin/com/tonihacks/qalam/delivery/routes/WordRoutes.kt` — mount new sub-routes
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/ai/AiClient.kt` — add `enrichWord()`
- `backend/src/main/kotlin/com/tonihacks/qalam/infrastructure/koin/AppModule.kt` — wire new repos
- `backend/src/main/resources/openapi/documentation.yaml` — add all new endpoints

**Create (frontend):**

- `frontend/src/lib/components/word/WordMorphologyStrip.svelte`
- `frontend/src/lib/components/word/WordPluralChips.svelte`
- `frontend/src/lib/components/word/WordRelationsPanel.svelte`
- `frontend/src/lib/components/word/WordEnrichDrawer.svelte`

**Modify (frontend):**

- `frontend/src/lib/stores/words.ts` — add morphology/plural/relation queries + enrich mutation
- `frontend/src/routes/words/[id]/+page.svelte` — integrate new panels + notes field + Enrich button

---

## Task 1: DB Migrations

**Files:**

- Create: `V018__add_word_notes.sql`
- Create: `V019__create_word_morphology.sql`
- Create: `V020__create_word_plurals.sql`
- Create: `V021__create_word_relations.sql`

- [ ] **Step 1: Add `notes` to `words`**
  ```sql
  ALTER TABLE words ADD COLUMN notes TEXT;
  ```

- [ ] **Step 2: Create `word_morphology`**
  ```sql
  CREATE TABLE word_morphology (
      word_id      UUID PRIMARY KEY REFERENCES words(id) ON DELETE CASCADE,
      gender       VARCHAR(12) CHECK (gender IN ('MASCULINE', 'FEMININE')),
      verb_pattern VARCHAR(5)  CHECK (verb_pattern IN ('I','II','III','IV','V','VI','VII','VIII','IX','X'))
  );
  ```

- [ ] **Step 3: Create `word_plurals`**
  ```sql
  CREATE TABLE word_plurals (
      id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      word_id     UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
      plural_form TEXT NOT NULL,
      plural_type VARCHAR(20) NOT NULL DEFAULT 'BROKEN'
                  CHECK (plural_type IN ('SOUND_MASC','SOUND_FEM','BROKEN','PAUCAL','COLLECTIVE','OTHER'))
  );
  CREATE INDEX idx_word_plurals_word_id ON word_plurals(word_id);
  ```

- [ ] **Step 4: Create `word_relations`**
  ```sql
  CREATE TABLE word_relations (
      word_id         UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
      related_word_id UUID NOT NULL REFERENCES words(id) ON DELETE CASCADE,
      relation_type   VARCHAR(10) NOT NULL CHECK (relation_type IN ('SYNONYM','ANTONYM','RELATED')),
      PRIMARY KEY (word_id, related_word_id, relation_type),
      CONSTRAINT no_self_relation CHECK (word_id <> related_word_id)
  );
  CREATE INDEX idx_word_relations_related ON word_relations(related_word_id);
  ```

---

## Task 2: Domain Model

**Files:**

- Modify: `Word.kt` — add `WordMorphology`, `WordPlural`, `WordRelation`
- Modify: `WordEnums.kt` — add `Gender`, `VerbPattern`, `PluralType`, `RelationType`

- [ ] **Step 1: Add enums to `WordEnums.kt`**
  ```kotlin
  enum class Gender { MASCULINE, FEMININE }
  enum class VerbPattern { I, II, III, IV, V, VI, VII, VIII, IX, X }
  enum class PluralType { SOUND_MASC, SOUND_FEM, BROKEN, PAUCAL, COLLECTIVE, OTHER }
  enum class RelationType { SYNONYM, ANTONYM, RELATED }
  ```
  Add `fromString` companion objects following the existing pattern.

- [ ] **Step 2: Add domain classes to `Word.kt`**
  ```kotlin
  @JvmInline value class WordPluralId(val value: UUID)
  
  data class WordMorphology(
      val wordId: WordId,
      val gender: Gender?,
      val verbPattern: VerbPattern?,
  )
  
  data class WordPlural(
      val id: WordPluralId,
      val wordId: WordId,
      val pluralForm: String,
      val pluralType: PluralType,
  )
  
  data class WordRelation(
      val wordId: WordId,
      val relatedWordId: WordId,
      val relationType: RelationType,
  )
  ```

- [ ] **Step 3: Add `notes: String?` to `Word` data class**

---

## Task 3: Repository Interface

**Files:**

- Modify: `WordRepository.kt`

- [ ] **Step 1: Add morphology methods**
  ```kotlin
  suspend fun findMorphology(wordId: WordId): Either<DomainError, WordMorphology?>
  suspend fun upsertMorphology(morphology: WordMorphology): Either<DomainError, WordMorphology>
  ```

- [ ] **Step 2: Add plural methods**
  ```kotlin
  suspend fun findPlurals(wordId: WordId): Either<DomainError, List<WordPlural>>
  suspend fun addPlural(plural: WordPlural): Either<DomainError, WordPlural>
  suspend fun deletePlural(wordId: WordId, pluralId: WordPluralId): Either<DomainError, Unit>
  ```

- [ ] **Step 3: Add relation methods**
  ```kotlin
  // Returns all relations where word appears on either side
  suspend fun findRelations(wordId: WordId): Either<DomainError, List<WordRelation>>
  suspend fun addRelation(relation: WordRelation): Either<DomainError, WordRelation>
  suspend fun deleteRelation(wordId: WordId, relatedWordId: WordId, type: RelationType): Either<DomainError, Unit>
  ```

---

## Task 4: Exposed Table Objects + Repository Implementations

**Files:**

- Create: `WordMorphologyTable.kt`, `WordPluralsTable.kt`, `WordRelationsTable.kt`
- Create: `ExposedWordMorphologyRepository.kt`, `ExposedWordPluralsRepository.kt`, `ExposedWordRelationsRepository.kt`
- Modify: `ExposedWordRepository.kt` — handle `notes` in select/insert/update

- [ ] **Step 1: `WordMorphologyTable.kt`**
  ```kotlin
  object WordMorphologyTable : Table("word_morphology") {
      val wordId      = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
      val gender      = varchar("gender", 12).nullable()
      val verbPattern = varchar("verb_pattern", 5).nullable()
      override val primaryKey = PrimaryKey(wordId)
  }
  ```

- [ ] **Step 2: `WordPluralsTable.kt`**
  ```kotlin
  object WordPluralsTable : Table("word_plurals") {
      val id         = uuid("id")
      val wordId     = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
      val pluralForm = text("plural_form")
      val pluralType = varchar("plural_type", 20)
      override val primaryKey = PrimaryKey(id)
  }
  ```

- [ ] **Step 3: `WordRelationsTable.kt`**
  ```kotlin
  object WordRelationsTable : Table("word_relations") {
      val wordId        = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
      val relatedWordId = uuid("related_word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
      val relationType  = varchar("relation_type", 10)
      override val primaryKey = PrimaryKey(wordId, relatedWordId, relationType)
  }
  ```

- [ ] **Step 4: Implement `ExposedWordMorphologyRepository`** — upsert via `insertIgnore` + `update`, or Exposed
  `upsert`.

- [ ] **Step 5: Implement `ExposedWordPluralsRepository`** — straightforward insert/select/delete.

- [ ] **Step 6: Implement `ExposedWordRelationsRepository`**
    - `findRelations`: `SELECT WHERE word_id = ? OR related_word_id = ?`, normalise result so `wordId` is always the
      queried word.
    - `addRelation`: insert; handle unique constraint violation as `DomainError.Conflict`.
    - `deleteRelation`: delete both directions (`(a,b,t)` and `(b,a,t)`).

- [ ] **Step 7: Update `ExposedWordRepository`** — add `notes` to `WordsTable` mapping, include in all reads/writes.

---

## Task 5: DTOs

**Files:**

- Create: `WordEnrichmentDtos.kt`
- Modify: `WordDtos.kt`

- [ ] **Step 1: Add `notes` to `WordResponse`, `CreateWordRequest`, `UpdateWordRequest`**

- [ ] **Step 2: Create `WordEnrichmentDtos.kt`**
  ```kotlin
  @Serializable data class WordMorphologyResponse(
      val gender: String?,
      val verbPattern: String?,
      val plurals: List<WordPluralResponse>,
  )
  @Serializable data class WordPluralResponse(val id: String, val pluralForm: String, val pluralType: String)
  @Serializable data class CreateWordPluralRequest(val pluralForm: String, val pluralType: String = "BROKEN")
  @Serializable data class UpsertWordMorphologyRequest(val gender: String?, val verbPattern: String?)
  @Serializable data class WordRelationResponse(
      val relatedWordId: String,
      val relatedWordArabic: String,   // denormalised for display — avoids a second fetch in frontend
      val relatedWordTranslation: String?,
      val relationType: String,
  )
  @Serializable data class CreateWordRelationRequest(val relatedWordId: String, val relationType: String)
  
  // AI enrichment — preview only, never auto-saved
  @Serializable data class WordEnrichmentSuggestion(
      val notes: String?,
      val gender: String?,
      val verbPattern: String?,
      val plurals: List<AiPluralSuggestion>,
      val relations: List<AiRelationSuggestion>,
  )
  @Serializable data class AiPluralSuggestion(val pluralForm: String, val pluralType: String)
  @Serializable data class AiRelationSuggestion(
      val arabicText: String,          // free-text from AI; frontend resolves to word via autocomplete
      val relationType: String,
  )
  ```
  Note: AI relation suggestions carry Arabic text (not UUIDs) — the user must confirm and the frontend resolves each to
  an existing word before saving.

---

## Task 6: Service Layer

**Files:**

- Modify: `WordService.kt`

- [ ] **Step 1: Inject morphology, plural, relation repos** (or split into a new `WordEnrichmentService` if
  `WordService` grows too large — decide at impl time based on line count)

- [ ] **Step 2: Morphology service methods**
  ```kotlin
  suspend fun getMorphology(wordId: String): Either<DomainError, WordMorphologyResponse>
  suspend fun upsertMorphology(wordId: String, req: UpsertWordMorphologyRequest): Either<DomainError, WordMorphologyResponse>
  ```

- [ ] **Step 3: Plural service methods**
  ```kotlin
  suspend fun getPlurals(wordId: String): Either<DomainError, List<WordPluralResponse>>
  suspend fun addPlural(wordId: String, req: CreateWordPluralRequest): Either<DomainError, WordPluralResponse>
  suspend fun deletePlural(wordId: String, pluralId: String): Either<DomainError, Unit>
  ```

- [ ] **Step 4: Relation service methods**
  ```kotlin
  suspend fun getRelations(wordId: String): Either<DomainError, List<WordRelationResponse>>
  suspend fun addRelation(wordId: String, req: CreateWordRelationRequest): Either<DomainError, WordRelationResponse>
  suspend fun deleteRelation(wordId: String, relatedWordId: String, type: String): Either<DomainError, Unit>
  ```
  `getRelations` must fetch the related word's `arabicText` + `translation` to populate `WordRelationResponse` (one
  extra repo lookup per relation, acceptable at this scale).

- [ ] **Step 5: AI enrichment method**
  ```kotlin
  suspend fun enrichWord(wordId: String): Either<DomainError, WordEnrichmentSuggestion>
  ```
  Loads word + root + existing examples, calls `aiClient.enrichWord(context)`, returns suggestion. **No persistence.**

---

## Task 7: Routes

**Files:**

- Modify: `WordRoutes.kt`

Mount all new routes under the existing `route("/words/{id}")` block:

- [ ] **Step 1: Morphology endpoints**
    - `GET  /words/{id}/morphology`
    - `PUT  /words/{id}/morphology`

- [ ] **Step 2: Plural endpoints**
    - `GET    /words/{id}/plurals`
    - `POST   /words/{id}/plurals`
    - `DELETE /words/{id}/plurals/{pluralId}`

- [ ] **Step 3: Relation endpoints**
    - `GET    /words/{id}/relations`
    - `POST   /words/{id}/relations`
    - `DELETE /words/{id}/relations/{relatedWordId}/{type}`

- [ ] **Step 4: AI enrichment endpoint**
    - `POST /words/{id}/enrich` — returns `WordEnrichmentSuggestion`, 503 when AI not configured

---

## Task 8: OpenAPI

**Files:**

- Modify: `backend/src/main/resources/openapi/documentation.yaml`

- [ ] Add `notes` field to `WordResponse`, `CreateWordRequest`, `UpdateWordRequest` schemas
- [ ] Add schemas: `WordMorphologyResponse`, `WordPluralResponse`, `CreateWordPluralRequest`,
  `UpsertWordMorphologyRequest`, `WordRelationResponse`, `CreateWordRelationRequest`, `WordEnrichmentSuggestion`,
  `AiPluralSuggestion`, `AiRelationSuggestion`
- [ ] Add all 9 new endpoints with request/response schemas
- [ ] Run `pnpm generate:types` to regenerate frontend types — verify no type errors

---

## Task 9: AI Client

**Files:**

- Modify: `AiClient.kt`

- [ ] **Step 1: Add `enrichWord(context: WordEnrichContext): Either<DomainError, WordEnrichmentSuggestion>`**
    - `WordEnrichContext`: arabicText, translation, partOfSpeech, dialect, rootLetters, rootMeaning, existingExamples
    - Prompt asks for: gender (if noun), verb pattern (if verb), plural forms with types (if noun), synonyms, antonyms,
      related words (all as Arabic text), brief notes/mnemonic
    - Response parsed from structured JSON (use `json_object` response format via OpenRouter)
    - Degrades gracefully: if AI not configured, return `DomainError.AiNotConfigured`

---

## Task 10: Frontend — Stores

**Files:**

- Modify: `frontend/src/lib/stores/words.ts`

- [ ] **Step 1: Add morphology query**
  ```ts
  export function createMorphologyQuery(wordId: string) {
    return createQuery({ queryKey: ['words', wordId, 'morphology'], queryFn: ... })
  }
  export function createUpsertMorphologyMutation(wordId: string) { ... }
  ```

- [ ] **Step 2: Add plural queries/mutations**
    - `createPluralsQuery(wordId)`
    - `createAddPluralMutation(wordId)` — invalidates `['words', wordId, 'morphology']`
    - `createDeletePluralMutation(wordId)`

- [ ] **Step 3: Add relation queries/mutations**
    - `createRelationsQuery(wordId)`
    - `createAddRelationMutation(wordId)`
    - `createDeleteRelationMutation(wordId)`

- [ ] **Step 4: Add enrich mutation**
    - `createEnrichMutation(wordId)` — returns `WordEnrichmentSuggestion`, no cache invalidation (preview only)

---

## Task 11: Frontend — Components

All components follow existing shadcn-svelte patterns: props in, events out, no direct API calls.

**Files:**

- Create: `WordMorphologyStrip.svelte`
- Create: `WordPluralChips.svelte`
- Create: `WordRelationsPanel.svelte`
- Create: `WordEnrichDrawer.svelte`

- [ ] **Step 1: `WordMorphologyStrip.svelte`**
  Props: `morphology: WordMorphologyResponse | null`
    - Gender badge: `م` (masculine) / `ف` (feminine) — subtle pill, hidden if null
    - Verb pattern badge: `Form III` style — hidden if null
    - Edit trigger: pencil icon opens inline form for gender + verb pattern (PUT /morphology)
    - Empty state: nothing rendered (component returns empty if no morphology data)

- [ ] **Step 2: `WordPluralChips.svelte`**
  Props: `plurals: WordPluralResponse[]`, `editable: boolean`
    - Each plural: chip showing Arabic form + small type label in parens: `كُتُب (broken)`
    - Delete button on chip (editable mode only)
    - Add form: text input (Arabic) + plural type select + submit
    - Right-to-left text direction on Arabic inputs

- [ ] **Step 3: `WordRelationsPanel.svelte`**
  Props: `relations: WordRelationResponse[]`, `editable: boolean`
    - Three sections: Synonyms / Antonyms / Related — each hidden when empty
    - Each chip: Arabic text link → navigates to that word's detail page
    - Delete button on chip (editable mode only)
    - Add form: word autocomplete input (reuse existing autocomplete) + relation type select
    - On add: user picks existing word via autocomplete, selects type, submits

- [ ] **Step 4: `WordEnrichDrawer.svelte`**
  Props: `wordId: string`
    - Triggered by "AI Enrich" button on word detail page
    - Opens slide-in drawer
    - On open: calls `createEnrichMutation` → shows loading state
    - Displays suggestions field-by-field with Accept/Reject toggles:
        - Notes: textarea pre-filled with suggestion, editable
        - Gender: radio (accept/reject) — only shown for nouns
        - Verb pattern: radio — only shown for verbs
        - Plurals: list, each with Accept checkbox
        - Relations: list (Arabic text + type), each with Accept checkbox + word autocomplete to resolve to DB word
    - "Save accepted" button: fires individual mutations for each accepted field
    - Graceful: if 503 AI_NOT_CONFIGURED, hide the Enrich button entirely

---

## Task 12: Word Detail Page Integration

**Files:**

- Modify: `frontend/src/routes/words/[id]/+page.svelte`

- [ ] **Step 1: Add morphology strip** — below word header, above existing content
- [ ] **Step 2: Add plural chips** — within morphology section (nouns), or a dedicated "Plurals" row
- [ ] **Step 3: Add notes block** — italic freetext, editable inline, at bottom of main card; edit saves via existing
  PATCH `/words/{id}` with `notes` field
- [ ] **Step 4: Add relations panel** — new card section, after examples
- [ ] **Step 5: Add AI Enrich button** — in the word actions area (top-right); hidden when AI not configured
- [ ] **Step 6: Wire `WordEnrichDrawer`**

**Layout guidance:**

```
[Word header: arabic + transliteration + translation]
[Morphology strip: gender pill · verb pattern pill]
[Plurals: كُتُب (broken) · أَكْتَاب (paucal)  [+ add]]
────────────────────────────────────────────────────
[Examples section — existing]
────────────────────────────────────────────────────
[Relations]
  Synonyms: [chip] [chip]
  Antonyms: [chip]
  Related:  [chip] [chip] [chip]
────────────────────────────────────────────────────
[Notes: italic freetext]
────────────────────────────────────────────────────
[Dictionary links — existing]
```

---

## Task 13: Integration Tests

**Files:**

- Create: `WordEnrichmentIntegrationTest.kt`

- [ ] Test morphology upsert (create + update)
- [ ] Test plural CRUD
- [ ] Test relation add + delete (verify both directions queryable)
- [ ] Test self-relation rejected (constraint)
- [ ] Test duplicate relation rejected
- [ ] Test `notes` persisted on word create + update
- [ ] Test `/enrich` returns 503 when AI not configured

---

## Implementation Order

1. Task 1 (migrations) — foundation
2. Task 2–3 (domain + repo interface) — compile-time contract
3. Task 4 (Exposed impl) — DB layer complete
4. Task 5 (DTOs) — wire contract to HTTP layer
5. Task 6 (service) — business logic
6. Task 7 (routes) — HTTP surface
7. Task 8 (OpenAPI) → `pnpm generate:types`
8. Task 9 (AI client) — can parallel with 6–7
9. Task 10 (stores) — after types generated
10. Task 11–12 (components + page) — after stores
11. Task 13 (tests) — last, covers full stack
