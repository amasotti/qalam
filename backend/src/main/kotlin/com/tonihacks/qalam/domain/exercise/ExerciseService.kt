package com.tonihacks.qalam.domain.exercise

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.training.MasteryPromotionResponse
import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.training.computeProgressUpdate
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.math.max
import kotlin.time.Clock

class ExerciseService(
    private val exerciseRepo: ExerciseRepository,
    private val wordRepo: WordRepository,
) {
    private val log = KotlinLogging.logger {}

    suspend fun createSession(
        modeStr: String,
        size: Int,
        wordListIds: List<String> = emptyList(),
        exerciseTypeStrs: List<String> = listOf(ExerciseType.MULTIPLE_CHOICE_MEANING.name),
        optionCount: Int = 4,
    ): Either<DomainError, Pair<ExerciseSession, List<ExerciseSessionItem>>> = either {
        val parsedSize = size.coerceIn(1, 50)
        val parsedOptionCount = optionCount.coerceIn(3, 4)
        val mode = parseMode(modeStr).bind()
        val exerciseTypes = parseExerciseTypes(exerciseTypeStrs).bind()
        val parsedWordListIds = parseWordListIds(wordListIds).bind()
        val masteryFilter = mode.toMasteryFilter()

        val words = wordRepo.findForTraining(masteryFilter, parsedWordListIds, parsedSize).bind()
        ensure(words.isNotEmpty()) {
            DomainError.NotEnoughWords(requested = parsedSize, available = 0)
        }

        val now = Clock.System.now()
        val session = ExerciseSession(
            id             = ExerciseSessionId(UUID.randomUUID()),
            mode           = mode,
            status         = SessionStatus.ACTIVE,
            totalItems     = words.size,
            correctCount   = 0,
            incorrectCount = 0,
            skippedCount   = 0,
            createdAt      = now,
            completedAt    = null,
        )

        val items = words.mapIndexed { index, word ->
            val type = exerciseTypes[index % exerciseTypes.size]
            buildItem(
                sessionId       = session.id,
                position        = index,
                target          = word,
                type            = type,
                optionCount     = parsedOptionCount,
                wordListIds     = parsedWordListIds,
            ).bind()
        }

        exerciseRepo.createSession(session, items).bind()
        session to items
    }.logDomainFailure(log) {
        "Failed to create exercise session mode=$modeStr requestedSize=$size wordListCount=${wordListIds.size}: $it"
    }

    suspend fun getSession(
        sessionIdStr: String,
    ): Either<DomainError, Pair<ExerciseSession, List<ExerciseSessionItem>>> = either {
        val id = parseSessionId(sessionIdStr).bind()
        exerciseRepo.findSessionWithItems(id).bind()
    }.logDomainFailure(log) { "Failed to load exercise session id=$sessionIdStr: $it" }

    suspend fun answerItem(
        sessionIdStr: String,
        itemIdStr: String,
        selectedOptionIdStr: String,
    ): Either<DomainError, AnswerExerciseItemResponse> = either {
        val sessionId = parseSessionId(sessionIdStr).bind()
        val itemId = parseItemId(itemIdStr).bind()
        val selectedOptionId = parseOptionId(selectedOptionIdStr).bind()

        val (session, items) = exerciseRepo.findSessionWithItems(sessionId).bind()
        ensure(session.status == SessionStatus.ACTIVE) {
            DomainError.SessionAlreadyCompleted(sessionIdStr)
        }

        val item = items.singleOrNull { it.id == itemId }
            ?: raise(DomainError.NotFound("ExerciseItem", itemIdStr))
        ensure(item.result == null) {
            DomainError.Conflict("ExerciseItem", itemIdStr)
        }

        val selected = item.options.singleOrNull { it.id == selectedOptionId }
            ?: raise(DomainError.InvalidInput("Selected option does not belong to item: $selectedOptionIdStr"))
        val correctOption = item.options.single { it.isCorrect }
        val result = if (selected.isCorrect) TrainingResult.CORRECT else TrainingResult.INCORRECT

        val wordId = item.wordId
        val progress = wordRepo.getProgress(wordId).bind()
        val word = wordRepo.findById(wordId).bind()
        val (updatedProgress, newMastery) = computeProgressUpdate(progress, word.masteryLevel, result)

        wordRepo.updateProgress(updatedProgress).bind()
        if (newMastery != null) {
            wordRepo.updateMasteryLevel(wordId, newMastery).bind()
        }

        exerciseRepo.recordAnswer(
            sessionId         = sessionId,
            itemId            = itemId,
            selectedOptionId  = selectedOptionId,
            result            = result,
            masteryPromotedTo = newMastery?.name,
            answeredAt        = Clock.System.now(),
        ).bind()

        AnswerExerciseItemResponse(
            itemId           = itemIdStr,
            wordId           = wordId.value.toString(),
            result           = result.name,
            correctOptionId  = correctOption.id.value.toString(),
            masteryPromotion = newMastery?.let {
                MasteryPromotionResponse(
                    wordId = wordId.value.toString(),
                    from   = word.masteryLevel.name,
                    to     = it.name,
                )
            },
        )
    }.logDomainFailure(log) {
        "Failed to answer exercise item sessionId=$sessionIdStr itemId=$itemIdStr: $it"
    }

    suspend fun completeSession(
        sessionIdStr: String,
    ): Either<DomainError, ExerciseSessionSummaryResponse> = either {
        val sessionId = parseSessionId(sessionIdStr).bind()
        val (session, items) = exerciseRepo.findSessionWithItems(sessionId).bind()

        ensure(session.status == SessionStatus.ACTIVE) {
            DomainError.SessionAlreadyCompleted(sessionIdStr)
        }

        val correct = items.count { it.result == TrainingResult.CORRECT }
        val incorrect = items.count { it.result == TrainingResult.INCORRECT }
        val skipped = items.count { it.result == null || it.result == TrainingResult.SKIPPED }
        val answered = correct + incorrect
        val accuracy = if (answered > 0) correct.toDouble() / answered else 0.0

        val completed = exerciseRepo.completeSession(
            id             = sessionId,
            correctCount   = correct,
            incorrectCount = incorrect,
            skippedCount   = skipped,
            completedAt    = Clock.System.now(),
        ).bind()

        ExerciseSessionSummaryResponse(
            sessionId   = sessionIdStr,
            mode        = session.mode.name,
            totalItems  = session.totalItems,
            correct     = correct,
            incorrect   = incorrect,
            skipped     = skipped,
            accuracy    = accuracy,
            promotions  = items.mapNotNull { it.toPromotion() },
            completedAt = completed.completedAt.toString(),
        )
    }.logDomainFailure(log) { "Failed to complete exercise session id=$sessionIdStr: $it" }

    private suspend fun buildItem(
        sessionId: ExerciseSessionId,
        position: Int,
        target: Word,
        type: ExerciseType,
        optionCount: Int,
        wordListIds: Set<UUID>,
    ): Either<DomainError, ExerciseSessionItem> = either {
        val distractors = exerciseRepo.findDistractorCandidates(target, wordListIds).bind()
        val rankedDistractors = rankDistractors(target, distractors, type)
            .take(max(optionCount - 1, 1))
        ensure(rankedDistractors.isNotEmpty()) {
            DomainError.NotEnoughWords(requested = optionCount, available = 1)
        }

        val itemId = ExerciseItemId(UUID.randomUUID())
        val optionWords = (rankedDistractors + target).shuffled()
        val options = optionWords.mapIndexed { optionIndex, word ->
            ExerciseOption(
                id              = ExerciseOptionId(UUID.randomUUID()),
                itemId          = itemId,
                wordId          = word.id,
                position        = optionIndex,
                arabicText      = word.arabicText,
                transliteration = word.transliteration,
                translation     = word.translation,
                isCorrect       = word.id == target.id,
            )
        }

        ExerciseSessionItem(
            id                = itemId,
            sessionId         = sessionId,
            wordId            = target.id,
            position          = position,
            type              = type,
            promptKind        = type.promptKind(),
            promptText        = type.promptText(target),
            result            = null,
            selectedOptionId  = null,
            answeredAt        = null,
            masteryPromotedTo = null,
            options           = options,
        )
    }
}

