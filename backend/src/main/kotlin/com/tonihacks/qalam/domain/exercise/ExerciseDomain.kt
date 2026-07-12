package com.tonihacks.qalam.domain.exercise

import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.word.WordId
import java.util.UUID
import kotlin.time.Instant

@JvmInline
value class ExerciseSessionId(val value: UUID)

@JvmInline
value class ExerciseItemId(val value: UUID)

@JvmInline
value class ExerciseOptionId(val value: UUID)

enum class ExerciseType {
    MULTIPLE_CHOICE_MEANING,
    MULTIPLE_CHOICE_ARABIC,
    CONFUSABLE_MEANING,
    CONFUSABLE_ARABIC,
}

enum class ExercisePromptKind { ARABIC_WORD, TRANSLATION }

data class ExerciseSession(
    val id: ExerciseSessionId,
    val mode: TrainingMode,
    val status: SessionStatus,
    val totalItems: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val createdAt: Instant,
    val completedAt: Instant?,
)

data class ExerciseSessionItem(
    val id: ExerciseItemId,
    val sessionId: ExerciseSessionId,
    val wordId: WordId,
    val position: Int,
    val type: ExerciseType,
    val promptKind: ExercisePromptKind,
    val promptText: String,
    val result: TrainingResult?,
    val selectedOptionId: ExerciseOptionId?,
    val answeredAt: Instant?,
    val masteryPromotedTo: String?,
    val options: List<ExerciseOption> = emptyList(),
)

data class ExerciseOption(
    val id: ExerciseOptionId,
    val itemId: ExerciseItemId,
    val wordId: WordId,
    val position: Int,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val isCorrect: Boolean,
)
