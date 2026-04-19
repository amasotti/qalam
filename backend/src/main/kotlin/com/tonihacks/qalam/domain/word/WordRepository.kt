package com.tonihacks.qalam.domain.word

import arrow.core.Either
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.RootId

data class WordFilters(
    val q: String? = null,
    val rootId: RootId? = null,
    val dialect: Dialect? = null,
    val difficulty: Difficulty? = null,
    val partOfSpeech: PartOfSpeech? = null,
    val masteryLevel: MasteryLevel? = null,
)

interface WordRepository {
    suspend fun findById(id: WordId): Either<DomainError, Word>
    suspend fun list(page: PageRequest, filters: WordFilters): Either<DomainError, PaginatedResponse<Word>>
    suspend fun autocomplete(query: String, limit: Int): Either<DomainError, List<Word>>
    suspend fun create(word: Word): Either<DomainError, Word>
    suspend fun update(word: Word): Either<DomainError, Word>
    suspend fun delete(id: WordId): Either<DomainError, Unit>
    suspend fun findDictionaryLinks(wordId: WordId): Either<DomainError, List<DictionaryLink>>
    suspend fun addDictionaryLink(link: DictionaryLink): Either<DomainError, DictionaryLink>
    suspend fun deleteDictionaryLink(wordId: WordId, linkId: DictionaryLinkId): Either<DomainError, Unit>
    suspend fun findExamples(wordId: WordId): Either<DomainError, List<WordExample>>
    suspend fun addExample(example: WordExample): Either<DomainError, WordExample>
    suspend fun deleteExample(wordId: WordId, exampleId: WordExampleId): Either<DomainError, Unit>
}
