package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Gender
import com.tonihacks.qalam.domain.word.VerbPattern
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.domain.word.WordMorphology
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedWordMorphologyRepository {

    suspend fun findMorphology(wordId: WordId): Either<DomainError, WordMorphology?> =
        suspendTransaction {
            try {
                WordMorphologyTable
                    .selectAll()
                    .where { WordMorphologyTable.wordId eq wordId.value.toKotlinUuid() }
                    .singleOrNull()
                    ?.toWordMorphology()
                    .right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    suspend fun upsertMorphology(morphology: WordMorphology): Either<DomainError, WordMorphology> =
        suspendTransaction {
            try {
                WordMorphologyTable.upsert {
                    it[wordId]      = morphology.wordId.value.toKotlinUuid()
                    it[gender]      = morphology.gender?.name
                    it[verbPattern] = morphology.verbPattern?.name
                }
                morphology.right()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                DomainError.DatabaseError.left()
            }
        }
}

private fun ResultRow.toWordMorphology() = WordMorphology(
    wordId      = WordId(this[WordMorphologyTable.wordId].toJavaUuid()),
    gender      = this[WordMorphologyTable.gender]?.let { Gender.fromString(it) },
    verbPattern = this[WordMorphologyTable.verbPattern]?.let { VerbPattern.fromString(it) },
)
