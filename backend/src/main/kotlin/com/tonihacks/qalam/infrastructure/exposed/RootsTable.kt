package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Instant

object RootsTable : Table("arabic_roots") {
    val id = uuid("id")
    val letters = array<String>("letters")
    val normalizedForm = varchar("normalized_form", 12)
    val displayForm = varchar("display_form", 24)
    val letterCount = short("letter_count")
    val meaning = text("meaning").nullable()
    val analysis = text("analysis").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
