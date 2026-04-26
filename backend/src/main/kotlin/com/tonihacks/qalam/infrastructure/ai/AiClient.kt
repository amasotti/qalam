package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.sentence.TokenInputDto
import com.tonihacks.qalam.delivery.dto.word.AiExampleSentence
import com.tonihacks.qalam.delivery.dto.word.WordAnalysisResponse
import com.tonihacks.qalam.delivery.dto.word.WordEnrichmentSuggestion
import com.tonihacks.qalam.domain.ai.InsightContext
import com.tonihacks.qalam.domain.ai.InsightMode
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Word
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

class AiClient : java.io.Closeable {

    private var apiKey: String? = System.getenv("OPENROUTER_API_KEY")

    init {
        if (apiKey.isNullOrBlank()) {
            println("Warning: OPENROUTER_API_KEY is not set. AI features will be unavailable.")
        }
    }

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    // Created lazily so that test environments with no API key never spin up a CIO engine.
    private val lazyHttpClient = lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(jsonConfig) }
        }
    }
    private val httpClient: HttpClient get() = lazyHttpClient.value

    override fun close() {
        if (lazyHttpClient.isInitialized()) lazyHttpClient.value.close()
    }

    suspend fun generateExamples(
        arabicText: String,
        translation: String?,
    ): Either<DomainError, List<AiExampleSentence>> {
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
- "transliteration": practical Latin transliteration of the token
- "translation": concise English gloss for the token

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

    suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini"
        val prompt = """Analyze the Arabic word "$arabicText" and return a JSON object with:
- "transliteration": Latin transliteration using practical chat-alphabet style
- "translation": English translation or meaning
- "partOfSpeech": one of NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION, PARTICLE, INTERJECTION, CONJUNCTION, PRONOUN, UNKNOWN
- "rootLetters": Arabic trilateral/quadrilateral root consonants separated by dashes (e.g. "ك-ت-ب"), or null if not applicable
- "exampleSentence": object with "arabic", "transliteration", "translation" keys showing the word in a short sentence, or null"""

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

            val payload = jsonConfig.decodeFromString<WordAnalysisPayload>(content)
            WordAnalysisResponse(
                arabicText = arabicText,
                transliteration = payload.transliteration,
                translation = payload.translation,
                partOfSpeech = payload.partOfSpeech,
                rootLetters = payload.rootLetters,
                exampleSentence = payload.exampleSentence,
            ).right()
        } catch (_: Exception) {
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    // TODO Task 9: implement full prompt + JSON parsing
    suspend fun enrichWord(word: Word): Either<DomainError, WordEnrichmentSuggestion> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()
        return DomainError.InvalidInput("enrichWord not yet implemented — coming in Task 9").left()
    }

    suspend fun generateInsight(context: InsightContext): Either<DomainError, String> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini"
        val userPrompt = buildInsightPrompt(context)

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterInsightRequest(
                    model = model,
                    messages = listOf(
                        Message("system", INSIGHT_SYSTEM_PROMPT),
                        Message("user", userPrompt),
                    ),
                ))
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            content.right()
        } catch (_: Exception) {
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

    private fun buildPrompt(arabicText: String, translation: String?): String {
        val translationHint = if (!translation.isNullOrBlank()) " (meaning: \"$translation\")" else ""
        return """Given the Arabic word "$arabicText"$translationHint, provide exactly 2 example sentences.
One sentence should be in MSA and the other in Tunisian Arabic dialect.
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
    private data class OpenRouterInsightRequest(
        val model: String,
        val messages: List<Message>,
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
    private data class ExamplesPayload(val examples: List<AiExampleSentence>)

    @Serializable
    private data class WordAnalysisPayload(
        val transliteration: String? = null,
        val translation: String? = null,
        val partOfSpeech: String? = null,
        val rootLetters: String? = null,
        val exampleSentence: AiExampleSentence? = null,
    )

    @Serializable
    private data class TokensPayload(val tokens: List<TokenInputDto>)

    @Serializable
    private data class TransliterationPayload(val transliteration: String)

    private companion object {
        const val SYSTEM_PROMPT = "You are a structured assistant as companion for an Arabic language teacher. " +
                "Return only valid JSON."

        const val INSIGHT_SYSTEM_PROMPT = """You are an experienced Arabic language teacher and tandem partner, specialising in Tunisian Arabic and MSA.

Rules:
- Respond in English unless Arabic script is needed for examples
- Never add vocalization (tashkeel) to Arabic script
- Be concise and academic in tone — no motivational filler
- The user is between beginner and B1 level
- Do not break down every word unless it is the focus of the insight
- Return plain text with minimal markdown — bold for key terms, no headers"""
    }
}
