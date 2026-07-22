package com.tonihacks.qalam.infrastructure.ai

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.application.InsightGenerator
import com.tonihacks.qalam.domain.ai.InsightContext
import com.tonihacks.qalam.domain.ai.InsightMode
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging

internal class OpenRouterInsightClient(
    private val openRouter: OpenRouterClient,
) : InsightGenerator {
    private val log = KotlinLogging.logger {}

    override suspend fun generateInsight(context: InsightContext): Either<DomainError, String> =
        openRouter.complete(
            OpenRouterCompletionRequest(
                systemPrompt = PromptLoader.loadPrompt("ai-prompts/InsightSystemPrompt.md"),
                userPrompt = buildPrompt(context),
            ),
        ).fold(
            { error ->
                log.warn { "OpenRouter generateInsight failed: $error" }
                if (error == DomainError.AiNotConfigured) error.left()
                else DomainError.InvalidInput("AI request failed").left()
            },
            { it.right() },
        )

    private fun buildPrompt(context: InsightContext): String = when (context) {
        is InsightContext.WordInsight -> PromptLoader.loadPrompt(
            "ai-prompts/GenerateWordInsightUserPrompt.md",
            mapOf(
                "arabicText" to context.arabicText,
                "translationClause" to context.translation?.let { " (\"$it\")" }.orEmpty(),
                "partOfSpeech" to context.partOfSpeech,
                "dialect" to context.dialect,
                "rootClause" to context.rootLetters?.let { rootLetters ->
                    "\nRoot: $rootLetters" + context.rootMeaning?.let { " — meaning: \"$it\"." }.orEmpty() + "."
                }.orEmpty(),
                "examplesClause" to context.examples.takeIf { it.isNotEmpty() }?.joinToString(
                    prefix = "\nExample usage: ",
                    separator = " / ",
                ) { "\"$it\"" }.orEmpty(),
            ),
        )
        is InsightContext.SentenceInsight -> PromptLoader.loadPrompt(
            "ai-prompts/GenerateSentenceInsightUserPrompt.md",
            mapOf(
                "targetIndex" to context.targetIndex.toString(),
                "textTitle" to (context.textTitle ?: "untitled"),
                "dialect" to context.dialect,
                "truncationNote" to if (context.allSentences.size <= 10) "Note: context truncated to ±5 sentences around the target.\n" else "",
                "sentences" to formatSentences(context),
                "modeInstruction" to when (context.mode) {
                    InsightMode.HOMEWORK -> "This is a student-authored sentence. Prioritise corrections and natural alternatives over analysis."
                    InsightMode.READING -> "This is a native-authored sentence. Focus on nuance, notable constructions, and vocabulary choices."
                },
            ),
        )
    }

    private fun formatSentences(context: InsightContext.SentenceInsight): String {
        val targetListIndex = context.allSentences.indexOfFirst { (arabic, _) -> arabic == context.targetArabic }
        return context.allSentences.mapIndexed { index, (arabic, translation) ->
            val translationPart = translation?.let { " — $it" }.orEmpty()
            if (index == targetListIndex) "→ ${context.targetIndex}. $arabic$translationPart    ← target"
            else "  ${index + 1}. $arabic$translationPart"
        }.joinToString("\n")
    }
}
