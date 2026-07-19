package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.sentence.TokenInputDto
import com.tonihacks.qalam.delivery.dto.word.AiExampleSentence
import com.tonihacks.qalam.delivery.dto.word.WordAnalysisResponse
import com.tonihacks.qalam.delivery.dto.word.WordEnrichmentSuggestion
import com.tonihacks.qalam.delivery.dto.wordlist.AiListWordSuggestion
import com.tonihacks.qalam.domain.ai.InsightContext
import com.tonihacks.qalam.domain.ai.InsightMode
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Word
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

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

    suspend fun generateExamples(
        arabicText: String,
        translation: String?,
    ): Either<DomainError, List<AiExampleSentence>> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val prompt = buildPrompt(arabicText, translation)

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"

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
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter generateExamples failed for '$arabicText'" }
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    suspend fun autoTokenize(arabicText: String): Either<DomainError, List<TokenInputDto>> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
        val prompt = """Split this Arabic sentence into individual word tokens.
Return a JSON object with a "tokens" array. Each element must have:
- "position": integer index starting at 0
- "arabic": the Arabic word
- "transliteration": practical Latin transliteration of the token. Use chat Arabic conventions (e.g. "3" for ع, "7" for ح) and avoid diacritics.
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
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter autoTokenize failed for '$arabicText'" }
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    suspend fun transliterate(arabicText: String): Either<DomainError, String> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
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
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter transliterate failed for '$arabicText'" }
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    suspend fun analyzeWord(arabicText: String): Either<DomainError, WordAnalysisResponse> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
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
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter analyzeWord failed for '$arabicText'" }
            DomainError.InvalidInput("AI request failed").left()
        }
    }

    suspend fun enrichWord(word: Word): Either<DomainError, WordEnrichmentSuggestion> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
        val dialectClause = ", dialect: ${word.dialect}"
        val transliterationClause = word.transliteration?.let { " [$it]" } ?: ""
        val prompt = """Analyse the Arabic word: ${word.arabicText}$transliterationClause (${word.translation ?: "no translation"}, partOfSpeech: ${word.partOfSpeech}$dialectClause)

Provide:
- gender (MASCULINE/FEMININE, nouns only, null for others)
- verbDetails (verbs only, null for others): verbForm (Roman numeral I-X), pastPattern and presentPattern (Form I only: fa3ala/fa3ila/fa3ula and yaf3ulu/yaf3ilu/yaf3alu), weaknessType (SOUND/ASSIMILATED/HOLLOW/GEMINATE/DEFECTIVE/DOUBLY_WEAK)
- plurals: list of plural forms with types (SOUND_MASC/SOUND_FEM/BROKEN/PAUCAL/COLLECTIVE/OTHER)
- relations: 2–4 high-value entries — near-synonyms with register/nuance difference, strong antonyms, or words from the same semantic field. Avoid generic filler. Each entry: arabicText (unvoweled), transliteration (practical chat-style), translation (concise English gloss), relationType (SYNONYM/ANTONYM/RELATED)
- notes: brief mnemonic or usage note in English, focusing on common learner confusions, collocations, or register constraints (null if nothing genuinely useful)

