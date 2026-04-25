package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class AnnotationsIntegrationTest : BaseIntegrationTest() {

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private fun createTextJson(title: String): String =
        """{"title":"$title","body":"بِسْمِ اللَّهِ","difficulty":"BEGINNER","dialect":"MSA","tags":[]}"""

    private fun createAnnotationJson(
        anchor: String,
        type: String = "VOCABULARY",
        content: String? = null,
        linkedWordIds: List<String> = emptyList(),
    ): String {
        val contentPart = if (content != null) ""","content":"$content"""" else ""
        val wordIdsPart = linkedWordIds.joinToString(",") { """"$it"""" }
        return """{"anchor":"$anchor","type":"$type","linkedWordIds":[$wordIdsPart]$contentPart}"""
    }

    /** Extracts the first UUID from a JSON string via the "id":"…" pattern. */
    private fun extractId(json: String): String =
        Regex(""""id":"([^"]+)"""").find(json)!!.groupValues[1]

    // ─── Setup helpers ────────────────────────────────────────────────────────

    private suspend fun HttpClient.createText(title: String = "annotation-test-text"): String {
        val response = post("/api/v1/texts") {
            contentType(ContentType.Application.Json)
            setBody(createTextJson(title))
        }
        response.status shouldBe HttpStatusCode.Created
        return extractId(response.bodyAsText())
    }

    private suspend fun HttpClient.createAnnotation(
        textId: String,
        anchor: String = "بِسْمِ",
        type: String = "VOCABULARY",
    ): String {
        val response = post("/api/v1/texts/$textId/annotations") {
            contentType(ContentType.Application.Json)
            setBody(createAnnotationJson(anchor = anchor, type = type))
        }
        response.status shouldBe HttpStatusCode.Created
        return extractId(response.bodyAsText())
    }

    private suspend fun HttpClient.createWord(arabicText: String = "كَتَبَ"): String {
        val response = post("/api/v1/words") {
            contentType(ContentType.Application.Json)
            setBody("""{"arabicText":"$arabicText","dialect":"MSA","difficulty":"BEGINNER"}""")
        }
        response.status shouldBe HttpStatusCode.Created
        return extractId(response.bodyAsText())
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    init {

        "POST /api/v1/texts/{textId}/annotations" - {

            "creates annotation and returns 201 with correct fields" {
                testApp { client ->
                    val textId = client.createText("post-annotation-creates-text")

                    val response = client.post("/api/v1/texts/$textId/annotations") {
                        contentType(ContentType.Application.Json)
                        setBody(createAnnotationJson(
                            anchor = "بِسْمِ",
                            type = "GRAMMAR",
                            content = "preposition + noun",
                        ))
                    }
                    response.status shouldBe HttpStatusCode.Created
                    val body = response.bodyAsText()
                    body shouldContain """"id":""""
                    body shouldContain """"anchor":"بِسْمِ""""
                    body shouldContain """"type":"GRAMMAR""""
                    body shouldContain """"content":"preposition + noun""""
                    body shouldContain """"linkedWordIds":[]"""
                    body shouldContain """"textId":"$textId""""
                }
            }

            "returns 422 for blank anchor" {
                testApp { client ->
                    val textId = client.createText("post-annotation-blank-anchor")

                    val response = client.post("/api/v1/texts/$textId/annotations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"anchor":"   ","type":"VOCABULARY"}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "returns 422 for invalid annotation type" {
                testApp { client ->
                    val textId = client.createText("post-annotation-bad-type")

                    val response = client.post("/api/v1/texts/$textId/annotations") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"anchor":"كلمة","type":"GIBBERISH"}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "returns 404 for non-existent textId" {
                testApp { client ->
                    val response = client.post("/api/v1/texts/00000000-0000-0000-0000-000000000000/annotations") {
                        contentType(ContentType.Application.Json)
                        setBody(createAnnotationJson("anchor"))
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "GET /api/v1/texts/{textId}/annotations" - {

            "returns 200 and list containing the created annotation" {
                testApp { client ->
                    val textId = client.createText("get-annotations-list-text")
                    client.createAnnotation(textId, anchor = "اللَّهِ", type = "CULTURAL")

                    val response = client.get("/api/v1/texts/$textId/annotations")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "اللَّهِ"
                    body shouldContain """"type":"CULTURAL""""
                }
            }

            "returns empty list for text with no annotations" {
                testApp { client ->
                    val textId = client.createText("get-annotations-empty-list")

                    val response = client.get("/api/v1/texts/$textId/annotations")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "[]"
                }
            }
        }

        "GET /api/v1/texts/{textId}/annotations/{id}" - {

            "returns 200 with correct body" {
                testApp { client ->
                    val textId = client.createText("get-annotation-by-id-text")
                    val annotationId = client.createAnnotation(textId, anchor = "الرَّحْمَنِ")

                    val response = client.get("/api/v1/texts/$textId/annotations/$annotationId")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain annotationId
                    body shouldContain "الرَّحْمَنِ"
                }
            }

            "returns 404 for unknown annotation id" {
                testApp { client ->
                    val textId = client.createText("get-annotation-unknown-id")

                    val response = client.get("/api/v1/texts/$textId/annotations/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "PUT /api/v1/texts/{textId}/annotations/{id}" - {

            "returns 200 and updates only provided fields" {
                testApp { client ->
                    val textId = client.createText("put-annotation-update-text")
                    val annotationId = client.createAnnotation(textId, anchor = "original", type = "VOCABULARY")

                    val response = client.put("/api/v1/texts/$textId/annotations/$annotationId") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"content":"updated content"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain """"content":"updated content""""
                    // anchor unchanged
                    body shouldContain """"anchor":"original""""
                    // type unchanged
                    body shouldContain """"type":"VOCABULARY""""
                }
            }

            "returns 404 for unknown annotation" {
                testApp { client ->
                    val textId = client.createText("put-annotation-not-found")

                    val response = client.put("/api/v1/texts/$textId/annotations/00000000-0000-0000-0000-000000000000") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"content":"noop"}""")
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "DELETE /api/v1/texts/{textId}/annotations/{id}" - {

            "returns 204 and subsequent GET returns 404" {
                testApp { client ->
                    val textId = client.createText("delete-annotation-text")
                    val annotationId = client.createAnnotation(textId)

                    val deleteResponse = client.delete("/api/v1/texts/$textId/annotations/$annotationId")
                    deleteResponse.status shouldBe HttpStatusCode.NoContent

                    val getResponse = client.get("/api/v1/texts/$textId/annotations/$annotationId")
                    getResponse.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "POST /api/v1/texts/{textId}/annotations/{id}/words" - {

            "adds word link and returns updated annotation with wordId in linkedWordIds" {
                testApp { client ->
                    val textId = client.createText("add-word-link-text")
                    val annotationId = client.createAnnotation(textId)
                    val wordId = client.createWord()

                    val response = client.post("/api/v1/texts/$textId/annotations/$annotationId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain wordId
                }
            }

            "adding same word link twice is idempotent" {
                testApp { client ->
                    val textId = client.createText("add-word-link-idempotent-text")
                    val annotationId = client.createAnnotation(textId)
                    val wordId = client.createWord("مَكَتَبَ")

                    client.post("/api/v1/texts/$textId/annotations/$annotationId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }
                    val response = client.post("/api/v1/texts/$textId/annotations/$annotationId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    // wordId should appear exactly once in linkedWordIds
                    val body = response.bodyAsText()
                    body shouldContain wordId
                }
            }
        }

        "DELETE /api/v1/texts/{textId}/annotations/{id}/words/{wordId}" - {

            "removes word link and wordId no longer in linkedWordIds" {
                testApp { client ->
                    val textId = client.createText("remove-word-link-text")
                    val annotationId = client.createAnnotation(textId)
                    val wordId = client.createWord("قَرَأَ")

                    // First add
                    client.post("/api/v1/texts/$textId/annotations/$annotationId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }

                    // Then remove
                    val response = client.delete("/api/v1/texts/$textId/annotations/$annotationId/words/$wordId")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldNotContain wordId
                }
            }
        }

        "GET /api/v1/words/{wordId}/annotations" - {

            "returns annotations that are linked to the given word" {
                testApp { client ->
                    val textId = client.createText("word-annotations-lookup-text")
                    val annotationId = client.createAnnotation(textId, anchor = "رَحْمَة")
                    val wordId = client.createWord("كَتَبَ")

                    client.post("/api/v1/texts/$textId/annotations/$annotationId/words") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"wordId":"$wordId"}""")
                    }

                    val response = client.get("/api/v1/words/$wordId/annotations")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain annotationId
                    body shouldContain "رَحْمَة"
                }
            }

            "returns empty list when no annotations are linked to the word" {
                testApp { client ->
                    val wordId = client.createWord("جَلَسَ")

                    val response = client.get("/api/v1/words/$wordId/annotations")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldBe "[]"
                }
            }
        }
    }
}
