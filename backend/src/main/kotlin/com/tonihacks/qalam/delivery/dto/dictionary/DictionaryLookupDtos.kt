package com.tonihacks.qalam.delivery.dto.dictionary

import kotlinx.serialization.Serializable

@Serializable
data class DictionaryLookupResponse(
    val source: String,
    val query: String,
    val items: List<DictionaryLookupItemResponse>
)

@Serializable
data class DictionaryLookupItemResponse(
    val externalId: String,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val plural: DictionaryLookupPluralResponse?,
    val hasExactWordMatch: Boolean,
)

@Serializable
data class DictionaryLookupPluralResponse(
    val arabicText: String,
    val transliteration: String?,
)
