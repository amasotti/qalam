package com.tonihacks.qalam.domain.annotation

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId

interface AnnotationRepository {
    suspend fun findAllByTextId(textId: TextId): Either<DomainError, List<Annotation>>
    suspend fun findById(id: AnnotationId): Either<DomainError, Annotation>
    suspend fun save(annotation: Annotation): Either<DomainError, Annotation>
    suspend fun update(annotation: Annotation): Either<DomainError, Annotation>
    suspend fun delete(id: AnnotationId): Either<DomainError, Unit>
    suspend fun addWordLink(annotationId: AnnotationId, wordId: WordId): Either<DomainError, Annotation>
    suspend fun removeWordLink(annotationId: AnnotationId, wordId: WordId): Either<DomainError, Annotation>
    suspend fun findAllByWordId(wordId: WordId): Either<DomainError, List<Annotation>>
}
