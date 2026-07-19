package com.tonihacks.qalam.domain.conjugationexercise

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tonihacks.qalam.domain.conjugation.ConjugationEngine
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.infrastructure.exposed.ExposedVerbDetailsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.Clock

class ConjugationExerciseService(
    private val sessionRepo: ConjugationExerciseRepository,
    private val candidateRepo: ConjugatableVerbRepository,
    private val rootRepo: RootRepository,
    private val verbDetailsRepo: ExposedVerbDetailsRepository,
    private val engine: ConjugationEngine,
) {
    private val log = KotlinLogging.logger {}

    suspend fun createMatchingSession(
        mode: TrainingMode,
        size: Int,
        wordListIds: Set<UUID>,
        tense: Tense,
        voice: Voice,
    ): Either<DomainError, Pair<ConjugationExerciseSession, List<ConjugationExerciseItem>>> = either {
        val requestedSize = size.coerceIn(MIN_SESSION_SIZE, MAX_SESSION_SIZE)
        val candidates = candidateRepo.findForTraining(mode.toMasteryFilter(), wordListIds, requestedSize).bind()
        ensure(candidates.size >= requestedSize) {
            DomainError.NotEnoughConjugatableVerbs(requestedSize, candidates.size)
        }

        val now = Clock.System.now()
        val session = ConjugationExerciseSession(
            id = ConjugationExerciseSessionId(UUID.randomUUID()),
            mode = mode,
            status = SessionStatus.ACTIVE,
            tense = tense,
            voice = voice,
            totalItems = requestedSize,
            correctCount = 0,
            incorrectCount = 0,
            skippedCount = 0,
            createdAt = now,
            completedAt = null,
        )
        val items = buildList {
            candidates.take(requestedSize).forEachIndexed { position, word ->
                add(buildMatchingItem(session.id, position, word, tense, voice))
            }
        }

        sessionRepo.createSession(session, items).bind()
        session to items
    }.logDomainFailure(log) {
        "Failed to create conjugation matching session mode=$mode requestedSize=$size wordListCount=${wordListIds.size}"
    }

    suspend fun getSession(
        sessionIdStr: String,
    ): Either<DomainError, Pair<ConjugationExerciseSession, List<ConjugationExerciseItem>>> = either {
        sessionRepo.findSessionWithItems(parseSessionId(sessionIdStr).bind()).bind()
    }.logDomainFailure(log) { "Failed to load conjugation exercise session id=$sessionIdStr" }

    suspend fun answerItem(
        sessionIdStr: String,
        itemIdStr: String,
        mappings: List<ConjugationExerciseMappingRequest>,
    ): Either<DomainError, AnswerConjugationExerciseItemResponse> = either {
        val sessionId = parseSessionId(sessionIdStr).bind()
        val itemId = parseItemId(itemIdStr).bind()
        val (session, items) = sessionRepo.findSessionWithItems(sessionId).bind()
        ensure(session.status == SessionStatus.ACTIVE) { DomainError.SessionAlreadyCompleted(sessionIdStr) }
        val item = items.singleOrNull { it.id == itemId }
            ?: raise(DomainError.NotFound("ConjugationExerciseItem", itemIdStr))
        ensure(item.result == null) { DomainError.Conflict("ConjugationExerciseItem", itemIdStr) }
        ensure(mappings.size == MATCHING_PAIR_COUNT) {
            DomainError.InvalidInput("Exactly $MATCHING_PAIR_COUNT form-to-label mappings are required")
        }

        val parsedMappings = mappings.map { mapping ->
            parseUuid(mapping.formId, "form ID").bind() to parseUuid(mapping.labelId, "label ID").bind()
        }
        ensure(parsedMappings.map { it.first }.distinct().size == MATCHING_PAIR_COUNT) {
            DomainError.InvalidInput("Each form must occur exactly once")
        }
        ensure(parsedMappings.map { it.second }.distinct().size == MATCHING_PAIR_COUNT) {
            DomainError.InvalidInput("Each label must occur exactly once")
        }
        val pairsByFormId = item.pairs.associateBy { it.formId }
        ensure(parsedMappings.all { (formId, labelId) -> pairsByFormId[formId]?.labelId != null && item.pairs.any { it.labelId == labelId } }) {
            DomainError.InvalidInput("Mappings must belong to the unanswered item")
        }

        val answers = parsedMappings.map { (formId, labelId) ->
            ConjugationExerciseAnswer(
                itemId = itemId,
                formId = formId,
                selectedLabelId = labelId,
                submittedText = null,
                isCorrect = pairsByFormId.getValue(formId).labelId == labelId,
            )
        }
        val result = if (answers.all { it.isCorrect }) TrainingResult.CORRECT else TrainingResult.INCORRECT
        sessionRepo.recordAnswer(sessionId, itemId, answers, result, Clock.System.now()).bind()

        AnswerConjugationExerciseItemResponse(
            itemId = itemIdStr,
            result = result.name,
            submittedMappings = answers.map { answer ->
                ConjugationExerciseMappingResponse(answer.formId.toString(), answer.selectedLabelId.toString(), answer.isCorrect)
            },
            correctMappings = item.pairs.map { pair ->
                ConjugationExerciseMappingResponse(pair.formId.toString(), pair.labelId.toString())
            },
        )
    }.logDomainFailure(log) { "Failed to answer conjugation exercise item sessionId=$sessionIdStr itemId=$itemIdStr" }

    suspend fun completeSession(
        sessionIdStr: String,
    ): Either<DomainError, ConjugationExerciseSessionSummaryResponse> = either {
        val sessionId = parseSessionId(sessionIdStr).bind()
        val (session, items) = sessionRepo.findSessionWithItems(sessionId).bind()
        ensure(session.status == SessionStatus.ACTIVE) { DomainError.SessionAlreadyCompleted(sessionIdStr) }
        val correct = items.count { it.result == TrainingResult.CORRECT }
        val incorrect = items.count { it.result == TrainingResult.INCORRECT }
        val skipped = items.size - correct - incorrect
        val completed = sessionRepo.completeSession(sessionId, correct, incorrect, skipped, Clock.System.now()).bind()
        val answered = correct + incorrect
        ConjugationExerciseSessionSummaryResponse(
            sessionId = sessionIdStr,
            mode = session.mode.name,
            totalItems = session.totalItems,
            correct = correct,
            incorrect = incorrect,
            skipped = skipped,
            accuracy = if (answered == 0) 0.0 else correct.toDouble() / answered,
            completedAt = completed.completedAt.toString(),
        )
    }.logDomainFailure(log) { "Failed to complete conjugation exercise session id=$sessionIdStr" }

    suspend fun listSessions(page: Int, size: Int): Either<DomainError, PaginatedConjugationExerciseSessionsResponse> = either {
        ensure(page >= 1) { DomainError.InvalidInput("page must be at least 1") }
        val (sessions, total) = sessionRepo.listSessions(page, size.coerceIn(1, 500)).bind()
        PaginatedConjugationExerciseSessionsResponse(
            items = sessions.map { it.toListItemResponse() }, total = total, page = page, size = size.coerceIn(1, 500),
        )
    }.logDomainFailure(log) { "Failed to list conjugation exercise sessions page=$page size=$size" }

    private suspend fun arrow.core.raise.Raise<DomainError>.buildMatchingItem(
        sessionId: ConjugationExerciseSessionId,
        position: Int,
        word: Word,
        tense: Tense,
        voice: Voice,
    ): ConjugationExerciseItem {
        val details = verbDetailsRepo.find(word.id).bind()
            ?: raise(DomainError.NotEnoughConjugatableVerbs(MATCHING_PAIR_COUNT, 0))
        val rootId = word.rootId ?: raise(DomainError.NotEnoughConjugatableVerbs(MATCHING_PAIR_COUNT, 0))
        val root = rootRepo.findById(rootId).bind()
        val forms = engine.conjugate(
            root.letters,
            details.verbForm,
            details.pastPattern,
            details.presentPattern,
            details.weaknessType,
        ).matchingForms(tense, voice)
        ensure(forms.size == MATCHING_PAIR_COUNT) {
            DomainError.NotEnoughConjugatableVerbs(MATCHING_PAIR_COUNT, forms.size)
        }

        val itemId = ConjugationExerciseItemId(UUID.randomUUID())
        val formPositions = forms.indices.shuffled()
        val labelPositions = forms.indices.shuffled()
        return ConjugationExerciseItem(
            id = itemId,
            sessionId = sessionId,
            wordId = word.id,
            position = position,
            lemmaSnapshot = word.arabicText,
            translationSnapshot = word.translation,
            verbFormSnapshot = details.verbForm.name,
            result = null,
            answeredAt = null,
            pairs = forms.mapIndexed { pairPosition, form ->
                ConjugationExercisePair(
                    id = ConjugationExercisePairId(UUID.randomUUID()),
                    itemId = itemId,
                    position = pairPosition,
                    formPosition = formPositions[pairPosition],
                    labelPosition = labelPositions[pairPosition],
                    formId = UUID.randomUUID(),
                    labelId = UUID.randomUUID(),
                    arabic = form.arabic,
                    segments = form.segments,
                    tense = tense,
                    voice = voice,
                    person = form.person,
                )
            },
        )
    }
}

