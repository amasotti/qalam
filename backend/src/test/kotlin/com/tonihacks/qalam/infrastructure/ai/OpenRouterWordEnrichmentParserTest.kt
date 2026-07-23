package com.tonihacks.qalam.infrastructure.ai

import com.tonihacks.qalam.domain.word.PartOfSpeech
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class OpenRouterWordEnrichmentParserTest : FreeSpec({
    "keeps useful noun enrichment when the model returns incomplete derived-verb details" {
        val result = parseWordEnrichmentSuggestion(
            """
            {
              "gender": "MASCULINE",
              "verbDetails": {
                "verbForm": "VIII",
                "pastPattern": null,
                "presentPattern": null,
                "weaknessType": null
              },
              "plurals": [{"pluralForm": "اِسْتِفْسارات", "pluralType": "SOUND_MASC"}],
              "relations": [{"arabicText": "سُؤال", "relationType": "SYNONYM"}],
              "notes": "Formal noun."
            }
            """.trimIndent(),
            PartOfSpeech.NOUN,
        )

        result.gender shouldBe "MASCULINE"
        result.verbDetails shouldBe null
        result.plurals.single().pluralForm shouldBe "اِسْتِفْسارات"
        result.relations.single().arabicText shouldBe "سُؤال"
        result.notes shouldBe "Formal noun."
    }

    "retains complete verb details for verbs" {
        val result = parseWordEnrichmentSuggestion(
            """
            {
              "verbDetails": {"verbForm": "I", "weaknessType": "HOLLOW"}
            }
            """.trimIndent(),
            PartOfSpeech.VERB,
        )

        result.verbDetails?.verbForm shouldBe "I"
        result.verbDetails?.weaknessType shouldBe "HOLLOW"
        result.plurals shouldBe emptyList()
        result.relations shouldBe emptyList()
    }
})
