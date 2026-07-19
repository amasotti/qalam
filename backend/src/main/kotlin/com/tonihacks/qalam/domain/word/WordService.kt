package com.tonihacks.qalam.domain.word

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.word.AiExamplesResponse
import com.tonihacks.qalam.delivery.dto.word.WordAnalysisResponse
import com.tonihacks.qalam.delivery.dto.word.CreateDictionaryLinkRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordExampleRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordPluralRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordRelationRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordRequest
import com.tonihacks.qalam.delivery.dto.word.DictionaryLinkResponse
import com.tonihacks.qalam.delivery.dto.word.UpdateWordRequest
import com.tonihacks.qalam.delivery.dto.word.UpsertVerbDetailsRequest
import com.tonihacks.qalam.delivery.dto.word.UpsertWordMorphologyRequest
import com.tonihacks.qalam.delivery.dto.word.VerbDetailsResponse
import com.tonihacks.qalam.delivery.dto.word.WordAutocompleteResponse
import com.tonihacks.qalam.delivery.dto.word.WordEnrichmentSuggestion
import com.tonihacks.qalam.delivery.dto.word.WordExampleResponse
import com.tonihacks.qalam.delivery.dto.word.WordMorphologyResponse
import com.tonihacks.qalam.delivery.dto.word.WordPluralResponse
import com.tonihacks.qalam.delivery.dto.word.WordRelationResponse
import com.tonihacks.qalam.delivery.dto.word.WordResponse
import com.tonihacks.qalam.delivery.dto.word.toAutocompleteResponse
import com.tonihacks.qalam.delivery.dto.word.toResponse
import com.tonihacks.qalam.infrastructure.exposed.ExposedVerbDetailsRepository
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.RootId
import com.tonihacks.qalam.infrastructure.ai.AiClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.Clock

