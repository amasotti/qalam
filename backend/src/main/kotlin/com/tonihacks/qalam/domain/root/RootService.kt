package com.tonihacks.qalam.domain.root

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.root.CreateRootRequest
import com.tonihacks.qalam.delivery.dto.root.NormalizeRequest
import com.tonihacks.qalam.delivery.dto.root.NormalizeResponse
import com.tonihacks.qalam.delivery.dto.root.RootResponse
import com.tonihacks.qalam.delivery.dto.root.UpdateRootRequest
import com.tonihacks.qalam.delivery.dto.root.toResponse
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.Clock

class RootService(private val repo: RootRepository) {
    private val log = KotlinLogging.logger {}

    suspend fun list(page: Int?, size: Int?, letterCount: Int?, sortBy: String?, sortDesc: Boolean?): Either<DomainError, PaginatedResponse<RootResponse>> {
        val parsedSortBy = when (sortBy?.uppercase()) {
            "CREATED_AT" -> RootSortField.CREATED_AT
            "NORMALIZED_FORM" -> RootSortField.NORMALIZED_FORM
            "LETTER_COUNT" -> RootSortField.LETTER_COUNT
            else -> RootSortField.UPDATED_AT
        }
        val filters = RootFilters(letterCount = letterCount, sortBy = parsedSortBy, sortDesc = sortDesc ?: true)
        return repo.list(PageRequest.from(page, size), filters)
            .map { p ->
                PaginatedResponse(
                    items = p.items.map { it.toResponse() },
                    total = p.total,
                    page = p.page,
                    size = p.size,
                )
            }.logDomainFailure(log) { "Failed to list roots page=$page size=$size filters=$filters: $it" }
    }

    suspend fun getById(id: String): Either<DomainError, RootResponse> =
        parseId(id)
            .flatMap { repo.findById(it) }
            .map { it.toResponse() }
            .logDomainFailure(log) { "Failed to get root id=$id: $it" }

    suspend fun create(req: CreateRootRequest): Either<DomainError, RootResponse> = either {
        log.info { "Creating root inputLength=${req.root.length}" }

        val normalized = RootNormalizer.normalize(req.root).bind()
        val rootExistsAlready = repo.existsByNormalizedForm(normalized.normalizedForm).bind()

        ensure(rootExistsAlready.not()) {
            DomainError.AlreadyExists("ArabicRoot", "root '${normalized.displayForm}' already exists")
        }

        // The DB also has triggers for updated_at and created_at but those are fallback
        // Important is when - from the perspective of the application - a root was created
        // which is why it is good to have it here in the service.
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
    }.logDomainFailure(log) { "Failed to create root inputLength=${req.root.length}: $it" }

    suspend fun update(id: String, req: UpdateRootRequest): Either<DomainError, RootResponse> =
        parseId(id)
            .flatMap { repo.findById(it) }
            .flatMap { existing ->
                log.info { "Updating root id=$id" }
                repo.update(existing.copy(meaning = req.meaning, analysis = req.analysis))
            }
            .map { it.toResponse() }
            .logDomainFailure(log) { "Failed to update root id=$id: $it" }

    suspend fun delete(id: String): Either<DomainError, Unit> =
        parseId(id).flatMap { repo.delete(it) }
            .logDomainFailure(log) { "Failed to delete root id=$id: $it" }

    fun normalize(req: NormalizeRequest): Either<DomainError, NormalizeResponse> =
        RootNormalizer.normalize(req.input)
            .map { NormalizeResponse(it.letters, it.normalizedForm, it.displayForm, it.letterCount) }
            .logDomainFailure(log) { "Failed to normalize root inputLength=${req.input.length}: $it" }

    private fun parseId(id: String): Either<DomainError, RootId> =
        try {
            RootId(UUID.fromString(id)).right()
        } catch (_: IllegalArgumentException) {
            DomainError.InvalidInput("'$id' is not a valid UUID").left()
        }

}
