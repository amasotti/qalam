package com.tonihacks.qalam.domain.annotation

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

@OptIn(ExperimentalTime::class)
class AnnotationService(private val repo: AnnotationRepository) {
    private val log = KotlinLogging.logger {}

    suspend fun listByText(textId: TextId): Either<DomainError, List<Annotation>> =
        repo.findAllByTextId(textId)
            .logDomainFailure(log) { "Failed to list annotations for textId=$textId: $it" }

    suspend fun getById(id: AnnotationId): Either<DomainError, Annotation> =
        repo.findById(id)
            .logDomainFailure(log) { "Failed to get annotation id=$id: $it" }

    suspend fun create(
        textId: TextId,
        anchor: String,
        type: AnnotationType,
        content: String?,
        linkedWordIds: List<WordId>,
    ): Either<DomainError, Annotation> = either {
        log.info { "Creating annotation textId=$textId type=$type linkedWords=${linkedWordIds.size}" }
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
    }.logDomainFailure(log) { "Failed to create annotation textId=$textId type=$type: $it" }

    suspend fun update(
        id: AnnotationId,
        anchor: String? = null,
        type: AnnotationType? = null,
        content: String? = null,
    ): Either<DomainError, Annotation> = either {
        log.info { "Updating annotation id=$id" }
        val existing = repo.findById(id).bind()

        if (anchor != null && anchor.isBlank()) raise(DomainError.ValidationError("anchor", "anchor must not be blank"))

        val updated = existing.copy(
            anchor = anchor ?: existing.anchor,
            type = type ?: existing.type,
            content = content ?: existing.content,
            updatedAt = Clock.System.now(),
        )
        repo.update(updated).bind()
    }.logDomainFailure(log) { "Failed to update annotation id=$id: $it" }

    suspend fun delete(id: AnnotationId): Either<DomainError, Unit> =
        repo.delete(id)
            .logDomainFailure(log) { "Failed to delete annotation id=$id: $it" }

    suspend fun addWordLink(id: AnnotationId, wordId: WordId): Either<DomainError, Annotation> =
        repo.addWordLink(id, wordId)
            .logDomainFailure(log) { "Failed to link annotation id=$id to wordId=$wordId: $it" }

    suspend fun removeWordLink(id: AnnotationId, wordId: WordId): Either<DomainError, Annotation> =
        repo.removeWordLink(id, wordId)
            .logDomainFailure(log) { "Failed to unlink annotation id=$id from wordId=$wordId: $it" }

    suspend fun getAnnotationsForWord(wordId: WordId): Either<DomainError, List<Annotation>> =
        repo.findAllByWordId(wordId)
            .logDomainFailure(log) { "Failed to list annotations for wordId=$wordId: $it" }
}
