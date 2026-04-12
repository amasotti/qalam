package com.tonihacks.qalam.domain.annotation

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.MasteryLevel
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
        masteryLevel: MasteryLevel?,
        reviewFlag: Boolean,
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
            masteryLevel = masteryLevel,
            reviewFlag = reviewFlag,
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
        masteryLevel: MasteryLevel? = null,
        reviewFlag: Boolean? = null,
    ): Either<DomainError, Annotation> = either {
        val existing = repo.findById(id).bind()

        if (anchor != null && anchor.isBlank()) raise(DomainError.ValidationError("anchor", "anchor must not be blank"))

        val updated = existing.copy(
            anchor = anchor ?: existing.anchor,
            type = type ?: existing.type,
            content = content ?: existing.content,
            masteryLevel = masteryLevel ?: existing.masteryLevel,
            reviewFlag = reviewFlag ?: existing.reviewFlag,
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
