package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object SentencesTable : Table("sentences") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class)
    val textId = uuid("text_id")
    val position = integer("position")
    val arabicText = text("arabic_text")
    val transliteration = text("transliteration").nullable()
    val freeTranslation = text("free_translation").nullable()
    val notes = text("notes").nullable()
    val tokensValid = bool("tokens_valid").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}

object AlignmentTokensTable : Table("alignment_tokens") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id")
    @OptIn(ExperimentalUuidApi::class)
    val sentenceId = uuid("sentence_id")
    val position = integer("position")
    val arabic = text("arabic")
    val transliteration = text("transliteration").nullable()
    val translation = text("translation").nullable()
    @OptIn(ExperimentalUuidApi::class)
    val wordId = uuid("word_id").nullable()

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}
