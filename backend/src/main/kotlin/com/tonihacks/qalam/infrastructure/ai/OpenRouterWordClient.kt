package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.word.AiExampleSentence
import com.tonihacks.qalam.delivery.dto.word.AiPluralSuggestion
import com.tonihacks.qalam.delivery.dto.word.AiRelationSuggestion
import com.tonihacks.qalam.delivery.dto.word.AiVerbDetailsSuggestion
import com.tonihacks.qalam.delivery.dto.word.WordAnalysisResponse
import com.tonihacks.qalam.delivery.dto.word.WordEnrichmentSuggestion
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** OpenRouter-backed capabilities for the word AI endpoints. */
internal class OpenRouterWordClient(
    private val openRouter: OpenRouterClient,
) {
    private val log = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateExamples(
        arabicText: String,
        translation: String?,
    ): Either<DomainError, List<AiExampleSentence>> = complete("generateExamples", arabicText) {
        OpenRouterCompletionRequest(
            systemPrompt = PromptLoader.loadPrompt("ai-prompts/VocabularyExpertSystemPrompt.md"),
            userPrompt = PromptLoader.loadPrompt(
                "ai-prompts/GenerateWordExamplesUserPrompt.md",
                mapOf(
                    "arabicText" to arabicText,
                    "translationHint" to translation?.takeIf { it.isNotBlank() }?.let { " (meaning: \"$it\")" }
                        .orEmpty(),
                ),
            ),
            responseFormat = JSON_OBJECT_RESPONSE_FORMAT,
        )
    }.flatMap { content -> parse("generateExamples", content) { json.decodeFromString<ExamplesPayload>(it).examples } }

    suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> =
        complete("analyzeWord", arabicText) {
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/VocabularyExpertSystemPrompt.md"),
                userPrompt = PromptLoader.loadPrompt(
                    "ai-prompts/AnalyzeWordUserPrompt.md",
                    mapOf(
                        "arabicText" to arabicText,
                        "partOfSpeechValues" to PartOfSpeech.entries.joinToString { it.name },
                    ),
                ),
                responseFormat = JSON_OBJECT_RESPONSE_FORMAT,
            )
        }.flatMap { content ->
            parse("analyzeWord", content) {
                val payload = json.decodeFromString<WordAnalysisPayload>(content)
                WordAnalysisResponse(
                    arabicText = arabicText,
                    transliteration = payload.transliteration,
                    translation = payload.translation,
                    partOfSpeech = payload.partOfSpeech,
                )
            }
        }

    suspend fun enrichWord(word: Word): Either<DomainError, WordEnrichmentSuggestion> =
        complete("enrichWord", word.arabicText) {
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/VocabularyExpertSystemPrompt.md"),
                userPrompt = PromptLoader.loadPrompt(
                    "ai-prompts/EnrichWordUserPrompt.md",
                    mapOf(
                        "arabicText" to word.arabicText,
                        "transliterationClause" to word.transliteration?.let { " [$it]" }.orEmpty(),
                        "translation" to (word.translation ?: "no translation"),
                        "partOfSpeech" to word.partOfSpeech.name,
                        "dialect" to word.dialect.name,
                    ),
                ),
                responseFormat = JSON_OBJECT_RESPONSE_FORMAT,
            )
        }.flatMap { content -> parse("enrichWord", content) { parseWordEnrichmentSuggestion(it, word.partOfSpeech) } }

    private suspend fun complete(
        capability: String,
        arabicText: String,
        request: () -> OpenRouterCompletionRequest,
    ): Either<DomainError, String> = openRouter.complete(request()).fold(
        { error ->
            log.warn { "OpenRouter $capability failed arabicLength=${arabicText.length}: $error" }
            if (error == DomainError.AiNotConfigured) error.left()
            else DomainError.InvalidInput("AI $capability request failed").left()
        },
        { content -> content.right() },
    )

    private inline fun <T> parse(capability: String, content: String, decode: (String) -> T): Either<DomainError, T> =
        try {
            decode(content).right()
        } catch (error: SerializationException) {
            log.warn(error) { "OpenRouter $capability returned invalid JSON" }
            DomainError.InvalidInput("AI $capability request failed").left()
        }

    @Serializable
    private data class ExamplesPayload(val examples: List<AiExampleSentence>)

    @Serializable
    private data class WordAnalysisPayload(
        val transliteration: String? = null,
        val translation: String? = null,
        val partOfSpeech: String? = null,
    )

    private companion object {
        val JSON_OBJECT_RESPONSE_FORMAT = OpenRouterResponseFormat(type = "json_object")
    }
}

private val enrichmentJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Decodes an untrusted AI preview without allowing one incomplete suggestion to discard useful ones.
 * The response returned to the frontend retains the strict API shape.
 */
internal fun parseWordEnrichmentSuggestion(
    content: String,
    partOfSpeech: PartOfSpeech,
): WordEnrichmentSuggestion =
    enrichmentJson.decodeFromString<AiWordEnrichmentPayload>(content).toSuggestion(partOfSpeech)

@Serializable
private data class AiWordEnrichmentPayload(
    val notes: String? = null,
    val gender: String? = null,
    val verbDetails: AiVerbDetailsPayload? = null,
    val plurals: List<AiPluralSuggestion> = emptyList(),
    val relations: List<AiRelationSuggestion> = emptyList(),
) {
    fun toSuggestion(partOfSpeech: PartOfSpeech) = WordEnrichmentSuggestion(
        notes = notes,
        gender = gender,
        verbDetails = verbDetails
            ?.takeIf { partOfSpeech == PartOfSpeech.VERB }
            ?.toSuggestionOrNull(),
        plurals = plurals,
        relations = relations,
    )
}

@Serializable
private data class AiVerbDetailsPayload(
    val verbForm: String? = null,
    val pastPattern: String? = null,
    val presentPattern: String? = null,
    val weaknessType: String? = null,
) {
    fun toSuggestionOrNull(): AiVerbDetailsSuggestion? {
        val form = verbForm?.takeIf { it.isNotBlank() } ?: return null
        val weakness = weaknessType?.takeIf { it.isNotBlank() } ?: return null
        return AiVerbDetailsSuggestion(form, pastPattern, presentPattern, weakness)
    }
}
