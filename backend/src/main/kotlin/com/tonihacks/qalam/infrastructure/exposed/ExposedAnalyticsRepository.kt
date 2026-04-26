package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tonihacks.qalam.domain.analytics.AnalyticsRepository
import com.tonihacks.qalam.domain.analytics.SessionAccuracyPoint
import com.tonihacks.qalam.domain.analytics.TextStats
import com.tonihacks.qalam.domain.analytics.TrainingAnalytics
import com.tonihacks.qalam.domain.analytics.WordStats
import com.tonihacks.qalam.domain.error.DomainError
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

class ExposedAnalyticsRepository : AnalyticsRepository {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getWordStats(): Either<DomainError, WordStats> =
        suspendTransaction {
            try {
                val rows = WordsTable.selectAll().toList()
                WordStats(
                    total = rows.size,
                    byDialect = rows.groupBy { it[WordsTable.dialect] }.mapValues { it.value.size },
                    byDifficulty = rows.groupBy { it[WordsTable.difficulty] }.mapValues { it.value.size },
                    byMastery = rows.groupBy { it[WordsTable.masteryLevel] }.mapValues { it.value.size },
                    byPartOfSpeech = rows.groupBy { it[WordsTable.partOfSpeech] }.mapValues { it.value.size },
                ).right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getTextStats(): Either<DomainError, TextStats> =
        suspendTransaction {
            try {
                val rows = TextsTable.selectAll().toList()
                TextStats(
                    total = rows.size,
                    byDialect = rows.groupBy { it[TextsTable.dialect] }.mapValues { it.value.size },
                    byDifficulty = rows.groupBy { it[TextsTable.difficulty] }.mapValues { it.value.size },
                ).right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getRootCount(): Either<DomainError, Int> =
        suspendTransaction {
            try {
                RootsTable.selectAll().count().toInt().right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getTrainingAnalytics(): Either<DomainError, TrainingAnalytics> =
        suspendTransaction {
            try {
                val total = TrainingSessionsTable.selectAll().count().toInt()
                val completed = TrainingSessionsTable
                    .selectAll()
                    .where { TrainingSessionsTable.status eq "COMPLETED" }
                    .toList()
                val avgAccuracy = completed
                    .mapNotNull { row ->
                        val c = row[TrainingSessionsTable.correctCount]
                        val answered = c + row[TrainingSessionsTable.incorrectCount]
                        if (answered > 0) c.toDouble() / answered else null
                    }
                    .average()
                    .let { if (it.isNaN()) 0.0 else it }
                val promotions = TrainingSessionWordsTable
                    .selectAll()
                    .where { TrainingSessionWordsTable.masteryPromotedTo.isNotNull() }
                    .count().toInt()
                val recent = completed
                    .sortedByDescending { it[TrainingSessionsTable.createdAt] }
                    .take(20)
                    .map { row ->
                        val c = row[TrainingSessionsTable.correctCount]
                        val answered = c + row[TrainingSessionsTable.incorrectCount]
                        SessionAccuracyPoint(
                            date = row[TrainingSessionsTable.createdAt].toString(),
                            accuracy = if (answered > 0) c.toDouble() / answered else 0.0,
                            mode = row[TrainingSessionsTable.mode],
                        )
                    }
                TrainingAnalytics(
                    totalSessions = total,
                    completedSessions = completed.size,
                    averageAccuracy = avgAccuracy,
                    totalPromotions = promotions,
                    recentSessions = recent,
                ).right()
            } catch (e: Exception) {
                DomainError.DatabaseError.left()
            }
        }
}
