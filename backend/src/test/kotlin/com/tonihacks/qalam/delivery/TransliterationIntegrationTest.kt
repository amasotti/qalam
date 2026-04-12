package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class TransliterationIntegrationTest : BaseIntegrationTest() {
    init {
        "POST /api/v1/transliterate" - {
            "returns 200 with transliteration" {
                testApp { client ->
                    val response = client.post("/api/v1/transliterate") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabic":"بِسْمِ اللَّهِ"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "transliteration"
                }
            }

            "returns 422 for blank arabic" {
                testApp { client ->
                    val response = client.post("/api/v1/transliterate") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabic":"   "}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }
        }
    }
}
