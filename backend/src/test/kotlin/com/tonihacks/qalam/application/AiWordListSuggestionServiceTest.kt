package com.tonihacks.qalam.application

import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.wordlist.WordList
import com.tonihacks.qalam.domain.wordlist.WordListId
import com.tonihacks.qalam.domain.wordlist.WordListRepository
import com.tonihacks.qalam.infrastructure.ai.ExistingVocabularyWord
import com.tonihacks.qalam.infrastructure.ai.OpenRouterVocabularyClient
import com.tonihacks.qalam.infrastructure.ai.OpenRouterVocabularySuggestion
import com.tonihacks.qalam.infrastructure.ai.VocabularySuggestionContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlin.time.Instant

class AiWordListSuggestionServiceTest: FunSpec({
    val repository = mockk<WordListRepository>()
    val vocabularyClient = mockk<OpenRouterVocabularyClient>()
    val service = AiWordListSuggestionService(repository, vocabularyClient)

    // ---- Test fixtures -----
    val listId = WordListId(UUID.randomUUID())
    val list = WordList(
        id = listId,
        title = "Food",
        description = "Everyday Tunisian food vocabulary",
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
    val existingWord = Word(
        id = WordId(UUID.randomUUID()),
        arabicText = "خُبْز",
        transliteration = "khobz",
        translation = "bread",
        partOfSpeech = PartOfSpeech.NOUN,
        dialect = Dialect.TUNISIAN,
        difficulty = Difficulty.BEGINNER,
        masteryLevel = MasteryLevel.NEW,
        pronunciationUrl = null,
        rootId = null,
        derivedFromId = null,
        notes = null,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    beforeTest { clearMocks(repository, vocabularyClient) }

    test("passes list context to vocabulary client and maps preview response") {
        val context = slot<VocabularySuggestionContext>()
        coEvery { repository.findById(listId) } returns list.right()
        coEvery { repository.membersOf(listId) } returns listOf(existingWord).right()
        coEvery { vocabularyClient.suggestWordsForList(capture(context)) } returns listOf(
            OpenRouterVocabularySuggestion(
                arabicText = "بَصَل",
                transliteration = "bsel",
                translation = "onion",
                partOfSpeech = "NOUN",
                difficulty = "BEGINNER",
                dialect = "TUNISIAN",
            ),
        ).right()

        val result = service.suggestWords(listId.toString()).getOrNull()!!

        context.captured shouldBe VocabularySuggestionContext(
            title = "Food",
            description = "Everyday Tunisian food vocabulary",
            existingWords = listOf(ExistingVocabularyWord("خُبْز", "bread")),
        )
        result.suggestions.single().arabicText shouldBe "بَصَل"
        result.suggestions.single().transliteration shouldBe "bsel"
        result.suggestions.single().translation shouldBe "onion"
        result.suggestions.single().partOfSpeech shouldBe "NOUN"
        result.suggestions.single().difficulty shouldBe "BEGINNER"
        result.suggestions.single().dialect shouldBe "TUNISIAN"
    }

    test("rejects malformed UUID before calling vocabulary client") {
        service.suggestWords("not-a-uuid") shouldBe
                DomainError.InvalidInput("'not-a-uuid' is not a valid UUID").left()

        coVerify(exactly = 0) { vocabularyClient.suggestWordsForList(any()) }
    }

    test("returns missing list before calling vocabulary client") {
        coEvery { repository.findById(listId) } returns
                DomainError.NotFound("WordList", listId.toString()).left()

        service.suggestWords(listId.toString()) shouldBe
                DomainError.NotFound("WordList", listId.toString()).left()

        coVerify(exactly = 0) { vocabularyClient.suggestWordsForList(any()) }
    }

    test("rejects a list without a description before calling vocabulary client") {
        coEvery { repository.findById(listId) } returns list.copy(description = null).right()
        coEvery { repository.membersOf(listId) } returns emptyList<Word>().right()

        service.suggestWords(listId.toString()) shouldBe
                DomainError.ValidationError("description", "description is required for AI word suggestions").left()

        coVerify(exactly = 0) { vocabularyClient.suggestWordsForList(any()) }
    }

    test("propagates AI_NOT_CONFIGURED") {
        coEvery { repository.findById(listId) } returns list.right()
        coEvery { repository.membersOf(listId) } returns emptyList<Word>().right()
        coEvery { vocabularyClient.suggestWordsForList(any()) } returns
                DomainError.AiNotConfigured.left()

        service.suggestWords(listId.toString()) shouldBe DomainError.AiNotConfigured.left()
    }
})
