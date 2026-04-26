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
    suspend fun findByArabicText(arabicText: String): Either<DomainError, Word?>
    suspend fun create(word: Word): Either<DomainError, Word>
    suspend fun update(word: Word): Either<DomainError, Word>
    suspend fun delete(id: WordId): Either<DomainError, Unit>
    suspend fun findDictionaryLinks(wordId: WordId): Either<DomainError, List<DictionaryLink>>
    suspend fun addDictionaryLink(link: DictionaryLink): Either<DomainError, DictionaryLink>
    suspend fun deleteDictionaryLink(wordId: WordId, linkId: DictionaryLinkId): Either<DomainError, Unit>
    suspend fun findExamples(wordId: WordId): Either<DomainError, List<WordExample>>
    suspend fun addExample(example: WordExample): Either<DomainError, WordExample>
    suspend fun deleteExample(wordId: WordId, exampleId: WordExampleId): Either<DomainError, Unit>

    /** Random sample of words for training, optionally filtered by mastery level. */
    suspend fun findForTraining(masteryLevel: MasteryLevel?, limit: Int): Either<DomainError, List<Word>>

    suspend fun getProgress(wordId: WordId): Either<DomainError, WordProgress>

    suspend fun updateProgress(progress: WordProgress): Either<DomainError, Unit>

    suspend fun updateMasteryLevel(wordId: WordId, level: MasteryLevel): Either<DomainError, Unit>

    // Morphology
    suspend fun findMorphology(wordId: WordId): Either<DomainError, WordMorphology?>
    suspend fun upsertMorphology(morphology: WordMorphology): Either<DomainError, WordMorphology>

    // Plurals
    suspend fun findPlurals(wordId: WordId): Either<DomainError, List<WordPlural>>
    suspend fun addPlural(plural: WordPlural): Either<DomainError, WordPlural>
    suspend fun deletePlural(wordId: WordId, pluralId: WordPluralId): Either<DomainError, Unit>

    // Relations (query both directions)
    suspend fun findRelations(wordId: WordId): Either<DomainError, List<WordRelation>>
    suspend fun addRelation(relation: WordRelation): Either<DomainError, WordRelation>
    suspend fun deleteRelation(wordId: WordId, relatedWordId: WordId, type: RelationType): Either<DomainError, Unit>
}
