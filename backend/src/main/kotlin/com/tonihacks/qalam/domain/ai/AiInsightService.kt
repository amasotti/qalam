package com.tonihacks.qalam.domain.ai

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.root.RootRepository
import com.tonihacks.qalam.domain.sentence.SentenceId
import com.tonihacks.qalam.domain.sentence.SentenceRepository
import com.tonihacks.qalam.domain.text.TextRepository
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRepository
import com.tonihacks.qalam.infrastructure.ai.AiClient
import java.util.UUID

class AiInsightService(
    private val wordRepository: WordRepository,
    private val rootRepository: RootRepository,
    private val sentenceRepository: SentenceRepository,
    private val textRepository: TextRepository,
    private val aiClient: AiClient,
) {

    suspend fun generateInsight(
        entityType: String,
        entityId: String,
        mode: InsightMode?,
    ): Either<DomainError, String> = either {
        val context = when (entityType) {
            "WORD" -> buildWordInsight(entityId).bind()
            "SENTENCE" -> buildSentenceInsight(entityId, mode ?: InsightMode.HOMEWORK).bind()
            else -> raise(DomainError.InvalidInput("entityType must be WORD or SENTENCE, got '$entityType'"))
        }
        aiClient.generateInsight(context).bind()
    }

    private suspend fun buildWordInsight(entityId: String): Either<DomainError, InsightContext.WordInsight> = either {
        val wordId = WordId(parseUuid(entityId).bind())
        val word = wordRepository.findById(wordId).bind()

        val root = word.rootId?.let { rootRepository.findById(it).bind() }

        val examples = wordRepository.findExamples(wordId).bind()
            .sortedByDescending { it.createdAt }
            .take(3)
            .map { it.arabic }

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
        val sentenceId = SentenceId(parseUuid(entityId).bind())
        val sentence = sentenceRepository.findById(sentenceId).bind()
        val text = textRepository.findById(sentence.textId).bind()
        val allSentences = sentenceRepository.findAllByTextId(sentence.textId).bind()
            .sortedBy { it.position }

        val targetIndex = sentence.position

        val truncated = if (allSentences.size > 30) {
            val targetListIndex = allSentences.indexOfFirst { it.id == sentence.id }
            val from = maxOf(0, targetListIndex - 5)
            val to = minOf(allSentences.size, targetListIndex + 6)
            allSentences.subList(from, to)
        } else {
            allSentences
        }

        InsightContext.SentenceInsight(
            targetArabic = sentence.arabicText,
            targetTranslation = sentence.freeTranslation,
            targetIndex = targetIndex,
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
