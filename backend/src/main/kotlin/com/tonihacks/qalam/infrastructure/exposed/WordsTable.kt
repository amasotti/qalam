package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object WordsTable : Table("words") {
    val id = uuid("id")
    val arabicText = text("arabic_text")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val partOfSpeech = varchar("part_of_speech", 20)
    val dialect = varchar("dialect", 20)
    val difficulty = varchar("difficulty", 20)
    val masteryLevel = varchar("mastery_level", 20)
    val pronunciationUrl = text("pronunciation_url").nullable()
    val rootId = uuid("root_id").nullable()
    val derivedFromId = uuid("derived_from_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object WordDictionaryLinksTable : Table("word_dictionary_links") {
    val id = uuid("id")
    val wordId = uuid("word_id")
    val linkSource = varchar("source", 30)
    val url = text("url")

    override val primaryKey = PrimaryKey(id)
}

object WordExamplesTable : Table("word_examples") {
    val id = uuid("id")
    val wordId = uuid("word_id")
    val arabic = text("arabic")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object WordProgressTable : Table("word_progress") {
    val wordId = uuid("word_id")
    val consecutiveCorrect = integer("consecutive_correct")
    val totalAttempts = integer("total_attempts")
    val totalCorrect = integer("total_correct")
    val lastReviewedAt = timestamp("last_reviewed_at").nullable()

    override val primaryKey = PrimaryKey(wordId)
}
