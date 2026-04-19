package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object WordsTable : Table("words") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    val arabicText = text("arabic_text")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val partOfSpeech = varchar("part_of_speech", 20)
    val dialect = varchar("dialect", 20)
    val difficulty = varchar("difficulty", 20)
    val masteryLevel = varchar("mastery_level", 20)
    val pronunciationUrl = text("pronunciation_url").nullable()
    @OptIn(ExperimentalUuidApi::class)
    val rootId = uuid("root_id").nullable()
    @OptIn(ExperimentalUuidApi::class)
    val derivedFromId = uuid("derived_from_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}

object WordDictionaryLinksTable : Table("word_dictionary_links") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class)
    val wordId = uuid("word_id")
    val linkSource = varchar("source", 30)
    val url = text("url")

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}

object WordExamplesTable : Table("word_examples") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class)
    val wordId = uuid("word_id")
    val arabic = text("arabic")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val createdAt = timestamp("created_at")

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}

object WordProgressTable : Table("word_progress") {
    @OptIn(ExperimentalUuidApi::class)
    val wordId = uuid("word_id")
    val consecutiveCorrect = integer("consecutive_correct")
    val totalAttempts = integer("total_attempts")
    val totalCorrect = integer("total_correct")
    val lastReviewedAt = timestamp("last_reviewed_at").nullable()

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(wordId)
}
