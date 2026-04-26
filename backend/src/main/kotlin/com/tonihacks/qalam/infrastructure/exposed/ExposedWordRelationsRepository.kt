package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.RelationType
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordRelation
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLState
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedWordRelationsRepository {

    suspend fun findRelations(wordId: WordId): Either<DomainError, List<WordRelation>> =
        suspendTransaction {
            try {
                val id = wordId.value.toKotlinUuid()
                WordRelationsTable
                    .selectAll()
                    .where {
                        (WordRelationsTable.wordId eq id) or (WordRelationsTable.relatedWordId eq id)
                    }
                    .map { row ->
                        val storedWordId = row[WordRelationsTable.wordId]
                        val type = RelationType.fromString(row[WordRelationsTable.relationType])
                            ?: RelationType.RELATED
                        if (storedWordId == id) {
                            WordRelation(wordId, WordId(row[WordRelationsTable.relatedWordId].toJavaUuid()), type)
                        } else {
                            WordRelation(wordId, WordId(storedWordId.toJavaUuid()), type)
                        }
                    }
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    suspend fun addRelation(relation: WordRelation): Either<DomainError, WordRelation> =
        suspendTransaction {
            try {
                WordRelationsTable.insert {
                    it[wordId]        = relation.wordId.value.toKotlinUuid()
                    it[relatedWordId] = relation.relatedWordId.value.toKotlinUuid()
                    it[relationType]  = relation.relationType.name
                }
                relation.right()
            } catch (e: java.sql.SQLException) {
                when (e.sqlState) {
                    PSQLState.UNIQUE_VIOLATION.state ->
                        DomainError.Conflict("WordRelation", "${relation.wordId}-${relation.relatedWordId}").left()
                    PSQLState.FOREIGN_KEY_VIOLATION.state ->
                        DomainError.NotFound("Word", relation.relatedWordId.toString()).left()
                    else -> DomainError.DatabaseError.left()
                }
            }
        }

    suspend fun deleteRelation(
        wordId: WordId,
        relatedWordId: WordId,
        type: RelationType,
    ): Either<DomainError, Unit> =
        suspendTransaction {
            try {
                either {
                    val a = wordId.value.toKotlinUuid()
                    val b = relatedWordId.value.toKotlinUuid()
                    val t = type.name

                    val deletedCount = WordRelationsTable.deleteWhere {
                        (
                            (WordRelationsTable.wordId eq a) and
                            (WordRelationsTable.relatedWordId eq b) and
                            (WordRelationsTable.relationType eq t)
                        ) or (
                            (WordRelationsTable.wordId eq b) and
                            (WordRelationsTable.relatedWordId eq a) and
                            (WordRelationsTable.relationType eq t)
                        )
                    }
                    ensure(deletedCount > 0) { DomainError.NotFound("WordRelation", "$wordId-$relatedWordId-$type") }
                }
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }
}

private fun ResultRow.toWordRelation() = WordRelation(
    wordId        = WordId(this[WordRelationsTable.wordId].toJavaUuid()),
    relatedWordId = WordId(this[WordRelationsTable.relatedWordId].toJavaUuid()),
    relationType  = RelationType.fromString(this[WordRelationsTable.relationType]) ?: RelationType.RELATED,
)
