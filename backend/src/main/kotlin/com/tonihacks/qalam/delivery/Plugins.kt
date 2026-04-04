package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.delivery.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json

fun Application.configurePlugins() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CallLogging)

    install(CORS) {
        allowHost("localhost:3000")
        allowHost("localhost:4173")
        allowHost("localhost:5173")
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }

    // Validators are registered per-domain in Milestone 2+.
    install(RequestValidation)

    install(StatusPages) {
        // DomainErrors are handled at route level via Either.fold { call.respondError(it) }.
        // StatusPages is the catch-all for truly unexpected exceptions.
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Internal server error", code = "INTERNAL_ERROR"),
            )
        }
    }
}
