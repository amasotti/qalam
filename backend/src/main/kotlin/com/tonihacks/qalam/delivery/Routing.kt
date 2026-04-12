package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.delivery.routes.rootRoutes
import com.tonihacks.qalam.delivery.routes.wordRoutes
import com.tonihacks.qalam.domain.root.RootService
import com.tonihacks.qalam.domain.word.WordService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
private data class HealthResponse(val status: String = "ok")

fun Application.configureRouting() {
    val rootService by inject<RootService>()
    val wordService by inject<WordService>()

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse())
        }

        route("/api/v1") {
            swaggerUI(path = "swagger-ui", swaggerFile = "openapi/documentation.yaml")
            rootRoutes(rootService)
            wordRoutes(wordService)
        }
    }
}
