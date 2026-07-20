package com.tonihacks.qalam.delivery.dto.wordlist

import com.tonihacks.qalam.delivery.dto.word.WordResponse
import com.tonihacks.qalam.delivery.dto.word.toResponse
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.wordlist.WordList
import com.tonihacks.qalam.domain.wordlist.WordListSummary
import kotlinx.serialization.Serializable

@Serializable
data class CreateWordListRequest(
    val title: String,
    val description: String? = null,
)

@Serializable
data class UpdateWordListRequest(
    val title: String? = null,
    val description: String? = null,
)

@Serializable
data class AddWordToListRequest(
    val wordId: String,
)

/** Index/summary view — includes member count, no member words. */
@Serializable
data class WordListResponse(
    val id: String,
    val title: String,
    val description: String?,
    val itemCount: Long,
    val createdAt: String,
    val updatedAt: String,
)

/** Detail view — includes the ordered member words. */
@Serializable
data class WordListDetailResponse(
    val id: String,
    val title: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val words: List<WordResponse>,
)

/** Lightweight reference used for word-detail membership chips. */
@Serializable
data class WordListRefResponse(
    val id: String,
    val title: String,
)

/**
 * AI-suggested new word for a list — ephemeral preview, never auto-saved.
 * Fields align with CreateWordRequest so the frontend can create the word directly.
 */
@Serializable
data class AiListWordSuggestion(
    val arabicText: String,
    val transliteration: String? = null,
    val translation: String? = null,
    val partOfSpeech: String? = null,
    val difficulty: String? = null,
    val dialect: String? = null,
)

@Serializable
data class WordListSuggestionsResponse(
    val suggestions: List<AiListWordSuggestion>,
)

fun WordListSummary.toResponse() = WordListResponse(
    id = list.id.toString(),
    title = list.title,
    description = list.description,
    itemCount = itemCount,
    createdAt = list.createdAt.toString(),
    updatedAt = list.updatedAt.toString(),
)

fun WordList.toDetailResponse(words: List<Word>) = WordListDetailResponse(
    id = id.toString(),
    title = title,
    description = description,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    words = words.map { it.toResponse() },
)

fun WordList.toRefResponse() = WordListRefResponse(
    id = id.toString(),
    title = title,
)
