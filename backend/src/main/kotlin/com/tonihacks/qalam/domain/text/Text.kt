package com.tonihacks.qalam.domain.text

import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class TextId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class Text(
    val id: TextId,
    val title: String,
    val body: String,
    val transliteration: String?,
    val translation: String?,
    val difficulty: Difficulty,
    val dialect: Dialect,
    val comments: String?,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
