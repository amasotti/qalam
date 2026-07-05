package com.tonihacks.qalam.domain.analytics

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging

class AnalyticsService(private val repo: AnalyticsRepository) {
    private val log = KotlinLogging.logger {}

    suspend fun getOverview(): Either<DomainError, AnalyticsOverviewResponse> = either {
        log.debug { "Loading analytics overview" }
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
    }.logDomainFailure(log) { "Failed to load analytics overview: $it" }
}
