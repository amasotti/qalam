package com.tonihacks.qalam.infrastructure.ai

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenRouterSchemasTest : FreeSpec({
    val json = Json { ignoreUnknownKeys = true }

    "parses the documented suggestions wrapper" {
        val content = """
            {"suggestions":[
              {"arabicText":"أحمر","transliteration":"a7mar","translation":"red","partOfSpeech":"ADJECTIVE","difficulty":"BEGINNER","dialect":"MSA"}
            ]}
        """.trimIndent()

        val result = parseAiWordSuggestions(content, json)

        result.map { it.arabicText } shouldBe listOf("أحمر")
        result.single().dialect shouldBe "MSA"
    }

    "parses a bare top-level array" {
        val content = """[{"arabicText":"أزرق","transliteration":"azraq","translation":"blue","partOfSpeech":"ADJECTIVE","difficulty":"BEGINNER","dialect":"MSA"}]"""

        parseAiWordSuggestions(content, json).map { it.arabicText } shouldBe listOf("أزرق")
    }

    "defaults missing difficulty to intermediate" {
        val content = """{"suggestions":[{"arabicText":"أزرق","transliteration":"azraq","translation":"blue","partOfSpeech":"ADJECTIVE","dialect":"MSA"}]}"""

        parseAiWordSuggestions(content, json).single().difficulty shouldBe "INTERMEDIATE"
    }

    "returns an empty list for malformed or incomplete suggestions" {
        parseAiWordSuggestions("not json", json) shouldBe emptyList()
        parseAiWordSuggestions("""{"suggestions":[{"arabicText":"أسود"}]}""", json) shouldBe emptyList()
    }

    "structured schema satisfies OpenRouter strict-mode invariants" {
        fun assertStrict(obj: JsonObject) {
            obj["additionalProperties"]!!.jsonPrimitive.boolean shouldBe false
            val properties = obj["properties"]!!.jsonObject.keys
            val required = obj["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
            required shouldBe properties
        }

        assertStrict(wordSuggestionSchema)
        assertStrict(wordSuggestionSchema["properties"]!!.jsonObject["suggestions"]!!
            .jsonObject["items"]!!.jsonObject)
    }
})
