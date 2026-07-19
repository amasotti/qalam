package com.tonihacks.qalam.domain.conjugationexercise

import kotlinx.serialization.Serializable

@Serializable
data class CreateConjugationExerciseSessionRequest(
    val mode: String,
    val size: Int = 5,
    val wordListIds: List<String> = emptyList(),
    val tense: String = "PRESENT",
    val voice: String = "ACTIVE",
    val exerciseType: String = "MATCH_FORM",
)

@Serializable
data class ConjugationExerciseEligibilityResponse(val availableVerbs: Int)

@Serializable
data class ConjugationExerciseFormResponse(
    val formId: String,
    val arabic: String,
    val segments: List<ConjugationExerciseSegmentResponse>,
)

@Serializable
data class ConjugationExerciseSegmentResponse(val text: String, val type: String)

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
    val exerciseType: String,
    val forms: List<ConjugationExerciseFormResponse>,
    val labels: List<ConjugationExerciseLabelResponse>,
    val result: String?,
    val submittedMappings: List<ConjugationExerciseMappingResponse>?,
    val correctMappings: List<ConjugationExerciseMappingResponse>?,
)

@Serializable
data class ConjugationExerciseMappingRequest(val formId: String, val labelId: String)

@Serializable
data class AnswerConjugationExerciseItemRequest(
    val itemId: String,
    val mappings: List<ConjugationExerciseMappingRequest> = emptyList(),
    val submittedText: String? = null,
)

@Serializable
data class ConjugationExerciseMappingResponse(
    val formId: String,
    val labelId: String,
    val isCorrect: Boolean? = null,
)

@Serializable
data class AnswerConjugationExerciseItemResponse(
    val itemId: String,
    val result: String,
    val submittedMappings: List<ConjugationExerciseMappingResponse>,
    val correctMappings: List<ConjugationExerciseMappingResponse>,
    val expectedArabic: String? = null,
    val submittedText: String? = null,
)

@Serializable
data class ConjugationExerciseSessionResponse(
    val id: String,
    val mode: String,
    val status: String,
    val exerciseType: String,
    val items: List<ConjugationExerciseItemResponse>,
    val createdAt: String,
    val completedAt: String?,
)

@Serializable
data class ConjugationExerciseSessionSummaryResponse(
    val sessionId: String,
    val mode: String,
    val totalItems: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val accuracy: Double,
    val completedAt: String,
)

@Serializable
data class ConjugationExerciseSessionListItemResponse(
    val id: String,
    val mode: String,
    val status: String,
    val tense: String,
    val voice: String,
    val totalItems: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val accuracy: Double,
    val createdAt: String,
    val completedAt: String?,
)

@Serializable
data class PaginatedConjugationExerciseSessionsResponse(
    val items: List<ConjugationExerciseSessionListItemResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
)
