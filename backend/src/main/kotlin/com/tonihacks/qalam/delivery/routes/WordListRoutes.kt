package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.application.AiWordListSuggestionService
import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.wordlist.AddWordToListRequest
import com.tonihacks.qalam.delivery.dto.wordlist.CreateWordListRequest
import com.tonihacks.qalam.delivery.dto.wordlist.UpdateWordListRequest
import com.tonihacks.qalam.domain.wordlist.WordListService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

fun Route.wordListRoutes(service: WordListService, suggestionService: AiWordListSuggestionService) {
    route("/word-lists") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            service.list(page, size).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.getById(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post {
            val req = call.receive<CreateWordListRequest>()
            service.create(req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it) },
            )
        }

        patch("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            val req = call.receive<UpdateWordListRequest>()
            service.update(id, req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        delete("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.delete(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        membershipRoutes(service, suggestionService)
    }
}

private fun Route.membershipRoutes(service: WordListService, suggestionService: AiWordListSuggestionService) {
    post("/{id}/words") {
        val id = call.pathParameters.getOrFail<String>("id")
        val req = call.receive<AddWordToListRequest>()
        service.addWord(id, req).fold(
            { call.respondError(it) },
            { call.respond(HttpStatusCode.NoContent) },
        )
    }

    delete("/{id}/words/{wordId}") {
        val id = call.pathParameters.getOrFail<String>("id")
        val wordId = call.pathParameters.getOrFail<String>("wordId")
        service.removeWord(id, wordId).fold(
            { call.respondError(it) },
            { call.respond(HttpStatusCode.NoContent) },
        )
    }

    // Lists a given word belongs to — powers word-detail membership chips.
    get("/by-word/{wordId}") {
        val wordId = call.pathParameters.getOrFail<String>("wordId")
        service.listsForWord(wordId).fold(
            { call.respondError(it) },
            { call.respond(HttpStatusCode.OK, it) },
        )
    }

    // AI word suggestions for a list (ephemeral preview — never auto-saved)
    post("/{id}/suggest") {
        val id = call.pathParameters.getOrFail<String>("id")
        suggestionService.suggestWords(id).fold(
            { call.respondError(it) },
            { call.respond(HttpStatusCode.OK, it) },
        )
    }
}
