package com.tonihacks.qalam.domain.training

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordProgress
import com.tonihacks.qalam.domain.word.WordRepository
import java.util.UUID
import kotlin.time.Clock

class TrainingService(
    private val trainingRepo: TrainingRepository,
    private val wordRepo: WordRepository,
) {

    suspend fun createSession(
        modeStr: String,
        size: Int,
    ): Either<DomainError, Pair<TrainingSession, List<TrainingSessionWord>>> = either {
        val parsedSize = size.coerceIn(1, 50)
        val mode = runCatching { TrainingMode.valueOf(modeStr.uppercase()) }
            .getOrElse { raise(DomainError.InvalidInput("Unknown training mode: $modeStr")) }

        val masteryFilter = when (mode) {
            TrainingMode.NEW      -> MasteryLevel.NEW
            TrainingMode.LEARNING -> MasteryLevel.LEARNING
            TrainingMode.KNOWN    -> MasteryLevel.KNOWN
            TrainingMode.MIXED    -> null
        }

        val words = wordRepo.findForTraining(masteryFilter, parsedSize).bind()
        ensure(words.isNotEmpty()) {
            DomainError.NotEnoughWords(requested = parsedSize, available = 0)
        }

        val now = Clock.System.now()
        val session = TrainingSession(
            id             = TrainingSessionId(UUID.randomUUID()),
            mode           = mode,
            status         = SessionStatus.ACTIVE,
            totalWords     = words.size,
            correctCount   = 0,
            incorrectCount = 0,
            skippedCount   = 0,
            createdAt      = now,
            completedAt    = null,
        )

        val sides = FlashcardSide.entries
        val sessionWords = words.mapIndexed { index, word ->
            TrainingSessionWord(
                id               = UUID.randomUUID(),
                sessionId        = session.id,
                wordId           = word.id,
                position         = index,
                frontSide        = sides.random(),
                arabicText       = word.arabicText,
                transliteration  = word.transliteration,
                translation      = word.translation,
                masteryLevel     = word.masteryLevel.name,
                result           = null,
                masteryPromotedTo= null,
                answeredAt       = null,
            )
        }

        trainingRepo.createSession(session, sessionWords).bind()
        session to sessionWords
    }

    suspend fun getSession(
        sessionIdStr: String,
    ): Either<DomainError, Pair<TrainingSession, List<TrainingSessionWord>>> = either {
        val id = parseSessionId(sessionIdStr).bind()
        trainingRepo.findSessionWithWords(id).bind()
    }

    suspend fun recordResult(
        sessionIdStr: String,
        wordIdStr: String,
        resultStr: String,
    ): Either<DomainError, RecordResultResponse> = either {
        val sessionId = parseSessionId(sessionIdStr).bind()
        val wordId    = parseWordId(wordIdStr).bind()
        val result    = runCatching { TrainingResult.valueOf(resultStr.uppercase()) }
            .getOrElse { raise(DomainError.InvalidInput("Unknown result: $resultStr")) }

        val (session, _) = trainingRepo.findSessionWithWords(sessionId).bind()
        ensure(session.status == SessionStatus.ACTIVE) {
            DomainError.SessionAlreadyCompleted(sessionIdStr)
        }

        val progress = wordRepo.getProgress(wordId).bind()
        val word     = wordRepo.findById(wordId).bind()

        val (updatedProgress, newMastery) = computeProgressUpdate(progress, word.masteryLevel, result)
        wordRepo.updateProgress(updatedProgress).bind()

        if (newMastery != null) {
            wordRepo.updateMasteryLevel(wordId, newMastery).bind()
        }

        val now = Clock.System.now()
        trainingRepo.recordResult(
            sessionId         = sessionId,
            wordId            = wordId,
            result            = result,
            masteryPromotedTo = newMastery?.name,
            answeredAt        = now,
        ).bind()

        RecordResultResponse(
            wordId           = wordIdStr,
            result           = result.name,
            masteryPromotion = newMastery?.let {
                MasteryPromotionResponse(
                    wordId = wordIdStr,
                    from   = word.masteryLevel.name,
                    to     = it.name,
                )
            },
        )
    }

    suspend fun completeSession(
        sessionIdStr: String,
    ): Either<DomainError, SessionSummaryResponse> = either {
        val sessionId = parseSessionId(sessionIdStr).bind()
        val (session, words) = trainingRepo.findSessionWithWords(sessionId).bind()

        ensure(session.status == SessionStatus.ACTIVE) {
            DomainError.SessionAlreadyCompleted(sessionIdStr)
        }

        val correct   = words.count { it.result == TrainingResult.CORRECT }
        val incorrect = words.count { it.result == TrainingResult.INCORRECT }
        val skipped   = words.count { it.result == TrainingResult.SKIPPED }
        val answered  = correct + incorrect
        val accuracy  = if (answered > 0) correct.toDouble() / answered else 0.0

        val completed = trainingRepo.completeSession(
            id             = sessionId,
            correctCount   = correct,
            incorrectCount = incorrect,
            skippedCount   = skipped,
            completedAt    = Clock.System.now(),
        ).bind()

        val promotions = words
            .filter { it.masteryPromotedTo != null }
            .map { w ->
                val fromLevel = when (w.masteryPromotedTo) {
                    MasteryLevel.LEARNING.name -> MasteryLevel.NEW.name
                    MasteryLevel.KNOWN.name    -> MasteryLevel.LEARNING.name
                    MasteryLevel.MASTERED.name -> MasteryLevel.KNOWN.name
                    else                       -> "UNKNOWN"
                }
                MasteryPromotionResponse(
                    wordId = w.wordId.value.toString(),
                    from   = fromLevel,
                    to     = w.masteryPromotedTo!!,
                )
            }

        SessionSummaryResponse(
            sessionId   = sessionIdStr,
            mode        = session.mode.name,
            totalWords  = session.totalWords,
            correct     = correct,
            incorrect   = incorrect,
            skipped     = skipped,
            accuracy    = accuracy,
            promotions  = promotions,
            completedAt = completed.completedAt.toString(),
        )
    }

    suspend fun listSessions(page: Int, size: Int): Either<DomainError, PaginatedSessionsResponse> = either {
        val (items, total) = trainingRepo.listSessions(page, size).bind()
        PaginatedSessionsResponse(
            items = items.map { it.toListItemResponse() },
            total = total,
            page  = page,
            size  = size,
        )
    }

    suspend fun getStats(): Either<DomainError, TrainingStatsResponse> = either {
        val distribution         = trainingRepo.getMasteryDistribution().bind()
        val (recentItems, total) = trainingRepo.listSessions(1, 10).bind()
        TrainingStatsResponse(
            masteryDistribution = distribution,
            totalSessions       = total.toInt(),
            recentSessions      = recentItems.map { it.toListItemResponse() },
        )
    }
}

