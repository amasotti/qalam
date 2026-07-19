package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object ConjugationExerciseSessionsTable : Table("conjugation_exercise_sessions") {
    val id = uuid("id")
    val mode = varchar("mode", 20)
    val status = varchar("status", 20)
    val tense = varchar("tense", 20)
    val voice = varchar("voice", 20)
    val totalItems = integer("total_items")
    val correctCount = integer("correct_count")
    val incorrectCount = integer("incorrect_count")
    val skippedCount = integer("skipped_count")
    val createdAt = timestamp("created_at")
    val completedAt = timestamp("completed_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ConjugationExerciseItemsTable : Table("conjugation_exercise_items") {
    val id = uuid("id")
    val sessionId = uuid("session_id")
    val wordId = uuid("word_id")
    val position = integer("position")
    val lemmaSnapshot = text("lemma_snapshot")
    val translationSnapshot = text("translation_snapshot").nullable()
    val verbFormSnapshot = varchar("verb_form_snapshot", 5)
    val result = varchar("result", 20).nullable()
    val answeredAt = timestamp("answered_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ConjugationExercisePairsTable : Table("conjugation_exercise_pairs") {
    val id = uuid("id")
    val itemId = uuid("item_id")
    val position = integer("position")
    val formPosition = integer("form_position")
    val labelPosition = integer("label_position")
    val formId = uuid("form_id")
    val labelId = uuid("label_id")
    val arabic = text("arabic")
    val segmentsJson = jsonb("segments_json")
    val tense = varchar("tense", 20)
    val voice = varchar("voice", 20)
    val person = varchar("person", 10)

    override val primaryKey = PrimaryKey(id)
}
