package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.BaseIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class SentencesIntegrationTest : BaseIntegrationTest() {

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private fun createTextJson(title: String): String =
        """{"title":"$title","body":"بِسْمِ اللَّهِ","difficulty":"BEGINNER","dialect":"MSA","tags":[]}"""

    private fun createSentenceJson(
        arabicText: String,
        position: Int? = null,
        transliteration: String? = null,
        freeTranslation: String? = null,
        notes: String? = null,
    ): String {
        val extras = buildString {
            if (position != null) append(""","position":$position""")
            if (transliteration != null) append(""","transliteration":"$transliteration"""")
            if (freeTranslation != null) append(""","freeTranslation":"$freeTranslation"""")
            if (notes != null) append(""","notes":"$notes"""")
        }
        return """{"arabicText":"$arabicText"$extras}"""
    }

    /** Extracts the first UUID from a JSON string via the "id":"…" pattern. */
    private fun extractId(json: String): String =
        Regex(""""id":"([^"]+)"""").find(json)!!.groupValues[1]

    // ─── Setup helpers ────────────────────────────────────────────────────────

    private suspend fun HttpClient.createText(title: String = "sentence-test-text"): String {
        val response = post("/api/v1/texts") {
            contentType(ContentType.Application.Json)
            setBody(createTextJson(title))
        }
        response.status shouldBe HttpStatusCode.Created
        return extractId(response.bodyAsText())
    }

    private suspend fun HttpClient.createSentence(textId: String, arabicText: String): String {
        val response = post("/api/v1/texts/$textId/sentences") {
            contentType(ContentType.Application.Json)
            setBody(createSentenceJson(arabicText))
        }
        response.status shouldBe HttpStatusCode.Created
        return extractId(response.bodyAsText())
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    init {

        "POST /api/v1/texts/{textId}/sentences" - {

            "creates sentence and returns 201 with id, arabicText, tokensValid=true, tokens=[]" {
                testApp { client ->
                    val textId = client.createText("post-sentence-creates-text")

                    val response = client.post("/api/v1/texts/$textId/sentences") {
                        contentType(ContentType.Application.Json)
                        setBody(createSentenceJson("بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ"))
                    }
                    response.status shouldBe HttpStatusCode.Created
                    val body = response.bodyAsText()
                    body shouldContain """"id":""""
                    body shouldContain "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ"
                    body shouldContain """"tokensValid":true"""
                    body shouldContain """"tokens":[]"""
                }
            }

            "returns 422 for blank arabicText" {
                testApp { client ->
                    val textId = client.createText("post-sentence-blank-arabic-text")

                    val response = client.post("/api/v1/texts/$textId/sentences") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"   "}""")
                    }
                    response.status shouldBe HttpStatusCode.UnprocessableEntity
                }
            }

            "returns 404 for non-existent textId" {
                testApp { client ->
                    val response = client.post("/api/v1/texts/00000000-0000-0000-0000-000000000000/sentences") {
                        contentType(ContentType.Application.Json)
                        setBody(createSentenceJson("الْحَمْدُ لِلَّهِ"))
                    }
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "GET /api/v1/texts/{textId}/sentences" - {

            "returns 200 and list containing the created sentence" {
                testApp { client ->
                    val textId = client.createText("get-sentences-list-text")
                    client.createSentence(textId, "قُلْ هُوَ اللَّهُ أَحَدٌ")

                    val response = client.get("/api/v1/texts/$textId/sentences")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain "قُلْ هُوَ اللَّهُ أَحَدٌ"
                }
            }
        }

        "GET /api/v1/texts/{textId}/sentences/{id}" - {

            "returns 200 with correct body" {
                testApp { client ->
                    val textId = client.createText("get-sentence-by-id-text")
                    val arabicText = "اللَّهُ لَا إِلَهَ إِلَّا هُوَ"
                    val sentenceId = client.createSentence(textId, arabicText)

                    val response = client.get("/api/v1/texts/$textId/sentences/$sentenceId")
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain sentenceId
                    body shouldContain arabicText
                    body shouldContain """"tokensValid":true"""
                }
            }

            "returns 404 for unknown sentence id" {
                testApp { client ->
                    val textId = client.createText("get-sentence-unknown-id-text")

                    val response = client.get("/api/v1/texts/$textId/sentences/00000000-0000-0000-0000-000000000000")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "PUT /api/v1/texts/{textId}/sentences/{id}" - {

            "returns 200 and only the provided fields change" {
                testApp { client ->
                    val textId = client.createText("put-sentence-partial-update-text")
                    val sentenceId = client.createSentence(textId, "وَمَا أَرْسَلْنَاكَ إِلَّا رَحْمَةً")

                    // Update only freeTranslation
                    val response = client.put("/api/v1/texts/$textId/sentences/$sentenceId") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"freeTranslation":"We have not sent you except as a mercy"}""")
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "We have not sent you except as a mercy"
                    // arabicText unchanged
                    body shouldContain "وَمَا أَرْسَلْنَاكَ إِلَّا رَحْمَةً"
                }
            }
        }

        "DELETE /api/v1/texts/{textId}/sentences/{id}" - {

            "returns 204 and subsequent GET returns 404" {
                testApp { client ->
                    val textId = client.createText("delete-sentence-text")
                    val sentenceId = client.createSentence(textId, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ")

                    val deleteResponse = client.delete("/api/v1/texts/$textId/sentences/$sentenceId")
                    deleteResponse.status shouldBe HttpStatusCode.NoContent

                    val getResponse = client.get("/api/v1/texts/$textId/sentences/$sentenceId")
                    getResponse.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

        "Stale-token invariant" - {

            "PUT arabicText after replaceTokens sets tokensValid=false, tokens remain, GET confirms" {
                testApp { client ->
                    val textId = client.createText("stale-token-invariant-text")
                    val sentenceId = client.createSentence(textId, "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ")

                    // Populate tokens via replaceTokens
                    val replaceResponse = client.put("/api/v1/texts/$textId/sentences/$sentenceId/tokens") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """{"tokens":[
                                {"position":1,"arabic":"إِنَّا","transliteration":"inna","translation":"indeed we"},
                                {"position":2,"arabic":"لِلَّهِ","transliteration":"lillahi","translation":"belong to Allah"}
                            ]}"""
                        )
                    }
                    replaceResponse.status shouldBe HttpStatusCode.OK
                    replaceResponse.bodyAsText() shouldContain """"tokensValid":true"""

                    // Now update arabicText → should set tokensValid=false
                    val updateResponse = client.put("/api/v1/texts/$textId/sentences/$sentenceId") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"arabicText":"إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ — updated"}""")
                    }
                    updateResponse.status shouldBe HttpStatusCode.OK
                    val updateBody = updateResponse.bodyAsText()
                    updateBody shouldContain """"tokensValid":false"""
                    // Tokens should still be present (not wiped)
                    updateBody shouldContain "إِنَّا"

                    // Verify GET also reflects tokensValid=false
                    val getResponse = client.get("/api/v1/texts/$textId/sentences/$sentenceId")
                    getResponse.status shouldBe HttpStatusCode.OK
                    getResponse.bodyAsText() shouldContain """"tokensValid":false"""
                }
            }
        }

        "PUT /api/v1/texts/{textId}/sentences/{id}/tokens" - {

            "replaces tokens and returns 200 with tokens and tokensValid=true" {
                testApp { client ->
                    val textId = client.createText("replace-tokens-text")
                    val sentenceId = client.createSentence(textId, "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ — tokens")

                    val response = client.put("/api/v1/texts/$textId/sentences/$sentenceId/tokens") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """{"tokens":[
                                {"position":1,"arabic":"بِسْمِ","transliteration":"bismi","translation":"in the name of"},
                                {"position":2,"arabic":"اللَّهِ","transliteration":"allahi","translation":"Allah"}
                            ]}"""
                        )
                    }
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldContain "بِسْمِ"
                    body shouldContain "اللَّهِ"
                    body shouldContain """"tokensValid":true"""
                }
            }
        }

        "DELETE /api/v1/texts/{textId}/sentences/{id}/tokens" - {

            "with confirm=true returns 200 and tokens=[]" {
                testApp { client ->
                    val textId = client.createText("delete-tokens-confirmed-text")
                    val sentenceId = client.createSentence(textId, "وَهُوَ بِكُلِّ شَيْءٍ عَلِيمٌ")

                    // First add some tokens
                    client.put("/api/v1/texts/$textId/sentences/$sentenceId/tokens") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"tokens":[{"position":1,"arabic":"وَهُوَ","translation":"and He"}]}""")
                    }

                    val response = client.delete("/api/v1/texts/$textId/sentences/$sentenceId/tokens?confirm=true")
                    response.status shouldBe HttpStatusCode.OK
                    response.bodyAsText() shouldContain """"tokens":[]"""
                }
            }

            "without confirm param returns 400" {
                testApp { client ->
                    val textId = client.createText("delete-tokens-no-confirm-text")
                    val sentenceId = client.createSentence(textId, "وَاللَّهُ بِمَا تَعْمَلُونَ بَصِيرٌ")

                    val response = client.delete("/api/v1/texts/$textId/sentences/$sentenceId/tokens")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }
}