Respond ONLY with this JSON structure:
{
  "gender": "MASCULINE" | "FEMININE" | null,
  "verbDetails": {"verbForm": "I", "pastPattern": "fa3ala", "presentPattern": "yaf3ulu", "weaknessType": "SOUND"} | null,
  "plurals": [{"pluralForm": "...", "pluralType": "BROKEN"}],
  "relations": [{"arabicText": "...", "transliteration": "...", "translation": "...", "relationType": "SYNONYM"}],
  "notes": "..." | null
}"""

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        Message("system", ENRICH_SYSTEM_PROMPT),
                        Message("user", prompt),
                    ),
                    responseFormat = ResponseFormat("json_object"),
                ))
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            jsonConfig.decodeFromString<WordEnrichmentSuggestion>(content).right()
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter enrichWord failed for '${word.arabicText}'" }
            DomainError.InvalidInput("AI enrichment request failed").left()
        }
    }

    suspend fun suggestWordsForList(
        title: String,
        description: String?,
        existing: List<Word>,
    ): Either<DomainError, List<AiListWordSuggestion>> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
        val descriptionClause = description?.takeIf { it.isNotBlank() }?.let { "\nDescription: $it" } ?: ""
        val existingClause = if (existing.isEmpty()) {
            "\nThe list is currently empty."
        } else {
            "\nWords already in the list (do NOT suggest these or close variants):\n" +
                existing.joinToString("\n") { "- ${it.arabicText}${it.translation?.let { t -> " — $t" } ?: ""}" }
        }
        val prompt = """Suggest between 5 and 10 NEW Arabic vocabulary words that belong in this themed list.
List title: $title$descriptionClause$existingClause

Each suggestion must fit the theme and must not duplicate an existing word. Prefer common, useful words.
For each word provide:
- arabicText: the word in Arabic script, unvoweled
- transliteration: practical chat-alphabet style
- translation: concise English gloss
- partOfSpeech: one of NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION, PARTICLE, INTERJECTION, CONJUNCTION, PRONOUN, UNKNOWN
- difficulty: one of BEGINNER, INTERMEDIATE, ADVANCED

Respond ONLY with this JSON structure:
{
  "suggestions": [
    {"arabicText": "...", "transliteration": "...", "translation": "...", "partOfSpeech": "NOUN", "difficulty": "BEGINNER"}
  ]
}"""

        return try {
            val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        Message("system", ENRICH_SYSTEM_PROMPT),
                        Message("user", prompt),
                    ),
                    // Structured output: force the model to emit exactly this schema.
                    responseFormat = ResponseFormat(
                        type = "json_schema",
                        jsonSchema = JsonSchemaSpec("word_suggestions", strict = true, schema = WORD_SUGGESTIONS_SCHEMA),
                    ),
                    // Hard-fail if the routed provider can't honour structured outputs.
                    provider = ProviderPreferences(requireParameters = true),
                ))
            }

            val body = response.body<OpenRouterResponse>()
            val content = body.choices.firstOrNull()?.message?.content
                ?: return DomainError.InvalidInput("Empty AI response").left()

            // Structured output makes the shape reliable; the tolerant parser stays as a
            // defence-in-depth backstop so any residual drift degrades to empty, never a 400.
            parseListSuggestions(content, jsonConfig).right()
        } catch (e: Exception) {
            log.warn(e) { "OpenRouter suggestWordsForList failed for '$title'" }
            DomainError.InvalidInput("AI list suggestion request failed").left()
        }
    }

    suspend fun generateInsight(context: InsightContext): Either<DomainError, String> {
        if (apiKey.isNullOrBlank()) return DomainError.AiNotConfigured.left()

        val model = System.getenv("OPENROUTER_MODEL") ?: "google/gemini-2.5-flash-lite"
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

    private fun buildPrompt(arabicText: String, translation: String?): String {
        val translationHint = if (!translation.isNullOrBlank()) " (meaning: \"$translation\")" else ""
        return """Given the Arabic word "$arabicText"$translationHint, provide exactly 2 example sentences.
