package com.tonihacks.qalam.domain.wordlist

import arrow.core.Either
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId

interface WordListRepository {
    suspend fun list(page: PageRequest): Either<DomainError, PaginatedResponse<WordListSummary>>
    suspend fun findById(id: WordListId): Either<DomainError, WordList>
    suspend fun create(list: WordList): Either<DomainError, WordList>
    suspend fun update(list: WordList): Either<DomainError, WordList>
    suspend fun delete(id: WordListId): Either<DomainError, Unit>

    /** Member words, ordered by position. Empty for an unknown or empty list — callers pre-check existence. */
    suspend fun membersOf(id: WordListId): Either<DomainError, List<Word>>

    /** Append a word at the next position. Rejects unknown list/word and duplicates. */
    suspend fun addWord(listId: WordListId, wordId: WordId): Either<DomainError, Unit>

    /** Remove a membership. Fails with NotFound if the word is not in the list. */
    suspend fun removeWord(listId: WordListId, wordId: WordId): Either<DomainError, Unit>

    /** Lists a given word belongs to (word-detail membership). */
    suspend fun listsForWord(wordId: WordId): Either<DomainError, List<WordList>>
}
