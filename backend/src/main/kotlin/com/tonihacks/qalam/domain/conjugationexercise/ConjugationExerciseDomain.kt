package com.tonihacks.qalam.domain.conjugationexercise

import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.Segment
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class ConjugationExerciseSessionId(val value: UUID)

@JvmInline
value class ConjugationExerciseItemId(val value: UUID)

@JvmInline
value class ConjugationExercisePairId(val value: UUID)

data class ConjugationExerciseSession(
    val id: ConjugationExerciseSessionId,
    val mode: TrainingMode,
    val status: SessionStatus,
    val tense: Tense,
    val voice: Voice,
    val totalItems: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val createdAt: Instant,
    val completedAt: Instant?,
)

data class ConjugationExerciseItem(
    val id: ConjugationExerciseItemId,
    val sessionId: ConjugationExerciseSessionId,
    val wordId: WordId,
    val position: Int,
    val lemmaSnapshot: String,
    val translationSnapshot: String?,
    val verbFormSnapshot: String,
    val result: TrainingResult?,
    val answeredAt: Instant?,
    val pairs: List<ConjugationExercisePair>,
)

/** One correct form-to-label relation. Form and label identifiers stay separate in the API. */
data class ConjugationExercisePair(
    val id: ConjugationExercisePairId,
    val itemId: ConjugationExerciseItemId,
    val position: Int,
    val formPosition: Int,
    val labelPosition: Int,
    val formId: UUID,
    val labelId: UUID,
    val arabic: String,
    val segments: List<Segment>,
    val tense: Tense,
    val voice: Voice,
    val person: Person,
)

data class ConjugationExerciseAnswer(
    val itemId: ConjugationExerciseItemId,
    val formId: UUID,
    val selectedLabelId: UUID?,
    val submittedText: String?,
    val isCorrect: Boolean,
)
