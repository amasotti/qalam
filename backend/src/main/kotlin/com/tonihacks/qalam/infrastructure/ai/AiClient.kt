package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.sentence.TokenInputDto
import com.tonihacks.qalam.delivery.dto.word.ExampleSentenceResponse
import com.tonihacks.qalam.domain.error.DomainError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AiClient(private val apiKey: String?) {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(jsonConfig) }
    }

    suspend fun generateExamples(
        arabicText: String,
        translation: String?,
    ): Either<DomainError, List<ExampleSentenceResponse>> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val prompt = buildPrompt(arabicText, translation)

        val model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini"

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        Message("system", SYSTEM_PROMPT),
                        Message("user", prompt),
                    ),
                    responseFormat = ResponseFormat("json_object"),
                ))
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            val examples = jsonConfig.decodeFromString<ExamplesPayload>(content)
            examples.examples.right()
        } catch (_: Exception) {
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    suspend fun autoTokenize(arabicText: String): Either<DomainError, List<TokenInputDto>> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini"
        val prompt = """Split this Arabic sentence into individual word tokens.
Return a JSON object with a "tokens" array. Each element must have:
- "position": integer index starting at 0
- "arabic": the Arabic word

Sentence: "$arabicText""""

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        Message("system", SYSTEM_PROMPT),
                        Message("user", prompt),
                    ),
                    responseFormat = ResponseFormat("json_object"),
                ))
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            val payload = jsonConfig.decodeFromString<TokensPayload>(content)
            payload.tokens.right()
        } catch (_: Exception) {
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    suspend fun transliterate(arabicText: String): Either<DomainError, String> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini"
        val prompt = """Transliterate this Arabic sentence into Latin script using a practical/Buckwalter-inspired scheme.
Return a JSON object with a single "transliteration" string field.

Sentence: "$arabicText""""

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        Message("system", SYSTEM_PROMPT),
                        Message("user", prompt),
                    ),
                    responseFormat = ResponseFormat("json_object"),
                ))
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            val payload = jsonConfig.decodeFromString<TransliterationPayload>(content)
            payload.transliteration.right()
        } catch (_: Exception) {
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    private fun buildPrompt(arabicText: String, translation: String?): String {
        val translationHint = if (!translation.isNullOrBlank()) " (meaning: \"$translation\")" else ""
        return """Given the Arabic word "$arabicText"$translationHint, provide exactly 2 example sentences.
Return a JSON object with an "examples" array. Each element must have:
- "arabic": the sentence in Arabic script
- "transliteration": Latin transliteration
- "translation": English translation"""
    }

    @Serializable
    private data class OpenRouterRequest(
        val model: String,
        val messages: List<Message>,
        @SerialName("response_format") val responseFormat: ResponseFormat,
    )

    @Serializable
    private data class ResponseFormat(val type: String)

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class OpenRouterResponse(val choices: List<Choice>)

    @Serializable
    private data class Choice(val message: Message)

    @Serializable
    private data class ExamplesPayload(val examples: List<ExampleSentenceResponse>)

    @Serializable
    private data class TokensPayload(val tokens: List<TokenInputDto>)

    @Serializable
    private data class TransliterationPayload(val transliteration: String)

    private companion object {
        const val SYSTEM_PROMPT = "You are a structured assistant as companion for an Arabic language teacher. " +
                "Return only valid JSON."
    }
}
