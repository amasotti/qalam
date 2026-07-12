package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.exercise.ExerciseItemId
import com.tonihacks.qalam.domain.exercise.ExerciseOption
import com.tonihacks.qalam.domain.exercise.ExerciseOptionId
import com.tonihacks.qalam.domain.exercise.ExercisePromptKind
import com.tonihacks.qalam.domain.exercise.ExerciseRepository
import com.tonihacks.qalam.domain.exercise.ExerciseSession
import com.tonihacks.qalam.domain.exercise.ExerciseSessionId
import com.tonihacks.qalam.domain.exercise.ExerciseSessionItem
import com.tonihacks.qalam.domain.exercise.ExerciseType
import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.PartOfSpeech
import com.tonihacks.qalam.domain.word.Word
import com.tonihacks.qalam.domain.word.WordId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.Instant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedExerciseRepository : ExerciseRepository {

    private val log = KotlinLogging.logger {}

    override suspend fun createSession(
        session: ExerciseSession,
        items: List<ExerciseSessionItem>,
    ): Either<DomainError, ExerciseSession> =
        suspendTransaction {
            try {
                ExerciseSessionsTable.insert {
                    it[id] = session.id.value.toKotlinUuid()
                    it[mode] = session.mode.name
                    it[status] = session.status.name
                    it[totalItems] = session.totalItems
                    it[correctCount] = session.correctCount
                    it[incorrectCount] = session.incorrectCount
                    it[skippedCount] = session.skippedCount
                    it[createdAt] = session.createdAt
                    it[completedAt] = session.completedAt
                }
                items.forEach { item ->
                    ExerciseSessionItemsTable.insert {
                        it[id] = item.id.value.toKotlinUuid()
                        it[sessionId] = item.sessionId.value.toKotlinUuid()
                        it[wordId] = item.wordId.value.toKotlinUuid()
                        it[position] = item.position
                        it[type] = item.type.name
                        it[promptKind] = item.promptKind.name
                        it[promptText] = item.promptText
                        it[result] = item.result?.name
                        it[selectedOptionId] = item.selectedOptionId?.value?.toKotlinUuid()
                        it[answeredAt] = item.answeredAt
                        it[masteryPromotedTo] = item.masteryPromotedTo
                    }
                    item.options.forEach { option ->
                        ExerciseItemOptionsTable.insert {
                            it[id] = option.id.value.toKotlinUuid()
                            it[itemId] = option.itemId.value.toKotlinUuid()
                            it[wordId] = option.wordId.value.toKotlinUuid()
                            it[position] = option.position
                            it[arabicText] = option.arabicText
                            it[transliteration] = option.transliteration
                            it[translation] = option.translation
                            it[isCorrect] = option.isCorrect
                        }
                    }
                }
                session.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Exercise repository createSession failed" }
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun findSessionWithItems(
        id: ExerciseSessionId,
    ): Either<DomainError, Pair<ExerciseSession, List<ExerciseSessionItem>>> =
        suspendTransaction {
            try {
                val sessionRow = ExerciseSessionsTable
                    .selectAll()
                    .where { ExerciseSessionsTable.id eq id.value.toKotlinUuid() }
                    .singleOrNull()
                    ?: return@suspendTransaction DomainError.NotFound("ExerciseSession", id.value.toString()).left()

                val session = sessionRow.toExerciseSession()
                val itemRows = ExerciseSessionItemsTable
                    .selectAll()
                    .where { ExerciseSessionItemsTable.sessionId eq id.value.toKotlinUuid() }
                    .orderBy(ExerciseSessionItemsTable.position)
                    .toList()

                val itemIds = itemRows.map { it[ExerciseSessionItemsTable.id] }
                val optionsByItemId = if (itemIds.isEmpty()) {
                    emptyMap()
                } else {
                    ExerciseItemOptionsTable
                        .selectAll()
                        .where { ExerciseItemOptionsTable.itemId inList itemIds }
                        .orderBy(ExerciseItemOptionsTable.position)
                        .groupBy { it[ExerciseItemOptionsTable.itemId] }
                        .mapValues { (_, rows) -> rows.map { it.toExerciseOption() } }
                }

                val items = itemRows.map { row ->
                    val itemId = row[ExerciseSessionItemsTable.id]
                    row.toExerciseSessionItem(optionsByItemId[itemId] ?: emptyList())
                }
                (session to items).right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Exercise repository findSessionWithItems failed" }
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun findDistractorCandidates(
        target: Word,
        wordListIds: Set<UUID>,
    ): Either<DomainError, List<Word>> =
        suspendTransaction {
            try {
                val baseQuery = if (wordListIds.isEmpty()) {
                    WordsTable.selectAll()
                } else {
                    WordsTable
                        .join(
                            WordListItemsTable,
                            JoinType.INNER,
                            additionalConstraint = { WordsTable.id eq WordListItemsTable.wordId },
                        )
                        .selectAll()
                        .where { WordListItemsTable.listId inList wordListIds.map { it.toKotlinUuid() } }
                }

                baseQuery
                    .toList()
                    .map { it.toWord() }
                    .filter { it.id != target.id }
                    .distinctBy { it.id }
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Exercise repository findDistractorCandidates failed target=${target.id}" }
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun recordAnswer(
        sessionId: ExerciseSessionId,
        itemId: ExerciseItemId,
        selectedOptionId: ExerciseOptionId,
        result: TrainingResult,
        masteryPromotedTo: String?,
        answeredAt: Instant,
    ): Either<DomainError, Unit> =
        suspendTransaction {
            try {
                ExerciseSessionItemsTable.update({
                    (ExerciseSessionItemsTable.sessionId eq sessionId.value.toKotlinUuid()) and
                        (ExerciseSessionItemsTable.id eq itemId.value.toKotlinUuid())
                }) {
                    it[ExerciseSessionItemsTable.result] = result.name
                    it[ExerciseSessionItemsTable.selectedOptionId] = selectedOptionId.value.toKotlinUuid()
                    it[ExerciseSessionItemsTable.answeredAt] = answeredAt
                    it[ExerciseSessionItemsTable.masteryPromotedTo] = masteryPromotedTo
                }
                Unit.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Exercise repository recordAnswer failed item=$itemId" }
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun completeSession(
        id: ExerciseSessionId,
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        completedAt: Instant,
    ): Either<DomainError, ExerciseSession> =
        suspendTransaction {
            try {
                ExerciseSessionsTable.update({
                    ExerciseSessionsTable.id eq id.value.toKotlinUuid()
                }) {
                    it[status] = SessionStatus.COMPLETED.name
                    it[ExerciseSessionsTable.correctCount] = correctCount
                    it[ExerciseSessionsTable.incorrectCount] = incorrectCount
                    it[ExerciseSessionsTable.skippedCount] = skippedCount
                    it[ExerciseSessionsTable.completedAt] = completedAt
                }
                ExerciseSessionsTable
                    .selectAll()
                    .where { ExerciseSessionsTable.id eq id.value.toKotlinUuid() }
                    .single()
                    .toExerciseSession()
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Exercise repository completeSession failed id=$id" }
                DomainError.DatabaseError.left()
            }
        }
}

private fun ResultRow.toExerciseSession() = ExerciseSession(
    id = ExerciseSessionId(this[ExerciseSessionsTable.id].toJavaUuid()),
    mode = TrainingMode.valueOf(this[ExerciseSessionsTable.mode]),
    status = SessionStatus.valueOf(this[ExerciseSessionsTable.status]),
    totalItems = this[ExerciseSessionsTable.totalItems],
    correctCount = this[ExerciseSessionsTable.correctCount],
    incorrectCount = this[ExerciseSessionsTable.incorrectCount],
    skippedCount = this[ExerciseSessionsTable.skippedCount],
    createdAt = this[ExerciseSessionsTable.createdAt],
    completedAt = this[ExerciseSessionsTable.completedAt],
)

private fun ResultRow.toExerciseSessionItem(options: List<ExerciseOption>) = ExerciseSessionItem(
    id = ExerciseItemId(this[ExerciseSessionItemsTable.id].toJavaUuid()),
    sessionId = ExerciseSessionId(this[ExerciseSessionItemsTable.sessionId].toJavaUuid()),
    wordId = WordId(this[ExerciseSessionItemsTable.wordId].toJavaUuid()),
    position = this[ExerciseSessionItemsTable.position],
    type = ExerciseType.valueOf(this[ExerciseSessionItemsTable.type]),
    promptKind = ExercisePromptKind.valueOf(this[ExerciseSessionItemsTable.promptKind]),
    promptText = this[ExerciseSessionItemsTable.promptText],
    result = this[ExerciseSessionItemsTable.result]?.let { TrainingResult.valueOf(it) },
    selectedOptionId = this[ExerciseSessionItemsTable.selectedOptionId]?.let {
        ExerciseOptionId(it.toJavaUuid())
    },
    answeredAt = this[ExerciseSessionItemsTable.answeredAt],
    masteryPromotedTo = this[ExerciseSessionItemsTable.masteryPromotedTo],
    options = options,
)

private fun ResultRow.toExerciseOption() = ExerciseOption(
    id = ExerciseOptionId(this[ExerciseItemOptionsTable.id].toJavaUuid()),
    itemId = ExerciseItemId(this[ExerciseItemOptionsTable.itemId].toJavaUuid()),
    wordId = WordId(this[ExerciseItemOptionsTable.wordId].toJavaUuid()),
    position = this[ExerciseItemOptionsTable.position],
    arabicText = this[ExerciseItemOptionsTable.arabicText],
    transliteration = this[ExerciseItemOptionsTable.transliteration],
    translation = this[ExerciseItemOptionsTable.translation],
    isCorrect = this[ExerciseItemOptionsTable.isCorrect],
)

private fun ResultRow.toWord() = Word(
    id = WordId(this[WordsTable.id].toJavaUuid()),
    arabicText = this[WordsTable.arabicText],
    transliteration = this[WordsTable.transliteration],
    translation = this[WordsTable.translation],
    partOfSpeech = PartOfSpeech.valueOf(this[WordsTable.partOfSpeech]),
    dialect = Dialect.valueOf(this[WordsTable.dialect]),
    difficulty = Difficulty.valueOf(this[WordsTable.difficulty]),
    masteryLevel = MasteryLevel.valueOf(this[WordsTable.masteryLevel]),
    pronunciationUrl = this[WordsTable.pronunciationUrl],
    rootId = this[WordsTable.rootId]?.let { com.tonihacks.qalam.domain.root.RootId(it.toJavaUuid()) },
    derivedFromId = this[WordsTable.derivedFromId]?.let { WordId(it.toJavaUuid()) },
    notes = this[WordsTable.notes],
    createdAt = this[WordsTable.createdAt],
    updatedAt = this[WordsTable.updatedAt],
)
