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
import com.tonihacks.qalam.delivery.dto.word.CreateWordRequest
import com.tonihacks.qalam.delivery.dto.word.DictionaryLinkResponse
import com.tonihacks.qalam.delivery.dto.word.UpdateWordRequest
import com.tonihacks.qalam.delivery.dto.word.WordAutocompleteResponse
import com.tonihacks.qalam.delivery.dto.word.WordExampleResponse
import com.tonihacks.qalam.delivery.dto.word.WordResponse
import com.tonihacks.qalam.delivery.dto.word.toAutocompleteResponse
import com.tonihacks.qalam.delivery.dto.word.toResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.RootId
import com.tonihacks.qalam.infrastructure.ai.AiClient
import java.util.UUID
import kotlin.time.Clock

class WordService(
    private val repo: WordRepository,
    private val aiClient: AiClient,
) {

    suspend fun list(
        page: Int?,
        size: Int?,
        q: String?,
        rootId: String?,
        dialect: String?,
        difficulty: String?,
        partOfSpeech: String?,
        masteryLevel: String?,
    ): Either<DomainError, PaginatedResponse<WordResponse>> = either {
        val filters = WordFilters(
            q = q,
            rootId = rootId?.let { parseWordUuid(it, "rootId").bind()?.let { u -> RootId(u) } },
            dialect = dialect?.let { parseWordEnum("dialect", it) { s -> Dialect.fromString(s) }.bind() },
            difficulty = difficulty?.let { parseWordEnum("difficulty", it) { s -> Difficulty.fromString(s) }.bind() },
            partOfSpeech = partOfSpeech?.let { parseWordEnum("partOfSpeech", it) { s -> PartOfSpeech.fromString(s) }.bind() },
            masteryLevel = masteryLevel?.let { parseWordEnum("masteryLevel", it) { s -> MasteryLevel.fromString(s) }.bind() },
        )
        val paged = repo.list(PageRequest.from(page, size), filters).bind()
        PaginatedResponse(
            items = paged.items.map { it.toResponse() },
            total = paged.total,
            page = paged.page,
            size = paged.size,
        )
    }

    suspend fun getById(id: String): Either<DomainError, WordResponse> =
        parseWordId(id).flatMap { repo.findById(it) }.map { it.toResponse() }

    suspend fun findByArabicText(arabicText: String): Either<DomainError, WordResponse?> =
        repo.findByArabicText(arabicText).map { it?.toResponse() }

    suspend fun create(req: CreateWordRequest): Either<DomainError, WordResponse> = either {
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
            createdAt = now,
            updatedAt = now,
        )
        repo.create(word).bind().toResponse()
    }

    @Suppress("CyclomaticComplexMethod") // Inherent complexity of partial-update for 8+ typed fields.
    suspend fun update(id: String, req: UpdateWordRequest): Either<DomainError, WordResponse> = either {
        val wordId = parseWordId(id).bind()
        val existing = repo.findById(wordId).bind()

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
        )).bind().toResponse()
    }

    suspend fun delete(id: String): Either<DomainError, Unit> =
        parseWordId(id).flatMap { repo.delete(it) }

    suspend fun autocomplete(query: String, limit: Int?): Either<DomainError, List<WordAutocompleteResponse>> =
        repo.autocomplete(query, limit?.coerceIn(1, 50) ?: 10)
            .map { words -> words.map { it.toAutocompleteResponse() } }

    suspend fun getDictionaryLinks(wordId: String): Either<DomainError, List<DictionaryLinkResponse>> =
        parseWordId(wordId)
            .flatMap { id -> repo.findById(id).flatMap { repo.findDictionaryLinks(id) } }
            .map { links -> links.map { it.toResponse() } }

    suspend fun addDictionaryLink(
        wordId: String,
        req: CreateDictionaryLinkRequest,
    ): Either<DomainError, DictionaryLinkResponse> = either {
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        val source = parseWordEnum("source", req.source) { DictionarySource.fromString(it) }.bind()
        repo.addDictionaryLink(
            DictionaryLink(id = DictionaryLinkId(UUID.randomUUID()), wordId = id, source = source, url = req.url)
        ).bind().toResponse()
    }

    suspend fun deleteDictionaryLink(wordId: String, linkId: String): Either<DomainError, Unit> = either {
        val wId = parseWordId(wordId).bind()
        repo.findById(wId).bind()
        val lId = try {
            DictionaryLinkId(UUID.fromString(linkId))
        } catch (_: IllegalArgumentException) {
            raise(DomainError.InvalidInput("'$linkId' is not a valid UUID"))
        }
        repo.deleteDictionaryLink(wId, lId).bind()
    }

    suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> =
        aiClient.analyzeWord(arabicText)

    suspend fun generateExamples(wordId: String): Either<DomainError, AiExamplesResponse> = either {
        val id = parseWordId(wordId).bind()
        val word = repo.findById(id).bind()
        val examples = aiClient.generateExamples(word.arabicText, word.translation).bind()
        AiExamplesResponse(examples)
    }

    suspend fun getExamples(wordId: String): Either<DomainError, List<WordExampleResponse>> = either {
        val id = parseWordId(wordId).bind()
        repo.findById(id).bind()
        repo.findExamples(id).bind().map { it.toResponse() }
    }

    suspend fun saveExample(wordId: String, req: CreateWordExampleRequest): Either<DomainError, WordExampleResponse> = either {
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
    }

    suspend fun deleteExample(wordId: String, exampleId: String): Either<DomainError, Unit> = either {
        val wId = parseWordId(wordId).bind()
        repo.findById(wId).bind()
        val eId = try {
            WordExampleId(UUID.fromString(exampleId))
        } catch (_: IllegalArgumentException) {
            raise(DomainError.InvalidInput("'$exampleId' is not a valid UUID"))
        }
        repo.deleteExample(wId, eId).bind()
    }
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
