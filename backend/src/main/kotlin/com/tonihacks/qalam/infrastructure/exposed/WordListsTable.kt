package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object WordListsTable : Table("word_lists") {
    val id = uuid("id")
    val title = varchar("title", 200)
    val description = text("description").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object WordListItemsTable : Table("word_list_items") {
    val listId = uuid("list_id")
    val wordId = uuid("word_id")
    val position = integer("position")
    val addedAt = timestamp("added_at")

    override val primaryKey = PrimaryKey(listId, wordId)
}
