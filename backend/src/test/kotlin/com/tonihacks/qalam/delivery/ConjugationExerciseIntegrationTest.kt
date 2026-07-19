package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ConjugationExerciseIntegrationTest : BaseIntegrationTest() {
    init {
        beforeEach {
            postgres.createConnection("").use { connection ->
                connection.createStatement().execute(
                    "TRUNCATE TABLE conjugation_exercise_sessions CASCADE; " +
                        "TRUNCATE TABLE words CASCADE; TRUNCATE TABLE arabic_roots CASCADE",
                )
            }
        }

        "conjugation exercise sessions" - {
            "create, load, answer, complete, and list without leaking mappings before answer" {
                testApp { client ->
                    seedConjugatableVerbs(client)
                    val eligibility = client.get("/api/v1/conjugation-exercise-sessions/eligibility?mode=MIXED")
                    eligibility.status shouldBe HttpStatusCode.OK
                    eligibility.json()["availableVerbs"]!!.jsonPrimitive.content.toInt() shouldBe 3
                    val created = client.post("/api/v1/conjugation-exercise-sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":3,"tense":"PRESENT","voice":"ACTIVE"}""")
                    }
                    created.status shouldBe HttpStatusCode.Created
                    val session = created.json()
                    val sessionId = requireNotNull(session["id"]) { session.toString() }.jsonPrimitive.content
                    val firstItem = session["items"]!!.jsonArray.first().jsonObject
                    firstItem["correctMappings"]?.jsonPrimitive?.contentOrNull shouldBe null
                    firstItem["submittedMappings"]?.jsonPrimitive?.contentOrNull shouldBe null

                    val forms = firstItem["forms"]!!.jsonArray
                    val labels = firstItem["labels"]!!.jsonArray
                    val mappings = forms.indices.joinToString(",") { index ->
                        """{"formId":"${forms[index].jsonObject["formId"]!!.jsonPrimitive.content}","labelId":"${labels[index].jsonObject["labelId"]!!.jsonPrimitive.content}"}"""
                    }
                    val answer = client.post("/api/v1/conjugation-exercise-sessions/$sessionId/answers") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"itemId":"${firstItem["itemId"]!!.jsonPrimitive.content}","mappings":[$mappings]}""")
                    }
                    answer.status shouldBe HttpStatusCode.OK
                    answer.json()["correctMappings"]!!.jsonArray.size shouldBe 4

                    val repeated = client.post("/api/v1/conjugation-exercise-sessions/$sessionId/answers") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"itemId":"${firstItem["itemId"]!!.jsonPrimitive.content}","mappings":[$mappings]}""")
                    }
                    repeated.status shouldBe HttpStatusCode.Conflict

                    val loaded = client.get("/api/v1/conjugation-exercise-sessions/$sessionId")
                    loaded.status shouldBe HttpStatusCode.OK
                    loaded.json()["items"]!!.jsonArray.first().jsonObject["correctMappings"]!!.jsonArray.size shouldBe 4

                    val completed = client.post("/api/v1/conjugation-exercise-sessions/$sessionId/complete")
                    completed.status shouldBe HttpStatusCode.OK
                    completed.json()["skipped"]!!.jsonPrimitive.content.toInt() shouldBe 2

                    val listed = client.get("/api/v1/conjugation-exercise-sessions?page=1&size=20")
                    listed.status shouldBe HttpStatusCode.OK
                    listed.json()["total"]!!.jsonPrimitive.content.toLong() shouldBe 1L
                }
            }
        }
    }

    private suspend fun seedConjugatableVerbs(client: HttpClient) {
        val root = client.post("/api/v1/roots") {
            contentType(ContentType.Application.Json)
            setBody("""{"root":"كتب"}""")
        }
        root.status shouldBe HttpStatusCode.Created
        val rootId = root.json()["id"]!!.jsonPrimitive.content
        listOf("كَتَبَ", "دَرَسَ", "فَتَحَ").forEach { arabic ->
            val word = client.post("/api/v1/words") {
                contentType(ContentType.Application.Json)
                setBody("""{"arabicText":"$arabic","translation":"to test","partOfSpeech":"VERB","dialect":"MSA","rootId":"$rootId"}""")
            }
            word.status shouldBe HttpStatusCode.Created
            val wordId = word.json()["id"]!!.jsonPrimitive.content
            client.put("/api/v1/words/$wordId/verb-details") {
                contentType(ContentType.Application.Json)
                setBody("""{"verbForm":"I","pastPattern":"A","presentPattern":"U","weaknessType":"SOUND"}""")
            }.status shouldBe HttpStatusCode.OK
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.json() =
        Json.parseToJsonElement(bodyAsText()).jsonObject
}
