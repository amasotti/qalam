package com.tonihacks.qalam.application

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.ArabicRoot
import com.tonihacks.qalam.domain.root.RootId
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordFilters
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRepository
import com.tonihacks.qalam.infrastructure.ai.ExistingVocabularyWord
import com.tonihacks.qalam.infrastructure.ai.OpenRouterVocabularyClient
import com.tonihacks.qalam.infrastructure.ai.OpenRouterVocabularySuggestion
import com.tonihacks.qalam.infrastructure.ai.RootFamilySuggestionContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlin.time.Instant

class AiRootFamilySuggestionServiceTest : FunSpec({
    val rootRepository = mockk<RootRepository>()
    val wordRepository = mockk<WordRepository>()
    val vocabularyClient = mockk<OpenRouterVocabularyClient>()
    val service = AiRootFamilySuggestionService(rootRepository, wordRepository, vocabularyClient)

    val rootId = RootId(UUID.randomUUID())
    val root = ArabicRoot(
        id = rootId,
        letters = listOf("ك", "ت", "ب"),
        normalizedForm = "كتب",
        displayForm = "ك ت ب",
        letterCount = 3,
        meaning = "writing",
        analysis = "A common root for writing and books.",
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
    val familyWord = Word(
        id = WordId(UUID.randomUUID()),
        arabicText = "كِتَاب",
        transliteration = "kitāb",
        translation = "book",
        partOfSpeech = PartOfSpeech.NOUN,
        dialect = Dialect.MSA,
        difficulty = Difficulty.BEGINNER,
        masteryLevel = MasteryLevel.NEW,
        pronunciationUrl = null,
        rootId = rootId,
        derivedFromId = null,
        notes = null,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    fun suggestion(index: Int) = OpenRouterVocabularySuggestion(
        arabicText = "كَلِمَة$index",
        transliteration = "kalima$index",
        translation = "word $index",
        partOfSpeech = "NOUN",
        difficulty = "INTERMEDIATE",
        dialect = "MSA",
    )

    beforeTest { clearMocks(rootRepository, wordRepository, vocabularyClient) }

    test("passes root metadata and family context to vocabulary client, then caps preview at five") {
        val context = slot<RootFamilySuggestionContext>()
        coEvery { rootRepository.findById(rootId) } returns root.right()
        coEvery { wordRepository.list(PageRequest.from(1, 500), WordFilters(rootId = rootId)) } returns
            PaginatedResponse(listOf(familyWord), 1, 1, 500).right()
        coEvery { vocabularyClient.suggestWordsForRoot(capture(context)) } returns
            (1..6).map(::suggestion).right()

        val result = service.suggestWords(rootId.toString()).getOrNull()!!

        context.captured shouldBe RootFamilySuggestionContext(
            displayForm = "ك ت ب",
            meaning = "writing",
            analysis = "A common root for writing and books.",
            existingWords = listOf(ExistingVocabularyWord("كِتَاب", "book")),
        )
        result.suggestions.map { it.arabicText } shouldBe (1..5).map { "كَلِمَة$it" }
    }

    test("rejects malformed UUID before calling repositories or vocabulary client") {
        service.suggestWords("not-a-uuid") shouldBe
            DomainError.InvalidInput("'not-a-uuid' is not a valid UUID").left()

        coVerify(exactly = 0) { rootRepository.findById(any()) }
        coVerify(exactly = 0) { wordRepository.list(any(), any()) }
        coVerify(exactly = 0) { vocabularyClient.suggestWordsForRoot(any()) }
    }

    test("returns missing root before loading family or calling vocabulary client") {
        coEvery { rootRepository.findById(rootId) } returns
            DomainError.NotFound("Root", rootId.toString()).left()

        service.suggestWords(rootId.toString()) shouldBe
            DomainError.NotFound("Root", rootId.toString()).left()

        coVerify(exactly = 0) { wordRepository.list(any(), any()) }
        coVerify(exactly = 0) { vocabularyClient.suggestWordsForRoot(any()) }
    }

    test("rejects a root without meaning before loading family or calling vocabulary client") {
        coEvery { rootRepository.findById(rootId) } returns root.copy(meaning = null).right()

        service.suggestWords(rootId.toString()) shouldBe
            DomainError.InvalidInput("Root should have a meaning to allow AI to better identify them").left()

        coVerify(exactly = 0) { wordRepository.list(any(), any()) }
        coVerify(exactly = 0) { vocabularyClient.suggestWordsForRoot(any()) }
    }

    test("propagates AI_NOT_CONFIGURED") {
        coEvery { rootRepository.findById(rootId) } returns root.right()
        coEvery { wordRepository.list(PageRequest.from(1, 500), WordFilters(rootId = rootId)) } returns
            PaginatedResponse<Word>(emptyList(), 0, 1, 500).right()
        coEvery { vocabularyClient.suggestWordsForRoot(any()) } returns DomainError.AiNotConfigured.left()

        service.suggestWords(rootId.toString()) shouldBe DomainError.AiNotConfigured.left()
    }
})
