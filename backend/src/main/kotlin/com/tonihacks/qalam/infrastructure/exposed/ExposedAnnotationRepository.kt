package com.tonihacks.qalam.infrastructure.exposed

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tonihacks.qalam.domain.annotation.Annotation
import com.tonihacks.qalam.domain.annotation.AnnotationId
import com.tonihacks.qalam.domain.annotation.AnnotationRepository
import com.tonihacks.qalam.domain.annotation.AnnotationType
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.text.TextId
import com.tonihacks.qalam.domain.word.MasteryLevel
import com.tonihacks.qalam.domain.word.WordId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.postgresql.util.PSQLState
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid as KotlinUUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class ExposedAnnotationRepository : AnnotationRepository {

    override suspend fun findAllByTextId(textId: TextId): Either<DomainError, List<Annotation>> =
        suspendTransaction {
            val rows = AnnotationsTable
                .selectAll()
                .where { AnnotationsTable.textId eq textId.value.toKotlinUuid() }
                .toList()

            if (rows.isEmpty()) return@suspendTransaction emptyList<Annotation>().right()

            val annotationIds = rows.map { it[AnnotationsTable.id] }
            val wordIdsByAnnotationId = loadWordIdsByAnnotationId(annotationIds)

            rows.map { row ->
                val aid = row[AnnotationsTable.id]
                row.toAnnotation(wordIdsByAnnotationId[aid] ?: emptyList())
            }.right()
        }

    override suspend fun findById(id: AnnotationId): Either<DomainError, Annotation> =
        suspendTransaction {
            val row = AnnotationsTable
                .selectAll()
                .where { AnnotationsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?: return@suspendTransaction DomainError.NotFound("Annotation", id.toString()).left()

            val wordIds = loadWordIdsByAnnotationId(listOf(row[AnnotationsTable.id]))[row[AnnotationsTable.id]]
                ?: emptyList()
            row.toAnnotation(wordIds).right()
        }

    override suspend fun save(annotation: Annotation): Either<DomainError, Annotation> =
        try {
            suspendTransaction {
                AnnotationsTable.insert {
                    it[id] = annotation.id.value.toKotlinUuid()
                    it[textId] = annotation.textId.value.toKotlinUuid()
                    it[anchor] = annotation.anchor
                    it[type] = annotation.type.name
                    it[content] = annotation.content
                    it[masteryLevel] = annotation.masteryLevel?.name
                    it[reviewFlag] = annotation.reviewFlag
                    it[createdAt] = annotation.createdAt
                    it[updatedAt] = annotation.updatedAt
                }
                insertWordLinks(annotation.id, annotation.linkedWordIds)
                annotation.right()
            }
        } catch (e: ExposedSQLException) {
            when (e.sqlState) {
                PSQLState.FOREIGN_KEY_VIOLATION.state -> DomainError.NotFound("Text", annotation.textId.toString()).left()
                PSQLState.UNIQUE_VIOLATION.state -> DomainError.Conflict("Annotation", annotation.id.toString()).left()
                else -> throw e
            }
        }

    override suspend fun update(annotation: Annotation): Either<DomainError, Annotation> =
        suspendTransaction {
            either {
                val updatedCount = AnnotationsTable.update({ AnnotationsTable.id eq annotation.id.value.toKotlinUuid() }) {
                    it[anchor] = annotation.anchor
                    it[type] = annotation.type.name
                    it[content] = annotation.content
                    it[masteryLevel] = annotation.masteryLevel?.name
                    it[reviewFlag] = annotation.reviewFlag
                    it[updatedAt] = annotation.updatedAt
                }
                ensure(updatedCount > 0) { DomainError.NotFound("Annotation", annotation.id.toString()) }

                val refreshed = AnnotationsTable
                    .selectAll()
                    .where { AnnotationsTable.id eq annotation.id.value.toKotlinUuid() }
                    .singleOrNull()
                ensureNotNull(refreshed) { DomainError.NotFound("Annotation", annotation.id.toString()) }

                val wordIds = loadWordIdsByAnnotationId(listOf(refreshed[AnnotationsTable.id]))[refreshed[AnnotationsTable.id]]
                    ?: emptyList()
                refreshed.toAnnotation(wordIds)
            }
        }

    override suspend fun delete(id: AnnotationId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = AnnotationsTable.deleteWhere { AnnotationsTable.id eq id.value.toKotlinUuid() }
                ensure(deleteCount > 0) { DomainError.NotFound("Annotation", id.toString()) }
            }
        }

    override suspend fun addWordLink(annotationId: AnnotationId, wordId: WordId): Either<DomainError, Annotation> =
        try {
            suspendTransaction {
                either {
                    val row = AnnotationsTable
                        .selectAll()
                        .where { AnnotationsTable.id eq annotationId.value.toKotlinUuid() }
                        .singleOrNull()
                    ensureNotNull(row) { DomainError.NotFound("Annotation", annotationId.toString()) }

                    // Insert if not already present (ignore duplicate)
                    val existing = AnnotationWordsTable
                        .selectAll()
                        .where {
                            (AnnotationWordsTable.annotationId eq annotationId.value.toKotlinUuid())
                                .and(AnnotationWordsTable.wordId eq wordId.value.toKotlinUuid())
                        }
                        .singleOrNull()
                    if (existing == null) {
                        AnnotationWordsTable.insert {
                            it[AnnotationWordsTable.annotationId] = annotationId.value.toKotlinUuid()
                            it[AnnotationWordsTable.wordId] = wordId.value.toKotlinUuid()
                        }
                    }

                    val wordIds = loadWordIdsByAnnotationId(listOf(row[AnnotationsTable.id]))[row[AnnotationsTable.id]]
                        ?: emptyList()
                    // Ensure the newly added word is in the list
                    val allWordIds = (wordIds + wordId).distinctBy { it.value }
                    row.toAnnotation(allWordIds)
                }
            }
        } catch (e: ExposedSQLException) {
            if (e.sqlState == PSQLState.FOREIGN_KEY_VIOLATION.state) {
                DomainError.NotFound("Word", wordId.toString()).left()
            } else {
                throw e
            }
        }

    override suspend fun removeWordLink(annotationId: AnnotationId, wordId: WordId): Either<DomainError, Annotation> =
        suspendTransaction {
            either {
                val row = AnnotationsTable
                    .selectAll()
                    .where { AnnotationsTable.id eq annotationId.value.toKotlinUuid() }
                    .singleOrNull()
                ensureNotNull(row) { DomainError.NotFound("Annotation", annotationId.toString()) }

                AnnotationWordsTable.deleteWhere {
                    (AnnotationWordsTable.annotationId eq annotationId.value.toKotlinUuid())
                        .and(AnnotationWordsTable.wordId eq wordId.value.toKotlinUuid())
                }

                val wordIds = loadWordIdsByAnnotationId(listOf(row[AnnotationsTable.id]))[row[AnnotationsTable.id]]
                    ?: emptyList()
                row.toAnnotation(wordIds)
            }
        }

    override suspend fun findAllByWordId(wordId: WordId): Either<DomainError, List<Annotation>> =
        suspendTransaction {
            val annotationKotlinIds = AnnotationWordsTable
                .selectAll()
                .where { AnnotationWordsTable.wordId eq wordId.value.toKotlinUuid() }
                .map { it[AnnotationWordsTable.annotationId] }

            if (annotationKotlinIds.isEmpty()) return@suspendTransaction emptyList<Annotation>().right()

            val rows = AnnotationsTable
                .selectAll()
                .where { AnnotationsTable.id inList annotationKotlinIds }
                .toList()

            val wordIdsByAnnotationId = loadWordIdsByAnnotationId(annotationKotlinIds)

            rows.map { row ->
                val aid = row[AnnotationsTable.id]
                row.toAnnotation(wordIdsByAnnotationId[aid] ?: emptyList())
            }.right()
        }

    // --- private helpers ---

    private fun loadWordIdsByAnnotationId(annotationKotlinIds: List<KotlinUUID>): Map<KotlinUUID, List<WordId>> {
        if (annotationKotlinIds.isEmpty()) return emptyMap()
        return AnnotationWordsTable
            .selectAll()
            .where { AnnotationWordsTable.annotationId inList annotationKotlinIds }
            .groupBy(
                { it[AnnotationWordsTable.annotationId] },
                { WordId(it[AnnotationWordsTable.wordId].toJavaUuid()) },
            )
    }

    private fun insertWordLinks(annotationId: AnnotationId, wordIds: List<WordId>) {
        wordIds.forEach { wordId ->
            AnnotationWordsTable.insert {
                it[AnnotationWordsTable.annotationId] = annotationId.value.toKotlinUuid()
                it[AnnotationWordsTable.wordId] = wordId.value.toKotlinUuid()
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
private fun ResultRow.toAnnotation(wordIds: List<WordId>) = Annotation(
    id = AnnotationId(this[AnnotationsTable.id].toJavaUuid()),
    textId = TextId(this[AnnotationsTable.textId].toJavaUuid()),
    anchor = this[AnnotationsTable.anchor],
    type = AnnotationType.valueOf(this[AnnotationsTable.type]),
    content = this[AnnotationsTable.content],
    masteryLevel = this[AnnotationsTable.masteryLevel]?.let { MasteryLevel.valueOf(it) },
    reviewFlag = this[AnnotationsTable.reviewFlag],
    linkedWordIds = wordIds,
    createdAt = this[AnnotationsTable.createdAt],
    updatedAt = this[AnnotationsTable.updatedAt],
)
