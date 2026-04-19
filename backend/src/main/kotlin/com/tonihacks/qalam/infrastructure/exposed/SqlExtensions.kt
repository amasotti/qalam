package com.tonihacks.qalam.infrastructure.exposed

import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.stringParam

/**
 * PostgreSQL ILIKE — case-insensitive LIKE.
 * Uses GIN trgm indexes when present (e.g. `gin_trgm_ops`).
 * Not available as a built-in in Exposed 1.2.0.
 */
infix fun <T : String?> Expression<T>.ilike(pattern: String): Op<Boolean> {
    val col = this
    return object : ComparisonOp(col, stringParam(pattern), "ILIKE") {}
}