// ── Pure helper (unit-testable) ─────────────────────────────────────────────

internal fun computeProgressUpdate(
    progress: WordProgress,
    currentMastery: MasteryLevel,
    result: TrainingResult,
): Pair<WordProgress, MasteryLevel?> {
    val updated = when (result) {
        TrainingResult.CORRECT -> progress.copy(
            consecutiveCorrect = progress.consecutiveCorrect + 1,
            totalCorrect       = progress.totalCorrect + 1,
            totalAttempts      = progress.totalAttempts + 1,
            lastReviewedAt     = Clock.System.now(),
        )
        TrainingResult.INCORRECT -> progress.copy(
            consecutiveCorrect = 0,
            totalAttempts      = progress.totalAttempts + 1,
            lastReviewedAt     = Clock.System.now(),
        )
        TrainingResult.SKIPPED -> progress.copy(
            totalAttempts  = progress.totalAttempts + 1,
            lastReviewedAt = Clock.System.now(),
        )
    }

    val promotion = when {
        currentMastery == MasteryLevel.NEW      && updated.consecutiveCorrect >= 3  -> MasteryLevel.LEARNING
        currentMastery == MasteryLevel.LEARNING && updated.totalCorrect >= 10       -> MasteryLevel.KNOWN
        currentMastery == MasteryLevel.KNOWN    && updated.totalCorrect >= 15       -> MasteryLevel.MASTERED
        else -> null
    }

    return updated to promotion
}

private fun TrainingSession.toListItemResponse(): TrainingSessionListItemResponse {
    val answered = correctCount + incorrectCount
    return TrainingSessionListItemResponse(
        id             = id.value.toString(),
        mode           = mode.name,
        status         = status.name,
        totalWords     = totalWords,
        correctCount   = correctCount,
        incorrectCount = incorrectCount,
        skippedCount   = skippedCount,
        accuracy       = if (answered > 0) correctCount.toDouble() / answered else 0.0,
        createdAt      = createdAt.toString(),
        completedAt    = completedAt?.toString(),
    )
}

private fun parseSessionId(s: String): Either<DomainError, TrainingSessionId> =
    runCatching { TrainingSessionId(UUID.fromString(s)) }
        .fold({ it.right() }, { DomainError.InvalidInput("Invalid session id: $s").left() })

private fun parseWordId(s: String): Either<DomainError, WordId> =
    runCatching { WordId(UUID.fromString(s)) }
        .fold({ it.right() }, { DomainError.InvalidInput("Invalid word id: $s").left() })
