package com.tonihacks.qalam.delivery.dto.root

import com.tonihacks.qalam.domain.root.ArabicRoot
import com.tonihacks.qalam.domain.word.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class CreateRootRequest(
    val root: String,
    val meaning: String? = null,
    val analysis: String? = null,
)

@Serializable
data class UpdateRootRequest(
    val meaning: String? = null,
    val analysis: String? = null,
)

@Serializable
data class RootResponse(
    val id: String,
    val letters: List<String>,
    val normalizedForm: String,
    val displayForm: String,
    val letterCount: Int,
    val meaning: String?,
    val analysis: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class NormalizeRequest(val input: String)

@Serializable
data class NormalizeResponse(
    val letters: List<String>,
    val normalizedForm: String,
    val displayForm: String,
    val letterCount: Int,
)

@Serializable
data class AiRootWordSuggestion(
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val partOfSpeech: String,
    val dialect: String,
    // default to intermediate when unsure (middle ground)
    val difficulty: String = Difficulty.INTERMEDIATE.toString(),
)

@Serializable
data class RootWordSuggestionsResponse(val suggestions: List<AiRootWordSuggestion>)

fun ArabicRoot.toResponse() = RootResponse(
    id = id.toString(),
    letters = letters,
    normalizedForm = normalizedForm,
    displayForm = displayForm,
    letterCount = letterCount,
    meaning = meaning,
    analysis = analysis,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
