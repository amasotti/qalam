package com.tonihacks.qalam.application

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.root.AiRootWordSuggestion
import com.tonihacks.qalam.delivery.dto.root.RootWordSuggestionsResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.removeArabicDiacritics
import com.tonihacks.qalam.domain.root.RootId
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.word.WordFilters
import com.tonihacks.qalam.domain.word.WordRepository
import com.tonihacks.qalam.infrastructure.ai.ExistingVocabularyWord
import com.tonihacks.qalam.infrastructure.ai.OpenRouterVocabularyClient
import com.tonihacks.qalam.infrastructure.ai.RootFamilySuggestionContext
import java.util.*

class AiRootFamilySuggestionService internal constructor(
    private val rootRepository: RootRepository,
    private val wordRepository: WordRepository,
    private val vocabularyClient: OpenRouterVocabularyClient,
) {
    suspend fun suggestWords(id: String): Either<DomainError, RootWordSuggestionsResponse> = either {
        val rootId = parseId(id).bind()
        val root = rootRepository.findById(rootId).bind()
        ensure(root.meaning != null) {
            DomainError.InvalidInput("Root should have a meaning to allow AI to better identify them")
        }
        val analysis = root.analysis.takeUnless { it.isNullOrBlank() }?.take(200) ?: ""
        val family = wordRepository.list(PageRequest.from(1, 500), WordFilters(rootId = rootId)).bind()

        val suggestions = vocabularyClient.suggestWordsForRoot(
            RootFamilySuggestionContext(
                displayForm = root.displayForm,
                meaning = root.meaning,
                analysis = analysis,
                existingWords = family.items.map { ExistingVocabularyWord(it.arabicText, it.translation) },
            ),
        ).bind()
            .filter { !family.items.any { w -> w.arabicText.removeArabicDiacritics() == it.arabicText.removeArabicDiacritics() } }

        RootWordSuggestionsResponse(suggestions.take(5).map {
            AiRootWordSuggestion(
                it.arabicText,
                it.transliteration,
                it.translation,
                it.partOfSpeech,
                it.dialect,
                it.difficulty
            )
        })
    }

    private fun parseId(id: String): Either<DomainError, RootId> =
        try {
            RootId(UUID.fromString(id)).right()
        } catch (_: IllegalArgumentException) {
            DomainError.InvalidInput("'$id' is not a valid UUID").left()
        }
}