private fun parseMode(value: String): Either<DomainError, TrainingMode> = either {
    runCatching { TrainingMode.valueOf(value.uppercase()) }
        .getOrElse { raise(DomainError.InvalidInput("Unknown training mode: $value")) }
}

private fun parseExerciseTypes(values: List<String>): Either<DomainError, List<ExerciseType>> = either {
    val normalized = values.ifEmpty { listOf(ExerciseType.MULTIPLE_CHOICE_MEANING.name) }
    normalized.map { value ->
        runCatching { ExerciseType.valueOf(value.uppercase()) }
            .getOrElse { raise(DomainError.InvalidInput("Unknown exercise type: $value")) }
    }
}

private fun parseWordListIds(values: List<String>): Either<DomainError, Set<UUID>> = either {
    values
        .filter { it.isNotBlank() }
        .map { id ->
            runCatching { UUID.fromString(id) }
                .getOrElse { raise(DomainError.InvalidInput("Invalid word list id: $id")) }
        }
        .toSet()
}

private fun parseSessionId(value: String): Either<DomainError, ExerciseSessionId> = either {
    ExerciseSessionId(
        runCatching { UUID.fromString(value) }
            .getOrElse { raise(DomainError.InvalidInput("Invalid exercise session id: $value")) },
    )
}

