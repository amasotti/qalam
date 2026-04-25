package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object AnnotationsTable : Table("annotations") {
    val id = uuid("id")
    val textId = uuid("text_id")
    val anchor = varchar("anchor", 1000)
    val type = varchar("type", 50)
    val content = text("content").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object AnnotationWordsTable : Table("annotation_words") {
    val annotationId = uuid("annotation_id")
    val wordId = uuid("word_id")
    override val primaryKey = PrimaryKey(annotationId, wordId)
}
