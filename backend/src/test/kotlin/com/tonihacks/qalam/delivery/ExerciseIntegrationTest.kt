package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ExerciseIntegrationTest : BaseIntegrationTest() {

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement()
                    .execute(
                        "TRUNCATE TABLE exercise_sessions CASCADE; " +
                            "TRUNCATE TABLE training_sessions CASCADE; " +
                            "TRUNCATE TABLE words CASCADE; " +
                            "TRUNCATE TABLE arabic_roots CASCADE",
                    )
            }
        }

        suspend fun createWord(client: HttpClient, arabic: String, translation: String): String {
            val response = client.post("/api/v1/words") {
                contentType(ContentType.Application.Json)
                setBody("""{"arabicText":"$arabic","translation":"$translation","dialect":"MSA"}""")
            }
            response.status shouldBe HttpStatusCode.Created
            return Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["id"]!!.jsonPrimitive.content
        }

        suspend fun seedWords(client: HttpClient) {
            createWord(client, "كِتَابٌ", "book")
            createWord(client, "بَيْتٌ", "house")
            createWord(client, "بَابٌ", "door")
            createWord(client, "مَدْرَسَةٌ", "school")
        }

        fun JsonObject.items(): JsonArray = this["items"]!!.jsonArray

        "POST /api/v1/exercise-sessions" - {
            "creates multiple-choice session with stable options" {
                testApp { client ->
                    seedWords(client)

                    val response = client.post("/api/v1/exercise-sessions") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """{
                              "mode":"MIXED",
                              "size":1,
                              "exerciseTypes":["MULTIPLE_CHOICE_MEANING"],
                              "optionCount":4
                            }""".trimIndent(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.Created
                    val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val items = body.items()
                    items.size shouldBe 1

                    val item = items.first().jsonObject
                    item["type"]!!.jsonPrimitive.content shouldBe "MULTIPLE_CHOICE_MEANING"
                    item["prompt"]!!.jsonObject["kind"]!!.jsonPrimitive.content shouldBe "ARABIC_WORD"
                    item["options"]!!.jsonArray.size shouldBe 4

                    val sessionId = body["id"]!!.jsonPrimitive.content
                    val getResponse = client.get("/api/v1/exercise-sessions/$sessionId")
                    getResponse.status shouldBe HttpStatusCode.OK
                    getResponse.bodyAsText() shouldContain sessionId
                }
            }

            "creates confusable Arabic session with translation prompt" {
                testApp { client ->
                    seedWords(client)

                    val response = client.post("/api/v1/exercise-sessions") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """{
                              "mode":"MIXED",
                              "size":1,
                              "exerciseTypes":["CONFUSABLE_ARABIC"],
                              "optionCount":3
                            }""".trimIndent(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.Created
                    val item = Json.parseToJsonElement(response.bodyAsText())
                        .jsonObject.items().first().jsonObject
                    item["type"]!!.jsonPrimitive.content shouldBe "CONFUSABLE_ARABIC"
                    item["prompt"]!!.jsonObject["kind"]!!.jsonPrimitive.content shouldBe "TRANSLATION"
                    item["options"]!!.jsonArray.size shouldBe 3
                }
            }
        }

        "POST /api/v1/exercise-sessions/{id}/answers" - {
            "evaluates correct and wrong selected options" {
                testApp { client ->
                    seedWords(client)

                    val session = createExerciseSession(client)
                    val sessionId = session["id"]!!.jsonPrimitive.content
                    val firstItem = session.items().first().jsonObject
                    val itemId = firstItem["itemId"]!!.jsonPrimitive.content
                    val targetWordId = firstItem["wordId"]!!.jsonPrimitive.content
                    val correctOption = firstItem["options"]!!.jsonArray
                        .first { it.jsonObject["wordId"]!!.jsonPrimitive.content == targetWordId }
                        .jsonObject
                    val wrongOption = firstItem["options"]!!.jsonArray
                        .first { it.jsonObject["wordId"]!!.jsonPrimitive.content != targetWordId }
                        .jsonObject

                    val wrongResponse = client.post("/api/v1/exercise-sessions/$sessionId/answers") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"itemId":"$itemId","selectedOptionId":"${wrongOption["optionId"]!!.jsonPrimitive.content}"}""")
                    }
                    wrongResponse.status shouldBe HttpStatusCode.OK
                    wrongResponse.bodyAsText() shouldContain """"result":"INCORRECT""""

                    val repeatedResponse = client.post("/api/v1/exercise-sessions/$sessionId/answers") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"itemId":"$itemId","selectedOptionId":"${correctOption["optionId"]!!.jsonPrimitive.content}"}""")
                    }
                    repeatedResponse.status shouldBe HttpStatusCode.Conflict
                }
            }

            "promotes mastery through shared word progress" {
                testApp { client ->
                    seedWords(client)

                    repeat(3) {
                        val session = createExerciseSession(client, size = 4)
                        val sessionId = session["id"]!!.jsonPrimitive.content
                        session.items().forEach { rawItem ->
                            val item = rawItem.jsonObject
                            val itemId = item["itemId"]!!.jsonPrimitive.content
                            val targetWordId = item["wordId"]!!.jsonPrimitive.content
                            val correctOptionId = item["options"]!!.jsonArray
                                .first { it.jsonObject["wordId"]!!.jsonPrimitive.content == targetWordId }
                                .jsonObject["optionId"]!!.jsonPrimitive.content

                            val answer = client.post("/api/v1/exercise-sessions/$sessionId/answers") {
                                contentType(ContentType.Application.Json)
                                setBody("""{"itemId":"$itemId","selectedOptionId":"$correctOptionId"}""")
                            }
                            answer.status shouldBe HttpStatusCode.OK
                        }
                    }

                    val stats = client.get("/api/v1/training/stats")
                    stats.status shouldBe HttpStatusCode.OK
                    stats.bodyAsText() shouldContain "LEARNING"
                }
            }
        }

        "POST /api/v1/exercise-sessions/{id}/complete" - {
            "returns summary with skipped unanswered items" {
                testApp { client ->
                    seedWords(client)
                    val session = createExerciseSession(client, size = 2)
                    val sessionId = session["id"]!!.jsonPrimitive.content

                    val complete = client.post("/api/v1/exercise-sessions/$sessionId/complete")
                    complete.status shouldBe HttpStatusCode.OK
                    val body = complete.bodyAsText()
                    body shouldContain """"totalItems":2"""
                    body shouldContain """"skipped":2"""
                }
            }
        }
    }

    private suspend fun createExerciseSession(client: HttpClient, size: Int = 1): JsonObject {
        val response = client.post("/api/v1/exercise-sessions") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "mode":"MIXED",
                  "size":$size,
                  "exerciseTypes":["MULTIPLE_CHOICE_MEANING"],
                  "optionCount":4
                }""".trimIndent(),
            )
        }
        response.status shouldBe HttpStatusCode.Created
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }
}
