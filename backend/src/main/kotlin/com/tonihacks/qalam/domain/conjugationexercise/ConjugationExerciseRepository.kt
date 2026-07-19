package com.tonihacks.qalam.domain.conjugationexercise

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.Word
import java.util.UUID
import kotlin.time.Instant

interface ConjugationExerciseRepository {
    suspend fun createSession(
        session: ConjugationExerciseSession,
        items: List<ConjugationExerciseItem>,
    ): Either<DomainError, ConjugationExerciseSession>

    suspend fun findSessionWithItems(
        id: ConjugationExerciseSessionId,
    ): Either<DomainError, Pair<ConjugationExerciseSession, List<ConjugationExerciseItem>>>

    suspend fun recordAnswer(
        sessionId: ConjugationExerciseSessionId,
        itemId: ConjugationExerciseItemId,
        answers: List<ConjugationExerciseAnswer>,
        result: com.tonihacks.qalam.domain.training.TrainingResult,
        answeredAt: Instant,
    ): Either<DomainError, Unit>

    suspend fun completeSession(
        id: ConjugationExerciseSessionId,
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        completedAt: Instant,
    ): Either<DomainError, ConjugationExerciseSession>

    suspend fun listSessions(
        page: Int,
        size: Int,
    ): Either<DomainError, Pair<List<ConjugationExerciseSession>, Long>>
}

interface ConjugatableVerbRepository {
    /** Saved verbs with a root and verb details, eligible for a generated matching board. */
    suspend fun findForTraining(
        masteryLevel: MasteryLevel?,
        wordListIds: Set<UUID>,
        limit: Int,
    ): Either<DomainError, List<Word>>
}
