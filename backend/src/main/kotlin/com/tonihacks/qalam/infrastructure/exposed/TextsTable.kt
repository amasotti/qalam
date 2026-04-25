package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object TextsTable : Table("texts") {
    val id = uuid("id")
    val title = text("title")
    val body = text("body")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val difficulty = varchar("difficulty", 20)
    val dialect = varchar("dialect", 20)
    val comments = text("comments").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object TextTagsTable : Table("text_tags") {
    val textId = uuid("text_id")
    val tag = varchar("tag", 100)
}
