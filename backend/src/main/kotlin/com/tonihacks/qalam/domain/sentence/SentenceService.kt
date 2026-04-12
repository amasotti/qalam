package com.tonihacks.qalam.domain.sentence

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
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

    suspend fun listByText(textId: TextId): Either<DomainError, List<Sentence>> =
        repo.findAllByTextId(textId)

    suspend fun getById(id: SentenceId): Either<DomainError, Sentence> =
        repo.findById(id)

    suspend fun create(
        textId: TextId,
        arabicText: String,
        position: Int? = null,
        transliteration: String? = null,
        freeTranslation: String? = null,
        notes: String? = null,
    ): Either<DomainError, Sentence> = either {
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
    }

    suspend fun update(
        id: SentenceId,
        arabicText: String? = null,
        position: Int? = null,
        transliteration: String? = null,
        freeTranslation: String? = null,
        notes: String? = null,
    ): Either<DomainError, Sentence> = either {
        val existing = repo.findById(id).bind()

        val arabicChanged = arabicText != null && arabicText != existing.arabicText

        val updated = existing.copy(
            arabicText = arabicText ?: existing.arabicText,
            position = position ?: existing.position,
            transliteration = transliteration ?: existing.transliteration,
            freeTranslation = freeTranslation ?: existing.freeTranslation,
            notes = notes ?: existing.notes,
            tokensValid = if (arabicChanged) false else existing.tokensValid,
            updatedAt = Clock.System.now(),
        )
        repo.update(updated).bind()
    }

    suspend fun delete(id: SentenceId): Either<DomainError, Unit> =
        repo.delete(id)

    suspend fun replaceTokens(
        id: SentenceId,
        tokenInputs: List<TokenInput>,
    ): Either<DomainError, Sentence> = either {
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
    }

    suspend fun clearTokens(id: SentenceId): Either<DomainError, Sentence> =
        repo.replaceTokens(id, emptyList())
}
