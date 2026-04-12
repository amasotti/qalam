package com.tonihacks.qalam.delivery.dto.text

import com.tonihacks.qalam.domain.text.Text
import kotlinx.serialization.Serializable

@Serializable
data class CreateTextRequest(
    val title: String,
    val body: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val difficulty: String = "BEGINNER",
    val dialect: String = "MSA",
    val comments: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class UpdateTextRequest(
    val title: String? = null,
    val body: String? = null,
    val transliteration: String? = null,
    val translation: String? = null,
    val difficulty: String? = null,
    val dialect: String? = null,
    val comments: String? = null,
    val tags: List<String>? = null,
)

@Serializable
data class TextResponse(
    val id: String,
    val title: String,
    val body: String,
    val transliteration: String?,
    val translation: String?,
    val difficulty: String,
    val dialect: String,
    val comments: String?,
    val tags: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

fun Text.toResponse() = TextResponse(
    id = id.toString(),
    title = title,
    body = body,
    transliteration = transliteration,
    translation = translation,
    difficulty = difficulty.name,
    dialect = dialect.name,
    comments = comments,
    tags = tags,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
