package com.tonihacks.qalam.delivery.routes

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.application.productionpractice.ProductionPracticePrompt
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReview
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReviewCommand
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeService
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeWord
import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.productionpractice.ProductionPracticePromptResponse
import com.tonihacks.qalam.delivery.dto.productionpractice.ProductionPracticeReviewResponse
import com.tonihacks.qalam.delivery.dto.productionpractice.ProductionPracticeWordResponse
import com.tonihacks.qalam.delivery.dto.productionpractice.ReviewProductionPracticeRequest
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.WordId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.productionPracticeRoutes(service: ProductionPracticeService) {
    route("/production-practice") {
        get("/prompt") {
            service.createPrompt().fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        post("/reviews") {
            val request = call.receive<ReviewProductionPracticeRequest>()
            toCommand(request).fold(
                { call.respondError(it) },
                { command ->
                    service.review(command).fold(
                        { call.respondError(it) },
                        { call.respond(HttpStatusCode.OK, it.toResponse()) },
                    )
                },
            )
        }
    }
}

private fun toCommand(request: ReviewProductionPracticeRequest): Either<DomainError, ProductionPracticeReviewCommand> = either {
    ProductionPracticeReviewCommand(
        sentence = request.sentence,
        targetWordIds = request.targetWordIds.map { parseWordId(it, "targetWordIds").bind() },
        usedWordIds = request.usedWordIds.map { parseWordId(it, "usedWordIds").bind() },
    )
}

private fun parseWordId(value: String, field: String): Either<DomainError, WordId> =
    try {
        Either.Right(WordId(UUID.fromString(value)))
    } catch (_: IllegalArgumentException) {
        Either.Left(DomainError.InvalidInput("$field contains an invalid UUID: '$value'"))
    }

private fun ProductionPracticePrompt.toResponse() = ProductionPracticePromptResponse(words.map(ProductionPracticeWord::toResponse))

private fun ProductionPracticeWord.toResponse() = ProductionPracticeWordResponse(
    id = id.toString(),
    arabicText = arabicText,
    transliteration = transliteration,
    translation = translation,
    partOfSpeech = partOfSpeech.name,
    dialect = dialect.name,
)

private fun ProductionPracticeReview.toResponse() = ProductionPracticeReviewResponse(reviewMarkdown = markdown)
