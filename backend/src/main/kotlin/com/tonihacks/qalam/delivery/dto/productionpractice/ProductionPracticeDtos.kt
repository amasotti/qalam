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
)

@Serializable
data class ProductionPracticeReviewResponse(
    val verdict: String,
    val wordFeedback: List<ProductionPracticeWordFeedbackResponse>,
    val corrections: List<ProductionPracticeCorrectionResponse>,
    val improvedSentence: String?,
    val comment: String,
)

@Serializable
data class ProductionPracticeWordFeedbackResponse(
    val wordId: String,
    val usedNaturally: Boolean,
    val note: String,
)

@Serializable
data class ProductionPracticeCorrectionResponse(
    val original: String,
    val suggestion: String,
    val explanation: String,
)
