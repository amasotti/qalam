package com.tonihacks.qalam.domain.analytics

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError

interface AnalyticsRepository {
    suspend fun getWordStats(): Either<DomainError, WordStats>
    suspend fun getTextStats(): Either<DomainError, TextStats>
    suspend fun getRootCount(): Either<DomainError, Int>
    suspend fun getTrainingAnalytics(): Either<DomainError, TrainingAnalytics>
}
