package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object ExerciseSessionsTable : Table("exercise_sessions") {
    val id = uuid("id")
    val mode = varchar("mode", 20)
    val status = varchar("status", 20)
    val totalItems = integer("total_items")
    val correctCount = integer("correct_count")
    val incorrectCount = integer("incorrect_count")
    val skippedCount = integer("skipped_count")
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ExerciseSessionItemsTable : Table("exercise_session_items") {
    val id = uuid("id")
    val sessionId = uuid("session_id")
    val wordId = uuid("word_id")
    val position = integer("position")
    val type = varchar("type", 40)
    val promptKind = varchar("prompt_kind", 20)
    val promptText = text("prompt_text")
    val result = varchar("result", 20).nullable()
    val selectedOptionId = uuid("selected_option_id").nullable()
    val answeredAt = timestamp("answered_at").nullable()
    val masteryPromotedTo = varchar("mastery_promoted_to", 20).nullable()

    override val primaryKey = PrimaryKey(id)
}

object ExerciseItemOptionsTable : Table("exercise_item_options") {
    val id = uuid("id")
    val itemId = uuid("item_id")
    val wordId = uuid("word_id")
    val position = integer("position")
    val arabicText = text("arabic_text")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val isCorrect = bool("is_correct")

    override val primaryKey = PrimaryKey(id)
}
