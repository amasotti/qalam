package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class WordListsIntegrationTest : BaseIntegrationTest() {

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement().execute("TRUNCATE TABLE word_lists, words CASCADE")
            }
        }

        "POST /api/v1/word-lists" - {
            "creates a list and returns 201 with itemCount 0" {
                testApp { client ->
                    val response = client.post("/api/v1/word-lists") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"Colors","description":"basic colors"}""")
                    }
                    response.status shouldBe HttpStatusCode.Created
                    val body = response.bodyAsText()
                    body shouldContain "Colors"
                    body shouldContain "\"itemCount\":0"
                }
            }

            "returns 422 for a blank title" {
                testApp { client ->
                    val response = client.post("/api/v1/word-lists") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"   "}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }
        }

        "GET /api/v1/word-lists" - {
            "returns 200 with empty list when none exist" {
                testApp { client ->
                    val response = client.get("/api/v1/word-lists")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "\"items\":[]"
                }
            }
        }

        "GET /api/v1/word-lists/{id}" - {
            "returns 200 with ordered member words" {
                testApp { client ->
                    val listId = createList(client, "Family")
                    val wordId = createWord(client, "أب")
                    client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }

                    val response = client.get("/api/v1/word-lists/$listId")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "أب"
                }
            }

            "returns 404 for an unknown id" {
                testApp { client ->
                    val response = client.get("/api/v1/word-lists/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }

            "returns 400 for a malformed id" {
                testApp { client ->
                    val response = client.get("/api/v1/word-lists/not-a-uuid")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        "POST /api/v1/word-lists/{id}/words" - {
            "adds a word and bumps itemCount" {
                testApp { client ->
                    val listId = createList(client, "Colors")
                    val wordId = createWord(client, "أحمر")

                    val added = client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }
                    added.status shouldBe HttpStatusCode.NoContent

                    val list = client.get("/api/v1/word-lists")
                    list.bodyAsText() shouldContain "\"itemCount\":1"
                }
            }

            "returns 409 when the word is already in the list" {
                testApp { client ->
                    val listId = createList(client, "Colors")
                    val wordId = createWord(client, "أزرق")
                    val body = """{"wordId":"$wordId"}"""
                    client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json); setBody(body)
                    }
                    val dup = client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json); setBody(body)
                    }
                    dup.status shouldBe HttpStatusCode.Conflict
                }
            }

            "returns 404 for an unknown word" {
                testApp { client ->
                    val listId = createList(client, "Colors")
                    val response = client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"00000000-0000-0000-0000-000000000000"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "DELETE /api/v1/word-lists/{id}/words/{wordId}" - {
            "removes a member, then 404 on repeat" {
                testApp { client ->
                    val listId = createList(client, "Colors")
                    val wordId = createWord(client, "أخضر")
                    client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }

                    val removed = client.delete("/api/v1/word-lists/$listId/words/$wordId")
                    removed.status shouldBe HttpStatusCode.NoContent

                    val again = client.delete("/api/v1/word-lists/$listId/words/$wordId")
                    again.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "GET /api/v1/word-lists/by-word/{wordId}" - {
            "returns the lists a word belongs to" {
                testApp { client ->
                    val listId = createList(client, "Family")
                    val wordId = createWord(client, "أم")
                    client.post("/api/v1/word-lists/$listId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }

                    val response = client.get("/api/v1/word-lists/by-word/$wordId")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "Family"
                }
            }
        }

        "PATCH /api/v1/word-lists/{id}" - {
            "updates the title" {
                testApp { client ->
                    val listId = createList(client, "Old")
                    val response = client.patch("/api/v1/word-lists/$listId") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"New"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "New"
                }
            }
        }

        "POST /api/v1/word-lists/{id}/suggest" - {
            "returns 503 when AI is not configured" {
                testApp { client ->
                    val listId = createList(client, "Colors")
                    val response = client.post("/api/v1/word-lists/$listId/suggest")
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                }
            }

            "returns 404 for an unknown list" {
                testApp { client ->
                    val response =
                        client.post("/api/v1/word-lists/00000000-0000-0000-0000-000000000000/suggest")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "DELETE /api/v1/word-lists/{id}" - {
            "deletes a list, then 404 on fetch" {
                testApp { client ->
                    val listId = createList(client, "Temp")
                    client.delete("/api/v1/word-lists/$listId").status shouldBe HttpStatusCode.NoContent
                    client.get("/api/v1/word-lists/$listId").status shouldBe HttpStatusCode.NotFound
                }
            }
        }
    }

    private suspend fun createList(client: HttpClient, title: String): String {
        val res = client.post("/api/v1/word-lists") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"$title"}""")
        }
        return idOf(res.bodyAsText())
    }

    private suspend fun createWord(client: HttpClient, arabic: String): String {
        val res = client.post("/api/v1/words") {
            contentType(ContentType.Application.Json)
            setBody("""{"arabicText":"$arabic"}""")
        }
        return idOf(res.bodyAsText())
    }

    private fun idOf(body: String): String =
        Regex(""""id":"([^"]+)"""").find(body)!!.groupValues[1]
}
