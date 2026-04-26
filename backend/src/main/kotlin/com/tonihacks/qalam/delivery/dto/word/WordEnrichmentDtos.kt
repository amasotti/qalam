package com.tonihacks.qalam.delivery.dto.word

import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordMorphology
import com.tonihacks.qalam.domain.word.WordPlural
import com.tonihacks.qalam.domain.word.WordRelation
import kotlinx.serialization.Serializable

@Serializable
data class WordMorphologyResponse(
    val gender: String?,
    val verbPattern: String?,
    val plurals: List<WordPluralResponse>,
)

@Serializable
data class WordPluralResponse(
    val id: String,
    val pluralForm: String,
    val pluralType: String,
)

@Serializable
data class CreateWordPluralRequest(
    val pluralForm: String,
    val pluralType: String = "BROKEN",
)

@Serializable
data class UpsertWordMorphologyRequest(
    val gender: String? = null,
    val verbPattern: String? = null,
)

@Serializable
data class WordRelationResponse(
    val relatedWordId: String,
    val relatedWordArabic: String,
    val relatedWordTranslation: String?,
    val relationType: String,
)

@Serializable
data class CreateWordRelationRequest(
    val relatedWordId: String,
    val relationType: String,
)

// AI enrichment preview — never auto-saved
@Serializable
data class WordEnrichmentSuggestion(
    val notes: String?,
    val gender: String?,
    val verbPattern: String?,
    val plurals: List<AiPluralSuggestion>,
    val relations: List<AiRelationSuggestion>,
)

@Serializable
data class AiPluralSuggestion(
    val pluralForm: String,
    val pluralType: String,
)

@Serializable
data class AiRelationSuggestion(
    val arabicText: String,   // free-text from AI; frontend resolves to word via autocomplete
    val relationType: String,
)

fun WordMorphology.toResponse(plurals: List<WordPlural>) = WordMorphologyResponse(
    gender = gender?.name,
    verbPattern = verbPattern?.name,
    plurals = plurals.map { it.toResponse() },
)

fun WordPlural.toResponse() = WordPluralResponse(
    id = id.toString(),
    pluralForm = pluralForm,
    pluralType = pluralType.name,
)

// WordRelation needs relatedWord for Arabic text — mapper takes it as param
fun WordRelation.toResponse(relatedWord: Word) = WordRelationResponse(
    relatedWordId = relatedWordId.toString(),
    relatedWordArabic = relatedWord.arabicText,
    relatedWordTranslation = relatedWord.translation,
    relationType = relationType.name,
)
