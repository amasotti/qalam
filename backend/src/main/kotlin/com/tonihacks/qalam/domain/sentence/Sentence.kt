package com.tonihacks.qalam.domain.sentence

import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class SentenceId(val value: UUID) {
    override fun toString(): String = value.toString()
}

@JvmInline
value class AlignmentTokenId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class AlignmentToken(
    val id: AlignmentTokenId,
    val sentenceId: SentenceId,
    val position: Int,
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
    val wordId: WordId?,
)

/**
 * A single sentence within a [com.tonihacks.qalam.domain.text.Text].
 *
 * When [arabicText] is changed, [tokensValid] MUST be set to false.
 * This is enforced by [SentenceService], not by a DB trigger.
 * Any call to [SentenceRepository.update] with a changed arabicText
 * must supply a Sentence copy with tokensValid = false.
 */
data class Sentence(
    val id: SentenceId,
    val textId: TextId,
    val position: Int,
    val arabicText: String,
    val transliteration: String?,
    val freeTranslation: String?,
    val notes: String?,
    val tokensValid: Boolean,
    val tokens: List<AlignmentToken>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
