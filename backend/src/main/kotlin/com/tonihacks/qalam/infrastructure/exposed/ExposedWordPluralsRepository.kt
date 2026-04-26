package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.PluralType
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordPlural
import com.tonihacks.qalam.domain.word.WordPluralId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedWordPluralsRepository {

    suspend fun findPlurals(wordId: WordId): Either<DomainError, List<WordPlural>> =
        suspendTransaction {
            try {
                WordPluralsTable
                    .selectAll()
                    .where { WordPluralsTable.wordId eq wordId.value.toKotlinUuid() }
                    .map { it.toWordPlural() }
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    suspend fun addPlural(plural: WordPlural): Either<DomainError, WordPlural> =
        suspendTransaction {
            try {
                WordPluralsTable.insert {
                    it[id]         = plural.id.value.toKotlinUuid()
                    it[wordId]     = plural.wordId.value.toKotlinUuid()
                    it[pluralForm] = plural.pluralForm
                    it[pluralType] = plural.pluralType.name
                }
                plural.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    suspend fun deletePlural(wordId: WordId, pluralId: WordPluralId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deletedCount = WordPluralsTable.deleteWhere {
                    (WordPluralsTable.id eq pluralId.value.toKotlinUuid()) and
                    (WordPluralsTable.wordId eq wordId.value.toKotlinUuid())
                }
                ensure(deletedCount > 0) { DomainError.NotFound("WordPlural", pluralId.toString()) }
            }
        }
}

private fun ResultRow.toWordPlural() = WordPlural(
    id         = WordPluralId(this[WordPluralsTable.id].toJavaUuid()),
    wordId     = WordId(this[WordPluralsTable.wordId].toJavaUuid()),
    pluralForm = this[WordPluralsTable.pluralForm],
    pluralType = PluralType.fromString(this[WordPluralsTable.pluralType]) ?: PluralType.OTHER,
)
