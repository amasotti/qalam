package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.stringParam
import org.postgresql.util.PGobject

/**
 * PostgreSQL ILIKE — case-insensitive LIKE.
 * Uses GIN trgm indexes when present (e.g. `gin_trgm_ops`).
 * Not available as a built-in in Exposed 1.2.0.
 */
infix fun <T : String?> Expression<T>.ilike(pattern: String): Op<Boolean> {
    val col = this
    return object : ComparisonOp(col, stringParam(pattern), "ILIKE") {}
}

/** PostgreSQL function backed by an immutable expression index for harakat-insensitive search. */
fun Expression<String>.stripArabicDiacritics(): Expression<String> =
    CustomFunction("remove_arabic_diacritics", TextColumnType(), this)

private class JsonbColumnType : TextColumnType() {
    override fun sqlType(): String = "JSONB"

    override fun notNullValueToDB(value: String): Any =
        PGobject().apply {
            type = "jsonb"
            this.value = value
        }

    override fun valueFromDB(value: Any): String =
        if (value is PGobject) value.value.orEmpty() else super.valueFromDB(value)
}

fun Table.jsonb(name: String): Column<String> = registerColumn(name, JsonbColumnType())
