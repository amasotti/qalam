package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object WordPluralsTable : Table("word_plurals") {
    val id         = uuid("id")
    val wordId     = uuid("word_id").references(WordsTable.id, onDelete = ReferenceOption.CASCADE)
    val pluralForm = text("plural_form")
    val pluralType = varchar("plural_type", 20)
    override val primaryKey = PrimaryKey(id)
}
