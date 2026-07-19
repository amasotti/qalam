package com.tonihacks.qalam.domain.conjugationexercise

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.Word
import java.util.UUID

interface ConjugationExerciseRepository {
    suspend fun createSession(
        session: ConjugationExerciseSession,
        items: List<ConjugationExerciseItem>,
    ): Either<DomainError, ConjugationExerciseSession>
}

interface ConjugatableVerbRepository {
    /** Saved verbs with a root and verb details, eligible for a generated matching board. */
    suspend fun findForTraining(
        masteryLevel: MasteryLevel?,
        wordListIds: Set<UUID>,
        limit: Int,
    ): Either<DomainError, List<Word>>
}
