package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeCorrection
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReview
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReviewRequest
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeReviewer
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeVerdict
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeWordFeedback
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.WordId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

internal class OpenRouterProductionPracticeReviewer(
    private val openRouter: OpenRouterClient,
) : ProductionPracticeReviewer {
    private val log = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun review(request: ProductionPracticeReviewRequest): Either<DomainError, ProductionPracticeReview> {
        val result = openRouter.complete(
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/ProductionPracticeSystemPrompt.md"),
                userPrompt = buildProductionPracticeReviewPrompt(request),
                responseFormat = OpenRouterResponseFormat(
                    type = "json_schema",
                    jsonSchema = OpenRouterJsonSchema(
                        name = "production_practice_review",
                        strict = true,
                        schema = productionPracticeReviewSchema,
                    ),
                ),
                provider = OpenRouterProviderPreferences(requireParameters = true),
            ),
        )

        return result.fold(
            { error ->
                log.warn { "OpenRouter production-practice review failed: $error" }
                if (error == DomainError.AiNotConfigured) error.left()
                else DomainError.InvalidInput("AI production-practice review request failed").left()
            },
            { content ->
                parseProductionPracticeReview(content, json)
                    ?.toReview(request.targetWords.mapTo(linkedSetOf()) { it.id })
                    ?.right()
                    ?: DomainError.InvalidInput("AI production-practice review response was invalid").left()
            },
        )
    }
}

internal fun buildProductionPracticeReviewPrompt(request: ProductionPracticeReviewRequest): String =
    PromptLoader.loadPrompt(
        "ai-prompts/ReviewProductionPracticeUserPrompt.md",
        mapOf(
            "sentence" to request.sentence,
            "targetWords" to request.targetWords.joinToString("\n") { word ->
                "- id: ${word.id}; Arabic: ${word.arabicText}; translation: ${word.translation.orEmpty()}; " +
                    "part of speech: ${word.partOfSpeech}; dialect: ${word.dialect}"
            },
            "usedWordIds" to request.usedWordIds.joinToString(),
            "outputSchema" to productionPracticeReviewSchema.toString(),
        ),
    )

@Serializable
internal data class OpenRouterProductionPracticeReview(
    val verdict: String,
    val wordFeedback: List<OpenRouterProductionPracticeWordFeedback>,
    val corrections: List<OpenRouterProductionPracticeCorrection>,
    val improvedSentence: String?,
    val comment: String,
)

@Serializable
internal data class OpenRouterProductionPracticeWordFeedback(
    val wordId: String,
    val usedNaturally: Boolean,
    val note: String,
)

@Serializable
internal data class OpenRouterProductionPracticeCorrection(
    val original: String,
    val suggestion: String,
    val explanation: String,
)

internal fun parseProductionPracticeReview(content: String, json: Json): OpenRouterProductionPracticeReview? =
    runCatching { json.decodeFromString<OpenRouterProductionPracticeReview>(content) }.getOrNull()

internal fun OpenRouterProductionPracticeReview.toReview(targetWordIds: Set<WordId>): ProductionPracticeReview? {
    val parsedFeedback = wordFeedback.map { feedback ->
        val id = runCatching { WordId(UUID.fromString(feedback.wordId)) }.getOrNull() ?: return null
        ProductionPracticeWordFeedback(id, feedback.usedNaturally, feedback.note)
    }
    if (parsedFeedback.size != targetWordIds.size || parsedFeedback.mapTo(linkedSetOf()) { it.wordId } != targetWordIds) return null
    val verdict = runCatching { ProductionPracticeVerdict.valueOf(verdict) }.getOrNull() ?: return null
    if (comment.isBlank() || parsedFeedback.any { it.note.isBlank() }) return null

    return ProductionPracticeReview(
        verdict = verdict,
        wordFeedback = parsedFeedback,
        corrections = corrections.map { ProductionPracticeCorrection(it.original, it.suggestion, it.explanation) },
        improvedSentence = improvedSentence?.takeIf { it.isNotBlank() },
        comment = comment,
    )
}
