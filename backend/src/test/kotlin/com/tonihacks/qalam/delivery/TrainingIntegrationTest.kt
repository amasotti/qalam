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
                    .execute("TRUNCATE TABLE training_sessions CASCADE; TRUNCATE TABLE words CASCADE")
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
    }
}
