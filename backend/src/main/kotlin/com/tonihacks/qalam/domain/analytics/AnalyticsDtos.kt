package com.tonihacks.qalam.domain.analytics

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsOverviewResponse(
    val words: WordStats,
    val texts: TextStats,
    val roots: RootStats,
    val training: TrainingAnalytics,
)

@Serializable
data class WordStats(
    val total: Int,
    val byDialect: Map<String, Int>,
    val byDifficulty: Map<String, Int>,
    val byMastery: Map<String, Int>,
    val byPartOfSpeech: Map<String, Int>,
)

@Serializable
data class TextStats(
    val total: Int,
    val byDialect: Map<String, Int>,
    val byDifficulty: Map<String, Int>,
)

@Serializable
data class RootStats(
    val total: Int,
)

@Serializable
data class TrainingAnalytics(
    val totalSessions: Int,
    val completedSessions: Int,
    val averageAccuracy: Double,
    val totalPromotions: Int,
    val recentSessions: List<SessionAccuracyPoint>,
)

@Serializable
data class SessionAccuracyPoint(
    val date: String,
    val accuracy: Double,
    val mode: String,
)
