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
import com.tonihacks.qalam.domain.root.ArabicRoot
import com.tonihacks.qalam.domain.root.RootId
import com.tonihacks.qalam.domain.root.RootRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedRootRepository : RootRepository {

    override suspend fun findById(id: RootId): Either<DomainError, ArabicRoot> =
        suspendTransaction {
            RootsTable
                .selectAll()
                .where { RootsTable.id eq id.value.toKotlinUuid() }
                .singleOrNull()
                ?.toArabicRoot()
                ?.right()
                ?: DomainError.NotFound("ArabicRoot", id.toString()).left()
        }

    override suspend fun findByNormalizedForm(form: String): Either<DomainError, ArabicRoot?> =
        suspendTransaction {
            RootsTable
                .selectAll()
                .where { RootsTable.normalizedForm eq form }
                .singleOrNull()
                ?.toArabicRoot()
                .right()
        }

    override suspend fun existsByNormalizedForm(form: String): Either<DomainError, Boolean> =
        suspendTransaction {
            RootsTable
                .selectAll()
                .where { RootsTable.normalizedForm eq form }
                .empty()
                .not()
                .right()
        }

    override suspend fun list(page: PageRequest, letterCount: Int?): Either<DomainError, PaginatedResponse<ArabicRoot>> =
        suspendTransaction {
            val query = RootsTable.selectAll().let { q ->
                if (letterCount != null) q.where { RootsTable.letterCount eq letterCount.toShort() } else q
            }

            val total = query.count()
            val items = query
                .limit(page.size)
                .offset(page.offset)
                .map { it.toArabicRoot() }

            PaginatedResponse(
                items = items,
                total = total,
                page = page.page,
                size = page.size,
            ).right()
        }

    override suspend fun create(root: ArabicRoot): Either<DomainError, ArabicRoot> =
        suspendTransaction {
            RootsTable.insert {
                it[id] = root.id.value.toKotlinUuid()
                it[letters] = root.letters
                it[normalizedForm] = root.normalizedForm
                it[displayForm] = root.displayForm
                it[letterCount] = root.letterCount.toShort()
                it[meaning] = root.meaning
                it[analysis] = root.analysis
            }
            root.right()
        }

    override suspend fun update(root: ArabicRoot): Either<DomainError, ArabicRoot> =
        suspendTransaction {
            either {
                val rootId = root.id.value
                val updatedCount = RootsTable.update({ RootsTable.id eq rootId.toKotlinUuid()}) {
                    it[meaning] = root.meaning
                    it[analysis] = root.analysis
                }

                ensure (updatedCount > 0) {
                    DomainError.NotFound("ArabicRoot", rootId.toString())
                }

                RootsTable.selectAll()
                    .where { RootsTable.id eq rootId.toKotlinUuid() }
                    .singleOrNull()
                    ?.toArabicRoot()
                    .let { refreshed ->
                        ensureNotNull(refreshed) {
                            DomainError.NotFound("ArabicRoot", rootId.toString())
                        }
                    }
            }
        }

    override suspend fun delete(id: RootId): Either<DomainError, Unit> =
        suspendTransaction {
            either {
                val deleteCount = RootsTable.deleteWhere { RootsTable.id eq id.value.toKotlinUuid() }

                ensure (deleteCount > 0) {
                    DomainError.NotFound("ArabicRoot", id.toString())
                }

                Unit.right()
            }
        }

}

private fun ResultRow.toArabicRoot() = ArabicRoot(
    id = RootId(this[RootsTable.id].toJavaUuid()),
    letters = this[RootsTable.letters],
    normalizedForm = this[RootsTable.normalizedForm],
    displayForm = this[RootsTable.displayForm],
    letterCount = this[RootsTable.letterCount].toInt(),
    meaning = this[RootsTable.meaning],
    analysis = this[RootsTable.analysis],
    createdAt = this[RootsTable.createdAt],
    updatedAt = this[RootsTable.updatedAt],
)


