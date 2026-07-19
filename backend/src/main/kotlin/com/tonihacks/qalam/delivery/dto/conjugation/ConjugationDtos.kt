package com.tonihacks.qalam.delivery.dto.conjugation

import com.tonihacks.qalam.domain.conjugation.model.ConjugationKey
import com.tonihacks.qalam.domain.conjugation.model.ConjugationTable
import com.tonihacks.qalam.domain.conjugation.model.PersonConjugation
import com.tonihacks.qalam.domain.conjugation.model.Segment
import kotlinx.serialization.Serializable

@Serializable
data class ConjugationResponse(
    val word: WordSummary? = null,
    val verbDetails: VerbDetailsSummary,
    val root: RootSummary,
    val dialect: String,
    val conjugations: Map<String, List<PersonConjugationDto>>,
)

@Serializable
data class WordSummary(
    val id: String,
    val arabicText: String,
    val translation: String?,
)

@Serializable
data class VerbDetailsSummary(
    val verbForm: String,
    val pastPattern: String?,
    val presentPattern: String?,
    val weaknessType: String,
)

@Serializable
data class RootSummary(
    val letters: List<String>,
)

@Serializable
data class PersonConjugationDto(
    val person: String,
    val arabic: String,
    val segments: List<SegmentDto>,
)

@Serializable
data class SegmentDto(
    val text: String,
    val type: String,
)

@Serializable
data class AdHocConjugationRequest(
    val rootLetters: List<String>,
    val verbForm: String,
    val pastPattern: String? = null,
    val presentPattern: String? = null,
    val weaknessType: String = "SOUND",
    val dialect: String = "MSA",
)

fun ConjugationTable.toResponseMap(): Map<String, List<PersonConjugationDto>> =
    forms.entries.associate { (key, persons) ->
        key.toResponseKey() to persons.map { it.toDto() }
    }

private fun ConjugationKey.toResponseKey(): String =
    "${tense.name.lowercase()}_${voice.name.lowercase()}"

private fun PersonConjugation.toDto() = PersonConjugationDto(
    person = person.code,
    arabic = arabic,
    segments = segments.map { it.toDto() },
)

private fun Segment.toDto() = SegmentDto(
    text = text,
    type = type.name,
)
