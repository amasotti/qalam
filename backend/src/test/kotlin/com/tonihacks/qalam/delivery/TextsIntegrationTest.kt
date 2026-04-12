package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class TextsIntegrationTest : BaseIntegrationTest() {

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private fun createTextJson(
        title: String,
        body: String = "وَإِذِ اعْتَزَلْتُمُوهُمْ",
        difficulty: String = "INTERMEDIATE",
        dialect: String = "MSA",
        tags: List<String> = emptyList(),
        translation: String? = null,
    ): String {
        val tagArray = tags.joinToString(",") { "\"$it\"" }
        val translationField = if (translation != null) ""","translation":"$translation"""" else ""
        return """{"title":"$title","body":"$body","difficulty":"$difficulty","dialect":"$dialect","tags":[$tagArray]$translationField}"""
    }

    /** Extracts the first UUID from a JSON string via the "id":"…" pattern. */
    private fun extractId(json: String): String =
        Regex(""""id":"([^"]+)"""").find(json)!!.groupValues[1]

    // ─── tests ────────────────────────────────────────────────────────────────

    init {

        "POST /api/v1/texts" - {

            "creates text and returns 201 with id and tags" {
                testApp { client ->
                    val response = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(
                            title = "post-creates-title",
                            tags = listOf("tag-a", "tag-b"),
                        ))
                    }
                    response.status shouldBe HttpStatusCode.Created
                    val body = response.bodyAsText()
                    body shouldContain "post-creates-title"
                    body shouldContain "tag-a"
                    body shouldContain "tag-b"
                    body shouldContain """"id":""""
                }
            }

            "returns 422 for blank title" {
                testApp { client ->
                    val response = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"  ","body":"some body","difficulty":"BEGINNER","dialect":"MSA","tags":[]}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "returns 422 for unknown difficulty" {
                testApp { client ->
                    val response = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"valid","body":"valid","difficulty":"GIBBERISH","dialect":"MSA","tags":[]}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "returns 422 for unknown dialect" {
                testApp { client ->
                    val response = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"valid","body":"valid","difficulty":"BEGINNER","dialect":"KLINGON","tags":[]}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }
        }

        "GET /api/v1/texts/{id}" - {

            "returns 200 with correct body" {
                testApp { client ->
                    val created = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(
                            title = "get-by-id-title",
                            translation = "a unique translation for get-by-id",
                        ))
                    }
                    val id = extractId(created.bodyAsText())

                    val response = client.get("/api/v1/texts/$id")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain id
                    body shouldContain "get-by-id-title"
                    body shouldContain "a unique translation for get-by-id"
                }
            }

            "returns 404 for unknown id" {
                testApp { client ->
                    val response = client.get("/api/v1/texts/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }

            "returns 400 for malformed id" {
                testApp { client ->
                    val response = client.get("/api/v1/texts/not-a-uuid")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        "GET /api/v1/texts" - {

            "returns 200 with paginated response" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "list-paginated-alpha"))
                    }

                    val response = client.get("/api/v1/texts")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "\"items\":"
                    body shouldContain "\"total\":"
                    body shouldContain "\"page\":"
                    body shouldContain "\"size\":"
                }
            }

            "?q= filter returns matching texts only" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "qsearch-unique-xqz-one"))
                    }
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "qsearch-other-abc-two"))
                    }

                    val response = client.get("/api/v1/texts?q=xqz")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "xqz"
                    body shouldNotContain "abc"
                }
            }

            "?tag= filter returns matching texts only" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "tag-filter-tagged", tags = listOf("unique-tag-zz9")))
                    }
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "tag-filter-untagged", tags = listOf("other-tag-qq1")))
                    }

                    val response = client.get("/api/v1/texts?tag=unique-tag-zz9")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "tag-filter-tagged"
                    body shouldNotContain "tag-filter-untagged"
                }
            }

            "?dialect= filter works" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "dialect-filter-tunisian", dialect = "TUNISIAN"))
                    }
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "dialect-filter-egyptian", dialect = "EGYPTIAN"))
                    }

                    val response = client.get("/api/v1/texts?dialect=TUNISIAN")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "dialect-filter-tunisian"
                    body shouldNotContain "dialect-filter-egyptian"
                }
            }

            "?difficulty= filter works" {
                testApp { client ->
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "difficulty-filter-advanced", difficulty = "ADVANCED"))
                    }
                    client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "difficulty-filter-beginner", difficulty = "BEGINNER"))
                    }

                    val response = client.get("/api/v1/texts?difficulty=ADVANCED")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "difficulty-filter-advanced"
                    body shouldNotContain "difficulty-filter-beginner"
                }
            }
        }

        "PUT /api/v1/texts/{id}" - {

            "partial update (only title) leaves other fields unchanged" {
                testApp { client ->
                    val created = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(
                            title = "put-original-title",
                            translation = "original translation",
                            tags = listOf("original-tag"),
                        ))
                    }
                    val id = extractId(created.bodyAsText())

                    val response = client.put("/api/v1/texts/$id") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"put-updated-title"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "put-updated-title"
                    body shouldContain "original translation"   // unchanged
                    body shouldContain "original-tag"           // unchanged
                }
            }

            "tags are replaced when provided in update" {
                testApp { client ->
                    val created = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(
                            title = "put-tags-replace-title",
                            tags = listOf("old-tag"),
                        ))
                    }
                    val id = extractId(created.bodyAsText())

                    val response = client.put("/api/v1/texts/$id") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"tags":["new-tag"]}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "new-tag"
                    body shouldNotContain "old-tag"
                }
            }

            "returns 404 for unknown id" {
                testApp { client ->
                    val response = client.put("/api/v1/texts/00000000-0000-0000-0000-000000000000") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"anything"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "DELETE /api/v1/texts/{id}" - {

            "returns 204 and subsequent GET returns 404" {
                testApp { client ->
                    val created = client.post("/api/v1/texts") {
                        contentType(ContentType.Application.Json)
                        setBody(createTextJson(title = "delete-me-title"))
                    }
                    val id = extractId(created.bodyAsText())

                    val deleteResponse = client.delete("/api/v1/texts/$id")
                    deleteResponse.status shouldBe HttpStatusCode.NoContent

                    val getResponse = client.get("/api/v1/texts/$id")
                    getResponse.status shouldBe HttpStatusCode.NotFound
                }
            }

            "returns 404 for unknown id" {
                testApp { client ->
                    val response = client.delete("/api/v1/texts/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }
    }
}
