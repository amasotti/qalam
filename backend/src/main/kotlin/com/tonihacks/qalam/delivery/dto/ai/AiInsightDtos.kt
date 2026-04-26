package com.tonihacks.qalam.delivery.dto.ai

import com.tonihacks.qalam.domain.ai.InsightMode
import kotlinx.serialization.Serializable

@Serializable
data class InsightRequest(
    val entityType: String,   // "WORD" | "SENTENCE"
    val entityId: String,
    val mode: InsightMode? = null,
)

@Serializable
data class InsightResponse(
    val insight: String,
)
