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

private const val MIN_SESSION_SIZE = 3
private const val MAX_SESSION_SIZE = 10

private fun TrainingMode.toMasteryFilter(): MasteryLevel? = when (this) {
    TrainingMode.NEW -> MasteryLevel.NEW
    TrainingMode.LEARNING -> MasteryLevel.LEARNING
    TrainingMode.KNOWN -> MasteryLevel.KNOWN
    TrainingMode.MIXED -> null
}
