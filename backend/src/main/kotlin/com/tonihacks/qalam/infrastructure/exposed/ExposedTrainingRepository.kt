package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.training.FlashcardSide
import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingRepository
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.training.TrainingSession
import com.tonihacks.qalam.domain.training.TrainingSessionId
import com.tonihacks.qalam.domain.training.TrainingSessionWord
import com.tonihacks.qalam.domain.training.TrainingWordExample
import com.tonihacks.qalam.domain.training.TrainingWordRelation
import com.tonihacks.qalam.domain.word.WordId
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedTrainingRepository : TrainingRepository {

    override suspend fun createSession(
        session: TrainingSession,
        words: List<TrainingSessionWord>,
    ): Either<DomainError, TrainingSession> =
        suspendTransaction {
            try {
                TrainingSessionsTable.insert {
                    it[id]             = session.id.value.toKotlinUuid()
                    it[mode]           = session.mode.name
                    it[status]         = session.status.name
                    it[totalWords]     = session.totalWords
                    it[correctCount]   = 0
                    it[incorrectCount] = 0
                    it[skippedCount]   = 0
                    it[createdAt]      = session.createdAt
                    it[completedAt]    = null
                }
                words.forEach { w ->
                    TrainingSessionWordsTable.insert {
                        it[id]                = w.id.toKotlinUuid()
                        it[sessionId]         = session.id.value.toKotlinUuid()
                        it[wordId]            = w.wordId.value.toKotlinUuid()
                        it[position]          = w.position
                        it[frontSide]         = w.frontSide.name
                        it[result]            = null
                        it[answeredAt]        = null
                        it[masteryPromotedTo] = null
                    }
                }
                session.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun findSessionWithWords(
        id: TrainingSessionId,
    ): Either<DomainError, Pair<TrainingSession, List<TrainingSessionWord>>> =
        suspendTransaction {
            try {
                val sessionRow = TrainingSessionsTable
                    .selectAll()
                    .where { TrainingSessionsTable.id eq id.value.toKotlinUuid() }
                    .singleOrNull()
                    ?: return@suspendTransaction DomainError.NotFound("TrainingSession", id.value.toString()).left()

                val session = sessionRow.toTrainingSession()

                val wordRows = TrainingSessionWordsTable
                    .join(WordsTable, JoinType.INNER, additionalConstraint = { TrainingSessionWordsTable.wordId eq WordsTable.id })
                    .join(RootsTable, JoinType.LEFT, additionalConstraint = { WordsTable.rootId eq RootsTable.id })
                    .selectAll()
                    .where { TrainingSessionWordsTable.sessionId eq id.value.toKotlinUuid() }
                    .orderBy(TrainingSessionWordsTable.position)
                    .toList()

                val wordUuids = wordRows.map { it[TrainingSessionWordsTable.wordId] }
                val examplesByWordId  = fetchExamplesByWordId(wordUuids)
                val relationsByWordId = fetchRelationsByWordId(wordUuids)

                val words = wordRows.map { row ->
                    val wid = row[TrainingSessionWordsTable.wordId]
                    row.toTrainingSessionWord(
                        examples  = examplesByWordId[wid] ?: emptyList(),
                        relations = relationsByWordId[wid] ?: emptyList(),
                    )
                }

                (session to words).right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun recordResult(
        sessionId: TrainingSessionId,
        wordId: WordId,
        result: TrainingResult,
        masteryPromotedTo: String?,
        answeredAt: Instant,
    ): Either<DomainError, Unit> =
        suspendTransaction {
            try {
                TrainingSessionWordsTable.update({
                    (TrainingSessionWordsTable.sessionId eq sessionId.value.toKotlinUuid()) and
                    (TrainingSessionWordsTable.wordId eq wordId.value.toKotlinUuid())
                }) {
                    it[TrainingSessionWordsTable.result]            = result.name
                    it[TrainingSessionWordsTable.answeredAt]        = answeredAt
                    it[TrainingSessionWordsTable.masteryPromotedTo] = masteryPromotedTo
                }
                Unit.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun completeSession(
        id: TrainingSessionId,
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        completedAt: Instant,
    ): Either<DomainError, TrainingSession> =
        suspendTransaction {
            try {
                TrainingSessionsTable.update({
                    TrainingSessionsTable.id eq id.value.toKotlinUuid()
                }) {
                    it[status]                                   = SessionStatus.COMPLETED.name
                    it[TrainingSessionsTable.correctCount]       = correctCount
                    it[TrainingSessionsTable.incorrectCount]     = incorrectCount
                    it[TrainingSessionsTable.skippedCount]       = skippedCount
                    it[TrainingSessionsTable.completedAt]        = completedAt
                }
                TrainingSessionsTable
                    .selectAll()
                    .where { TrainingSessionsTable.id eq id.value.toKotlinUuid() }
                    .single()
                    .toTrainingSession()
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun listSessions(
        page: Int,
        size: Int,
    ): Either<DomainError, Pair<List<TrainingSession>, Long>> =
        suspendTransaction {
            try {
                val total = TrainingSessionsTable.selectAll().count()
                val items = TrainingSessionsTable
                    .selectAll()
                    .orderBy(TrainingSessionsTable.createdAt to SortOrder.DESC)
                    .limit(size)
                    .offset(((page - 1) * size).toLong())
                    .map { it.toTrainingSession() }
                (items to total).right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun getMasteryDistribution(): Either<DomainError, Map<String, Int>> =
        suspendTransaction {
            try {
                WordsTable
                    .selectAll()
                    .groupBy { it[WordsTable.masteryLevel] }
                    .mapValues { (_, rows) -> rows.size }
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }
}

// ── Batch-fetch helpers (run within existing transaction) ───────────────────

private fun fetchExamplesByWordId(
    wordUuids: List<kotlin.uuid.Uuid>,
): Map<kotlin.uuid.Uuid, List<TrainingWordExample>> {
    if (wordUuids.isEmpty()) return emptyMap()
    return WordExamplesTable
        .selectAll()
        .where { WordExamplesTable.wordId inList wordUuids }
        .orderBy(WordExamplesTable.createdAt)
        .groupBy { it[WordExamplesTable.wordId] }
        .mapValues { (_, rows) ->
            rows.take(2).map { row ->
                TrainingWordExample(
                    arabic          = row[WordExamplesTable.arabic],
                    transliteration = row[WordExamplesTable.transliteration],
                    translation     = row[WordExamplesTable.translation],
                )
            }
        }
}

private fun fetchRelationsByWordId(
    wordUuids: List<kotlin.uuid.Uuid>,
): Map<kotlin.uuid.Uuid, List<TrainingWordRelation>> {
    if (wordUuids.isEmpty()) return emptyMap()

    val allRelationRows = WordRelationsTable
        .selectAll()
        .where { WordRelationsTable.wordId inList wordUuids }
        .toList()

    val relatedWordIds = allRelationRows.map { it[WordRelationsTable.relatedWordId] }.distinct()

    val relatedWordsMap: Map<kotlin.uuid.Uuid, Pair<String, String?>> =
        if (relatedWordIds.isEmpty()) emptyMap()
        else WordsTable
            .selectAll()
            .where { WordsTable.id inList relatedWordIds }
            .associate { row -> row[WordsTable.id] to (row[WordsTable.arabicText] to row[WordsTable.translation]) }

    return allRelationRows
        .groupBy { it[WordRelationsTable.wordId] }
        .mapValues { (_, rows) ->
            rows.mapNotNull { row ->
                val rwId = row[WordRelationsTable.relatedWordId]
                val (arabic, translation) = relatedWordsMap[rwId] ?: return@mapNotNull null
                TrainingWordRelation(
                    relatedWordId          = rwId.toJavaUuid().toString(),
                    relatedWordArabic      = arabic,
                    relatedWordTranslation = translation,
                    relationType           = row[WordRelationsTable.relationType],
                )
            }
        }
}

// ── ResultRow mappers ───────────────────────────────────────────────────────

private fun ResultRow.toTrainingSession() = TrainingSession(
    id             = TrainingSessionId(this[TrainingSessionsTable.id].toJavaUuid()),
    mode           = TrainingMode.valueOf(this[TrainingSessionsTable.mode]),
    status         = SessionStatus.valueOf(this[TrainingSessionsTable.status]),
    totalWords     = this[TrainingSessionsTable.totalWords],
    correctCount   = this[TrainingSessionsTable.correctCount],
    incorrectCount = this[TrainingSessionsTable.incorrectCount],
    skippedCount   = this[TrainingSessionsTable.skippedCount],
    createdAt      = this[TrainingSessionsTable.createdAt],
    completedAt    = this[TrainingSessionsTable.completedAt],
)

private fun ResultRow.toTrainingSessionWord(
    examples: List<TrainingWordExample>,
    relations: List<TrainingWordRelation>,
) = TrainingSessionWord(
    id                = this[TrainingSessionWordsTable.id].toJavaUuid(),
    sessionId         = TrainingSessionId(this[TrainingSessionWordsTable.sessionId].toJavaUuid()),
    wordId            = WordId(this[TrainingSessionWordsTable.wordId].toJavaUuid()),
    position          = this[TrainingSessionWordsTable.position],
    frontSide         = FlashcardSide.valueOf(this[TrainingSessionWordsTable.frontSide]),
    arabicText        = this[WordsTable.arabicText],
    transliteration   = this[WordsTable.transliteration],
    translation       = this[WordsTable.translation],
    masteryLevel      = this[WordsTable.masteryLevel],
    result            = this[TrainingSessionWordsTable.result]?.let { TrainingResult.valueOf(it) },
    masteryPromotedTo = this[TrainingSessionWordsTable.masteryPromotedTo],
    answeredAt        = this[TrainingSessionWordsTable.answeredAt],
    root              = this.getOrNull(RootsTable.displayForm),
    notes             = this[WordsTable.notes],
    examples          = examples,
    relations         = relations,
)
