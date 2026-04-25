package com.tonihacks.qalam.domain.text

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import java.util.UUID
import kotlin.time.Clock

class TextService(private val repo: TextRepository) {

    suspend fun list(
        page: Int?,
        size: Int?,
        q: String?,
        dialect: String?,
        difficulty: String?,
        tag: String?,
        sortBy: String?,
        sortDesc: Boolean?,
    ): Either<DomainError, PaginatedResponse<Text>> = either {
        val parsedSortBy = when (sortBy?.uppercase()) {
            "UPDATED_AT" -> TextSortField.UPDATED_AT
            "TITLE" -> TextSortField.TITLE
            else -> TextSortField.CREATED_AT
        }
        val filters = TextFilters(
            q = q,
            dialect = dialect?.let { parseTextEnum("dialect", it) { s -> Dialect.fromString(s) }.bind() },
            difficulty = difficulty?.let { parseTextEnum("difficulty", it) { s -> Difficulty.fromString(s) }.bind() },
            tag = tag,
            sortBy = parsedSortBy,
            sortDesc = sortDesc ?: true,
        )
        repo.list(PageRequest.from(page, size), filters).bind()
    }

    suspend fun getById(id: String): Either<DomainError, Text> =
        parseTextId(id).flatMap { repo.findById(it) }

    suspend fun create(
        title: String,
        body: String,
        transliteration: String?,
        translation: String?,
        difficulty: String,
        dialect: String,
        comments: String?,
        tags: List<String>,
    ): Either<DomainError, Text> = either {
        if (title.isBlank()) raise(DomainError.ValidationError("title", "Title must not be blank"))

        val parsedDifficulty = parseTextEnum("difficulty", difficulty) { Difficulty.fromString(it) }.bind()
        val parsedDialect = parseTextEnum("dialect", dialect) { Dialect.fromString(it) }.bind()

        val now = Clock.System.now()
        val text = Text(
            id = TextId(UUID.randomUUID()),
            title = title.trim(),
            body = body,
            transliteration = transliteration,
            translation = translation,
            difficulty = parsedDifficulty,
            dialect = parsedDialect,
            comments = comments,
            tags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            createdAt = now,
            updatedAt = now,
        )
        repo.save(text).bind()
    }

    suspend fun update(
        id: String,
        title: String?,
        body: String?,
        transliteration: String?,
        translation: String?,
        difficulty: String?,
        dialect: String?,
        comments: String?,
        tags: List<String>?,
    ): Either<DomainError, Text> = either {
        val textId = parseTextId(id).bind()
        val existing = repo.findById(textId).bind()

        val parsedDifficulty = difficulty?.let {
            parseTextEnum("difficulty", it) { s -> Difficulty.fromString(s) }.bind()
        } ?: existing.difficulty

        val parsedDialect = dialect?.let {
            parseTextEnum("dialect", it) { s -> Dialect.fromString(s) }.bind()
        } ?: existing.dialect

        val updated = existing.copy(
            title = title?.trim() ?: existing.title,
            body = body ?: existing.body,
            transliteration = clearable(transliteration, existing.transliteration),
            translation = clearable(translation, existing.translation),
            difficulty = parsedDifficulty,
            dialect = parsedDialect,
            comments = clearable(comments, existing.comments),
            tags = tags?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct() ?: existing.tags,
            updatedAt = Clock.System.now(),
        )
        repo.update(updated).bind()
    }

    suspend fun delete(id: String): Either<DomainError, Unit> =
        parseTextId(id).flatMap { repo.delete(it) }
}

/** null = keep existing, blank = clear to null, non-blank = use new value */
private fun clearable(incoming: String?, existing: String?): String? = when {
    incoming == null -> existing
    incoming.isBlank() -> null
    else -> incoming
}

// --- top-level helpers ---

private fun parseTextId(id: String): Either<DomainError, TextId> =
    try { TextId(UUID.fromString(id)).right() }
    catch (_: IllegalArgumentException) { DomainError.InvalidInput("'$id' is not a valid UUID").left() }

private fun <T : Enum<T>> parseTextEnum(
    field: String,
    value: String,
    parser: (String) -> T?,
): Either<DomainError, T> =
    parser(value)?.right() ?: DomainError.ValidationError(field, "Unknown value: $value").left()
