package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class WordEnrichmentIntegrationTest : BaseIntegrationTest() {

    private val katabJson = """{"arabicText":"كَتَبَ","translation":"to write","partOfSpeech":"VERB","dialect":"MSA","difficulty":"BEGINNER"}"""
    private val kitabJson = """{"arabicText":"كِتَاب","translation":"book","partOfSpeech":"NOUN","dialect":"MSA","difficulty":"BEGINNER"}"""

    init {
        beforeEach {
            postgres.createConnection("").use { conn ->
                conn.createStatement().execute("TRUNCATE TABLE words CASCADE")
            }
        }

        // --- Notes ---

        "notes on word create and update" - {
            "POST /words persists notes" {
                testApp { client ->
                    val response = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"كَتَبَ","dialect":"MSA","notes":"Root k-t-b — writing family"}""")
                    }
                    response.status shouldBe HttpStatusCode.Created
                    response.bodyAsText() shouldContain "Root k-t-b"
                }
            }

            "PUT /words/{id} updates notes" {
                testApp { client ->
                    val created = client.post("/api/v1/words") {
                        contentType(ContentType.Application.Json)
                        setBody(katabJson)
                    }
                    val id = extractId(created.bodyAsText())

                    val updated = client.put("/api/v1/words/$id") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"notes":"added later"}""")
                    }
                    updated.status shouldBe HttpStatusCode.OK
                    updated.bodyAsText() shouldContain "added later"
                }
            }
        }

        // --- Morphology ---

        "PUT /api/v1/words/{id}/morphology" - {
            "creates morphology and GET returns it" {
                testApp { client ->
                    val id = createWord(client, kitabJson)

                    val put = client.put("/api/v1/words/$id/morphology") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"gender":"MASCULINE"}""")
                    }
                    put.status shouldBe HttpStatusCode.OK
                    put.bodyAsText() shouldContain "MASCULINE"

                    val get = client.get("/api/v1/words/$id/morphology")
                    get.status shouldBe HttpStatusCode.OK
                    get.bodyAsText() shouldContain "MASCULINE"
                }
            }

            "upserts — second PUT replaces previous value" {
                testApp { client ->
                    val id = createWord(client, kitabJson)

                    client.put("/api/v1/words/$id/morphology") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"gender":"MASCULINE"}""")
                    }
                    val updated = client.put("/api/v1/words/$id/morphology") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"gender":"FEMININE"}""")
                    }
                    updated.status shouldBe HttpStatusCode.OK
                    updated.bodyAsText() shouldContain "FEMININE"
                }
            }

            "returns 404 for unknown word" {
                testApp { client ->
                    val response = client.put("/api/v1/words/00000000-0000-0000-0000-000000000000/morphology") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"gender":"MASCULINE"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        // --- Plurals ---

        "plural CRUD /api/v1/words/{id}/plurals" - {
            "POST adds a plural and GET returns it" {
                testApp { client ->
                    val id = createWord(client, kitabJson)

                    val add = client.post("/api/v1/words/$id/plurals") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"pluralForm":"كُتُب","pluralType":"BROKEN"}""")
                    }
                    add.status shouldBe HttpStatusCode.Created
                    add.bodyAsText() shouldContain "كُتُب"
                    add.bodyAsText() shouldContain "BROKEN"

                    val list = client.get("/api/v1/words/$id/plurals")
                    list.status shouldBe HttpStatusCode.OK
                    list.bodyAsText() shouldContain "كُتُب"
                }
            }

            "DELETE removes a plural" {
                testApp { client ->
                    val id = createWord(client, kitabJson)

                    val add = client.post("/api/v1/words/$id/plurals") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"pluralForm":"كُتُب","pluralType":"BROKEN"}""")
                    }
                    val pluralId = extractId(add.bodyAsText())

                    val delete = client.delete("/api/v1/words/$id/plurals/$pluralId")
                    delete.status shouldBe HttpStatusCode.NoContent

                    val list = client.get("/api/v1/words/$id/plurals")
                    list.bodyAsText() shouldBe "[]"
                }
            }

            "returns 422 for blank pluralForm" {
                testApp { client ->
                    val id = createWord(client, kitabJson)
                    val response = client.post("/api/v1/words/$id/plurals") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"pluralForm":"  ","pluralType":"BROKEN"}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }
        }

        // --- Relations ---

        "relation add+delete /api/v1/words/{id}/relations" - {
            "POST adds relation and GET returns it from both word perspectives" {
                testApp { client ->
                    val id1 = createWord(client, katabJson)
                    val id2 = createWord(client, kitabJson)

                    val add = client.post("/api/v1/words/$id1/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"$id2","relationType":"RELATED"}""")
                    }
                    add.status shouldBe HttpStatusCode.Created
                    add.bodyAsText() shouldContain "RELATED"

                    // queryable from the word that added it
                    val fromId1 = client.get("/api/v1/words/$id1/relations")
                    fromId1.status shouldBe HttpStatusCode.OK
                    fromId1.bodyAsText() shouldContain id2

                    // queryable from the related word too (bidirectional)
                    val fromId2 = client.get("/api/v1/words/$id2/relations")
                    fromId2.status shouldBe HttpStatusCode.OK
                    fromId2.bodyAsText() shouldContain id1
                }
            }

            "DELETE removes relation" {
                testApp { client ->
                    val id1 = createWord(client, katabJson)
                    val id2 = createWord(client, kitabJson)

                    client.post("/api/v1/words/$id1/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"$id2","relationType":"SYNONYM"}""")
                    }

                    val delete = client.delete("/api/v1/words/$id1/relations/$id2/SYNONYM")
                    delete.status shouldBe HttpStatusCode.NoContent

                    val list = client.get("/api/v1/words/$id1/relations")
                    list.bodyAsText() shouldBe "[]"
                }
            }

            "rejects self-relation with 422" {
                testApp { client ->
                    val id = createWord(client, katabJson)
                    val response = client.post("/api/v1/words/$id/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"$id","relationType":"SYNONYM"}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "rejects duplicate relation with 409" {
                testApp { client ->
                    val id1 = createWord(client, katabJson)
                    val id2 = createWord(client, kitabJson)

                    client.post("/api/v1/words/$id1/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"$id2","relationType":"ANTONYM"}""")
                    }
                    val dup = client.post("/api/v1/words/$id1/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"$id2","relationType":"ANTONYM"}""")
                    }
                    dup.status shouldBe HttpStatusCode.Conflict
                }
            }

            "returns 404 when related word does not exist" {
                testApp { client ->
                    val id = createWord(client, katabJson)
                    val response = client.post("/api/v1/words/$id/relations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"relatedWordId":"00000000-0000-0000-0000-000000000000","relationType":"SYNONYM"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        // --- AI enrichment ---

        "POST /api/v1/words/{id}/enrich" - {
            "returns 503 when AI is not configured" {
                testApp { client ->
                    val id = createWord(client, katabJson)
                    val response = client.post("/api/v1/words/$id/enrich")
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                    response.bodyAsText() shouldContain "AI_NOT_CONFIGURED"
                }
            }
        }
    }

    private suspend fun createWord(client: io.ktor.client.HttpClient, json: String): String {
        val response = client.post("/api/v1/words") {
            contentType(ContentType.Application.Json)
            setBody(json)
        }
        return extractId(response.bodyAsText())
    }

    private fun extractId(body: String): String =
        Regex(""""id":"([^"]+)"""").find(body)!!.groupValues[1]
}
