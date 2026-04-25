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
    val linkedWordIds: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateAnnotationRequest(
    val anchor: String,
    val type: String,
    val content: String? = null,
    val linkedWordIds: List<String> = emptyList(),
)

@Serializable
data class UpdateAnnotationRequest(
    val anchor: String? = null,
    val type: String? = null,
    val content: String? = null,
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
    linkedWordIds = linkedWordIds.map { it.toString() },
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