private fun parseItemId(value: String): Either<DomainError, ExerciseItemId> = either {
    ExerciseItemId(
        runCatching { UUID.fromString(value) }
            .getOrElse { raise(DomainError.InvalidInput("Invalid exercise item id: $value")) },
    )
}

private fun parseOptionId(value: String): Either<DomainError, ExerciseOptionId> = either {
    ExerciseOptionId(
        runCatching { UUID.fromString(value) }
            .getOrElse { raise(DomainError.InvalidInput("Invalid exercise option id: $value")) },
    )
}

private fun TrainingMode.toMasteryFilter(): MasteryLevel? = when (this) {
    TrainingMode.NEW      -> MasteryLevel.NEW
    TrainingMode.LEARNING -> MasteryLevel.LEARNING
    TrainingMode.KNOWN    -> MasteryLevel.KNOWN
    TrainingMode.MIXED    -> null
}

private fun ExerciseType.promptKind(): ExercisePromptKind = when (this) {
    ExerciseType.MULTIPLE_CHOICE_MEANING,
    ExerciseType.CONFUSABLE_MEANING -> ExercisePromptKind.ARABIC_WORD
    ExerciseType.MULTIPLE_CHOICE_ARABIC,
    ExerciseType.CONFUSABLE_ARABIC -> ExercisePromptKind.TRANSLATION
}

private fun ExerciseType.promptText(word: Word): String = when (promptKind()) {
    ExercisePromptKind.ARABIC_WORD -> word.arabicText
    ExercisePromptKind.TRANSLATION -> word.translation ?: word.arabicText
}

private fun rankDistractors(
    target: Word,
    candidates: List<Word>,
    type: ExerciseType,
): List<Word> {
    val shouldPreferConfusables = type == ExerciseType.CONFUSABLE_MEANING || type == ExerciseType.CONFUSABLE_ARABIC
    if (!shouldPreferConfusables) return candidates.shuffled()

    return candidates
        .sortedByDescending { candidate -> confusableScore(target, candidate) }
        .ifEmpty { candidates.shuffled() }
}

private fun confusableScore(target: Word, candidate: Word): Int =
    rootScore(target, candidate) +
        partOfSpeechScore(target, candidate) +
        textSimilarityScore(target.arabicText, candidate.arabicText) +
        textSimilarityScore(target.transliteration.orEmpty(), candidate.transliteration.orEmpty())

private fun rootScore(target: Word, candidate: Word): Int =
    if (target.rootId != null && target.rootId == candidate.rootId) 100 else 0

private fun partOfSpeechScore(target: Word, candidate: Word): Int =
    if (target.partOfSpeech == candidate.partOfSpeech) 25 else 0

private fun textSimilarityScore(left: String, right: String): Int {
    if (left.isBlank() || right.isBlank()) return 0
    val leftPairs = left.windowed(2).toSet()
    val rightPairs = right.windowed(2).toSet()
    if (leftPairs.isEmpty() || rightPairs.isEmpty()) return commonPrefixLength(left, right)
    return (leftPairs intersect rightPairs).size * 5 + commonPrefixLength(left, right)
}

private fun commonPrefixLength(left: String, right: String): Int =
    left.zip(right).takeWhile { (a, b) -> a == b }.size

private fun ExerciseSessionItem.toPromotion(): MasteryPromotionResponse? {
    val promotedTo = masteryPromotedTo ?: return null
    val from = when (promotedTo) {
        MasteryLevel.LEARNING.name -> MasteryLevel.NEW.name
        MasteryLevel.KNOWN.name    -> MasteryLevel.LEARNING.name
        MasteryLevel.MASTERED.name -> MasteryLevel.KNOWN.name
        else                       -> "UNKNOWN"
    }
    return MasteryPromotionResponse(
        wordId = wordId.value.toString(),
        from   = from,
        to     = promotedTo,
    )
}
