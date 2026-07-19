package com.tonihacks.qalam.delivery.dto.word

import com.tonihacks.qalam.domain.word.VerbDetails
import kotlinx.serialization.Serializable

@Serializable
data class VerbDetailsResponse(
    val verbForm: String,
    val pastPattern: String?,
    val presentPattern: String?,
    val weaknessType: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class UpsertVerbDetailsRequest(
    val verbForm: String,
    val pastPattern: String? = null,
    val presentPattern: String? = null,
    val weaknessType: String = "SOUND",
)

fun VerbDetails.toResponse() = VerbDetailsResponse(
    verbForm = verbForm.name,
    pastPattern = pastPattern,
    presentPattern = presentPattern,
    weaknessType = weaknessType.name,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
