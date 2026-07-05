package com.tonihacks.qalam

import com.tonihacks.qalam.infrastructure.ai.parseListSuggestions
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

// Regression coverage for the shape-tolerant AI suggestion parser.
// The model (under response_format=json_object) does not reliably wrap results in
// {"suggestions":[...]}, so parsing must never throw on shape variance.
class ParseListSuggestionsTest : FreeSpec({

    val json = Json { ignoreUnknownKeys = true }

    "decodes the documented {\"suggestions\":[...]} wrapper" {
        val content = """
            {"suggestions":[
              {"arabicText":"أحمر","transliteration":"ahmar","translation":"red","partOfSpeech":"ADJECTIVE","difficulty":"BEGINNER"}
            ]}
        """.trimIndent()
        val result = parseListSuggestions(content, json)
        result.map { it.arabicText } shouldBe listOf("أحمر")
        result.first().translation shouldBe "red"
    }

    "decodes a bare top-level array" {
        val content = """[{"arabicText":"أزرق","translation":"blue"}]"""
        parseListSuggestions(content, json).map { it.arabicText } shouldBe listOf("أزرق")
    }

    "finds the array under a differently-named wrapper key" {
        val content = """{"words":[{"arabicText":"أخضر"},{"arabicText":"أصفر"}]}"""
        parseListSuggestions(content, json).map { it.arabicText } shouldBe listOf("أخضر", "أصفر")
    }

    "skips elements missing the required arabicText" {
        val content = """{"suggestions":[{"translation":"no arabic"},{"arabicText":"أسود"}]}"""
        parseListSuggestions(content, json).map { it.arabicText } shouldBe listOf("أسود")
    }

    "returns empty for non-JSON content" {
        parseListSuggestions("sorry, I cannot help with that", json) shouldBe emptyList()
    }

    "returns empty for a JSON object with no array" {
        parseListSuggestions("""{"note":"none"}""", json) shouldBe emptyList()
    }
})
