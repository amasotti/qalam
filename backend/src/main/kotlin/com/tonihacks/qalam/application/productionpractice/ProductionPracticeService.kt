package com.tonihacks.qalam.application.productionpractice

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId

class ProductionPracticeService(
    private val wordSource: ProductionPracticeWordSource,
    private val reviewer: ProductionPracticeReviewer,
) {
    suspend fun createPrompt(): Either<DomainError, ProductionPracticePrompt> = either {
        val nouns = wordSource.sample(PartOfSpeech.NOUN, REQUIRED_NOUNS, emptySet()).bind()
            .requireSelection(REQUIRED_NOUNS, PartOfSpeech.NOUN, emptySet()).bind()
        val verbs = wordSource.sample(PartOfSpeech.VERB, REQUIRED_VERBS, nouns.ids()).bind()
            .requireSelection(REQUIRED_VERBS, PartOfSpeech.VERB, nouns.ids()).bind()
        val selected = nouns + verbs
        val extras = wordSource.sample(null, RANDOM_WORDS, selected.ids()).bind()
            .requireSelection(RANDOM_WORDS, null, selected.ids()).bind()

        ProductionPracticePrompt((selected + extras).map(Word::toProductionPracticeWord))
    }

    suspend fun review(command: ProductionPracticeReviewCommand): Either<DomainError, ProductionPracticeReview> = either {
        val sentence = command.sentence.trim()
        ensure(sentence.isNotEmpty()) { DomainError.ValidationError("sentence", "Sentence must not be blank") }
        ensure(sentence.length <= MAX_SENTENCE_LENGTH) {
            DomainError.ValidationError("sentence", "Sentence must not exceed $MAX_SENTENCE_LENGTH characters")
        }
        val intendedMeaning = command.intendedMeaning?.trim()?.takeIf(String::isNotEmpty)
        ensure(intendedMeaning == null || intendedMeaning.length <= MAX_INTENDED_MEANING_LENGTH) {
            DomainError.ValidationError(
                "intendedMeaning",
                "Intended meaning must not exceed $MAX_INTENDED_MEANING_LENGTH characters",
            )
        }
        ensure(command.targetWordIds.size == PROMPT_WORD_COUNT && command.targetWordIds.toSet().size == PROMPT_WORD_COUNT) {
            DomainError.ValidationError("targetWordIds", "Exactly $PROMPT_WORD_COUNT distinct target words are required")
        }
        ensure(command.usedWordIds.size >= MIN_USED_WORDS && command.usedWordIds.distinct().size == command.usedWordIds.size) {
            DomainError.ValidationError("usedWordIds", "At least $MIN_USED_WORDS distinct used words are required")
        }
        ensure(command.usedWordIds.all(command.targetWordIds::contains)) {
            DomainError.ValidationError("usedWordIds", "Used words must be selected target words")
        }

        val targetWordIds = command.targetWordIds.toSet()
        val usedWordIds = command.usedWordIds.toSet()
        val targetWords = wordSource.findByIds(targetWordIds).bind()
        ensure(targetWords.size == targetWordIds.size) {
            DomainError.NotFound("Word", "one or more target word IDs")
        }
        reviewer.review(
            ProductionPracticeReviewRequest(
                sentence = sentence,
                targetWords = targetWords.map(Word::toProductionPracticeWord),
                usedWordIds = usedWordIds,
                intendedMeaning = intendedMeaning,
            ),
        ).bind()
    }

    private fun List<Word>.requireSelection(
        required: Int,
        expectedPartOfSpeech: PartOfSpeech?,
        excludedWordIds: Set<WordId>,
    ): Either<DomainError, List<Word>> {
        val valid = size == required &&
            distinctBy(Word::id).size == required &&
            none { it.id in excludedWordIds } &&
            (expectedPartOfSpeech == null || all { it.partOfSpeech == expectedPartOfSpeech })
        return if (valid) Either.Right(this) else Either.Left(DomainError.NotEnoughWords(required, size))
    }

    private fun List<Word>.ids(): Set<WordId> = mapTo(linkedSetOf(), Word::id)

    private companion object {
        const val REQUIRED_NOUNS = 2
        const val REQUIRED_VERBS = 2
        const val RANDOM_WORDS = 3
        const val PROMPT_WORD_COUNT = REQUIRED_NOUNS + REQUIRED_VERBS + RANDOM_WORDS
        const val MIN_USED_WORDS = 2
        const val MAX_SENTENCE_LENGTH = 1_000
        const val MAX_INTENDED_MEANING_LENGTH = 1_000
    }
}

private fun Word.toProductionPracticeWord() = ProductionPracticeWord(
    id = id,
    arabicText = arabicText,
    transliteration = transliteration,
    translation = translation,
    partOfSpeech = partOfSpeech,
    dialect = dialect,
)
