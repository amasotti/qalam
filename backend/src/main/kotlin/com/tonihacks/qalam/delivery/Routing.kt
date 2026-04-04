package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.delivery.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
private data class HealthResponse(val status: String = "ok")

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse())
        }

        route("/api/v1") {
            openAPI(path = "openapi.json", swaggerFile = "openapi/documentation.yaml")
            swaggerUI(path = "swagger-ui", swaggerFile = "openapi/documentation.yaml")
        }
    }
}
