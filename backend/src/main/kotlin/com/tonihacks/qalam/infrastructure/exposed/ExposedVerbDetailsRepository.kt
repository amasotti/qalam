package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.VerbDetails
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WeaknessType
import com.tonihacks.qalam.domain.word.WordId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.time.Clock
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedVerbDetailsRepository {

    private val log = KotlinLogging.logger {}

    suspend fun find(wordId: WordId): Either<DomainError, VerbDetails?> =
        suspendTransaction {
            try {
                VerbDetailsTable
                    .selectAll()
                    .where { VerbDetailsTable.wordId eq wordId.value.toKotlinUuid() }
                    .singleOrNull()
                    ?.toVerbDetails()
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Verb details repository find failed" }
                DomainError.DatabaseError.left()
            }
        }

    suspend fun upsert(details: VerbDetails): Either<DomainError, VerbDetails> =
        suspendTransaction {
            try {
                val now = Clock.System.now()
                VerbDetailsTable.upsert {
                    it[wordId]         = details.wordId.value.toKotlinUuid()
                    it[verbForm]       = details.verbForm.name
                    it[pastPattern]    = details.pastPattern
                    it[presentPattern] = details.presentPattern
                    it[weaknessType]   = details.weaknessType.name
                    it[updatedAt]      = now
                    it[createdAt]      = details.createdAt
                }
                details.copy(updatedAt = now).right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Verb details repository upsert failed" }
                DomainError.DatabaseError.left()
            }
        }

    suspend fun delete(wordId: WordId): Either<DomainError, Unit> =
        suspendTransaction {
            try {
                VerbDetailsTable.deleteWhere { VerbDetailsTable.wordId eq wordId.value.toKotlinUuid() }
                Unit.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                log.error(e) { "Verb details repository delete failed" }
                DomainError.DatabaseError.left()
            }
        }
}

private fun ResultRow.toVerbDetails() = VerbDetails(
    wordId         = WordId(this[VerbDetailsTable.wordId].toJavaUuid()),
    verbForm       = VerbPattern.fromString(this[VerbDetailsTable.verbForm])!!,
    pastPattern    = this[VerbDetailsTable.pastPattern],
    presentPattern = this[VerbDetailsTable.presentPattern],
    weaknessType   = WeaknessType.fromString(this[VerbDetailsTable.weaknessType]) ?: WeaknessType.SOUND,
    createdAt      = this[VerbDetailsTable.createdAt],
    updatedAt      = this[VerbDetailsTable.updatedAt],
)
