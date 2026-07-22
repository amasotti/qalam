package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProductionPracticeIntegrationTest : BaseIntegrationTest() {
    init {
        beforeEach {
            postgres.createConnection("").use { connection ->
                connection.createStatement().execute("TRUNCATE TABLE words CASCADE; TRUNCATE TABLE arabic_roots CASCADE")
            }
        }

        "GET /api/v1/production-practice/prompt" - {
            "returns two nouns, two verbs, and three further unique words" {
                testApp { client ->
                    seedWords(client)

                    val response = client.get("/api/v1/production-practice/prompt")

                    response.status shouldBe HttpStatusCode.OK
                    val words = Json.parseToJsonElement(response.bodyAsText()).jsonObject["words"]!!.jsonArray
                    words.size shouldBe 7
                    words.count { it.jsonObject["partOfSpeech"]!!.jsonPrimitive.content == "NOUN" } shouldBe 2
                    words.count { it.jsonObject["partOfSpeech"]!!.jsonPrimitive.content == "VERB" } shouldBe 2
                    words.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet().size shouldBe 7
                }
            }
        }

        "POST /api/v1/production-practice/reviews" - {
            "rejects malformed target IDs before invoking AI" {
                testApp { client ->
                    val response = client.post("/api/v1/production-practice/reviews") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """{
                              "sentence":"أنا أكتب.",
                              "targetWordIds":["not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid", "not-a-uuid"],
                              "usedWordIds":["not-a-uuid", "also-not-a-uuid"]
                            }""".trimIndent(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                    response.bodyAsText() shouldBe
                        """{"error":"targetWordIds contains an invalid UUID: 'not-a-uuid'","code":"INVALID_INPUT"}"""
                }
            }
        }
    }

    private suspend fun seedWords(client: HttpClient) {
        createWord(client, "كِتَاب", "book", "NOUN")
        createWord(client, "بَيْت", "house", "NOUN")
        createWord(client, "كَتَبَ", "to write", "VERB")
        createWord(client, "ذَهَبَ", "to go", "VERB")
        createWord(client, "كَبِير", "big", "ADJECTIVE")
        createWord(client, "سَرِيعًا", "quickly", "ADVERB")
        createWord(client, "هُوَ", "he", "PRONOUN")
    }

    private suspend fun createWord(client: HttpClient, arabicText: String, translation: String, partOfSpeech: String) {
        val response = client.post("/api/v1/words") {
            contentType(ContentType.Application.Json)
            setBody("""{"arabicText":"$arabicText","translation":"$translation","partOfSpeech":"$partOfSpeech","dialect":"MSA"}""")
        }
        response.status shouldBe HttpStatusCode.Created
    }
}
