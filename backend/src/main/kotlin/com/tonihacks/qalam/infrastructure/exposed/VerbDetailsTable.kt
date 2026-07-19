package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object VerbDetailsTable : Table("verb_details") {
    val wordId         = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
    val verbForm       = varchar("verb_form", 5)
    val pastPattern    = varchar("past_pattern", 20).nullable()
    val presentPattern = varchar("present_pattern", 20).nullable()
    val weaknessType   = varchar("weakness_type", 20)
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    override val primaryKey = PrimaryKey(wordId)
}
