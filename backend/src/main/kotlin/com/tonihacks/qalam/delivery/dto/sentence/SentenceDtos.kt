package com.tonihacks.qalam.delivery.dto.sentence

import com.tonihacks.qalam.domain.sentence.AlignmentToken
import com.tonihacks.qalam.domain.sentence.Sentence
import kotlinx.serialization.Serializable

@Serializable
data class AlignmentTokenResponse(
    val id: String,
    val sentenceId: String,
    val position: Int,
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
    val wordId: String?,
)

@Serializable
data class SentenceResponse(
    val id: String,
    val textId: String,
    val position: Int,
    val arabicText: String,
    val transliteration: String?,
    val freeTranslation: String?,
    val notes: String?,
    val tokensValid: Boolean,
    val tokens: List<AlignmentTokenResponse>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateSentenceRequest(
    val arabicText: String,
    val position: Int? = null,
    val transliteration: String? = null,
    val freeTranslation: String? = null,
    val notes: String? = null,
)

@Serializable
data class UpdateSentenceRequest(
    val arabicText: String? = null,
    val position: Int? = null,
    val transliteration: String? = null,
    val freeTranslation: String? = null,
    val notes: String? = null,
)

@Serializable
data class TokenInputDto(
    val position: Int,
    val arabic: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val wordId: String? = null,
)

@Serializable
data class ReplaceTokensRequest(
    val tokens: List<TokenInputDto>,
)

@Serializable
data class ReorderSentencesRequest(
    val orderedIds: List<String>,
)

fun AlignmentToken.toResponse() = AlignmentTokenResponse(
    id = id.toString(),
    sentenceId = sentenceId.toString(),
    position = position,
    arabic = arabic,
    transliteration = transliteration,
    translation = translation,
    wordId = wordId?.toString(),
)

fun Sentence.toResponse() = SentenceResponse(
    id = id.toString(),
    textId = textId.toString(),
    position = position,
    arabicText = arabicText,
    transliteration = transliteration,
    freeTranslation = freeTranslation,
    notes = notes,
    tokensValid = tokensValid,
    tokens = tokens.map { it.toResponse() },
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
