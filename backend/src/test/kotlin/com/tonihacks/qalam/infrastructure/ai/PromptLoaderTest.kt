package com.tonihacks.qalam.infrastructure.ai

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class PromptLoaderTest : FunSpec({
    test("loads root resources and replaces every declared placeholder") {
        val prompt = PromptLoader.loadPrompt(
            "ai-prompts/SuggestWordsForListUserPrompt.md",
            mapOf(
                "title" to "Food",
                "description" to "Everyday vocabulary",
                "existingWords" to "The list is currently empty.",
                "partOfSpeechValues" to "NOUN, VERB",
                "difficultyValues" to "BEGINNER",
                "dialectValues" to "MSA, TUNISIAN",
            ),
        )

        prompt shouldNotContain "<title>"
        prompt shouldNotContain "<partOfSpeechValues>"
        prompt.contains("Description: Everyday vocabulary") shouldBe true
    }

    test("renders word capability prompts without placeholder leakage") {
        val prompts = listOf(
            PromptLoader.loadPrompt(
                "ai-prompts/GenerateWordExamplesUserPrompt.md",
                mapOf("arabicText" to "كتاب", "translationHint" to " (meaning: \"book\")"),
            ),
            PromptLoader.loadPrompt(
                "ai-prompts/AnalyzeWordUserPrompt.md",
                mapOf("arabicText" to "كتاب", "partOfSpeechValues" to "NOUN, VERB",),
            ),
            PromptLoader.loadPrompt(
                "ai-prompts/EnrichWordUserPrompt.md",
                mapOf(
                    "arabicText" to "كتاب",
                    "transliterationClause" to " [ktaab]",
                    "translation" to "book",
                    "partOfSpeech" to "NOUN",
                    "dialect" to "MSA",
                ),
            ),
        )

        prompts.forEach { prompt -> prompt shouldNotContain "<" }
    }
})
