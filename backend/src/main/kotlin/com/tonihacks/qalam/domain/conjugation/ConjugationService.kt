package com.tonihacks.qalam.domain.conjugation

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.tonihacks.qalam.delivery.dto.conjugation.AdHocConjugationRequest
import com.tonihacks.qalam.delivery.dto.conjugation.ConjugationResponse
import com.tonihacks.qalam.delivery.dto.conjugation.RootSummary
import com.tonihacks.qalam.delivery.dto.conjugation.VerbDetailsSummary
import com.tonihacks.qalam.delivery.dto.conjugation.WordSummary
import com.tonihacks.qalam.delivery.dto.conjugation.toResponseMap
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WeaknessType
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRepository
import com.tonihacks.qalam.infrastructure.exposed.ExposedVerbDetailsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

class ConjugationService(
    private val wordRepo: WordRepository,
    private val rootRepo: RootRepository,
    private val verbDetailsRepo: ExposedVerbDetailsRepository,
    private val engine: ConjugationEngine,
) {
    private val log = KotlinLogging.logger {}

    suspend fun conjugateWord(
        wordIdStr: String,
    ): Either<DomainError, ConjugationResponse> = either {
        val wordId = parseUuid(wordIdStr, "wordId").bind()

        val word = wordRepo.findById(WordId(wordId)).bind()
        if (word.partOfSpeech != PartOfSpeech.VERB) {
            raise(DomainError.ValidationError("partOfSpeech", "Word is not a verb"))
        }

        val verbDetails = verbDetailsRepo.find(word.id).bind()
            ?: raise(DomainError.NotFound("VerbDetails", wordIdStr))

        val root = word.rootId?.let { rootRepo.findById(it).bind() }
            ?: raise(DomainError.ValidationError("rootId", "Word has no linked root"))

        val table = engine.conjugate(
            rootLetters = root.letters,
            verbForm = verbDetails.verbForm,
            pastPattern = verbDetails.pastPattern,
            presentPattern = verbDetails.presentPattern,
            weaknessType = verbDetails.weaknessType,
        )

        ConjugationResponse(
            word = WordSummary(
                id = word.id.toString(),
                arabicText = word.arabicText,
                translation = word.translation,
            ),
            verbDetails = VerbDetailsSummary(
                verbForm = verbDetails.verbForm.name,
                pastPattern = verbDetails.pastPattern,
                presentPattern = verbDetails.presentPattern,
                weaknessType = verbDetails.weaknessType.name,
            ),
            root = RootSummary(letters = root.letters),
            dialect = "MSA",
            conjugations = table.toResponseMap(),
        )
    }.logDomainFailure(log) { "Failed to conjugate word id=$wordIdStr: $it" }

    suspend fun computeAdHoc(
        req: AdHocConjugationRequest,
    ): Either<DomainError, ConjugationResponse> = either {
        if (req.rootLetters.size < 3) {
            raise(DomainError.ValidationError("rootLetters", "Root must have at least 3 letters"))
        }

        val verbForm = VerbPattern.fromString(req.verbForm)
            ?: raise(DomainError.ValidationError("verbForm", "Unknown verb form: ${req.verbForm}"))

        val weaknessType = WeaknessType.fromString(req.weaknessType)
            ?: raise(DomainError.ValidationError("weaknessType", "Unknown weakness type: ${req.weaknessType}"))

        val table = engine.conjugate(
            rootLetters = req.rootLetters,
            verbForm = verbForm,
            pastPattern = req.pastPattern,
            presentPattern = req.presentPattern,
            weaknessType = weaknessType,
        )

        ConjugationResponse(
            word = null,
            verbDetails = VerbDetailsSummary(
                verbForm = verbForm.name,
                pastPattern = req.pastPattern,
                presentPattern = req.presentPattern,
                weaknessType = weaknessType.name,
            ),
            root = RootSummary(letters = req.rootLetters),
            dialect = req.dialect,
            conjugations = table.toResponseMap(),
        )
    }.logDomainFailure(log) { "Failed ad-hoc conjugation: $it" }

    private fun parseUuid(value: String, field: String): Either<DomainError, UUID> =
        try {
            Either.Right(UUID.fromString(value))
        } catch (@Suppress("SwallowedException") _: IllegalArgumentException) {
            DomainError.ValidationError(field, "Invalid UUID: $value").left()
        }
}
