package com.tonihacks.qalam.infrastructure.ai

import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.PartOfSpeech
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

internal val wordSuggestionSchema: JsonObject = buildJsonObject {
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
                    add("arabicText")
                    add("transliteration")
                    add("translation")
                    add("partOfSpeech")
                    add("difficulty")
                    add("dialect")
                }
                putJsonObject("properties") {
                    putJsonObject("arabicText") { put("type", "string") }
                    putJsonObject("transliteration") { put("type", "string") }
                    putJsonObject("translation") { put("type", "string") }
                    putJsonObject("partOfSpeech") {
                        put("type", "string")
                        putJsonArray("enum") { PartOfSpeech.entries.forEach { add(it.name) } }
                    }
                    putJsonObject("difficulty") {
                        put("type", "string")
                        putJsonArray("enum") { Difficulty.entries.forEach { add(it.name) } }
                    }
                    putJsonObject("dialect") {
                        put("type", "string")
                        putJsonArray("enum") { Dialect.entries.forEach { add(it.name) } }
                    }
                }
            }
        }
    }
}

@Serializable
internal data class OpenRouterVocabularySuggestion(
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val partOfSpeech: String,
    val difficulty: String,
    val dialect: String,
)

internal fun parseWordListSuggestions(content: String, json: Json): List<OpenRouterVocabularySuggestion> {
    val root = runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return emptyList()

    val suggestions = when (root) {
        is JsonArray -> root
        is JsonObject -> (root["suggestions"] as? JsonArray)
            ?: root.values.firstOrNull { it is JsonArray } as? JsonArray
        else -> null
    }

    return suggestions?.mapNotNull { element ->
        runCatching { json.decodeFromJsonElement<OpenRouterVocabularySuggestion>(element) }.getOrNull() }
        ?.filter { it.arabicText.isNotBlank() }
        ?: emptyList()
}