One sentence should be in MSA and the other in Tunisian Arabic dialect.
Return a JSON object with an "examples" array. Each element must have:
- "arabic": the sentence in Arabic script
- "transliteration": Latin transliteration
- "translation": English translation"""
    }

    private fun positiveLongEnv(name: String): Long? =
        System.getenv(name)?.toLongOrNull()?.takeIf { it > 0 }

    @Serializable
    private data class OpenRouterRequest(
        val model: String,
        val messages: List<Message>,
        @SerialName("response_format") val responseFormat: ResponseFormat,
        // Present only when we need a specific capability (e.g. structured outputs). Omitted otherwise.
        val provider: ProviderPreferences? = null,
    )

    @Serializable
    private data class OpenRouterInsightRequest(
        val model: String,
        val messages: List<Message>,
    )

    @Serializable
    private data class ResponseFormat(
        val type: String,
        // Only set for type == "json_schema"; omitted for plain "json_object".
        @SerialName("json_schema") val jsonSchema: JsonSchemaSpec? = null,
    )

    @Serializable
    private data class JsonSchemaSpec(
        val name: String,
        val strict: Boolean,
        val schema: JsonObject,
    )

    // require_parameters=true makes OpenRouter route ONLY to providers that honour the requested
    // parameters (here: structured outputs) and hard-fail otherwise — no silent downgrade.
    @Serializable
    private data class ProviderPreferences(
        @SerialName("require_parameters") val requireParameters: Boolean,
    )

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
        const val DEFAULT_AI_REQUEST_TIMEOUT_MS = 120_000L
        const val DEFAULT_AI_CONNECT_TIMEOUT_MS = 15_000L

        const val SYSTEM_PROMPT = "You are a structured assistant as companion for an Arabic language teacher. " +
                "Return only valid JSON."

        const val ENRICH_SYSTEM_PROMPT = "You are an expert Arabic linguist. Given an Arabic word, provide structured linguistic enrichment in JSON format. Always respond with valid JSON."

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

private val PART_OF_SPEECH_VALUES = listOf(
    "NOUN", "VERB", "ADJECTIVE", "ADVERB", "PREPOSITION",
    "PARTICLE", "INTERJECTION", "CONJUNCTION", "PRONOUN", "UNKNOWN",
)
private val DIFFICULTY_VALUES = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")

/**
 * Strict JSON Schema for word-list suggestions, sent as OpenRouter structured output.
 * Strict mode requires additionalProperties=false and every property listed in "required".
 */
internal val WORD_SUGGESTIONS_SCHEMA: JsonObject = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") { add("suggestions") }
    putJsonObject("properties") {
        putJsonObject("suggestions") {
            put("type", "array")
            putJsonObject("items") {
                put("type", "object")
                put("additionalProperties", false)
                putJsonArray("required") {
                    add("arabicText"); add("transliteration"); add("translation")
                    add("partOfSpeech"); add("difficulty")
                }
                putJsonObject("properties") {
                    putJsonObject("arabicText") { put("type", "string") }
                    putJsonObject("transliteration") { put("type", "string") }
                    putJsonObject("translation") { put("type", "string") }
                    putJsonObject("partOfSpeech") {
                        put("type", "string")
                        putJsonArray("enum") { PART_OF_SPEECH_VALUES.forEach { add(it) } }
                    }
                    putJsonObject("difficulty") {
                        put("type", "string")
                        putJsonArray("enum") { DIFFICULTY_VALUES.forEach { add(it) } }
                    }
                }
            }
        }
    }
}

/**
 * Extract word suggestions from an AI response, tolerant of shape variance.
 *
 * `response_format=json_object` guarantees valid JSON but not our exact schema: the model may
 * return `{"suggestions":[...]}`, a differently-named wrapper, or a bare top-level array. This
 * finds the first array (preferring the "suggestions" key), decodes each element leniently, and
 * skips anything malformed — so a shape mismatch degrades to an empty list instead of throwing.
 */
internal fun parseListSuggestions(content: String, json: Json): List<AiListWordSuggestion> {
    val root = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return emptyList()
    val array: JsonArray? = when (root) {
        is JsonArray -> root
        is JsonObject -> (root["suggestions"] as? JsonArray)
            ?: root.values.firstOrNull { it is JsonArray } as? JsonArray
        else -> null
    }
    return array
        ?.mapNotNull { element -> runCatching { json.decodeFromJsonElement<AiListWordSuggestion>(element) }.getOrNull() }
        ?.filter { it.arabicText.isNotBlank() }
        ?: emptyList()
}
