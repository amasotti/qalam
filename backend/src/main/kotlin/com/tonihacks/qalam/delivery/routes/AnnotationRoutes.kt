package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.annotation.AddWordLinkRequest
import com.tonihacks.qalam.delivery.dto.annotation.CreateAnnotationRequest
import com.tonihacks.qalam.delivery.dto.annotation.UpdateAnnotationRequest
import com.tonihacks.qalam.delivery.dto.annotation.toResponse
import com.tonihacks.qalam.domain.annotation.AnnotationId
import com.tonihacks.qalam.domain.annotation.AnnotationService
import com.tonihacks.qalam.domain.annotation.AnnotationType
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.WordId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun Route.annotationRoutes(service: AnnotationService) {
    route("/texts/{textId}/annotations") {

        get {
            val textId = call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))

            service.listByText(TextId(textId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.map { a -> a.toResponse() }) },
            )
        }

        post {
            val textId = call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))

            val req = call.receive<CreateAnnotationRequest>()

            val type = AnnotationType.entries.firstOrNull { it.name == req.type }
                ?: return@post call.respondError(DomainError.ValidationError("type", "'${req.type}' is not a valid annotation type"))

            val masteryLevel = if (req.masteryLevel != null) {
                MasteryLevel.fromString(req.masteryLevel)
                    ?: return@post call.respondError(DomainError.ValidationError("masteryLevel", "'${req.masteryLevel}' is not a valid mastery level"))
            } else null

            val linkedWordIds = req.linkedWordIds.map { raw ->
                val uuid = raw.toAnnotationUuidOrNull()
                    ?: return@post call.respondError(DomainError.InvalidInput("'$raw' is not a valid UUID for wordId"))
                WordId(uuid)
            }

            service.create(
                textId = TextId(textId),
                anchor = req.anchor,
                type = type,
                content = req.content,
                masteryLevel = masteryLevel,
                reviewFlag = req.reviewFlag,
                linkedWordIds = linkedWordIds,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it.toResponse()) },
            )
        }

        get("/{id}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            service.getById(AnnotationId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        put("/{id}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@put call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val req = call.receive<UpdateAnnotationRequest>()

            val type = if (req.type != null) {
                AnnotationType.entries.firstOrNull { it.name == req.type }
                    ?: return@put call.respondError(DomainError.ValidationError("type", "'${req.type}' is not a valid annotation type"))
            } else null

            val masteryLevel = if (req.masteryLevel != null) {
                MasteryLevel.fromString(req.masteryLevel)
                    ?: return@put call.respondError(DomainError.ValidationError("masteryLevel", "'${req.masteryLevel}' is not a valid mastery level"))
            } else null

            service.update(
                id = AnnotationId(id),
                anchor = req.anchor,
                type = type,
                content = req.content,
                masteryLevel = masteryLevel,
                reviewFlag = req.reviewFlag,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            service.delete(AnnotationId(id)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        post("/{id}/words") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))

            val req = call.receive<AddWordLinkRequest>()
            val wordId = req.wordId.toAnnotationUuidOrNull()
                ?: return@post call.respondError(DomainError.InvalidInput("'${req.wordId}' is not a valid UUID"))

            service.addWordLink(AnnotationId(id), WordId(wordId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}/words/{wordId}") {
            call.parameters["textId"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["textId"]}' is not a valid UUID"))
            val id = call.parameters["id"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["id"]}' is not a valid UUID"))
            val wordId = call.parameters["wordId"]?.toAnnotationUuidOrNull()
                ?: return@delete call.respondError(DomainError.InvalidInput("'${call.parameters["wordId"]}' is not a valid UUID"))

            service.removeWordLink(AnnotationId(id), WordId(wordId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }
    }
}

fun Route.annotationWordRoutes(service: AnnotationService) {
    route("/words/{wordId}/annotations") {
        get {
            val wordId = call.parameters["wordId"]?.toAnnotationUuidOrNull()
                ?: return@get call.respondError(DomainError.InvalidInput("'${call.parameters["wordId"]}' is not a valid UUID"))

            service.getAnnotationsForWord(WordId(wordId)).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.map { a -> a.toResponse() }) },
            )
        }
    }
}

private fun String.toAnnotationUuidOrNull(): UUID? =
    try { UUID.fromString(this) } catch (_: IllegalArgumentException) { null }
