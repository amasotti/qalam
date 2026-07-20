package com.tonihacks.qalam.application

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.wordlist.AiListWordSuggestion
import com.tonihacks.qalam.delivery.dto.wordlist.WordListSuggestionsResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.wordlist.WordListId
import com.tonihacks.qalam.domain.wordlist.WordListRepository
import com.tonihacks.qalam.infrastructure.ai.ExistingVocabularyWord
import com.tonihacks.qalam.infrastructure.ai.OpenRouterVocabularyClient
import com.tonihacks.qalam.infrastructure.ai.VocabularySuggestionContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID


class AiWordListSuggestionService internal constructor(
    private val wordListRepository: WordListRepository,
    private val vocabularyAiClient: OpenRouterVocabularyClient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun suggestWords(id: String): Either<DomainError, WordListSuggestionsResponse> = either {
        val listId = parseId(id).bind()
        val list = wordListRepository.findById(listId).bind()
        val existingWords = wordListRepository.membersOf(listId).bind()
        val description = list.description?.trim().orEmpty()
        ensure(description.isNotEmpty()) {
            DomainError.ValidationError("description", "description is required for AI word suggestions")
        }

        val suggestionContext = VocabularySuggestionContext(
            title = list.title,
            description = description,
            existingWords = existingWords.map { ExistingVocabularyWord(it.arabicText, it.translation) },
        )
        val suggestions = vocabularyAiClient.suggestWordsForList(suggestionContext).bind()

        WordListSuggestionsResponse(
            suggestions = suggestions.map {
                AiListWordSuggestion(
                    arabicText = it.arabicText,
                    translation = it.translation,
                    transliteration = it.transliteration,
                    partOfSpeech = it.partOfSpeech,
                    difficulty = it.difficulty,
                    dialect = it.dialect,
                )
            },
        )
    }.logDomainFailure(log) { "Failed to generate AI word-list suggestion for listId=$id: $it" }

    private fun parseId(id: String): Either<DomainError, WordListId> =
        try {
            WordListId(UUID.fromString(id)).right()
        } catch (_: IllegalArgumentException) {
            DomainError.InvalidInput("'$id' is not a valid UUID").left()
        }
}
