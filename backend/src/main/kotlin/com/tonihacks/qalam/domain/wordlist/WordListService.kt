package com.tonihacks.qalam.domain.wordlist

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.wordlist.AddWordToListRequest
import com.tonihacks.qalam.delivery.dto.wordlist.CreateWordListRequest
import com.tonihacks.qalam.delivery.dto.wordlist.UpdateWordListRequest
import com.tonihacks.qalam.delivery.dto.wordlist.WordListDetailResponse
import com.tonihacks.qalam.delivery.dto.wordlist.WordListRefResponse
import com.tonihacks.qalam.delivery.dto.wordlist.WordListResponse
import com.tonihacks.qalam.delivery.dto.wordlist.WordListSuggestionsResponse
import com.tonihacks.qalam.delivery.dto.wordlist.toDetailResponse
import com.tonihacks.qalam.delivery.dto.wordlist.toRefResponse
import com.tonihacks.qalam.delivery.dto.wordlist.toResponse
import com.tonihacks.qalam.domain.logDomainFailure
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.WordId
import com.tonihacks.qalam.infrastructure.ai.AiClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.Clock

class WordListService(
    private val repo: WordListRepository,
    private val aiClient: AiClient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun list(page: Int?, size: Int?): Either<DomainError, PaginatedResponse<WordListResponse>> =
        repo.list(PageRequest.from(page, size)).map { p ->
            PaginatedResponse(
                items = p.items.map { it.toResponse() },
                total = p.total,
                page = p.page,
                size = p.size,
            )
        }.logDomainFailure(log) { "Failed to list word lists page=$page size=$size: $it" }

    suspend fun getById(id: String): Either<DomainError, WordListDetailResponse> = either {
        log.debug { "Loading word list id=$id" }
        val listId = parseId(id).bind()
        val list = repo.findById(listId).bind()
        val words = repo.membersOf(listId).bind()
        list.toDetailResponse(words)
    }.logDomainFailure(log) { "Failed to load word list id=$id: $it" }

    suspend fun create(req: CreateWordListRequest): Either<DomainError, WordListResponse> = either {
        log.info { "Creating word list titleLength=${req.title.length}" }
        val title = req.title.trim()
        ensure(title.isNotEmpty()) {
            DomainError.ValidationError("title", "title must not be blank")
        }
        val now = Clock.System.now()
        val list = WordList(
            id = WordListId(UUID.randomUUID()),
            title = title,
            description = req.description?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
        )
        // A freshly created list has no members yet.
        WordListSummary(repo.create(list).bind(), itemCount = 0).toResponse()
    }.logDomainFailure(log) { "Failed to create word list titleLength=${req.title.length}: $it" }

    suspend fun update(id: String, req: UpdateWordListRequest): Either<DomainError, WordListResponse> = either {
        log.info { "Updating word list id=$id" }
        val listId = parseId(id).bind()
        val existing = repo.findById(listId).bind()

        val newTitle = req.title?.trim()
        ensure(newTitle == null || newTitle.isNotEmpty()) {
            DomainError.ValidationError("title", "title must not be blank")
        }

        val updated = repo.update(
            existing.copy(
                title = newTitle ?: existing.title,
                description = req.description?.trim()?.takeIf { it.isNotEmpty() } ?: existing.description,
            ),
        ).bind()
        val count = repo.membersOf(listId).bind().size.toLong()
        WordListSummary(updated, count).toResponse()
    }.logDomainFailure(log) { "Failed to update word list id=$id: $it" }

    suspend fun delete(id: String): Either<DomainError, Unit> =
        parseId(id).flatMap { repo.delete(it) }
            .logDomainFailure(log) { "Failed to delete word list id=$id: $it" }

    suspend fun addWord(listId: String, req: AddWordToListRequest): Either<DomainError, Unit> = either {
        log.info { "Adding word to list listId=$listId wordId=${req.wordId}" }
        val list = parseId(listId).bind()
        val word = parseWordId(req.wordId).bind()
        repo.addWord(list, word).bind()
    }.logDomainFailure(log) { "Failed to add word to list listId=$listId wordId=${req.wordId}: $it" }

    suspend fun removeWord(listId: String, wordId: String): Either<DomainError, Unit> = either {
        log.info { "Removing word from list listId=$listId wordId=$wordId" }
        val list = parseId(listId).bind()
        val word = parseWordId(wordId).bind()
        repo.removeWord(list, word).bind()
    }.logDomainFailure(log) { "Failed to remove word from list listId=$listId wordId=$wordId: $it" }

    suspend fun listsForWord(wordId: String): Either<DomainError, List<WordListRefResponse>> = either {
        log.debug { "Listing word lists for wordId=$wordId" }
        val word = parseWordId(wordId).bind()
        repo.listsForWord(word).bind().map { it.toRefResponse() }
    }.logDomainFailure(log) { "Failed to list word lists for wordId=$wordId: $it" }

    // --- AI word suggestions (ephemeral preview — never auto-saved) ---

    suspend fun suggestWords(id: String): Either<DomainError, WordListSuggestionsResponse> = either {
        log.info { "Generating AI word-list suggestions listId=$id" }
        val listId = parseId(id).bind()
        val list = repo.findById(listId).bind()
        val existing = repo.membersOf(listId).bind()
        val suggestions = aiClient.suggestWordsForList(list.title, list.description, existing).bind()
        WordListSuggestionsResponse(suggestions)
    }.logDomainFailure(log) { "Failed to generate AI word-list suggestions listId=$id: $it" }

    private fun parseId(id: String): Either<DomainError, WordListId> =
        try {
            WordListId(UUID.fromString(id)).right()
        } catch (_: IllegalArgumentException) {
            DomainError.InvalidInput("'$id' is not a valid UUID").left()
        }

    private fun parseWordId(id: String): Either<DomainError, WordId> =
        try {
            WordId(UUID.fromString(id)).right()
        } catch (_: IllegalArgumentException) {
            DomainError.InvalidInput("'$id' is not a valid UUID").left()
        }
}
