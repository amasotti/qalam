package com.tonihacks.qalam.delivery.dto.transliteration

import kotlinx.serialization.Serializable

@Serializable
data class TransliterateRequest(val arabic: String)

@Serializable
data class TransliterateResponse(val transliteration: String)
