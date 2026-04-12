package com.tonihacks.qalam.domain.annotation

import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@JvmInline
value class AnnotationId(val value: UUID) {
    override fun toString(): String = value.toString()
}

enum class AnnotationType { VOCAB, GRAMMAR, CULTURAL, STRUCTURE }

@OptIn(ExperimentalTime::class)
data class Annotation(
    val id: AnnotationId,
    val textId: TextId,
    val anchor: String,
    val type: AnnotationType,
    val content: String?,
    val masteryLevel: MasteryLevel?,
    val reviewFlag: Boolean,
    val linkedWordIds: List<WordId>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
