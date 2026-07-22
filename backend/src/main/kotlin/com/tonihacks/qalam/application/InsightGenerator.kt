package com.tonihacks.qalam.application

import arrow.core.Either
import com.tonihacks.qalam.domain.ai.InsightContext
import com.tonihacks.qalam.domain.error.DomainError

/** Generates a learner-facing linguistic insight from prepared domain context. */
interface InsightGenerator {
    suspend fun generateInsight(context: InsightContext): Either<DomainError, String>
}
