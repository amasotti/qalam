package com.tonihacks.qalam.delivery.dto.annotation

import com.tonihacks.qalam.domain.annotation.Annotation
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class AnnotationResponse(
    val id: String,
    val textId: String,
    val anchor: String,
    val type: String,
    val content: String?,
    val masteryLevel: String?,
    val reviewFlag: Boolean,
    val linkedWordIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateAnnotationRequest(
    val anchor: String,
    val type: String,
    val content: String? = null,
    val masteryLevel: String? = null,
    val reviewFlag: Boolean = false,
    val linkedWordIds: List<String> = emptyList(),
)

@Serializable
data class UpdateAnnotationRequest(
    val anchor: String? = null,
    val type: String? = null,
    val content: String? = null,
    val masteryLevel: String? = null,
    val reviewFlag: Boolean? = null,
)

@Serializable
data class AddWordLinkRequest(
    val wordId: String,
)

@OptIn(ExperimentalTime::class)
fun Annotation.toResponse() = AnnotationResponse(
    id = id.toString(),
    textId = textId.toString(),
    anchor = anchor,
    type = type.name,
    content = content,
    masteryLevel = masteryLevel?.name,
    reviewFlag = reviewFlag,
    linkedWordIds = linkedWordIds.map { it.toString() },
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
