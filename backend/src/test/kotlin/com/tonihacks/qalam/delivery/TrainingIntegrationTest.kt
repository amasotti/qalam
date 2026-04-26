package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TrainingIntegrationTest : BaseIntegrationTest() {

    private val katabJson =
        """{"arabicText":"كَتَبَ","translation":"to write","dialect":"MSA","difficulty":"BEGINNER"}"""

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement()
                    .execute("TRUNCATE TABLE training_sessions CASCADE; TRUNCATE TABLE words CASCADE; TRUNCATE TABLE arabic_roots CASCADE")
            }
        }

        // ── Helper ──────────────────────────────────────────────────────────────

        suspend fun createWord(client: io.ktor.client.HttpClient, body: String = katabJson): String {
            val resp = client.post("/api/v1/words") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            resp.status shouldBe HttpStatusCode.Created
            return Regex(""""id":"([^"]+)"""").find(resp.bodyAsText())!!.groupValues[1]
        }

        // ── Group 1: POST /api/v1/training/sessions ─────────────────────────────

        "POST /api/v1/training/sessions" - {
            "returns 422 when no words available" {
                testApp { client ->
                    val response = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"NEW","size":5}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                    response.bodyAsText() shouldContain "NOT_ENOUGH_WORDS"
                }
            }

            "creates session with words and returns 201" {
                testApp { client ->
                    createWord(client)
                    val response = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":1}""")
                    }
                    response.status shouldBe HttpStatusCode.Created
                    val body = response.bodyAsText()
                    body shouldContain "ACTIVE"
                    body shouldContain "كَتَبَ"
                }
            }

            "caps words at available count when size exceeds vocabulary" {
                testApp { client ->
                    createWord(client, """{"arabicText":"كَتَبَ","translation":"to write","dialect":"MSA"}""")
                    createWord(client, """{"arabicText":"قَرَأَ","translation":"to read","dialect":"MSA"}""")
                    createWord(client, """{"arabicText":"ذَهَبَ","translation":"to go","dialect":"MSA"}""")

                    val response = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":100}""")
                    }
                    response.status shouldBe HttpStatusCode.Created
                    val body = response.bodyAsText()
                    val words = Json.parseToJsonElement(body).jsonObject["words"]!!.jsonArray
                    words.size shouldBe 3
                }
            }
        }

        // ── Group 2: POST /api/v1/training/sessions/{id}/results ────────────────

        "POST /api/v1/training/sessions/{id}/results" - {
            "records CORRECT result and promotes mastery after 3 consecutive" {
                testApp { client ->
                    val wordId = createWord(client)

                    suspend fun runOneSession(): String {
                        val sessionResp = client.post("/api/v1/training/sessions") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"mode":"MIXED","size":1}""")
                        }
                        sessionResp.status shouldBe HttpStatusCode.Created
                        return Json.parseToJsonElement(sessionResp.bodyAsText())
                            .jsonObject["id"]!!.jsonPrimitive.content
                    }

                    // 3 sessions, each recording CORRECT for the same word
                    repeat(3) {
                        val sessionId = runOneSession()
                        val resultResp = client.post("/api/v1/training/sessions/$sessionId/results") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"wordId":"$wordId","result":"CORRECT"}""")
                        }
                        resultResp.status shouldBe HttpStatusCode.OK
                    }

                    // After 3 consecutive corrects, word should be promoted to LEARNING
                    // The 3rd call's response contains the promotion
                    // Verify via stats that the word is now in LEARNING
                    val statsResp = client.get("/api/v1/training/stats")
                    statsResp.status shouldBe HttpStatusCode.OK
                    statsResp.bodyAsText() shouldContain "LEARNING"
                }
            }

            "returns 409 when session already completed" {
                testApp { client ->
                    val wordId = createWord(client)

                    val sessionResp = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":1}""")
                    }
                    sessionResp.status shouldBe HttpStatusCode.Created
                    val sessionId = Json.parseToJsonElement(sessionResp.bodyAsText())
                        .jsonObject["id"]!!.jsonPrimitive.content

                    // Record a result so the session has data, then complete it
                    client.post("/api/v1/training/sessions/$sessionId/results") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId","result":"CORRECT"}""")
                    }
                    client.post("/api/v1/training/sessions/$sessionId/complete")

                    // Now try to record another result on the completed session
                    val response = client.post("/api/v1/training/sessions/$sessionId/results") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId","result":"CORRECT"}""")
                    }
                    response.status shouldBe HttpStatusCode.Conflict
                }
            }
        }

        // ── Group 3: POST /api/v1/training/sessions/{id}/complete ───────────────

        "POST /api/v1/training/sessions/{id}/complete" - {
            "returns summary with correct accuracy" {
                testApp { client ->
                    val wordId = createWord(client)

                    val sessionResp = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":1}""")
                    }
                    sessionResp.status shouldBe HttpStatusCode.Created
                    val sessionId = Json.parseToJsonElement(sessionResp.bodyAsText())
                        .jsonObject["id"]!!.jsonPrimitive.content

                    client.post("/api/v1/training/sessions/$sessionId/results") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId","result":"CORRECT"}""")
                    }

                    val completeResp = client.post("/api/v1/training/sessions/$sessionId/complete")
                    completeResp.status shouldBe HttpStatusCode.OK
                    val body = completeResp.bodyAsText()
                    body shouldContain """"correct":1"""
                    body shouldContain """"accuracy":1.0"""
                }
            }
        }

        // ── Group 4: GET /api/v1/training/stats ─────────────────────────────────

        "GET /api/v1/training/stats" - {
            "returns mastery distribution with at least one word" {
                testApp { client ->
                    createWord(client)
                    val response = client.get("/api/v1/training/stats")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "NEW"
                }
            }
        }

        // ── Group 5: enriched word data in session response ──────────────────────

        "GET /api/v1/training/sessions/{id} returns enriched word data" - {
            "word response includes root displayForm, examples, and relations" {
                testApp { client ->
                    // Create a root
                    val rootResp = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"كتب"}""")
                    }
                    rootResp.status shouldBe HttpStatusCode.Created
                    val rootBody = rootResp.bodyAsText()
                    val rootId = Regex(""""id":"([^"]+)"""").find(rootBody)!!.groupValues[1]
                    val rootDisplayForm = Json.parseToJsonElement(rootBody).jsonObject["displayForm"]!!.jsonPrimitive.content

                    // Create two words (need both for a relation)
                    val word1Id = createWord(
                        client,
                        """{"arabicText":"كَتَبَ","translation":"to write","dialect":"MSA","rootId":"$rootId","notes":"common verb"}""",
                    )
                    val word2Id = createWord(
                        client,
                        """{"arabicText":"كِتَابٌ","translation":"book","dialect":"MSA"}""",
                    )

                    // Add an example to word1
                    client.post("/api/v1/words/$word1Id/examples") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabic":"كَتَبَ الطَّالِبُ","translation":"The student wrote"}""")
                    }.status shouldBe HttpStatusCode.Created

                    // Add a relation: word1 RELATED word2
                    client.post("/api/v1/words/$word1Id/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"$word2Id","relationType":"RELATED"}""")
                    }.status shouldBe HttpStatusCode.Created

                    // Create a training session
                    val sessionResp = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":10}""")
                    }
                    sessionResp.status shouldBe HttpStatusCode.Created
                    val sessionId = Json.parseToJsonElement(sessionResp.bodyAsText())
                        .jsonObject["id"]!!.jsonPrimitive.content

                    // Fetch session and assert enriched data on word1's entry
                    val getResp = client.get("/api/v1/training/sessions/$sessionId")
                    getResp.status shouldBe HttpStatusCode.OK
                    val body = getResp.bodyAsText()

                    // root
                    body shouldContain rootDisplayForm
                    // notes
                    body shouldContain "common verb"
                    // example
                    body shouldContain "كَتَبَ الطَّالِبُ"
                    body shouldContain "The student wrote"
                    // relation
                    body shouldContain "كِتَابٌ"
                    body shouldContain "RELATED"
                }
            }
        }
    }
}
