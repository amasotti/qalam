package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.PartOfSpeech
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json

internal data class VocabularySuggestionContext(
    val title: String,
    val description: String,
    val existingWords: List<ExistingVocabularyWord>,
)

internal data class ExistingVocabularyWord(
    val arabicText: String,
    val translation: String?,
)

internal class OpenRouterVocabularyClient(
    private val openRouter: OpenRouterClient,
) {
    private val log = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun suggestWordsForList(
        context: VocabularySuggestionContext,
    ): Either<DomainError, List<OpenRouterVocabularySuggestion>> {
        val existingWords = if (context.existingWords.isEmpty()) {
            "The list is currently empty."
        } else {
            val existingWordsList = context.existingWords
                .filter { w -> w.translation != null }
                .joinToString("\n") { w -> "- ${w.arabicText} (${w.translation})" }
            "Words already in the list (do NOT suggest these or close variants):\n$existingWordsList"
        }

        val prompt = PromptLoader.loadPrompt(
            path = "ai-prompts/SuggestWordsForListUserPrompt.md",
            values = mapOf(
                "title" to context.title,
                "description" to context.description,
                "existingWords" to existingWords,
                "partOfSpeechValues" to PartOfSpeech.entries.joinToString { it.name },
                "difficultyValues" to Difficulty.entries.joinToString { it.name },
                "dialectValues" to Dialect.entries.joinToString { it.name },
            ),
        )
        val systemPrompt = PromptLoader.loadPrompt("ai-prompts/VocabularyExpertSystemPrompt.md")

        val result = openRouter.complete(
            OpenRouterCompletionRequest(
                systemPrompt = systemPrompt,
                userPrompt = prompt,
                responseFormat = OpenRouterResponseFormat(
                    type = "json_schema",
                    jsonSchema = OpenRouterJsonSchema(
                        name = "word_suggestions",
                        strict = true,
                        schema = wordSuggestionSchema,
                    ),
                ),
                provider = OpenRouterProviderPreferences(requireParameters = true),
            ),
        ).map { parseWordListSuggestions(it, json) }

        return result.fold(
            { error ->
                log.warn { "OpenRouter suggestWordsForList failed titleLength=${context.title.length}: $error" }
                if (error == DomainError.AiNotConfigured) error.left()
                else DomainError.InvalidInput("AI list suggestion request failed").left()
            },
            { suggestions -> suggestions.right() },
        )
    }
}
