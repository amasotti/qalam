package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReview
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReviewRequest
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReviewer
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.collections.filter

internal class OpenRouterProductionPracticeReviewer(
    private val openRouter: OpenRouterClient,
) : ProductionPracticeReviewer {
    private val log = KotlinLogging.logger {}

    override suspend fun review(request: ProductionPracticeReviewRequest): Either<DomainError, ProductionPracticeReview> =
        openRouter.complete(
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/ProductionPracticeSystemPrompt.md"),
                userPrompt = buildProductionPracticeReviewPrompt(request),
                model = "anthropic/claude-opus-4.8"
            ),
        ).fold(
            { error ->
                log.warn { "OpenRouter production-practice review failed: $error" }
                if (error == DomainError.AiNotConfigured) error.left()
                else DomainError.InvalidInput("AI production-practice review request failed").left()
            },
            { markdown ->
                val trimmedMarkdown = markdown.trim()
                if (trimmedMarkdown.isBlank()) {
                    DomainError.InvalidInput("AI production-practice review was empty").left()
                } else {
                    log.debug { "OpenRouter production-practice review:\n$trimmedMarkdown" }
                    ProductionPracticeReview(trimmedMarkdown).right()
                }
            },
        )
}

internal fun buildProductionPracticeReviewPrompt(request: ProductionPracticeReviewRequest): String =
    PromptLoader.loadPrompt(
        "ai-prompts/ReviewProductionPracticeUserPrompt.md",
        mapOf(
            "sentence" to request.sentence,
            "usedWords" to request.usedWords.joinToString("\n") { w -> "- ${w.arabicText} (${w.translation})" },
            "intendedMeaning" to request.intendedMeaning.orEmpty(),
        ),
)
