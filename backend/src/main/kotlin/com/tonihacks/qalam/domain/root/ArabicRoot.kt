package com.tonihacks.qalam.domain.root

import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class RootId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class ArabicRoot(
    val id: RootId,
    val letters: List<String>,
    val normalizedForm: String,
    val displayForm: String,
    val letterCount: Int,
    val meaning: String?,
    val analysis: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
