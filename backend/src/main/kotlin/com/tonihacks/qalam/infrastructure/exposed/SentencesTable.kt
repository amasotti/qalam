package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object SentencesTable : Table("sentences") {
    val id = uuid("id")
    val textId = uuid("text_id")
    val position = integer("position")
    val arabicText = text("arabic_text")
    val transliteration = text("transliteration").nullable()
    val freeTranslation = text("free_translation").nullable()
    val notes = text("notes").nullable()
    val tokensValid = bool("tokens_valid").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object AlignmentTokensTable : Table("alignment_tokens") {
    val id = uuid("id")
    val sentenceId = uuid("sentence_id")
    val position = integer("position")
    val arabic = text("arabic")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    val wordId = uuid("word_id").nullable()

    override val primaryKey = PrimaryKey(id)
}
