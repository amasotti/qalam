package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object AnnotationsTable : Table("annotations") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class)
    val textId = uuid("text_id")
    val anchor = varchar("anchor", 1000)
    val type = varchar("type", 50)
    val content = text("content").nullable()
    val masteryLevel = varchar("mastery_level", 50).nullable()
    val reviewFlag = bool("review_flag").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}

object AnnotationWordsTable : Table("annotation_words") {
    @OptIn(ExperimentalUuidApi::class)
    val annotationId = uuid("annotation_id")
    @OptIn(ExperimentalUuidApi::class)
    val wordId = uuid("word_id")

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(annotationId, wordId)
}
