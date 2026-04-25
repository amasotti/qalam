package com.tonihacks.qalam.domain.training

import com.tonihacks.qalam.domain.word.WordId
import kotlinx.datetime.Instant
import java.util.UUID

@JvmInline
value class TrainingSessionId(val value: UUID)

enum class TrainingMode { NEW, LEARNING, KNOWN, MIXED }
enum class FlashcardSide { ARABIC, TRANSLATION }
enum class TrainingResult { CORRECT, INCORRECT, SKIPPED }
enum class SessionStatus { ACTIVE, COMPLETED }

data class TrainingSession(
    val id: TrainingSessionId,
    val mode: TrainingMode,
    val status: SessionStatus,
    val totalWords: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val createdAt: Instant,
    val completedAt: Instant?,
)

data class TrainingSessionWord(
    val id: UUID,
    val sessionId: TrainingSessionId,
    val wordId: WordId,
    val position: Int,
    val frontSide: FlashcardSide,
    val arabicText: String,
    val transliteration: String?,
    val translation: String?,
    val masteryLevel: String,
    val result: TrainingResult?,
    val masteryPromotedTo: String?,
    val answeredAt: Instant?,
)

data class MasteryPromotion(
    val wordId: WordId,
    val from: String,
    val to: String,
)
