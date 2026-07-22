package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal class OpenRouterClient : AutoCloseable {
    private val log = KotlinLogging.logger { }
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val apiKey = System.getenv("OPENROUTER_API_KEY")
    private val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"

    private val requestTimeoutMs = positiveLongEnv("OPENROUTER_REQUEST_TIMEOUT_MS") ?: REQUEST_TIMEOUT_MS
    private val connectTimeoutMs = positiveLongEnv("OPENROUTER_CONNECT_TIMEOUT_MS") ?: CONNECT_TIMEOUT_MS

    private val lazyHttpClient = lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }

            install(HttpTimeout) {
                requestTimeoutMillis = requestTimeoutMs
                socketTimeoutMillis = requestTimeoutMs
                connectTimeoutMillis = connectTimeoutMs
            }
        }
    }

    init {
        if (apiKey.isNullOrBlank()) {
            log.warn { "OPENROUTER_API_KEY is not set — AI features will be unavailable." }
        }
        log.info { "AI client timeout configured requestTimeoutMs=$requestTimeoutMs connectTimeoutMs=$connectTimeoutMs" }
    }

    companion object {
        const val REQUEST_TIMEOUT_MS = 120_000L
        const val CONNECT_TIMEOUT_MS = 15_000L

        private const val OPENROUTER_COMPLETION_URL = "https://openrouter.ai/api/v1/chat/completions"
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun complete(req: OpenRouterCompletionRequest): Either<DomainError, String> {
        val configuredApiKey = apiKey?.takeIf { it.isNotBlank() } ?: return DomainError.AiNotConfigured.left()

        return try {
            val res = lazyHttpClient.value.post(OPENROUTER_COMPLETION_URL) {
                header(HttpHeaders.Authorization, "Bearer $configuredApiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    OpenRouterRequest(
                        model = model,
                        messages = listOf(
                            OpenRouterMessage("system", req.systemPrompt),
                            OpenRouterMessage("user", req.userPrompt),
                        ),
                        responseFormat = req.responseFormat,
                        provider = req.provider,
                        maxTokens = req.maxTokens,
                    ),
                )
            }
            val content = res.body<OpenRouterResponse>()
                .choices.firstOrNull()?.message?.content ?: return DomainError.InvalidInput("Empty AI response").left()
            content.right()
        } catch (error: Exception) {
            log.warn(error) { "OpenRouter completion failed" }
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    override fun close() {
        if (lazyHttpClient.isInitialized()) lazyHttpClient.value.close()
    }

    private fun positiveLongEnv(name: String): Long? =
        System.getenv(name)?.toLongOrNull()?.takeIf { it > 0 }
}

internal data class OpenRouterCompletionRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val responseFormat: OpenRouterResponseFormat? = null,
    val provider: OpenRouterProviderPreferences? = null,
    val maxTokens: Int? = null,
)

@Serializable
internal data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    @SerialName("response_format") val responseFormat: OpenRouterResponseFormat? = null,
    val provider: OpenRouterProviderPreferences? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
internal data class OpenRouterResponseFormat(
    val type: String,
    @SerialName("json_schema") val jsonSchema: OpenRouterJsonSchema? = null,
)

@Serializable
internal data class OpenRouterJsonSchema(
    val name: String,
    val strict: Boolean,
    val schema: JsonObject,
)

@Serializable
internal data class OpenRouterProviderPreferences(
    @SerialName("require_parameters") val requireParameters: Boolean,
)

@Serializable
internal data class OpenRouterMessage(val role: String, val content: String)

@Serializable
internal data class OpenRouterResponse(val choices: List<OpenRouterChoice>)

@Serializable
internal data class OpenRouterChoice(val message: OpenRouterMessage)
