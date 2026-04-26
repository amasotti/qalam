package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object WordRelationsTable : Table("word_relations") {
    val wordId        = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
    val relatedWordId = uuid("related_word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
    val relationType  = varchar("relation_type", 10)
    override val primaryKey = PrimaryKey(wordId, relatedWordId, relationType)
}
