package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.application.productionpractice.ProductionPracticeWordSource
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.uuid.toKotlinUuid

internal class ExposedProductionPracticeWordSource : ProductionPracticeWordSource {
    private val log = KotlinLogging.logger {}

    override suspend fun sample(
        partOfSpeech: PartOfSpeech?,
        limit: Int,
        excludedWordIds: Set<WordId>,
    ): Either<DomainError, List<Word>> =
        queryWords(partOfSpeech, excludedWordIds, limit)

    override suspend fun findByIds(ids: Set<WordId>): Either<DomainError, List<Word>> =
        if (ids.isEmpty()) emptyList<Word>().right() else try {
            suspendTransaction {
                WordsTable.selectAll()
                    .where { WordsTable.id inList ids.map { it.value.toKotlinUuid() } }
                    .map { it.toWord() }
                    .right()
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            log.error(error) { "Production practice target-word lookup failed count=${ids.size}" }
            DomainError.DatabaseError.left()
        }

    private suspend fun queryWords(
        partOfSpeech: PartOfSpeech?,
        excludedWordIds: Set<WordId>,
        limit: Int,
    ): Either<DomainError, List<Word>> = try {
        suspendTransaction {
            val predicate = selectionPredicate(partOfSpeech, excludedWordIds)
            WordsTable.selectAll()
                .where { predicate }
                .orderBy(CustomFunction<Double>("RANDOM", DoubleColumnType()) to SortOrder.ASC)
                .limit(limit)
                .map { it.toWord() }
                .right()
        }
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        log.error(error) {
            "Production practice word selection failed partOfSpeech=$partOfSpeech excluded=${excludedWordIds.size} limit=$limit"
        }
        DomainError.DatabaseError.left()
    }

    private fun selectionPredicate(
        partOfSpeech: PartOfSpeech?,
        excludedWordIds: Set<WordId>,
    ): Op<Boolean> {
        val partOfSpeechPredicate = partOfSpeech?.let { WordsTable.partOfSpeech eq it.name } ?: Op.TRUE
        val exclusionPredicate = excludedWordIds
            .takeIf { it.isNotEmpty() }
            ?.let { WordsTable.id notInList it.map { id -> id.value.toKotlinUuid() } }
            ?: Op.TRUE
        return partOfSpeechPredicate and exclusionPredicate
    }
}
