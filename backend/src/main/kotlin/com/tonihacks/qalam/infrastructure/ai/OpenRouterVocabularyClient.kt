package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.PartOfSpeech
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json

internal data class VocabularySuggestionContext(
    val title: String,
    val description: String?,
    val existingWords: List<ExistingVocabularyWord>
)

internal data class ExistingVocabularyWord(
    val arabicText: String,
    val translation: String
)

internal class OpenRouterVocabularyClient(
    private val openRouter: OpenRouterClient
) {
    private val log = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun suggestWordsForList(
        context: VocabularySuggestionContext
    ): Either<DomainError, List<OpenRouterVocabularySuggestion>> {

        val description = context.description?.takeIf { it.isNotBlank() }
            ?: let { "\nDescription: $it" }

        val existingWords = if (context.existingWords.isEmpty()) {
            "\nThe list has been just created and contains no words yet."
        } else {
            "\nThese words are already linked in this list (do not suggest them again!):\n" +
                    context.existingWords.joinToString("\n") { w ->
                        "- ${w.arabicText} (\"${w.translation}\")"
                    }
        }

        val prompt = PromptLoader.loadPrompt("ai-prompts/SuggestWordsForListUserPrompt.md")
            .replace("<title>", context.title)
            .replace("<description>", description)
            .replace("<existingWords>", existingWords)
            .trimIndent()

        val systemPrompt = PromptLoader.loadPrompt("ai-prompts/VocabularyExpertSystemPrompt.md")
            .trimIndent()

        return openRouter.complete(
            OpenRouterCompletionRequest(
                systemPrompt = systemPrompt,
                userPrompt = prompt,
                responseFormat = OpenRouterResponseFormat(
                    type = "json_schema",
                    jsonSchema = OpenRouterJsonSchema(
                        name = "word_suggestions",
                        strict = true,
                        schema = wordSuggestionSchema
                    ),
                ),
                provider = OpenRouterProviderPreferences(requireParameters = true)
            ),
        ).map { parseWordListSuggestions(it, json) }
            .mapLeft { err ->
                log.warn { "Openrouter suggestWordsForList failed for list ${context.title}" }
                err
            }
    }
}
