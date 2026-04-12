package com.tonihacks.qalam.delivery.dto.word

import com.tonihacks.qalam.domain.word.DictionaryLink
import com.tonihacks.qalam.domain.word.Word
import kotlinx.serialization.Serializable

@Serializable
data class CreateWordRequest(
    val arabicText: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val exampleSentence: String? = null,
    val partOfSpeech: String = "UNKNOWN",
    val dialect: String = "MSA",
    val difficulty: String = "BEGINNER",
    val pronunciationUrl: String? = null,
    val rootId: String? = null,
    val derivedFromId: String? = null,
)

@Serializable
data class UpdateWordRequest(
    val arabicText: String? = null,
    val transliteration: String? = null,
    val translation: String? = null,
    val exampleSentence: String? = null,
    val partOfSpeech: String? = null,
    val dialect: String? = null,
    val difficulty: String? = null,
    val masteryLevel: String? = null,
    val pronunciationUrl: String? = null,
    val rootId: String? = null,
    val derivedFromId: String? = null,
)

@Serializable
data class WordResponse(
    val id: String,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val exampleSentence: String?,
    val partOfSpeech: String,
    val dialect: String,
    val difficulty: String,
    val masteryLevel: String,
    val pronunciationUrl: String?,
    val rootId: String?,
    val derivedFromId: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class WordAutocompleteResponse(
    val id: String,
    val arabicText: String,
    val translation: String?,
)

@Serializable
data class CreateDictionaryLinkRequest(
    val source: String,
    val url: String,
)

@Serializable
data class DictionaryLinkResponse(
    val id: String,
    val source: String,
    val url: String,
)

@Serializable
data class ExampleSentenceResponse(
    val arabic: String,
    val transliteration: String,
    val translation: String,
)

@Serializable
data class ExamplesResponse(val examples: List<ExampleSentenceResponse>)

fun Word.toResponse() = WordResponse(
    id = id.toString(),
    arabicText = arabicText,
    transliteration = transliteration,
    translation = translation,
    exampleSentence = exampleSentence,
    partOfSpeech = partOfSpeech.name,
    dialect = dialect.name,
    difficulty = difficulty.name,
    masteryLevel = masteryLevel.name,
    pronunciationUrl = pronunciationUrl,
    rootId = rootId?.toString(),
    derivedFromId = derivedFromId?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun Word.toAutocompleteResponse() = WordAutocompleteResponse(
    id = id.toString(),
    arabicText = arabicText,
    translation = translation,
)

fun DictionaryLink.toResponse() = DictionaryLinkResponse(
    id = id.toString(),
    source = source.name,
    url = url,
)
