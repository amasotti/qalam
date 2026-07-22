package com.tonihacks.qalam.application.productionpractice

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId

data class ProductionPracticePrompt(
    val words: List<ProductionPracticeWord>,
)

data class ProductionPracticeWord(
    val id: WordId,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val partOfSpeech: PartOfSpeech,
    val dialect: Dialect,
)

data class ProductionPracticeReviewCommand(
    val sentence: String,
    val targetWordIds: List<WordId>,
    val usedWordIds: List<WordId>,
)

data class ProductionPracticeReviewRequest(
    val sentence: String,
    val targetWords: List<ProductionPracticeWord>,
    val usedWordIds: Set<WordId>,
)

data class ProductionPracticeReview(
    val markdown: String,
)

/** Application boundary for selecting vocabulary without exposing persistence details. */
interface ProductionPracticeWordSource {
    suspend fun sample(
        partOfSpeech: PartOfSpeech?,
        limit: Int,
        excludedWordIds: Set<WordId>,
    ): Either<DomainError, List<Word>>

    suspend fun findByIds(ids: Set<WordId>): Either<DomainError, List<Word>>
}

/** Application boundary for production-practice feedback generation. */
interface ProductionPracticeReviewer {
    suspend fun review(request: ProductionPracticeReviewRequest): Either<DomainError, ProductionPracticeReview>
}
