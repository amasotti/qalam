package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object TrainingSessionsTable : Table("training_sessions") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    val mode = varchar("mode", 20)
    val status = varchar("status", 20)
    val totalWords = integer("total_words")
    val correctCount = integer("correct_count")
    val incorrectCount = integer("incorrect_count")
    val skippedCount = integer("skipped_count")
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}

object TrainingSessionWordsTable : Table("training_session_words") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class)
    val sessionId = uuid("session_id")
    @OptIn(ExperimentalUuidApi::class)
    val wordId = uuid("word_id")
    val position = integer("position")
    val frontSide = varchar("front_side", 20)
    val result = varchar("result", 20).nullable()
    val answeredAt = timestamp("answered_at").nullable()
    val masteryPromotedTo = varchar("mastery_promoted_to", 20).nullable()

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}
