package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.delivery.routes.aiInsightRoutes
import com.tonihacks.qalam.delivery.routes.analyticsRoutes
import com.tonihacks.qalam.delivery.routes.annotationRoutes
import com.tonihacks.qalam.delivery.routes.annotationWordRoutes
import com.tonihacks.qalam.delivery.routes.rootRoutes
import com.tonihacks.qalam.delivery.routes.sentenceRoutes
import com.tonihacks.qalam.delivery.routes.textRoutes
import com.tonihacks.qalam.delivery.routes.trainingRoutes
import com.tonihacks.qalam.delivery.routes.transliterationRoutes
import com.tonihacks.qalam.delivery.routes.wordRoutes
import com.tonihacks.qalam.domain.ai.AiInsightService
import com.tonihacks.qalam.domain.analytics.AnalyticsService
import com.tonihacks.qalam.domain.annotation.AnnotationService
import com.tonihacks.qalam.domain.root.RootService
import com.tonihacks.qalam.domain.sentence.SentenceService
import com.tonihacks.qalam.domain.text.TextService
import com.tonihacks.qalam.domain.training.TrainingService
import com.tonihacks.qalam.domain.transliteration.TransliterationService
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
    val transliterationService by inject<TransliterationService>()
    val annotationService by inject<AnnotationService>()
    val aiClient by inject<AiClient>()
    val trainingService by inject<TrainingService>()
    val aiInsightService by inject<AiInsightService>()
    val analyticsService by inject<AnalyticsService>()

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
            transliterationRoutes(transliterationService)
            annotationRoutes(annotationService)
            annotationWordRoutes(annotationService)
            trainingRoutes(trainingService)
            aiInsightRoutes(aiInsightService)
            analyticsRoutes(analyticsService)
        }
    }
}
