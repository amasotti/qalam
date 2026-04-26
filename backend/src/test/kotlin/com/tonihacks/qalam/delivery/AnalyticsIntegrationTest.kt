package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class AnalyticsIntegrationTest : BaseIntegrationTest() {

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement().execute(
                    "TRUNCATE TABLE training_sessions CASCADE; TRUNCATE TABLE words CASCADE; TRUNCATE TABLE texts CASCADE; TRUNCATE TABLE arabic_roots CASCADE"
                )
            }
        }

        "GET /api/v1/analytics/overview" - {
            "returns 200 with zero counts on empty DB" {
                testApp { client ->
                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain """"total":0"""
                    body shouldContain """"totalSessions":0"""
                }
            }

            "counts a word and reflects it in words.total and byDialect" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"كَتَبَ","dialect":"MSA","difficulty":"BEGINNER"}""")
                    }

                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain """"MSA":1"""
                    body shouldContain """"BEGINNER":1"""
                    body shouldContain """"NEW":1"""
                }
            }

            "counts a text and reflects it in texts.total and byDialect" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"Test","body":"نص","dialect":"MSA","difficulty":"BEGINNER"}""")
                    }

                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain """"MSA":1"""
                    body shouldContain """"BEGINNER":1"""
                }
            }

            "training.completedSessions reflects completed training sessions" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"كَتَبَ","dialect":"MSA","difficulty":"BEGINNER"}""")
                    }

                    val sessionBody = client.post("/api/v1/training/sessions") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"mode":"MIXED","size":1}""")
                    }.bodyAsText()

                    val sessionId = Regex(""""id":"([^"]+)"""").find(sessionBody)!!.groupValues[1]
                    val wordId = Regex(""""wordId":"([^"]+)"""").find(sessionBody)!!.groupValues[1]

                    client.post("/api/v1/training/sessions/$sessionId/results") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId","result":"CORRECT"}""")
                    }
                    client.post("/api/v1/training/sessions/$sessionId/complete")

                    val response = client.get("/api/v1/analytics/overview")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain """"completedSessions":1"""
                    response.bodyAsText() shouldContain """"totalSessions":1"""
                }
            }
        }
    }
}
