package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.conjugation.AdHocConjugationRequest
import com.tonihacks.qalam.domain.conjugation.ConjugationService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

fun Route.conjugationRoutes(service: ConjugationService) {
    route("/conjugation") {
        get("/{wordId}") {
            val wordId = call.pathParameters.getOrFail<String>("wordId")
            service.conjugateWord(wordId).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post("/compute") {
            val req = call.receive<AdHocConjugationRequest>()
            service.computeAdHoc(req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}
