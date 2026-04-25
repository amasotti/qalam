package com.tonihacks.qalam.delivery.dto.word

import com.tonihacks.qalam.domain.word.DictionaryLink
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordExample
import kotlinx.serialization.Serializable

@Serializable
data class CreateWordRequest(
    val arabicText: String,
    val transliteration: String? = null,
    val translation: String? = null,
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
data class WordExampleResponse(
    val id: String,
    val arabic: String,
    val transliteration: String?,
    val translation: String?,
    val createdAt: String,
)

@Serializable
data class CreateWordExampleRequest(
    val arabic: String,
    val transliteration: String? = null,
    val translation: String? = null,
)

@Serializable
data class AiExampleSentence(
    val arabic: String,
    val transliteration: String,
    val translation: String,
)

@Serializable
data class AiExamplesResponse(val examples: List<AiExampleSentence>)

fun Word.toResponse() = WordResponse(
    id = id.toString(),
    arabicText = arabicText,
    transliteration = transliteration,
    translation = translation,
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

fun WordExample.toResponse() = WordExampleResponse(
    id = id.toString(),
    arabic = arabic,
    transliteration = transliteration,
    translation = translation,
    createdAt = createdAt.toString(),
)

fun DictionaryLink.toResponse() = DictionaryLinkResponse(
    id = id.toString(),
    source = source.name,
    url = url,
)

@Serializable
data class AnalyzeWordRequest(val arabicText: String)

@Serializable
data class WordAnalysisResponse(
    val arabicText: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val partOfSpeech: String? = null,
    val rootLetters: String? = null,
    val exampleSentence: AiExampleSentence? = null,
)
