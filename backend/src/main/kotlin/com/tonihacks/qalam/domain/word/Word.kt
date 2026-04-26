package com.tonihacks.qalam.domain.word

import com.tonihacks.qalam.domain.root.RootId
import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class WordId(val value: UUID) {
    override fun toString(): String = value.toString()
}

@JvmInline
value class DictionaryLinkId(val value: UUID) {
    override fun toString(): String = value.toString()
}

@JvmInline
value class WordExampleId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class WordExample(
    val id: WordExampleId,
    val wordId: WordId,
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
    val createdAt: Instant,
)

data class Word(
    val id: WordId,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val partOfSpeech: PartOfSpeech,
    val dialect: Dialect,
    val difficulty: Difficulty,
    val masteryLevel: MasteryLevel,
    val pronunciationUrl: String?,
    val rootId: RootId?,
    val derivedFromId: WordId?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@JvmInline
value class WordPluralId(val value: UUID) {
    override fun toString(): String = value.toString()
}

data class WordMorphology(
    val wordId: WordId,
    val gender: Gender?,
    val verbPattern: VerbPattern?,
)

data class WordPlural(
    val id: WordPluralId,
    val wordId: WordId,
    val pluralForm: String,
    val pluralType: PluralType,
)

data class WordRelation(
    val wordId: WordId,
    val relatedWordId: WordId,
    val relationType: RelationType,
)

data class DictionaryLink(
    val id: DictionaryLinkId,
    val wordId: WordId,
    val source: DictionarySource,
    val url: String,
)

data class WordProgress(
    val wordId: WordId,
    val consecutiveCorrect: Int,
    val totalAttempts: Int,
    val totalCorrect: Int,
    val lastReviewedAt: Instant?,
)
