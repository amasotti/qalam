package com.tonihacks.qalam.domain.sentence

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class TokenInput(
    val position: Int,
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
    val wordId: WordId?,
)

@OptIn(ExperimentalTime::class)
class SentenceService(private val repo: SentenceRepository) {
    private val log = KotlinLogging.logger {}

    suspend fun listByText(textId: TextId): Either<DomainError, List<Sentence>> =
        repo.findAllByTextId(textId)
            .logDomainFailure(log) { "Failed to list sentences for textId=$textId: $it" }

    suspend fun getById(id: SentenceId): Either<DomainError, Sentence> =
        repo.findById(id)
            .logDomainFailure(log) { "Failed to get sentence id=$id: $it" }

    suspend fun create(
        textId: TextId,
        arabicText: String,
        position: Int? = null,
        transliteration: String? = null,
        freeTranslation: String? = null,
        notes: String? = null,
    ): Either<DomainError, Sentence> = either {
        log.info { "Creating sentence textId=$textId requestedPosition=$position" }
        if (arabicText.isBlank()) raise(DomainError.ValidationError("arabicText", "arabicText must not be blank"))

        val resolvedPosition = if (position != null) {
            position
        } else {
            repo.maxPosition(textId).bind() + 1
        }

        val now = Clock.System.now()
        val sentence = Sentence(
            id = SentenceId(UUID.randomUUID()),
            textId = textId,
            position = resolvedPosition,
            arabicText = arabicText,
            transliteration = transliteration,
            freeTranslation = freeTranslation,
            notes = notes,
            tokensValid = true,
            tokens = emptyList(),
            createdAt = now,
            updatedAt = now,
        )
        repo.save(sentence).bind()
    }.logDomainFailure(log) { "Failed to create sentence textId=$textId: $it" }

    suspend fun update(
        id: SentenceId,
        arabicText: String? = null,
        position: Int? = null,
        transliteration: String? = null,
        freeTranslation: String? = null,
        notes: String? = null,
    ): Either<DomainError, Sentence> = either {
        log.info { "Updating sentence id=$id requestedPosition=$position" }
        val existing = repo.findById(id).bind()

        val arabicChanged = arabicText != null && arabicText != existing.arabicText

        val updated = existing.copy(
            arabicText = arabicText ?: existing.arabicText,
            position = position ?: existing.position,
            transliteration = clearable(transliteration, existing.transliteration),
            freeTranslation = clearable(freeTranslation, existing.freeTranslation),
            notes = clearable(notes, existing.notes),
            tokensValid = if (arabicChanged) false else existing.tokensValid,
            updatedAt = Clock.System.now(),
        )
        repo.update(updated).bind()
    }.logDomainFailure(log) { "Failed to update sentence id=$id: $it" }

    suspend fun delete(id: SentenceId): Either<DomainError, Unit> =
        repo.delete(id)
            .logDomainFailure(log) { "Failed to delete sentence id=$id: $it" }

    suspend fun replaceTokens(
        id: SentenceId,
        tokenInputs: List<TokenInput>,
    ): Either<DomainError, Sentence> = either {
        log.info { "Replacing sentence tokens id=$id tokenCount=${tokenInputs.size}" }
        val blankToken = tokenInputs.firstOrNull { it.arabic.isBlank() }
        if (blankToken != null) {
            raise(DomainError.ValidationError("arabic", "Token at position ${blankToken.position} must not have blank arabic"))
        }

        val existing = repo.findById(id).bind()

        val tokens = tokenInputs.map { input ->
            AlignmentToken(
                id = AlignmentTokenId(UUID.randomUUID()),
                sentenceId = existing.id,
                position = input.position,
                arabic = input.arabic,
                transliteration = input.transliteration,
                translation = input.translation,
                wordId = input.wordId,
            )
        }

        repo.replaceTokens(id, tokens).bind()
    }.logDomainFailure(log) { "Failed to replace tokens for sentence id=$id: $it" }

    suspend fun clearTokens(id: SentenceId): Either<DomainError, Sentence> =
        repo.replaceTokens(id, emptyList())
            .logDomainFailure(log) { "Failed to clear tokens for sentence id=$id: $it" }

    suspend fun reorder(textId: TextId, orderedIds: List<SentenceId>): Either<DomainError, List<Sentence>> =
        repo.reorder(textId, orderedIds)
            .logDomainFailure(log) { "Failed to reorder sentences for textId=$textId count=${orderedIds.size}: $it" }
}

/** null = keep existing, blank = clear to null, non-blank = use new value */
private fun clearable(incoming: String?, existing: String?): String? = when {
    incoming == null -> existing
    incoming.isBlank() -> null
    else -> incoming
}
