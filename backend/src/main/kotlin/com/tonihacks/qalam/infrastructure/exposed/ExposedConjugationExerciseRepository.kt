package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.conjugation.model.Segment
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseRepository
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSession
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItem
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.uuid.toKotlinUuid
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

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

private fun List<Segment>.toJson(): String =
    buildJsonArray {
        this@toJson.forEach { segment ->
            add(buildJsonObject {
                put("text", segment.text)
                put("type", segment.type.name)
            })
        }
    }.toString()
