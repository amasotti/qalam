package com.tonihacks.qalam.domain.exercise

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.Instant

interface ExerciseRepository {
    suspend fun createSession(
        session: ExerciseSession,
        items: List<ExerciseSessionItem>,
    ): Either<DomainError, ExerciseSession>

    suspend fun findSessionWithItems(
        id: ExerciseSessionId,
    ): Either<DomainError, Pair<ExerciseSession, List<ExerciseSessionItem>>>

    suspend fun findDistractorCandidates(
        target: Word,
        wordListIds: Set<UUID>,
    ): Either<DomainError, List<Word>>

    suspend fun recordAnswer(
        sessionId: ExerciseSessionId,
        itemId: ExerciseItemId,
        selectedOptionId: ExerciseOptionId,
        result: TrainingResult,
        masteryPromotedTo: String?,
        answeredAt: Instant,
    ): Either<DomainError, Unit>

    suspend fun completeSession(
        id: ExerciseSessionId,
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        completedAt: Instant,
    ): Either<DomainError, ExerciseSession>

    suspend fun listSessions(
        page: Int,
        size: Int,
    ): Either<DomainError, Pair<List<ExerciseSession>, Long>>
}
