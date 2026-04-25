package com.tonihacks.qalam.domain.training

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.WordId
import kotlin.time.Instant

interface TrainingRepository {
    suspend fun createSession(
        session: TrainingSession,
        words: List<TrainingSessionWord>,
    ): Either<DomainError, TrainingSession>

    suspend fun findSessionWithWords(
        id: TrainingSessionId,
    ): Either<DomainError, Pair<TrainingSession, List<TrainingSessionWord>>>

    suspend fun recordResult(
        sessionId: TrainingSessionId,
        wordId: WordId,
        result: TrainingResult,
        masteryPromotedTo: String?,
        answeredAt: Instant,
    ): Either<DomainError, Unit>

    suspend fun completeSession(
        id: TrainingSessionId,
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        completedAt: Instant,
    ): Either<DomainError, TrainingSession>

    suspend fun listSessions(
        page: Int,
        size: Int,
    ): Either<DomainError, Pair<List<TrainingSession>, Long>>

    suspend fun getMasteryDistribution(): Either<DomainError, Map<String, Int>>
}
