package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.delivery.routes.rootRoutes
import com.tonihacks.qalam.delivery.routes.sentenceRoutes
import com.tonihacks.qalam.delivery.routes.textRoutes
import com.tonihacks.qalam.delivery.routes.wordRoutes
import com.tonihacks.qalam.domain.root.RootService
import com.tonihacks.qalam.domain.sentence.SentenceService
import com.tonihacks.qalam.domain.text.TextService
import com.tonihacks.qalam.domain.word.WordService
import com.tonihacks.qalam.infrastructure.ai.AiClient
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
    val textService by inject<TextService>()
    val sentenceService by inject<SentenceService>()
    val aiClient by inject<AiClient>()

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse())
        }

        route("/api/v1") {
            swaggerUI(path = "swagger-ui", swaggerFile = "openapi/documentation.yaml")
            rootRoutes(rootService)
            wordRoutes(wordService)
            textRoutes(textService)
            sentenceRoutes(sentenceService, aiClient)
        }
    }
}
