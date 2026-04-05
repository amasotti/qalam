package com.tonihacks.qalam.domain.root

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.root.CreateRootRequest
import com.tonihacks.qalam.delivery.dto.root.NormalizeRequest
import com.tonihacks.qalam.delivery.dto.root.NormalizeResponse
import com.tonihacks.qalam.delivery.dto.root.RootResponse
import com.tonihacks.qalam.delivery.dto.root.UpdateRootRequest
import com.tonihacks.qalam.delivery.dto.root.toResponse
import com.tonihacks.qalam.domain.error.DomainError
import java.util.UUID
import kotlin.time.Clock

class RootService(private val repo: RootRepository) {

    suspend fun list(page: Int?,size: Int?,letterCount: Int?): Either<DomainError, PaginatedResponse<RootResponse>> =
        repo.list(PageRequest.from(page, size), letterCount)
            .map { p -> PaginatedResponse(
                items = p.items.map { it.toResponse() },
                total = p.total,
                page = p.page,
                size = p.size,
            )
        }

    suspend fun getById(id: String): Either<DomainError, RootResponse> =
        parseId(id)
            .flatMap { repo.findById(it) }
            .map { it.toResponse() }

    suspend fun create(req: CreateRootRequest): Either<DomainError, RootResponse> = either {

        // Unwrap the normalized form and existing root checks in a single transaction-like block
        val normalized = RootNormalizer.normalize(req.letters.joinToString(" ")).bind()
        repo.ensureUnique(normalized.normalizedForm).bind()

        val now = Clock.System.now()
        val root = ArabicRoot(
            id = RootId(UUID.randomUUID()),
            letters = normalized.letters,
            normalizedForm = normalized.normalizedForm,
            displayForm = normalized.displayForm,
            letterCount = normalized.letterCount,
            meaning = req.meaning,
            analysis = req.analysis,
            createdAt = now,
            updatedAt = now,
        )
        repo.create(root).bind().toResponse()
    }

    suspend fun update(id: String, req: UpdateRootRequest): Either<DomainError, RootResponse> =
        parseId(id)
            .flatMap { repo.findById(it) }
            .flatMap { existing ->
                repo.update(
                    existing.copy(
                        meaning = req.meaning,
                        analysis = req.analysis,
                        updatedAt = Clock.System.now(),
                    )
                )
            }
            .map { it.toResponse() }

    suspend fun delete(id: String): Either<DomainError, Unit> =
        parseId(id).flatMap { repo.delete(it) }

    fun normalize(req: NormalizeRequest): Either<DomainError, NormalizeResponse> =
        RootNormalizer.normalize(req.input)
            .map { NormalizeResponse(it.letters, it.normalizedForm, it.displayForm, it.letterCount) }

    private fun parseId(id: String): Either<DomainError, RootId> =
        try {
            RootId(UUID.fromString(id)).right()
        } catch (_: IllegalArgumentException) {
            DomainError.InvalidInput("'$id' is not a valid UUID").left()
        }

    private fun Raise<DomainError>.ensureUnique(root: NormalizedRoot, existing: Any?) {
        ensure(existing == null) {
            DomainError.AlreadyExists("ArabicRoot", "Root '${root.displayForm}' already exists")
        }
    }
}
