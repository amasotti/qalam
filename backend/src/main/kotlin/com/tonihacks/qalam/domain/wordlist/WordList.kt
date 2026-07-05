package com.tonihacks.qalam.domain.wordlist

import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class WordListId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class WordList(
    val id: WordListId,
    val title: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A list plus its member count — the read model for the index/summary view. */
data class WordListSummary(
    val list: WordList,
    val itemCount: Long,
)
