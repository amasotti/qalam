package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.application.AiRootFamilySuggestionService
import com.tonihacks.qalam.application.AiInsightService
import com.tonihacks.qalam.application.AiWordListSuggestionService
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeService
import com.tonihacks.qalam.delivery.routes.aiInsightRoutes
import com.tonihacks.qalam.delivery.routes.analyticsRoutes
import com.tonihacks.qalam.delivery.routes.annotationRoutes
import com.tonihacks.qalam.delivery.routes.conjugationRoutes
import com.tonihacks.qalam.delivery.routes.conjugationExerciseRoutes
import com.tonihacks.qalam.delivery.routes.annotationWordRoutes
import com.tonihacks.qalam.delivery.routes.exerciseRoutes
import com.tonihacks.qalam.delivery.routes.rootRoutes
import com.tonihacks.qalam.delivery.routes.sentenceRoutes
import com.tonihacks.qalam.delivery.routes.textRoutes
import com.tonihacks.qalam.delivery.routes.trainingRoutes
import com.tonihacks.qalam.delivery.routes.transliterationRoutes
import com.tonihacks.qalam.delivery.routes.wordListRoutes
import com.tonihacks.qalam.delivery.routes.wordRoutes
import com.tonihacks.qalam.delivery.routes.productionPracticeRoutes
import com.tonihacks.qalam.domain.analytics.AnalyticsService
import com.tonihacks.qalam.domain.conjugation.ConjugationService
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseService
import com.tonihacks.qalam.domain.annotation.AnnotationService
import com.tonihacks.qalam.domain.dictionary.DictionaryLookupService
import com.tonihacks.qalam.domain.exercise.ExerciseService
import com.tonihacks.qalam.domain.root.RootService
import com.tonihacks.qalam.domain.sentence.SentenceService
import com.tonihacks.qalam.domain.text.TextService
import com.tonihacks.qalam.domain.training.TrainingService
import com.tonihacks.qalam.domain.transliteration.TransliterationService
import com.tonihacks.qalam.domain.word.WordService
import com.tonihacks.qalam.domain.wordlist.WordListService
import com.tonihacks.qalam.infrastructure.ai.OpenRouterSentenceClient
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
    val dictionaryService by inject<DictionaryLookupService>()
    val textService by inject<TextService>()
    val sentenceService by inject<SentenceService>()
    val transliterationService by inject<TransliterationService>()
    val annotationService by inject<AnnotationService>()
    val sentenceAiClient by inject<OpenRouterSentenceClient>()
    val trainingService by inject<TrainingService>()
    val exerciseService by inject<ExerciseService>()
    val wordListService by inject<WordListService>()
    val aiWordListSuggestionService by inject<AiWordListSuggestionService>()
    val aiRootFamilySuggestionService by inject<AiRootFamilySuggestionService>()
    val aiInsightService by inject<AiInsightService>()
    val analyticsService by inject<AnalyticsService>()
    val conjugationService by inject<ConjugationService>()
    val conjugationExerciseService by inject<ConjugationExerciseService>()
    val productionPracticeService by inject<ProductionPracticeService>()

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse())
        }

        route("/api/v1") {
            swaggerUI(path = "swagger-ui", swaggerFile = "openapi/documentation.yaml")
            rootRoutes(rootService, aiRootFamilySuggestionService)

            wordRoutes(wordService, dictionaryService)
            wordListRoutes(wordListService, aiWordListSuggestionService)
            conjugationRoutes(conjugationService)
            conjugationExerciseRoutes(conjugationExerciseService)

            textRoutes(textService)
            sentenceRoutes(sentenceService, sentenceAiClient)

            transliterationRoutes(transliterationService)

            annotationRoutes(annotationService)
            annotationWordRoutes(annotationService)

            trainingRoutes(trainingService)
            exerciseRoutes(exerciseService)
            productionPracticeRoutes(productionPracticeService)

            aiInsightRoutes(aiInsightService)

            analyticsRoutes(analyticsService)
        }
    }
}
