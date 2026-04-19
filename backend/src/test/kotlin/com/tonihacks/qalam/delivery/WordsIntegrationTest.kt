package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class WordsIntegrationTest : BaseIntegrationTest() {

    private val katabJson = """{"arabicText":"كَتَبَ","translation":"to write","dialect":"MSA","difficulty":"BEGINNER"}"""
    private val qaraJson = """{"arabicText":"قَرَأَ","translation":"to read","dialect":"MSA","difficulty":"INTERMEDIATE"}"""

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement().execute("TRUNCATE TABLE words CASCADE")
            }
        }

        "POST /api/v1/words" - {
            "creates a word and returns 201" {
                testApp { client ->
                    val response = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    response.status shouldBe HttpStatusCode.Created
                    response.bodyAsText() shouldContain "كَتَبَ"
                    response.bodyAsText() shouldContain "to write"
                    response.bodyAsText() shouldContain """"masteryLevel":"NEW""""
                }
            }

            "returns 400 for blank arabicText" {
                testApp { client ->
                    val response = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"  "}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "returns 400 for unknown partOfSpeech" {
                testApp { client ->
                    val response = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"كَتَبَ","partOfSpeech":"GIBBERISH"}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }
        }

        "GET /api/v1/words" - {
            "returns empty list when no words exist" {
                testApp { client ->
                    val response = client.get("/api/v1/words")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "\"items\":[]"
                }
            }

            "returns items after creation" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val response = client.get("/api/v1/words")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "كَتَبَ"
                    response.bodyAsText() shouldContain "\"total\":1"
                }
            }

            "filters by dialect" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"واش","dialect":"MOROCCAN"}""")
                    }

                    val msaOnly = client.get("/api/v1/words?dialect=MSA")
                    msaOnly.status shouldBe HttpStatusCode.OK
                    msaOnly.bodyAsText() shouldContain "\"total\":1"
                    msaOnly.bodyAsText() shouldContain "كَتَبَ"
                }
            }

            "filters by difficulty" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)  // BEGINNER
                    }
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(qaraJson)  // INTERMEDIATE
                    }

                    val beginnerOnly = client.get("/api/v1/words?difficulty=BEGINNER")
                    beginnerOnly.status shouldBe HttpStatusCode.OK
                    beginnerOnly.bodyAsText() shouldContain "\"total\":1"
                }
            }

            "searches by arabic text" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(qaraJson)
                    }

                    val result = client.get("/api/v1/words?q=كَتَبَ")
                    result.status shouldBe HttpStatusCode.OK
                    result.bodyAsText() shouldContain "كَتَبَ"
                    result.bodyAsText() shouldNotContain "قَرَأَ"
                }
            }
        }

        "GET /api/v1/words/{id}" - {
            "returns 200 for an existing word" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val response = client.get("/api/v1/words/$id")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain id
                }
            }

            "returns 404 for unknown id" {
                testApp { client ->
                    val response = client.get("/api/v1/words/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }

            "returns 400 for malformed id" {
                testApp { client ->
                    val response = client.get("/api/v1/words/not-a-uuid")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        "PUT /api/v1/words/{id}" - {
            "returns 200 with updated fields" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val response = client.put("/api/v1/words/$id") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"translation":"to write (updated)"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "to write (updated)"
                }
            }

            "returns 404 for unknown id" {
                testApp { client ->
                    val response = client.put("/api/v1/words/00000000-0000-0000-0000-000000000000") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"translation":"anything"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "DELETE /api/v1/words/{id}" - {
            "returns 204 for an existing word" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val response = client.delete("/api/v1/words/$id")
                    response.status shouldBe HttpStatusCode.NoContent
                }
            }

            "returns 404 for unknown id" {
                testApp { client ->
                    val response = client.delete("/api/v1/words/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "GET /api/v1/words/autocomplete" - {
            "returns matching words" {
                testApp { client ->
                    client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }

                    val response = client.get("/api/v1/words/autocomplete?q=كَتَبَ")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "كَتَبَ"
                }
            }

            "returns empty array when no match" {
                testApp { client ->
                    val response = client.get("/api/v1/words/autocomplete?q=xyz")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "[]"
                }
            }
        }

        "Dictionary links /api/v1/words/{id}/dictionary-links" - {
            "POST adds a link and GET returns it" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val addResponse = client.post("/api/v1/words/$id/dictionary-links") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"source":"ALMANY","url":"https://www.almany.de/search/1"}""")
                    }
                    addResponse.status shouldBe HttpStatusCode.Created

                    val listResponse = client.get("/api/v1/words/$id/dictionary-links")
                    listResponse.status shouldBe HttpStatusCode.OK
                    listResponse.bodyAsText() shouldContain "ALMANY"
                }
            }

            "DELETE removes a link" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val wordId = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val addResponse = client.post("/api/v1/words/$wordId/dictionary-links") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"source":"ALMANY","url":"https://www.almany.de/search/1"}""")
                    }
                    val linkId = Regex(""""id":"([^"]+)"""").find(addResponse.bodyAsText())!!.groupValues[1]

                    val deleteResponse = client.delete("/api/v1/words/$wordId/dictionary-links/$linkId")
                    deleteResponse.status shouldBe HttpStatusCode.NoContent

                    val listResponse = client.get("/api/v1/words/$wordId/dictionary-links")
                    listResponse.bodyAsText() shouldBe "[]"
                }
            }

            "returns 422 for unknown dictionary source" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val id = Regex(""""id":"([^"]+)"""").find(created.bodyAsText())!!.groupValues[1]

                    val response = client.post("/api/v1/words/$id/dictionary-links") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"source":"BADLINK","url":"https://example.com"}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }
        }
    }
}
