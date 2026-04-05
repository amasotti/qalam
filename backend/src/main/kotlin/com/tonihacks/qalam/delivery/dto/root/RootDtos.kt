package com.tonihacks.qalam.delivery.dto.root

import com.tonihacks.qalam.domain.root.ArabicRoot
import kotlinx.serialization.Serializable

@Serializable
data class CreateRootRequest(
    val letters: List<String>,
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
