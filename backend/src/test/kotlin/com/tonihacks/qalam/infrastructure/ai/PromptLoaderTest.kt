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
})
