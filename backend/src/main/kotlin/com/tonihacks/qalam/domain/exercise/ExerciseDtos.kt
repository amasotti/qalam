package com.tonihacks.qalam.domain.exercise

import com.tonihacks.qalam.domain.training.MasteryPromotionResponse
import kotlinx.serialization.Serializable

@Serializable
data class CreateExerciseSessionRequest(
    val mode: String,
    val size: Int = 15,
    val wordListIds: List<String> = emptyList(),
    val exerciseTypes: List<String> = listOf(ExerciseType.MULTIPLE_CHOICE_MEANING.name),
    val optionCount: Int = 4,
)

@Serializable
data class AnswerExerciseItemRequest(
    val itemId: String,
    val selectedOptionId: String,
)

@Serializable
data class ExercisePromptResponse(
    val kind: String,
    val text: String,
)

@Serializable
data class ExerciseOptionResponse(
    val optionId: String,
    val wordId: String,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
)

@Serializable
data class ExerciseSessionItemResponse(
    val itemId: String,
    val wordId: String,
    val type: String,
    val prompt: ExercisePromptResponse,
    val options: List<ExerciseOptionResponse>,
    val result: String?,
    val selectedOptionId: String?,
    val answeredAt: String?,
)

@Serializable
data class ExerciseSessionResponse(
    val id: String,
    val mode: String,
    val status: String,
    val items: List<ExerciseSessionItemResponse>,
    val createdAt: String,
    val completedAt: String?,
)

@Serializable
data class AnswerExerciseItemResponse(
    val itemId: String,
    val wordId: String,
    val result: String,
    val correctOptionId: String,
    val masteryPromotion: MasteryPromotionResponse?,
)

@Serializable
data class ExerciseSessionSummaryResponse(
    val sessionId: String,
    val mode: String,
    val totalItems: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val accuracy: Double,
    val promotions: List<MasteryPromotionResponse>,
    val completedAt: String,
)

@Serializable
data class ExerciseSessionListItemResponse(
    val id: String,
    val mode: String,
    val status: String,
    val totalItems: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val accuracy: Double,
    val createdAt: String,
    val completedAt: String?,
)

@Serializable
data class PaginatedExerciseSessionsResponse(
    val items: List<ExerciseSessionListItemResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
)
