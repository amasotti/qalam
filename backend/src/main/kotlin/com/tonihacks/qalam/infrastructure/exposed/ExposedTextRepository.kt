package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.Text
import com.tonihacks.qalam.domain.text.TextFilters
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.text.TextRepository
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedTextRepository : TextRepository {

    override suspend fun findById(id: TextId): Either<DomainError, Text> =
        suspendTransaction {
            val row = TextsTable
                .selectAll()
                .where { TextsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?: return@suspendTransaction DomainError.NotFound("Text", id.toString()).left()

            val tags = loadTags(id)
            row.toText(tags).right()
        }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun list(page: PageRequest, filters: TextFilters): Either<DomainError, PaginatedResponse<Text>> =
        suspendTransaction {
            // When filtering by tag we join text_tags; otherwise query texts directly.
            val tagFilteredIds: Set<kotlin.uuid.Uuid>? = filters.tag?.let { tag ->
                TextTagsTable
                    .selectAll()
                    .where { TextTagsTable.tag eq tag }
                    .map { it[TextTagsTable.textId] }
                    .toSet()
                    .takeIf { it.isNotEmpty() }
                    ?: emptySet<kotlin.uuid.Uuid>().also {
                        // Return early with empty result — no texts have this tag.
                        return@suspendTransaction PaginatedResponse<Text>(
                            items = emptyList(),
                            total = 0L,
                            page = page.page,
                            size = page.size,
                        ).right()
                    }
            }

            val query = TextsTable.selectAll().let { q ->
                var condition: Op<Boolean>? = null

                filters.dialect?.let { d ->
                    condition = condition?.and(TextsTable.dialect eq d.name) ?: (TextsTable.dialect eq d.name)
                }
                filters.difficulty?.let { d ->
                    condition = condition?.and(TextsTable.difficulty eq d.name) ?: (TextsTable.difficulty eq d.name)
                }
                filters.q?.let { queryStr ->
                    val qCond = (TextsTable.title like "%$queryStr%") or (TextsTable.body like "%$queryStr%")
                    condition = condition?.and(qCond) ?: qCond
                }
                tagFilteredIds?.let { ids ->
                    val tagCond = TextsTable.id inList ids
                    condition = condition?.and(tagCond) ?: tagCond
                }

                condition?.let { q.where { it } } ?: q
            }

            val total = query.count()
            val rows = query
                .limit(page.size)
                .offset(page.offset)
                .toList()

            // Batch-load tags for all texts in this page in one query.
            val pageIds = rows.map { it[TextsTable.id] }
            val tagsByTextId = if (pageIds.isEmpty()) {
                emptyMap()
            } else {
                TextTagsTable
                    .selectAll()
                    .where { TextTagsTable.textId inList pageIds }
                    .groupBy(
                        { it[TextTagsTable.textId] },
                        { it[TextTagsTable.tag] },
                    )
            }

            val items = rows.map { row ->
                val tags = tagsByTextId[row[TextsTable.id]] ?: emptyList()
                row.toText(tags)
            }

            PaginatedResponse(items = items, total = total, page = page.page, size = page.size).right()
        }

    override suspend fun save(text: Text): Either<DomainError, Text> =
        suspendTransaction {
            TextsTable.insert {
                it[id] = text.id.value.toKotlinUuid()
                it[title] = text.title
                it[body] = text.body
                it[transliteration] = text.transliteration
                it[translation] = text.translation
                it[difficulty] = text.difficulty.name
                it[dialect] = text.dialect.name
                it[comments] = text.comments
                it[createdAt] = text.createdAt
                it[updatedAt] = text.updatedAt
            }
            insertTags(text.id, text.tags)
            text.right()
        }

    override suspend fun update(text: Text): Either<DomainError, Text> =
        suspendTransaction {
            either {
                val updatedCount = TextsTable.update({ TextsTable.id eq text.id.value.toKotlinUuid() }) {
                    it[title] = text.title
                    it[body] = text.body
                    it[transliteration] = text.transliteration
                    it[translation] = text.translation
                    it[difficulty] = text.difficulty.name
                    it[dialect] = text.dialect.name
                    it[comments] = text.comments
                    it[updatedAt] = text.updatedAt
                }

                ensure(updatedCount > 0) { DomainError.NotFound("Text", text.id.toString()) }

                // Replace tags: delete all, re-insert.
                TextTagsTable.deleteWhere { TextTagsTable.textId eq text.id.value.toKotlinUuid() }
                insertTags(text.id, text.tags)

                val refreshed = TextsTable
                    .selectAll()
                    .where { TextsTable.id eq text.id.value.toKotlinUuid() }
                    .singleOrNull()
                ensureNotNull(refreshed) { DomainError.NotFound("Text", text.id.toString()) }

                val tags = loadTags(text.id)
                refreshed.toText(tags)
            }
        }

    override suspend fun delete(id: TextId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = TextsTable.deleteWhere { TextsTable.id eq id.value.toKotlinUuid() }
                ensure(deleteCount > 0) { DomainError.NotFound("Text", id.toString()) }
            }
        }

    // --- private helpers ---

    private fun loadTags(id: TextId): List<String> =
        TextTagsTable
            .selectAll()
            .where { TextTagsTable.textId eq id.value.toKotlinUuid() }
            .map { it[TextTagsTable.tag] }

    private fun insertTags(id: TextId, tags: List<String>) {
        tags.forEach { tag ->
            TextTagsTable.insert {
                it[textId] = id.value.toKotlinUuid()
                it[TextTagsTable.tag] = tag
            }
        }
    }
}

private fun ResultRow.toText(tags: List<String>) = Text(
    id = TextId(this[TextsTable.id].toJavaUuid()),
    title = this[TextsTable.title],
    body = this[TextsTable.body],
    transliteration = this[TextsTable.transliteration],
    translation = this[TextsTable.translation],
    difficulty = Difficulty.fromString(this[TextsTable.difficulty]) ?: Difficulty.BEGINNER,
    dialect = Dialect.fromString(this[TextsTable.dialect]) ?: Dialect.MSA,
    comments = this[TextsTable.comments],
    tags = tags,
    createdAt = this[TextsTable.createdAt],
    updatedAt = this[TextsTable.updatedAt],
)
