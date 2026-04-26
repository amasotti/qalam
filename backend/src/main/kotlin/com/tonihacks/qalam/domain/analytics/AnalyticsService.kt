package com.tonihacks.qalam.domain.analytics

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.error.DomainError

class AnalyticsService(private val repo: AnalyticsRepository) {

    suspend fun getOverview(): Either<DomainError, AnalyticsOverviewResponse> = either {
        val words = repo.getWordStats().bind()
        val texts = repo.getTextStats().bind()
        val rootCount = repo.getRootCount().bind()
        val training = repo.getTrainingAnalytics().bind()
        AnalyticsOverviewResponse(
            words = words,
            texts = texts,
            roots = RootStats(total = rootCount),
            training = training,
        )
    }
}
