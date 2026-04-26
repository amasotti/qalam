package com.tonihacks.qalam.domain.ai

sealed class InsightContext {

    data class WordInsight(
        val arabicText: String,
        val translation: String?,
        val partOfSpeech: String,
        val dialect: String,
        val rootLetters: String?,
        val rootMeaning: String?,
        val examples: List<String>,
    ) : InsightContext()

    data class SentenceInsight(
        val targetArabic: String,
        val targetTranslation: String?,
        val targetIndex: Int,
        val dialect: String,
        val textTitle: String?,
        val allSentences: List<Pair<String, String?>>,
        val mode: InsightMode,
    ) : InsightContext()
}
