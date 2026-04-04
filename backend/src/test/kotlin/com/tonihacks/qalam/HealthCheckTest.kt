package com.tonihacks.qalam

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class HealthCheckTest : BaseIntegrationTest() {
    init {
        "GET /health" - {
            "returns 200 with status ok" {
                testApp { client ->
                    val response = client.get("/health")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "ok"
                }
            }
        }
    }
}
