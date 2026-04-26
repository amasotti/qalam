package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object WordMorphologyTable : Table("word_morphology") {
    val wordId      = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
    val gender      = varchar("gender", 12).nullable()
    val verbPattern = varchar("verb_pattern", 5).nullable()
    override val primaryKey = PrimaryKey(wordId)
}
