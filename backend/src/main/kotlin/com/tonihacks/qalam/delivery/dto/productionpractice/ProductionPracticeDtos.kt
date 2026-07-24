package com.tonihacks.qalam.delivery.dto.productionpractice

import kotlinx.serialization.Serializable

@Serializable
data class ProductionPracticePromptResponse(
    val words: List<ProductionPracticeWordResponse>,
)

@Serializable
data class ProductionPracticeWordResponse(
    val id: String,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val partOfSpeech: String,
    val dialect: String,
)

@Serializable
data class ReviewProductionPracticeRequest(
    val sentence: String,
    val targetWordIds: List<String>,
    val usedWordIds: List<String>,
    val intendedMeaning: String? = null,
)

@Serializable
data class ProductionPracticeReviewResponse(
    val reviewMarkdown: String,
)
