package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.dto.transliteration.TransliterateRequest
import com.tonihacks.qalam.delivery.dto.transliteration.TransliterateResponse
import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.transliteration.TransliterationService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transliterationRoutes(service: TransliterationService) {
    post("/transliterate") {
        val req = call.receive<TransliterateRequest>()
        if (req.arabic.isBlank()) {
            call.respondError(DomainError.ValidationError("arabic", "arabic must not be blank"))
            return@post
        }
        val result = service.transliterate(req.arabic)
        call.respond(HttpStatusCode.OK, TransliterateResponse(result))
    }
}
