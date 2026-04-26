package com.tonihacks.qalam.domain.training

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val mode: String,
    val size: Int = 15,
)

@Serializable
data class RecordResultRequest(
    val wordId: String,
    val result: String,
)

@Serializable
data class TrainingWordExampleResponse(
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
)

@Serializable
data class TrainingWordRelationResponse(
    val relatedWordId: String,
    val relatedWordArabic: String,
    val relatedWordTranslation: String?,
    val relationType: String,
)

@Serializable
data class TrainingSessionWordResponse(
    val wordId: String,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val frontSide: String,
    val position: Int,
    val result: String?,
    val masteryLevel: String,
    val root: String? = null,
    val notes: String? = null,
    val examples: List<TrainingWordExampleResponse> = emptyList(),
    val relations: List<TrainingWordRelationResponse> = emptyList(),
)

@Serializable
data class TrainingSessionResponse(
    val id: String,
    val mode: String,
    val status: String,
    val words: List<TrainingSessionWordResponse>,
    val createdAt: String,
    val completedAt: String?,
)

@Serializable
data class MasteryPromotionResponse(
    val wordId: String,
    val from: String,
    val to: String,
)

@Serializable
data class RecordResultResponse(
    val wordId: String,
    val result: String,
    val masteryPromotion: MasteryPromotionResponse?,
)

@Serializable
data class SessionSummaryResponse(
    val sessionId: String,
    val mode: String,
    val totalWords: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val accuracy: Double,
    val promotions: List<MasteryPromotionResponse>,
    val completedAt: String,
)

@Serializable
data class TrainingSessionListItemResponse(
    val id: String,
    val mode: String,
    val status: String,
    val totalWords: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val accuracy: Double,
    val createdAt: String,
    val completedAt: String?,
)

@Serializable
data class TrainingStatsResponse(
    val masteryDistribution: Map<String, Int>,
    val totalSessions: Int,
    val recentSessions: List<TrainingSessionListItemResponse>,
)

@Serializable
data class PaginatedSessionsResponse(
    val items: List<TrainingSessionListItemResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
)
