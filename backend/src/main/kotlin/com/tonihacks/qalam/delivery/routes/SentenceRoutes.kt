package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.sentence.CreateSentenceRequest
import com.tonihacks.qalam.delivery.dto.sentence.ReorderSentencesRequest
import com.tonihacks.qalam.delivery.dto.sentence.ReplaceTokensRequest
import com.tonihacks.qalam.delivery.dto.sentence.UpdateSentenceRequest
import com.tonihacks.qalam.delivery.dto.sentence.toResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.sentence.SentenceId
import com.tonihacks.qalam.domain.sentence.SentenceService
import com.tonihacks.qalam.domain.sentence.TokenInput
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.infrastructure.ai.AiClient
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun Route.sentenceRoutes(service: SentenceService, aiClient: AiClient) {
    route("/texts/{textId}/sentences") {

        get {
            val textId = call.parameters["textId"]?.toUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))

            service.listByText(TextId(textId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.map { s -> s.toResponse() }) },
            )
        }

        post {
            val textId = call.parameters["textId"]?.toUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))

            val req = call.receive<CreateSentenceRequest>()
            service.create(
                textId = TextId(textId),
                arabicText = req.arabicText,
                position = req.position,
                transliteration = req.transliteration,
                freeTranslation = req.freeTranslation,
                notes = req.notes,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it.toResponse()) },
            )
        }

        put("/reorder") {
            val textId = call.parameters["textId"]?.toUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))

            val req = call.receive<ReorderSentencesRequest>()
            val orderedIds = req.orderedIds.map { raw ->
                val uuid = raw.toUuidOrNull()
                    ?: return@put call.respondError(DomainError.InvalidInput("'$raw' is not a valid UUID"))
                SentenceId(uuid)
            }

            service.reorder(TextId(textId), orderedIds).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.map { s -> s.toResponse() }) },
            )
        }

        get("/{id}") {
            requireTextId(call.parameters["textId"])
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            service.getById(SentenceId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        put("/{id}") {
            requireTextId(call.parameters["textId"])
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val req = call.receive<UpdateSentenceRequest>()
            service.update(
                id = SentenceId(id),
                arabicText = req.arabicText,
                position = req.position,
                transliteration = req.transliteration,
                freeTranslation = req.freeTranslation,
                notes = req.notes,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}") {
            requireTextId(call.parameters["textId"])
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            service.delete(SentenceId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        put("/{id}/tokens") {
            requireTextId(call.parameters["textId"])
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val req = call.receive<ReplaceTokensRequest>()
            val tokenInputs = req.tokens.map { dto ->
                val wordId = dto.wordId?.let {
                    val uuid = it.toUuidOrNull()
                        ?: return@put call.respondError(DomainError.InvalidInput("'$it' is not a valid UUID for wordId"))
                    WordId(uuid)
                }
                TokenInput(
                    position = dto.position,
                    arabic = dto.arabic,
                    transliteration = dto.transliteration,
                    translation = dto.translation,
                    wordId = wordId,
                )
            }

            service.replaceTokens(SentenceId(id), tokenInputs).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}/tokens") {
            requireTextId(call.parameters["textId"])
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val confirm = call.request.queryParameters["confirm"]
            if (confirm != "true") {
                return@delete call.respondError(DomainError.InvalidInput("Query param 'confirm=true' is required to clear all tokens"))
            }

            service.clearTokens(SentenceId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        post("/{id}/auto-tokenize") {
            requireTextId(call.parameters["textId"])
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val sentence = service.getById(SentenceId(id)).fold(
                { return@post call.respondError(it) },
                { it },
            )

            val tokenDtos = aiClient.autoTokenize(sentence.arabicText).fold(
                { return@post call.respondError(it) },
                { it },
            )

            val tokenInputs = tokenDtos.map { dto ->
                val wordId = dto.wordId?.let {
                    val uuid = it.toUuidOrNull()
                        ?: return@post call.respondError(DomainError.InvalidInput("'$it' is not a valid UUID for wordId"))
                    WordId(uuid)
                }
                TokenInput(
                    position = dto.position,
                    arabic = dto.arabic,
                    transliteration = dto.transliteration,
                    translation = dto.translation,
                    wordId = wordId,
                )
            }

            service.replaceTokens(SentenceId(id), tokenInputs).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        post("/{id}/transliterate") {
            requireTextId(call.parameters["textId"])
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val sentence = service.getById(SentenceId(id)).fold(
                { return@post call.respondError(it) },
                { it },
            )

            val transliteration = aiClient.transliterate(sentence.arabicText).fold(
                { return@post call.respondError(it) },
                { it },
            )

            service.update(
                id = SentenceId(id),
                transliteration = transliteration,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }
    }
}

/** Validates textId param as UUID. Returns the UUID if valid, null if malformed (caller handles the error). */
private fun requireTextId(raw: String?): UUID? = raw?.toUuidOrNull()

private fun String.toUuidOrNull(): UUID? = try { UUID.fromString(this) } catch (_: IllegalArgumentException) { null }
