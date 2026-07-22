package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.ai.InsightContext
import com.tonihacks.qalam.domain.ai.InsightMode
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Every OpenRouter call catches broadly on purpose: network, timeout, and JSON-decode
// failures all collapse to a single degraded DomainError. The suppress keeps that intent
// explicit while we log the real cause for diagnostics.
@Suppress("TooGenericExceptionCaught")
class AiClient : java.io.Closeable {
    private val log = KotlinLogging.logger {}

    private var apiKey: String? = System.getenv("OPENROUTER_API_KEY")
    private val requestTimeoutMs = positiveLongEnv("OPENROUTER_REQUEST_TIMEOUT_MS")
        ?: DEFAULT_AI_REQUEST_TIMEOUT_MS
    private val connectTimeoutMs = positiveLongEnv("OPENROUTER_CONNECT_TIMEOUT_MS")
        ?: DEFAULT_AI_CONNECT_TIMEOUT_MS

    init {
        if (apiKey.isNullOrBlank()) {
            log.warn { "OPENROUTER_API_KEY is not set — AI features will be unavailable." }
        }
        log.info { "AI client timeout configured requestTimeoutMs=$requestTimeoutMs connectTimeoutMs=$connectTimeoutMs" }
    }

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    // Created lazily so that test environments with no API key never spin up a CIO engine.
    private val lazyHttpClient = lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonConfig) }
            install(HttpTimeout) {
                requestTimeoutMillis = requestTimeoutMs
                socketTimeoutMillis = requestTimeoutMs
                connectTimeoutMillis = connectTimeoutMs
            }
        }
    }
    private val httpClient: HttpClient get() = lazyHttpClient.value

    override fun close() {
        if (lazyHttpClient.isInitialized()) lazyHttpClient.value.close()
    }

    suspend fun generateInsight(context: InsightContext): Either<DomainError, String> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
        val userPrompt = buildInsightPrompt(context)

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    OpenRouterInsightRequest(
                        model = model,
                        messages = listOf(
                            Message("system", INSIGHT_SYSTEM_PROMPT),
                            Message("user", userPrompt),
                        ),
                    )
                )
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            content.right()
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter generateInsight failed" }
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    private fun buildInsightPrompt(context: InsightContext): String = when (context) {
        is InsightContext.WordInsight -> buildWordInsightPrompt(context)
        is InsightContext.SentenceInsight -> buildSentenceInsightPrompt(context)
    }

    private fun buildWordInsightPrompt(ctx: InsightContext.WordInsight): String {
        val translationClause = if (ctx.translation != null) " (\"${ctx.translation}\")" else ""
        val rootClause = if (ctx.rootLetters != null) {
            "\nRoot: ${ctx.rootLetters}" + if (ctx.rootMeaning != null) " — meaning: \"${ctx.rootMeaning}\"." else "."
        } else ""
        val examplesClause = if (ctx.examples.isNotEmpty()) {
            "\nExample usage: " + ctx.examples.joinToString(" / ") { "\"$it\"" }
        } else ""
        return """Analyse the Arabic word: ${ctx.arabicText}$translationClause.
Part of speech: ${ctx.partOfSpeech}. Dialect: ${ctx.dialect}.$rootClause$examplesClause

Give concise linguistic insights. Lead with semantic disambiguation if this word is commonly confused with a near-synonym (different nuance, register, or dialect). Then cover relevant synonyms/antonyms, common learner mistakes, or register notes — only what is genuinely interesting for this specific word. Skip empty sections."""
    }

    @Suppress("LongMethod")
    private fun buildSentenceInsightPrompt(ctx: InsightContext.SentenceInsight): String {
        // allSentences is already the (possibly truncated) window built by AiInsightService.
        // If it has ≤ 10 items (truncation window), add a note.
        val truncationNote = if (ctx.allSentences.size <= 10) {
            "\nNote: context truncated to ±5 sentences around the target.\n"
        } else ""

        val targetListIdx = ctx.allSentences.indexOfFirst { (arabic, _) -> arabic == ctx.targetArabic }

        val lines = ctx.allSentences.mapIndexed { i, (arabic, translation) ->
            val translationPart = if (translation != null) " — $translation" else ""
            if (i == targetListIdx) {
                "→ ${ctx.targetIndex}. $arabic$translationPart    ← target"
            } else {
                "  ${i + 1}. $arabic$translationPart"
            }
        }.joinToString("\n")

        val modeInstruction = when (ctx.mode) {
            InsightMode.HOMEWORK ->
                "This is a student-authored sentence. Prioritise corrections and natural alternatives over analysis."

            InsightMode.READING ->
                "This is a native-authored sentence. Focus on nuance, notable constructions, and vocabulary choices."
        }

        val titleClause = ctx.textTitle ?: "untitled"
        return """Analyse sentence ${ctx.targetIndex} from the text "$titleClause" (${ctx.dialect}):
$truncationNote
Full text:
$lines

$modeInstruction

Be concise."""
    }

    private fun positiveLongEnv(name: String): Long? =
        System.getenv(name)?.toLongOrNull()?.takeIf { it > 0 }

    @Serializable
    private data class OpenRouterInsightRequest(
        val model: String,
        val messages: List<Message>,
    )

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class OpenRouterResponse(val choices: List<Choice>)

    @Serializable
    private data class Choice(val message: Message)


    private companion object {
        const val DEFAULT_AI_REQUEST_TIMEOUT_MS = 120_000L
        const val DEFAULT_AI_CONNECT_TIMEOUT_MS = 15_000L

        const val INSIGHT_SYSTEM_PROMPT =
            """You are an experienced Arabic language teacher and tandem partner, specialising in Tunisian Arabic and MSA.

Rules:
- Respond in English unless Arabic script is needed for examples
- Never add vocalization (tashkeel) to Arabic script
- Be concise and academic in tone — no motivational filler
- The user is between beginner and B1 level
- Do not break down every word unless it is the focus of the insight
- Return plain text with minimal markdown — bold for key terms, no headers"""
    }
}

const val LEGACY_SYSTEM_PROMPT = "You are a structured assistant as companion for an Arabic language teacher. " +
        "Return only valid JSON."
