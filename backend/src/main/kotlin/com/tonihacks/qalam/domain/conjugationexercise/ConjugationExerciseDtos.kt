package com.tonihacks.qalam.domain.conjugationexercise

import kotlinx.serialization.Serializable

@Serializable
data class CreateConjugationExerciseSessionRequest(
    val mode: String,
    val size: Int = 5,
    val wordListIds: List<String> = emptyList(),
    val tense: String = "PRESENT",
    val voice: String = "ACTIVE",
)

@Serializable
data class ConjugationExerciseFormResponse(
    val formId: String,
    val arabic: String,
)

@Serializable
data class ConjugationExerciseLabelResponse(
    val labelId: String,
    val person: String,
    val label: String,
)

@Serializable
data class ConjugationExerciseItemResponse(
    val itemId: String,
    val wordId: String,
    val lemma: String,
    val translation: String?,
    val verbForm: String,
    val tense: String,
    val voice: String,
    val forms: List<ConjugationExerciseFormResponse>,
    val labels: List<ConjugationExerciseLabelResponse>,
)

@Serializable
data class ConjugationExerciseSessionResponse(
    val id: String,
    val mode: String,
    val status: String,
    val items: List<ConjugationExerciseItemResponse>,
    val createdAt: String,
)