@Suppress("TooManyFunctions") // Will be split when noun_details extracts morphology (Slice 11)
class WordService(
    private val repo: WordRepository,
    private val aiClient: AiClient,
    private val verbDetailsRepo: ExposedVerbDetailsRepository,
) {
    private val log = KotlinLogging.logger {}

    suspend fun list(
        page: Int?,
        size: Int?,
        q: String?,
        rootId: String?,
        dialect: String?,
        difficulty: String?,
        partOfSpeech: String?,
        masteryLevel: String?,
        sortBy: String?,
        sortDesc: Boolean?,
    ): Either<DomainError, PaginatedResponse<WordResponse>> = either {
        log.debug { "Listing words page=$page size=$size hasQuery=${q != null} rootId=$rootId" }
        val parsedSortBy = when (sortBy?.uppercase()) {
            "CREATED_AT" -> WordSortField.CREATED_AT
            "ARABIC_TEXT" -> WordSortField.ARABIC_TEXT
            "TRANSLATION" -> WordSortField.TRANSLATION
            "DIFFICULTY" -> WordSortField.DIFFICULTY
            "MASTERY_LEVEL" -> WordSortField.MASTERY_LEVEL
            else -> WordSortField.UPDATED_AT
        }
        val filters = WordFilters(
            q = q,
            rootId = rootId?.let { parseWordUuid(it, "rootId").bind()?.let { u -> RootId(u) } },
            dialect = dialect?.let { parseWordEnum("dialect", it) { s -> Dialect.fromString(s) }.bind() },
            difficulty = difficulty?.let { parseWordEnum("difficulty", it) { s -> Difficulty.fromString(s) }.bind() },
            partOfSpeech = partOfSpeech?.let { parseWordEnum("partOfSpeech", it) { s -> PartOfSpeech.fromString(s) }.bind() },
            masteryLevel = masteryLevel?.let { parseWordEnum("masteryLevel", it) { s -> MasteryLevel.fromString(s) }.bind() },
            sortBy = parsedSortBy,
            sortDesc = sortDesc ?: true,
        )
        val paged = repo.list(PageRequest.from(page, size), filters).bind()
        PaginatedResponse(
            items = paged.items.map { it.toResponse() },
            total = paged.total,
            page = paged.page,
            size = paged.size,
        )
    }.logDomainFailure(log) { "Failed to list words page=$page size=$size rootId=$rootId: $it" }

    suspend fun getById(id: String): Either<DomainError, WordResponse> =
        parseWordId(id).flatMap { repo.findById(it) }.map { it.toResponse() }
            .logDomainFailure(log) { "Failed to get word id=$id: $it" }

    suspend fun findByArabicText(arabicText: String): Either<DomainError, WordResponse?> =
        repo.findByArabicText(arabicText).map { it?.toResponse() }
            .logDomainFailure(log) { "Failed to find word by arabicText length=${arabicText.length}: $it" }

    suspend fun create(req: CreateWordRequest): Either<DomainError, WordResponse> = either {
        log.info { "Creating word arabicLength=${req.arabicText.length} partOfSpeech=${req.partOfSpeech}" }
        if (req.arabicText.isBlank()) raise(DomainError.ValidationError("arabicText", "Arabic text must not be blank"))

        val pos = parseWordEnum("partOfSpeech", req.partOfSpeech) { PartOfSpeech.fromString(it) }.bind()
        val dialect = parseWordEnum("dialect", req.dialect) { Dialect.fromString(it) }.bind()
        val difficulty = parseWordEnum("difficulty", req.difficulty) { Difficulty.fromString(it) }.bind()
        val rootId = req.rootId?.let { parseWordUuid(it, "rootId").bind()?.let { u -> RootId(u) } }
        val derivedFromId = req.derivedFromId?.let { parseWordUuid(it, "derivedFromId").bind()?.let { u -> WordId(u) } }

        val now = Clock.System.now()
        val word = Word(
            id = WordId(UUID.randomUUID()),
            arabicText = req.arabicText.trim(),
            transliteration = req.transliteration,
            translation = req.translation,
            partOfSpeech = pos,
            dialect = dialect,
            difficulty = difficulty,
            masteryLevel = MasteryLevel.NEW,
            pronunciationUrl = req.pronunciationUrl,
            rootId = rootId,
            derivedFromId = derivedFromId,
            notes = req.notes?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
        )
        repo.create(word).bind().toResponse()
    }.logDomainFailure(log) { "Failed to create word arabicLength=${req.arabicText.length}: $it" }

    @Suppress("CyclomaticComplexMethod") // Inherent complexity of partial-update for 8+ typed fields.
    suspend fun update(id: String, req: UpdateWordRequest): Either<DomainError, WordResponse> = either {
        log.info { "Updating word id=$id" }
        val wordId = parseWordId(id).bind()
        val existing = repo.findById(wordId).bind()
        if (req.arabicText?.isBlank() == true) {
            raise(DomainError.ValidationError("arabicText", "Arabic text must not be blank"))
        }

        val pos = req.partOfSpeech?.let { parseWordEnum("partOfSpeech", it) { s -> PartOfSpeech.fromString(s) }.bind() }
            ?: existing.partOfSpeech
        val dialect = req.dialect?.let { parseWordEnum("dialect", it) { s -> Dialect.fromString(s) }.bind() }
            ?: existing.dialect
        val difficulty = req.difficulty?.let { parseWordEnum("difficulty", it) { s -> Difficulty.fromString(s) }.bind() }
            ?: existing.difficulty
        val masteryLevel = req.masteryLevel?.let { parseWordEnum("masteryLevel", it) { s -> MasteryLevel.fromString(s) }.bind() }
            ?: existing.masteryLevel
        val rootId = if (req.rootId == null) existing.rootId
            else parseWordUuid(req.rootId, "rootId").bind()?.let { RootId(it) }
        val derivedFromId = if (req.derivedFromId == null) existing.derivedFromId
            else parseWordUuid(req.derivedFromId, "derivedFromId").bind()?.let { WordId(it) }

        repo.update(existing.copy(
            arabicText = req.arabicText?.trim() ?: existing.arabicText,
            transliteration = req.transliteration ?: existing.transliteration,
            translation = req.translation ?: existing.translation,
            partOfSpeech = pos,
            dialect = dialect,
            difficulty = difficulty,
            masteryLevel = masteryLevel,
            pronunciationUrl = req.pronunciationUrl ?: existing.pronunciationUrl,
            rootId = rootId,
            derivedFromId = derivedFromId,
            notes = req.notes?.trim()?.takeIf { it.isNotEmpty() } ?: existing.notes,
        )).bind().toResponse()
    }.logDomainFailure(log) { "Failed to update word id=$id: $it" }

    suspend fun delete(id: String): Either<DomainError, Unit> =
        parseWordId(id).flatMap { repo.delete(it) }
            .logDomainFailure(log) { "Failed to delete word id=$id: $it" }

    suspend fun autocomplete(query: String, limit: Int?): Either<DomainError, List<WordAutocompleteResponse>> =
        repo.autocomplete(query, limit?.coerceIn(1, 50) ?: 10)
            .map { words -> words.map { it.toAutocompleteResponse() } }
            .logDomainFailure(log) { "Failed to autocomplete words queryLength=${query.length} limit=$limit: $it" }

    suspend fun getDictionaryLinks(wordId: String): Either<DomainError, List<DictionaryLinkResponse>> =
        parseWordId(wordId)
            .flatMap { id -> repo.findById(id).flatMap { repo.findDictionaryLinks(id) } }
            .map { links -> links.map { it.toResponse() } }
            .logDomainFailure(log) { "Failed to get dictionary links wordId=$wordId: $it" }

    suspend fun addDictionaryLink(
        wordId: String,
        req: CreateDictionaryLinkRequest,
    ): Either<DomainError, DictionaryLinkResponse> = either {
        log.info { "Adding dictionary link wordId=$wordId source=${req.source}" }
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val source = parseWordEnum("source", req.source) { DictionarySource.fromString(it) }.bind()
        repo.addDictionaryLink(
            DictionaryLink(id = DictionaryLinkId(UUID.randomUUID()), wordId = id, source = source, url = req.url)
        ).bind().toResponse()
    }.logDomainFailure(log) { "Failed to add dictionary link wordId=$wordId source=${req.source}: $it" }

    suspend fun deleteDictionaryLink(wordId: String, linkId: String): Either<DomainError, Unit> = either {
        log.info { "Deleting dictionary link wordId=$wordId linkId=$linkId" }
        val wId = parseWordId(wordId).bind()
        repo.findById(wId).bind()
        val lId = try {
            DictionaryLinkId(UUID.fromString(linkId))
        } catch (_: IllegalArgumentException) {
            raise(DomainError.InvalidInput("'$linkId' is not a valid UUID"))
        }
        repo.deleteDictionaryLink(wId, lId).bind()
    }.logDomainFailure(log) { "Failed to delete dictionary link wordId=$wordId linkId=$linkId: $it" }

    suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> =
        aiClient.analyzeWord(arabicText)
            .logDomainFailure(log) { "Failed to analyze word arabicLength=${arabicText.length}: $it" }

    suspend fun generateExamples(wordId: String): Either<DomainError, AiExamplesResponse> = either {
        log.info { "Generating AI examples wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        val word = repo.findById(id).bind()
        val examples = aiClient.generateExamples(word.arabicText, word.translation).bind()
        AiExamplesResponse(examples)
    }.logDomainFailure(log) { "Failed to generate AI examples wordId=$wordId: $it" }

    suspend fun getExamples(wordId: String): Either<DomainError, List<WordExampleResponse>> = either {
        log.debug { "Loading examples wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        repo.findExamples(id).bind().map { it.toResponse() }
    }.logDomainFailure(log) { "Failed to load examples wordId=$wordId: $it" }

    suspend fun saveExample(wordId: String, req: CreateWordExampleRequest): Either<DomainError, WordExampleResponse> = either {
        log.info { "Saving word example wordId=$wordId arabicLength=${req.arabic.length}" }
        if (req.arabic.isBlank()) raise(DomainError.ValidationError("arabic", "Arabic text must not be blank"))
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val example = WordExample(
            id = WordExampleId(UUID.randomUUID()),
            wordId = id,
            arabic = req.arabic.trim(),
            transliteration = req.transliteration?.trim()?.takeIf { it.isNotEmpty() },
            translation = req.translation?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = Clock.System.now(),
        )
        repo.addExample(example).bind().toResponse()
    }.logDomainFailure(log) { "Failed to save word example wordId=$wordId: $it" }

    suspend fun deleteExample(wordId: String, exampleId: String): Either<DomainError, Unit> = either {
        log.info { "Deleting word example wordId=$wordId exampleId=$exampleId" }
        val wId = parseWordId(wordId).bind()
        repo.findById(wId).bind()
        val eId = try {
            WordExampleId(UUID.fromString(exampleId))
        } catch (_: IllegalArgumentException) {
            raise(DomainError.InvalidInput("'$exampleId' is not a valid UUID"))
        }
        repo.deleteExample(wId, eId).bind()
    }.logDomainFailure(log) { "Failed to delete word example wordId=$wordId exampleId=$exampleId: $it" }

    // --- Morphology ---

    suspend fun getMorphology(wordId: String): Either<DomainError, WordMorphologyResponse> = either {
        log.debug { "Loading word morphology wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val morphology = repo.findMorphology(id).bind()
        val plurals = repo.findPlurals(id).bind()
        morphology?.toResponse(plurals) ?: WordMorphologyResponse(gender = null, plurals = plurals.map { it.toResponse() })
    }.logDomainFailure(log) { "Failed to load morphology wordId=$wordId: $it" }

    suspend fun upsertMorphology(wordId: String, req: UpsertWordMorphologyRequest): Either<DomainError, WordMorphologyResponse> = either {
        log.info { "Upserting word morphology wordId=$wordId gender=${req.gender}" }
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val gender = req.gender?.let { parseWordEnum("gender", it) { s -> Gender.fromString(s) }.bind() }
        val morphology = WordMorphology(wordId = id, gender = gender)
        repo.upsertMorphology(morphology).bind()
        val plurals = repo.findPlurals(id).bind()
        morphology.toResponse(plurals)
    }.logDomainFailure(log) { "Failed to upsert morphology wordId=$wordId: $it" }

    // --- Plurals ---

    suspend fun getPlurals(wordId: String): Either<DomainError, List<WordPluralResponse>> = either {
        log.debug { "Loading word plurals wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        repo.findPlurals(id).bind().map { it.toResponse() }
    }.logDomainFailure(log) { "Failed to load plurals wordId=$wordId: $it" }

    suspend fun addPlural(wordId: String, req: CreateWordPluralRequest): Either<DomainError, WordPluralResponse> = either {
        log.info { "Adding word plural wordId=$wordId pluralType=${req.pluralType}" }
        if (req.pluralForm.isBlank()) raise(DomainError.ValidationError("pluralForm", "must not be blank"))
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val pluralType = parseWordEnum("pluralType", req.pluralType) { PluralType.fromString(it) }.bind()
        val plural = WordPlural(
            id = WordPluralId(UUID.randomUUID()),
            wordId = id,
            pluralForm = req.pluralForm.trim(),
            pluralType = pluralType,
        )
        repo.addPlural(plural).bind().toResponse()
    }.logDomainFailure(log) { "Failed to add plural wordId=$wordId: $it" }

    suspend fun deletePlural(wordId: String, pluralId: String): Either<DomainError, Unit> = either {
        log.info { "Deleting word plural wordId=$wordId pluralId=$pluralId" }
        val wId = parseWordId(wordId).bind()
        repo.findById(wId).bind()
        val pId = try {
            WordPluralId(UUID.fromString(pluralId))
        } catch (_: IllegalArgumentException) {
            raise(DomainError.InvalidInput("'$pluralId' is not a valid UUID"))
        }
        repo.deletePlural(wId, pId).bind()
    }.logDomainFailure(log) { "Failed to delete plural wordId=$wordId pluralId=$pluralId: $it" }

    // --- Relations ---

    suspend fun getRelations(wordId: String): Either<DomainError, List<WordRelationResponse>> = either {
        log.debug { "Loading word relations wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val relations = repo.findRelations(id).bind()
        relations.map { relation ->
            val relatedWord = repo.findById(relation.relatedWordId).bind()
            relation.toResponse(relatedWord)
        }
    }.logDomainFailure(log) { "Failed to load relations wordId=$wordId: $it" }

    suspend fun addRelation(wordId: String, req: CreateWordRelationRequest): Either<DomainError, WordRelationResponse> = either {
        log.info { "Adding word relation wordId=$wordId relatedWordId=${req.relatedWordId} type=${req.relationType}" }

        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()

        val relatedId = parseWordId(req.relatedWordId).bind()

        if (id == relatedId) raise(DomainError.ValidationError("relatedWordId", "cannot relate a word to itself"))

        val relatedWord = repo.findById(relatedId).bind()
        val type = parseWordEnum("relationType", req.relationType) { RelationType.fromString(it) }.bind()
        // findRelations queries both directions and normalises all results to wordId=id,
        // so this single check catches both (id→relatedId) and (relatedId→id) duplicates.
        val existing = repo.findRelations(id).bind()
        if (existing.any { it.relatedWordId == relatedId && it.relationType == type }) {
            raise(DomainError.Conflict("WordRelation", "${id}-${relatedId}"))
        }

        val relation = WordRelation(wordId = id, relatedWordId = relatedId, relationType = type)
        repo.addRelation(relation).bind()
        relation.toResponse(relatedWord)
    }.logDomainFailure(log) {
        "Failed to add relation wordId=$wordId relatedWordId=${req.relatedWordId} type=${req.relationType}: $it"
    }

    suspend fun deleteRelation(wordId: String, relatedWordId: String, type: String): Either<DomainError, Unit> = either {
        log.info { "Deleting word relation wordId=$wordId relatedWordId=$relatedWordId type=$type" }
        val wId = parseWordId(wordId).bind()
        repo.findById(wId).bind()
        val rId = parseWordId(relatedWordId).bind()
        val relationType = parseWordEnum("relationType", type) { RelationType.fromString(it) }.bind()
        repo.deleteRelation(wId, rId, relationType).bind()
    }.logDomainFailure(log) { "Failed to delete relation wordId=$wordId relatedWordId=$relatedWordId type=$type: $it" }

    // --- Verb Details ---

    suspend fun getVerbDetails(wordId: String): Either<DomainError, VerbDetailsResponse?> = either {
        log.debug { "Loading verb details wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        requireVerb(id).bind()
        verbDetailsRepo.find(id).bind()?.toResponse()
    }.logDomainFailure(log) { "Failed to load verb details wordId=$wordId: $it" }

    suspend fun upsertVerbDetails(wordId: String, req: UpsertVerbDetailsRequest): Either<DomainError, VerbDetailsResponse> = either {
        log.info { "Upserting verb details wordId=$wordId verbForm=${req.verbForm}" }
        val id = parseWordId(wordId).bind()
        requireVerb(id).bind()
        val verbForm = parseWordEnum("verbForm", req.verbForm) { s -> VerbPattern.fromString(s) }.bind()
        val weaknessType = parseWordEnum("weaknessType", req.weaknessType) { s -> WeaknessType.fromString(s) }.bind()
        val now = Clock.System.now()
        val existing = verbDetailsRepo.find(id).bind()
        val details = VerbDetails(
            wordId = id,
            verbForm = verbForm,
            pastPattern = req.pastPattern?.trim()?.takeIf { it.isNotEmpty() },
            presentPattern = req.presentPattern?.trim()?.takeIf { it.isNotEmpty() },
            weaknessType = weaknessType,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        verbDetailsRepo.upsert(details).bind().toResponse()
    }.logDomainFailure(log) { "Failed to upsert verb details wordId=$wordId: $it" }

    suspend fun deleteVerbDetails(wordId: String): Either<DomainError, Unit> = either {
        log.info { "Deleting verb details wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        requireVerb(id).bind()
        verbDetailsRepo.delete(id).bind()
    }.logDomainFailure(log) { "Failed to delete verb details wordId=$wordId: $it" }

    private suspend fun requireVerb(id: WordId): Either<DomainError, Word> {
        val word = repo.findById(id).fold({ return it.left() }, { it })
        if (word.partOfSpeech != PartOfSpeech.VERB) {
            return DomainError.ValidationError(
                "partOfSpeech",
                "Verb details require partOfSpeech=VERB, got ${word.partOfSpeech}"
            ).left()
        }
        return word.right()
    }

    // --- AI Enrichment (preview only — never auto-saved) ---

    suspend fun enrichWord(wordId: String): Either<DomainError, WordEnrichmentSuggestion> = either {
        log.info { "Generating AI word enrichment wordId=$wordId" }
        val id = parseWordId(wordId).bind()
        val word = repo.findById(id).bind()
        aiClient.enrichWord(word).bind()
    }.logDomainFailure(log) { "Failed to generate AI word enrichment wordId=$wordId: $it" }
}

// --- top-level helpers (excluded from TooManyFunctions count) ---

private fun parseWordId(id: String): Either<DomainError, WordId> =
    try { WordId(UUID.fromString(id)).right() }
    catch (_: IllegalArgumentException) { DomainError.InvalidInput("'$id' is not a valid UUID").left() }

private fun parseWordUuid(value: String, field: String): Either<DomainError, UUID?> =
    try { UUID.fromString(value).right() }
    catch (_: IllegalArgumentException) { DomainError.InvalidInput("'$value' is not a valid UUID for $field").left() }

private fun <T : Enum<T>> parseWordEnum(
    field: String,
    value: String,
    parser: (String) -> T?,
): Either<DomainError, T> =
    parser(value)?.right() ?: DomainError.ValidationError(field, "Unknown value: $value").left()
