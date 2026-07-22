package com.tonihacks.qalam.application

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.ai.InsightContext
import com.tonihacks.qalam.domain.ai.InsightMode
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.sentence.SentenceId
import com.tonihacks.qalam.domain.sentence.SentenceRepository
import com.tonihacks.qalam.domain.text.TextRepository
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

class AiInsightService(
    private val wordRepository: WordRepository,
    private val rootRepository: RootRepository,
    private val sentenceRepository: SentenceRepository,
    private val textRepository: TextRepository,
    private val insightGenerator: InsightGenerator,
) {
    private val log = KotlinLogging.logger {}

    suspend fun generateInsight(entityType: String, entityId: String, mode: InsightMode?): Either<DomainError, String> = either {
        log.info { "Generating AI insight entityType=$entityType entityId=$entityId mode=$mode" }
        val context = when (entityType) {
            "WORD" -> buildWordInsight(entityId).bind()
            "SENTENCE" -> buildSentenceInsight(entityId, mode ?: InsightMode.HOMEWORK).bind()
            else -> raise(DomainError.InvalidInput("entityType must be WORD or SENTENCE, got '$entityType'"))
        }
        insightGenerator.generateInsight(context).bind()
    }.logDomainFailure(log) { "Failed to generate AI insight entityType=$entityType entityId=$entityId mode=$mode: $it" }

    private suspend fun buildWordInsight(entityId: String): Either<DomainError, InsightContext.WordInsight> = either {
        log.debug { "Building word insight context entityId=$entityId" }
        val wordId = WordId(parseUuid(entityId).bind())
        val word = wordRepository.findById(wordId).bind()
        val root = word.rootId?.let { rootRepository.findById(it).bind() }
        val examples = wordRepository.findExamples(wordId).bind()
            .sortedByDescending { it.createdAt }.take(3).map { it.arabic }

        InsightContext.WordInsight(
            arabicText = word.arabicText,
            translation = word.translation,
            partOfSpeech = word.partOfSpeech.name,
            dialect = word.dialect.name,
            rootLetters = root?.displayForm,
            rootMeaning = root?.meaning,
            examples = examples,
        )
    }

    private suspend fun buildSentenceInsight(
        entityId: String,
        mode: InsightMode,
    ): Either<DomainError, InsightContext.SentenceInsight> = either {
        log.debug { "Building sentence insight context entityId=$entityId mode=$mode" }
        val sentenceId = SentenceId(parseUuid(entityId).bind())
        val sentence = sentenceRepository.findById(sentenceId).bind()
        val text = textRepository.findById(sentence.textId).bind()
        val allSentences = sentenceRepository.findAllByTextId(sentence.textId).bind().sortedBy { it.position }
        val truncated = if (allSentences.size > 30) {
            val targetListIndex = allSentences.indexOfFirst { it.id == sentence.id }
            allSentences.subList(maxOf(0, targetListIndex - 5), minOf(allSentences.size, targetListIndex + 6))
        } else allSentences

        InsightContext.SentenceInsight(
            targetArabic = sentence.arabicText,
            targetTranslation = sentence.freeTranslation,
            targetIndex = sentence.position,
            dialect = text.dialect.name,
            textTitle = text.title,
            allSentences = truncated.map { it.arabicText to it.freeTranslation },
            mode = mode,
        )
    }
}

private fun parseUuid(value: String): Either<DomainError, UUID> =
    try { Either.Right(UUID.fromString(value)) }
    catch (_: IllegalArgumentException) { Either.Left(DomainError.InvalidInput("'$value' is not a valid UUID for entityId")) }
