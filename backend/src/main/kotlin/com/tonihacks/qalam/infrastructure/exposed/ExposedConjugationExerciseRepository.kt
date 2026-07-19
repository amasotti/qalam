package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.conjugation.model.Person
import com.tonihacks.qalam.domain.conjugation.model.Segment
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseAnswer
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItem
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItemId
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExercisePair
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExercisePairId
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseRepository
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSession
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSessionId
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.training.SessionStatus
import com.tonihacks.qalam.domain.training.TrainingMode
import com.tonihacks.qalam.domain.training.TrainingResult
import com.tonihacks.qalam.domain.word.WordId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ExposedConjugationExerciseRepository : ConjugationExerciseRepository {
    private val log = KotlinLogging.logger {}

    override suspend fun createSession(
        session: ConjugationExerciseSession,
        items: List<ConjugationExerciseItem>,
    ): Either<DomainError, ConjugationExerciseSession> =
        suspendTransaction {
            try {
                ConjugationExerciseSessionsTable.insert {
                    it[id] = session.id.value.toKotlinUuid()
                    it[mode] = session.mode.name
                    it[status] = session.status.name
                    it[tense] = session.tense.name
                    it[voice] = session.voice.name
                    it[totalItems] = session.totalItems
                    it[correctCount] = session.correctCount
                    it[incorrectCount] = session.incorrectCount
                    it[skippedCount] = session.skippedCount
                    it[createdAt] = session.createdAt
                    it[completedAt] = session.completedAt
                }
                items.forEach { item -> insertItem(item) }
                session.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Conjugation exercise repository createSession failed" }
                DomainError.DatabaseError.left()
            }
        }

    override suspend fun findSessionWithItems(
        id: ConjugationExerciseSessionId,
    ): Either<DomainError, Pair<ConjugationExerciseSession, List<ConjugationExerciseItem>>> = suspendTransaction {
        try {
            val sessionRow = ConjugationExerciseSessionsTable.selectAll()
                .where { ConjugationExerciseSessionsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?: return@suspendTransaction DomainError.NotFound("ConjugationExerciseSession", id.value.toString()).left()
            val itemRows = ConjugationExerciseItemsTable.selectAll()
                .where { ConjugationExerciseItemsTable.sessionId eq id.value.toKotlinUuid() }
                .orderBy(ConjugationExerciseItemsTable.position).toList()
            val itemIds = itemRows.map { it[ConjugationExerciseItemsTable.id] }
            val pairsByItem = if (itemIds.isEmpty()) emptyMap() else ConjugationExercisePairsTable.selectAll()
                .where { ConjugationExercisePairsTable.itemId inList itemIds }
                .orderBy(ConjugationExercisePairsTable.position)
                .groupBy { it[ConjugationExercisePairsTable.itemId] }
                .mapValues { (_, rows) -> rows.map { it.toPair() } }
            val answersByItem = if (itemIds.isEmpty()) emptyMap() else ConjugationExerciseAnswersTable.selectAll()
                .where { ConjugationExerciseAnswersTable.itemId inList itemIds }
                .groupBy { it[ConjugationExerciseAnswersTable.itemId] }
                .mapValues { (_, rows) -> rows.map { it.toAnswer() } }
            (sessionRow.toSession() to itemRows.map { row ->
                row.toItem(
                    pairsByItem[row[ConjugationExerciseItemsTable.id]] ?: emptyList(),
                    answersByItem[row[ConjugationExerciseItemsTable.id]] ?: emptyList(),
                )
            }).right()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            log.error(e) { "Conjugation exercise repository findSessionWithItems failed" }
            DomainError.DatabaseError.left()
        }
    }

    override suspend fun recordAnswer(
        sessionId: ConjugationExerciseSessionId,
        itemId: ConjugationExerciseItemId,
        answers: List<ConjugationExerciseAnswer>,
        result: TrainingResult,
        answeredAt: Instant,
    ): Either<DomainError, Unit> = suspendTransaction {
        try {
            ConjugationExerciseItemsTable.update({
                (ConjugationExerciseItemsTable.sessionId eq sessionId.value.toKotlinUuid()) and
                    (ConjugationExerciseItemsTable.id eq itemId.value.toKotlinUuid())
            }) {
                it[ConjugationExerciseItemsTable.result] = result.name
                it[ConjugationExerciseItemsTable.answeredAt] = answeredAt
            }
            answers.forEach { answer ->
                ConjugationExerciseAnswersTable.insert {
                    it[ConjugationExerciseAnswersTable.itemId] = answer.itemId.value.toKotlinUuid()
                    it[formId] = answer.formId.toKotlinUuid()
                    it[selectedLabelId] = answer.selectedLabelId?.toKotlinUuid()
                    it[submittedText] = answer.submittedText
                    it[isCorrect] = answer.isCorrect
                }
            }
            Unit.right()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            log.error(e) { "Conjugation exercise repository recordAnswer failed item=$itemId" }
            DomainError.DatabaseError.left()
        }
    }

    override suspend fun completeSession(
        id: ConjugationExerciseSessionId,
        correctCount: Int,
        incorrectCount: Int,
        skippedCount: Int,
        completedAt: Instant,
    ): Either<DomainError, ConjugationExerciseSession> = suspendTransaction {
        try {
            ConjugationExerciseSessionsTable.update({ ConjugationExerciseSessionsTable.id eq id.value.toKotlinUuid() }) {
                it[status] = SessionStatus.COMPLETED.name
                it[ConjugationExerciseSessionsTable.correctCount] = correctCount
                it[ConjugationExerciseSessionsTable.incorrectCount] = incorrectCount
                it[ConjugationExerciseSessionsTable.skippedCount] = skippedCount
                it[ConjugationExerciseSessionsTable.completedAt] = completedAt
            }
            ConjugationExerciseSessionsTable.selectAll()
                .where { ConjugationExerciseSessionsTable.id eq id.value.toKotlinUuid() }
                .single().toSession().right()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            log.error(e) { "Conjugation exercise repository completeSession failed id=$id" }
            DomainError.DatabaseError.left()
        }
    }

    override suspend fun listSessions(page: Int, size: Int): Either<DomainError, Pair<List<ConjugationExerciseSession>, Long>> = suspendTransaction {
        try {
            val total = ConjugationExerciseSessionsTable.selectAll().count()
            val sessions = ConjugationExerciseSessionsTable.selectAll()
                .orderBy(ConjugationExerciseSessionsTable.createdAt to SortOrder.DESC)
                .limit(size).offset(((page - 1) * size).toLong())
                .map { it.toSession() }
            (sessions to total).right()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            log.error(e) { "Conjugation exercise repository listSessions failed" }
            DomainError.DatabaseError.left()
        }
    }

    private fun insertItem(item: ConjugationExerciseItem) {
        ConjugationExerciseItemsTable.insert {
            it[id] = item.id.value.toKotlinUuid()
            it[sessionId] = item.sessionId.value.toKotlinUuid()
            it[wordId] = item.wordId.value.toKotlinUuid()
            it[position] = item.position
            it[lemmaSnapshot] = item.lemmaSnapshot
            it[translationSnapshot] = item.translationSnapshot
            it[verbFormSnapshot] = item.verbFormSnapshot
            it[result] = item.result?.name
            it[answeredAt] = item.answeredAt
        }
        item.pairs.forEach { pair ->
            ConjugationExercisePairsTable.insert {
                it[id] = pair.id.value.toKotlinUuid()
                it[itemId] = pair.itemId.value.toKotlinUuid()
                it[position] = pair.position
                it[formPosition] = pair.formPosition
                it[labelPosition] = pair.labelPosition
                it[formId] = pair.formId.toKotlinUuid()
                it[labelId] = pair.labelId.toKotlinUuid()
                it[arabic] = pair.arabic
                it[segmentsJson] = pair.segments.toJson()
                it[tense] = pair.tense.name
                it[voice] = pair.voice.name
                it[person] = pair.person.code
            }
        }
    }
}

private fun ResultRow.toSession() = ConjugationExerciseSession(
    id = ConjugationExerciseSessionId(this[ConjugationExerciseSessionsTable.id].toJavaUuid()),
    mode = TrainingMode.valueOf(this[ConjugationExerciseSessionsTable.mode]),
    status = SessionStatus.valueOf(this[ConjugationExerciseSessionsTable.status]),
    tense = Tense.valueOf(this[ConjugationExerciseSessionsTable.tense]),
    voice = Voice.valueOf(this[ConjugationExerciseSessionsTable.voice]),
    totalItems = this[ConjugationExerciseSessionsTable.totalItems],
    correctCount = this[ConjugationExerciseSessionsTable.correctCount],
    incorrectCount = this[ConjugationExerciseSessionsTable.incorrectCount],
    skippedCount = this[ConjugationExerciseSessionsTable.skippedCount],
    createdAt = this[ConjugationExerciseSessionsTable.createdAt],
    completedAt = this[ConjugationExerciseSessionsTable.completedAt],
)

private fun ResultRow.toItem(
    pairs: List<ConjugationExercisePair>,
    answers: List<ConjugationExerciseAnswer>,
) = ConjugationExerciseItem(
    id = ConjugationExerciseItemId(this[ConjugationExerciseItemsTable.id].toJavaUuid()),
    sessionId = ConjugationExerciseSessionId(this[ConjugationExerciseItemsTable.sessionId].toJavaUuid()),
    wordId = WordId(this[ConjugationExerciseItemsTable.wordId].toJavaUuid()),
    position = this[ConjugationExerciseItemsTable.position],
    lemmaSnapshot = this[ConjugationExerciseItemsTable.lemmaSnapshot],
    translationSnapshot = this[ConjugationExerciseItemsTable.translationSnapshot],
    verbFormSnapshot = this[ConjugationExerciseItemsTable.verbFormSnapshot],
    result = this[ConjugationExerciseItemsTable.result]?.let(TrainingResult::valueOf),
    answeredAt = this[ConjugationExerciseItemsTable.answeredAt],
    pairs = pairs,
    answers = answers,
)

private fun ResultRow.toPair() = ConjugationExercisePair(
    id = ConjugationExercisePairId(this[ConjugationExercisePairsTable.id].toJavaUuid()),
    itemId = ConjugationExerciseItemId(this[ConjugationExercisePairsTable.itemId].toJavaUuid()),
    position = this[ConjugationExercisePairsTable.position],
    formPosition = this[ConjugationExercisePairsTable.formPosition],
    labelPosition = this[ConjugationExercisePairsTable.labelPosition],
    formId = this[ConjugationExercisePairsTable.formId].toJavaUuid(),
    labelId = this[ConjugationExercisePairsTable.labelId].toJavaUuid(),
    arabic = this[ConjugationExercisePairsTable.arabic],
    segments = this[ConjugationExercisePairsTable.segmentsJson].toSegments(),
    tense = Tense.valueOf(this[ConjugationExercisePairsTable.tense]),
    voice = Voice.valueOf(this[ConjugationExercisePairsTable.voice]),
    person = Person.entries.single { it.code == this[ConjugationExercisePairsTable.person] },
)

private fun String.toSegments(): List<Segment> = Json.parseToJsonElement(this).jsonArray.map { element ->
    val segment = element.jsonObject
    Segment(
        text = segment.getValue("text").jsonPrimitive.content,
        type = com.tonihacks.qalam.domain.conjugation.model.SegmentType.valueOf(segment.getValue("type").jsonPrimitive.content),
    )
}

private fun ResultRow.toAnswer() = ConjugationExerciseAnswer(
    itemId = ConjugationExerciseItemId(this[ConjugationExerciseAnswersTable.itemId].toJavaUuid()),
    formId = this[ConjugationExerciseAnswersTable.formId].toJavaUuid(),
    selectedLabelId = this[ConjugationExerciseAnswersTable.selectedLabelId]?.toJavaUuid(),
    submittedText = this[ConjugationExerciseAnswersTable.submittedText],
    isCorrect = this[ConjugationExerciseAnswersTable.isCorrect],
)

private fun List<Segment>.toJson(): String =
    buildJsonArray {
        this@toJson.forEach { segment ->
            add(buildJsonObject {
                put("text", segment.text)
                put("type", segment.type.name)
            })
        }
    }.toString()
