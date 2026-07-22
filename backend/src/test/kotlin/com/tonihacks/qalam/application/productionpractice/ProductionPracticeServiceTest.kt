package com.tonihacks.qalam.application.productionpractice

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID
import kotlin.time.Instant

class ProductionPracticeServiceTest : FunSpec({
    val source = FakeWordSource()
    val reviewer = FakeReviewer()
    val service = ProductionPracticeService(source, reviewer)

    beforeTest {
        source.reset()
        reviewer.reset()
    }

    test("creates seven unique words with two nouns and two verbs") {
        val prompt = service.createPrompt().getOrNull()!!

        prompt.words.size shouldBe 7
        prompt.words.count { it.partOfSpeech == PartOfSpeech.NOUN } shouldBe 2
        prompt.words.count { it.partOfSpeech == PartOfSpeech.VERB } shouldBe 2
        prompt.words.map { it.id }.toSet().size shouldBe 7
        source.calls shouldContainExactly listOf(
            SelectionCall(PartOfSpeech.NOUN, 2, emptySet()),
            SelectionCall(PartOfSpeech.VERB, 2, source.nouns.mapTo(linkedSetOf()) { it.id }),
            SelectionCall(null, 3, (source.nouns + source.verbs).mapTo(linkedSetOf()) { it.id }),
        )
    }

    test("rejects a review with fewer than two selected words before loading words") {
        val targetIds = source.allWords.take(7).map { it.id }

        service.review(ProductionPracticeReviewCommand("أنا أكتب", targetIds, listOf(targetIds.first()))) shouldBe
            DomainError.ValidationError("usedWordIds", "At least 2 distinct used words are required").left()
        source.findCalls shouldBe 0
        reviewer.calls shouldBe 0
    }

    test("loads canonical target words and delegates a valid review") {
        val targetIds = source.allWords.take(7).map { it.id }
        val usedIds = targetIds.take(2)

        val result = service.review(ProductionPracticeReviewCommand("  أنا أكتب كتاباً  ", targetIds, usedIds))

        result shouldBe reviewer.response.right()
        reviewer.lastRequest!!.sentence shouldBe "أنا أكتب كتاباً"
        reviewer.lastRequest!!.usedWordIds shouldBe usedIds.toSet()
        reviewer.lastRequest!!.targetWords.map { it.id }.toSet() shouldBe targetIds.toSet()
    }
})

private data class SelectionCall(
    val partOfSpeech: PartOfSpeech?,
    val limit: Int,
    val excludedWordIds: Set<WordId>,
)

private class FakeWordSource : ProductionPracticeWordSource {
    val nouns = listOf(word(PartOfSpeech.NOUN), word(PartOfSpeech.NOUN))
    val verbs = listOf(word(PartOfSpeech.VERB), word(PartOfSpeech.VERB))
    val extras = listOf(word(PartOfSpeech.ADJECTIVE), word(PartOfSpeech.ADVERB), word(PartOfSpeech.PRONOUN))
    val allWords = nouns + verbs + extras
    val calls = mutableListOf<SelectionCall>()
    var findCalls = 0

    override suspend fun sample(partOfSpeech: PartOfSpeech?, limit: Int, excludedWordIds: Set<WordId>) =
        allWords.filter { it.partOfSpeech == partOfSpeech || partOfSpeech == null }
            .filterNot { it.id in excludedWordIds }
            .take(limit)
            .also { calls += SelectionCall(partOfSpeech, limit, excludedWordIds) }
            .right()

    override suspend fun findByIds(ids: Set<WordId>) = allWords.filter { it.id in ids }.also { findCalls++ }.right()

    fun reset() {
        calls.clear()
        findCalls = 0
    }
}

private class FakeReviewer : ProductionPracticeReviewer {
    val response = ProductionPracticeReview(
        markdown = "## Overall feedback\nGood work",
    )
    var calls = 0
    var lastRequest: ProductionPracticeReviewRequest? = null

    override suspend fun review(request: ProductionPracticeReviewRequest) = response.also {
        calls++
        lastRequest = request
    }.right()

    fun reset() {
        calls = 0
        lastRequest = null
    }
}

private fun word(partOfSpeech: PartOfSpeech) = Word(
    id = WordId(UUID.randomUUID()),
    arabicText = "كلمة",
    transliteration = "kalima",
    translation = "word",
    partOfSpeech = partOfSpeech,
    dialect = Dialect.MSA,
    difficulty = Difficulty.BEGINNER,
    masteryLevel = MasteryLevel.NEW,
    pronunciationUrl = null,
    rootId = null,
    derivedFromId = null,
    notes = null,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
)
