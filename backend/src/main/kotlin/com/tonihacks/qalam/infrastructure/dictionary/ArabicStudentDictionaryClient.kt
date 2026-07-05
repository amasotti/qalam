package com.tonihacks.qalam.infrastructure.dictionary

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.dictionary.DictionaryLookupItemResponse
import com.tonihacks.qalam.delivery.dto.dictionary.DictionaryLookupPluralResponse
import com.tonihacks.qalam.delivery.dto.dictionary.DictionaryLookupResponse
import com.tonihacks.qalam.domain.dictionary.DictionaryLookupClient
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.removeArabicDiacritics
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable


class ArabicStudentDictionaryClient : DictionaryLookupClient, Closeable {
    private val jsonConfig = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://api.arabicstudentsdictionary.com"

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(jsonConfig) }
    }

    override suspend fun search(query: String): Either<DomainError, DictionaryLookupResponse> =
        try {
            val payload = httpClient.get("$baseUrl/search") {
                url {
                    parameters.append("query", query)
                }
                accept(ContentType.Application.Json)
                header(HttpHeaders.Origin, "https://www.arabicstudentsdictionary.com")
                header("Referer", "https://www.arabicstudentsdictionary.com/")
                header(HttpHeaders.UserAgent, "Qalam/1.0 dictionary lookup")
            }.body<AsdSearchPayload>()

            DictionaryLookupResponse(
                source = "ASD",
                query = query,
                items = payload.words.map { it.toResponse(query) },
            ).right()

        } catch (_: Exception) {
            DomainError.InvalidInput("Dictionary Lookup failed").left()
        }


    override fun close() {
        httpClient.close()
    }
}

@Serializable
private data class AsdSearchPayload(
    val words: List<AsdWordPayload> = emptyList(),
)

@Serializable
private data class AsdWordPayload(
    val id: String,
    val text: String,
    val form: String = "",
    val transliteration: String = "",
    val plural: AsdPluralPayload = AsdPluralPayload(),
    val translation: AsdTranslationPayload? = null,
    @SerialName("has_word_match") val hasWordMatch: Boolean = false,
)

@Serializable
private data class AsdPluralPayload(
    val text: String = "",
    val transliteration: String = "",
)

@Serializable
private data class AsdTranslationPayload(
    val text: String = "",
    val short: String = "",
)

private fun AsdWordPayload.toResponse(q: String): DictionaryLookupItemResponse {

    val isExactMatch = text.removeArabicDiacritics().trim() == q.removeArabicDiacritics().trim()
    return DictionaryLookupItemResponse(
        externalId = id,
        arabicText = text,
        transliteration = transliteration.takeIf { it.isNotBlank() },
        translation = translation?.short?.takeIf { it.isNotBlank() }, // ASD has also a long paragraph as comment / transl.
        plural = plural.takeIf { it.text.isNotBlank() }?.let {
            DictionaryLookupPluralResponse(
                arabicText = it.text,
                transliteration = it.transliteration.takeIf { tr -> tr.isNotBlank() },
            )
        },
        hasExactWordMatch = isExactMatch,
    )
}
