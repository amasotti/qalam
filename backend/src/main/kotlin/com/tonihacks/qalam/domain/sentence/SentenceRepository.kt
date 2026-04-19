package com.tonihacks.qalam.domain.sentence

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId

interface SentenceRepository {
    suspend fun findAllByTextId(textId: TextId): Either<DomainError, List<Sentence>>
    suspend fun findById(id: SentenceId): Either<DomainError, Sentence>
    suspend fun save(sentence: Sentence): Either<DomainError, Sentence>
    suspend fun update(sentence: Sentence): Either<DomainError, Sentence>
    suspend fun delete(id: SentenceId): Either<DomainError, Unit>
    /** Replace all tokens for a sentence (delete existing, insert new). */
    suspend fun replaceTokens(sentenceId: SentenceId, tokens: List<AlignmentToken>): Either<DomainError, Sentence>
    /** Returns the current max position for sentences in a text (0 if none). */
    suspend fun maxPosition(textId: TextId): Either<DomainError, Int>
    /** Reorder sentences: set positions 1..N in the given id order. Two-phase to avoid UNIQUE conflict. */
    suspend fun reorder(textId: TextId, orderedIds: List<SentenceId>): Either<DomainError, List<Sentence>>
}
