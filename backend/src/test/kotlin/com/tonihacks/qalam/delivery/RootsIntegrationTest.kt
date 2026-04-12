package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class RootsIntegrationTest : BaseIntegrationTest() {

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement().execute("TRUNCATE TABLE arabic_roots CASCADE")
            }
        }

        "POST /api/v1/roots" - {
            "creates a root and returns 201" {
                testApp { client ->
                    val response = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    response.status shouldBe HttpStatusCode.Created
                    response.bodyAsText() shouldContain "رحب"
                }
            }

            "returns 400 for fewer than 2 letters" {
                testApp { client ->
                    val response = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر"}""")
                    }
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }

            "returns 409 for a duplicate root" {
                testApp { client ->
                    client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    val response = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"something else"}""")
                    }
                    response.status shouldBe HttpStatusCode.Conflict
                }
            }
        }

        "GET /api/v1/roots" - {
            "returns 200 with empty list when no roots exist" {
                testApp { client ->
                    val response = client.get("/api/v1/roots")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "\"items\":[]"
                }
            }

            "returns 200 with item after create" {
                testApp { client ->
                    client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    val response = client.get("/api/v1/roots")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "رحب"
                }
            }

            "passes letterCount filter" {
                testApp { client ->
                    client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ك ت ب ه","meaning":"write"}""")
                    }

                    val threeOnly = client.get("/api/v1/roots?letterCount=3")
                    threeOnly.status shouldBe HttpStatusCode.OK
                    threeOnly.bodyAsText() shouldContain "\"total\":1"
                }
            }
        }

        "GET /api/v1/roots/{id}" - {
            "returns 200 for an existing root" {
                testApp { client ->
                    val created = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    val body = created.bodyAsText()
                    val id = Regex(""""id":"([^"]+)"""").find(body)!!.groupValues[1]

                    val response = client.get("/api/v1/roots/$id")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain id
                }
            }

            "returns 404 for an unknown id" {
                testApp { client ->
                    val response = client.get("/api/v1/roots/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }

            "returns 400 for a malformed id" {
                testApp { client ->
                    val response = client.get("/api/v1/roots/not-a-uuid")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        "PUT /api/v1/roots/{id}" - {
            "returns 200 with updated meaning" {
                testApp { client ->
                    val created = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val response = client.put("/api/v1/roots/$id") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"meaning":"updated meaning"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "updated meaning"
                }
            }

            "returns 404 for an unknown id" {
                testApp { client ->
                    val response = client.put("/api/v1/roots/00000000-0000-0000-0000-000000000000") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"meaning":"anything"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "DELETE /api/v1/roots/{id}" - {
            "returns 204 for an existing root" {
                testApp { client ->
                    val created = client.post("/api/v1/roots") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"root":"ر ح ب","meaning":"openness"}""")
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val response = client.delete("/api/v1/roots/$id")
                    response.status shouldBe HttpStatusCode.NoContent
                }
            }

            "returns 404 for an unknown id" {
                testApp { client ->
                    val response = client.delete("/api/v1/roots/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "POST /api/v1/roots/normalize" - {
            "returns 200 for space-separated input" {
                testApp { client ->
                    val response = client.post("/api/v1/roots/normalize") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"input":"ر ح ب"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "رحب"
                }
            }

            "returns 200 for dash-separated input" {
                testApp { client ->
                    val response = client.post("/api/v1/roots/normalize") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"input":"ر-ح-ب"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "رحب"
                }
            }

            "returns 400 for non-Arabic input" {
                testApp { client ->
                    val response = client.post("/api/v1/roots/normalize") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"input":"abc"}""")
                    }
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }
}
