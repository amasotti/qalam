package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.sentence.TokenInput
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** OpenRouter-backed assistance for sentence tokenization and transliteration. */
internal class OpenRouterSentenceClient(
    private val openRouter: OpenRouterClient,
) {
    private val log = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun autoTokenize(arabicText: String, translation: String?): Either<DomainError, List<TokenInput>> =
        complete("autoTokenize", arabicText) {
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/SentenceAssistantSystemPrompt.md"),
                userPrompt = PromptLoader.loadPrompt(
                    "ai-prompts/AutoTokenizeSentenceUserPrompt.md",
                    mapOf(
                        "arabicText" to arabicText,
                        "translation" to (translation?.takeUnless { it.isBlank() } ?: "")
                    ),
                ),
                responseFormat = JSON_OBJECT_RESPONSE_FORMAT,
            )
        }.flatMap { content ->
            parse("autoTokenize", content) {
                json.decodeFromString<TokensPayload>(content).tokens.map { token ->
                    TokenInput(token.position, token.arabic, token.transliteration, token.translation, wordId = null)
                }
            }
        }

    suspend fun transliterate(arabicText: String): Either<DomainError, String> =
        complete("transliterate", arabicText) {
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/SentenceAssistantSystemPrompt.md"),
                userPrompt = PromptLoader.loadPrompt(
                    "ai-prompts/TransliterateSentenceUserPrompt.md",
                    mapOf("arabicText" to arabicText),
                ),
                responseFormat = JSON_OBJECT_RESPONSE_FORMAT,
            )
        }.flatMap { content ->
            parse("transliterate", content) { json.decodeFromString<TransliterationPayload>(content).transliteration }
        }

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
        { it.right() },
    )

    private inline fun <T> parse(capability: String, content: String, decode: (String) -> T): Either<DomainError, T> =
        try {
            decode(content).right()
        } catch (error: SerializationException) {
            log.warn(error) { "OpenRouter $capability returned invalid JSON" }
            DomainError.InvalidInput("AI $capability request failed").left()
        }

    @Serializable
    private data class TokensPayload(val tokens: List<AiToken>)

    @Serializable
    private data class AiToken(
        val position: Int,
        val arabic: String,
        val transliteration: String? = null,
        val translation: String? = null,
    )

    @Serializable
    private data class TransliterationPayload(val transliteration: String)

    private companion object {
        val JSON_OBJECT_RESPONSE_FORMAT = OpenRouterResponseFormat(type = "json_object")
    }
}
