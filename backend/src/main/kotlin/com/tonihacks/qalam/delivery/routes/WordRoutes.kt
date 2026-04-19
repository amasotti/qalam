package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.word.CreateDictionaryLinkRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordExampleRequest
import com.tonihacks.qalam.delivery.dto.word.CreateWordRequest
import com.tonihacks.qalam.delivery.dto.word.UpdateWordRequest
import com.tonihacks.qalam.domain.word.WordService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Suppress("LongMethod")
fun Route.wordRoutes(service: WordService) {
    route("/words") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val q = call.request.queryParameters["q"]
            val dialect = call.request.queryParameters["dialect"]
            val difficulty = call.request.queryParameters["difficulty"]
            val partOfSpeech = call.request.queryParameters["partOfSpeech"]
            val masteryLevel = call.request.queryParameters["masteryLevel"]

            service.list(page, size, q, dialect, difficulty, partOfSpeech, masteryLevel).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/autocomplete") {
            val q = call.request.queryParameters["q"] ?: ""
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()

            service.autocomplete(q, limit).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/{id}") {
            val id = call.parameters["id"]!!
            service.getById(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post {
            val req = call.receive<CreateWordRequest>()
            service.create(req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it) },
            )
        }

        put("/{id}") {
            val id = call.parameters["id"]!!
            val req = call.receive<UpdateWordRequest>()
            service.update(id, req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        delete("/{id}") {
            val id = call.parameters["id"]!!
            service.delete(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        // Dictionary links sub-resource
        get("/{id}/dictionary-links") {
            val id = call.parameters["id"]!!
            service.getDictionaryLinks(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post("/{id}/dictionary-links") {
            val id = call.parameters["id"]!!
            val req = call.receive<CreateDictionaryLinkRequest>()
            service.addDictionaryLink(id, req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it) },
            )
        }

        delete("/{id}/dictionary-links/{linkId}") {
            val id = call.parameters["id"]!!
            val linkId = call.parameters["linkId"]!!
            service.deleteDictionaryLink(id, linkId).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        // Saved examples sub-resource
        get("/{id}/examples") {
            val id = call.parameters["id"]!!
            service.getExamples(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post("/{id}/examples") {
            val id = call.parameters["id"]!!
            val req = call.receive<CreateWordExampleRequest>()
            service.saveExample(id, req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it) },
            )
        }

        delete("/{id}/examples/{exampleId}") {
            val id = call.parameters["id"]!!
            val exampleId = call.parameters["exampleId"]!!
            service.deleteExample(id, exampleId).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        // AI generation (ephemeral — does not persist)
        post("/{id}/examples/generate") {
            val id = call.parameters["id"]!!
            service.generateExamples(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}
