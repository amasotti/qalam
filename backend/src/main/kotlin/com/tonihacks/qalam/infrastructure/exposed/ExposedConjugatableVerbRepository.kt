package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.conjugationexercise.ConjugatableVerbRepository
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.uuid.toKotlinUuid
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class ExposedConjugatableVerbRepository : ConjugatableVerbRepository {
    private val log = KotlinLogging.logger {}

    override suspend fun findForTraining(
        masteryLevel: MasteryLevel?,
        wordListIds: Set<UUID>,
        limit: Int,
    ): Either<DomainError, List<Word>> =
        suspendTransaction {
            try {
                val query = WordsTable
                    .join(
                        VerbDetailsTable,
                        JoinType.INNER,
                        additionalConstraint = { WordsTable.id eq VerbDetailsTable.wordId },
                    )
                    .let { base ->
                        if (wordListIds.isEmpty()) {
                            base.selectAll()
                        } else {
                            base
                                .join(
                                    WordListItemsTable,
                                    JoinType.INNER,
                                    additionalConstraint = { WordsTable.id eq WordListItemsTable.wordId },
                                )
                                .selectAll()
                        }
                    }

                val constrained = query.where {
                    val eligibility = (WordsTable.partOfSpeech eq PartOfSpeech.VERB.name) and WordsTable.rootId.isNotNull()
                    when {
                        masteryLevel != null && wordListIds.isNotEmpty() ->
                            eligibility and
                                (WordsTable.masteryLevel eq masteryLevel.name) and
                                (WordListItemsTable.listId inList wordListIds.map { it.toKotlinUuid() })
                        masteryLevel != null -> eligibility and (WordsTable.masteryLevel eq masteryLevel.name)
                        wordListIds.isNotEmpty() ->
                            eligibility and (WordListItemsTable.listId inList wordListIds.map { it.toKotlinUuid() })
                        else -> eligibility
                    }
                }

                constrained
                    .map { it.toWord() }
                    .distinctBy { it.id }
                    .shuffled()
                    .take(limit)
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                log.error(e) {
                    "Find conjugatable words failed masteryLevel=$masteryLevel wordListCount=${wordListIds.size} limit=$limit"
                }
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun countForTraining(
        masteryLevel: MasteryLevel?,
        wordListIds: Set<UUID>,
    ): Either<DomainError, Int> = findForTraining(masteryLevel, wordListIds, Int.MAX_VALUE).map { it.size }
}