private fun parseSessionId(value: String): Either<DomainError, ConjugationExerciseSessionId> = either {
    ConjugationExerciseSessionId(parseUuid(value, "session ID").bind())
}

private fun parseItemId(value: String): Either<DomainError, ConjugationExerciseItemId> = either {
    ConjugationExerciseItemId(parseUuid(value, "item ID").bind())
}

private fun parseUuid(value: String, field: String): Either<DomainError, UUID> =
    Either.catch { UUID.fromString(value) }.mapLeft { DomainError.InvalidInput("Invalid $field: $value") }

private fun ConjugationExerciseSession.toListItemResponse(): ConjugationExerciseSessionListItemResponse {
    val answered = correctCount + incorrectCount
    return ConjugationExerciseSessionListItemResponse(
        id = id.value.toString(), mode = mode.name, status = status.name, tense = tense.name, voice = voice.name,
        totalItems = totalItems, correctCount = correctCount, incorrectCount = incorrectCount, skippedCount = skippedCount,
        accuracy = if (answered == 0) 0.0 else correctCount.toDouble() / answered,
        createdAt = createdAt.toString(), completedAt = completedAt?.toString(),
    )
}

private const val MIN_SESSION_SIZE = 3
private const val MAX_SESSION_SIZE = 10

private fun TrainingMode.toMasteryFilter(): MasteryLevel? = when (this) {
    TrainingMode.NEW -> MasteryLevel.NEW
    TrainingMode.LEARNING -> MasteryLevel.LEARNING
    TrainingMode.KNOWN -> MasteryLevel.KNOWN
    TrainingMode.MIXED -> null
}
