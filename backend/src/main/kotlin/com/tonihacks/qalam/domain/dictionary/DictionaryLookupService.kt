package com.tonihacks.qalam.domain.dictionary

import arrow.core.Either
import arrow.core.raise.either
import com.tonihacks.qalam.delivery.dto.dictionary.DictionaryLookupResponse
import com.tonihacks.qalam.domain.error.DomainError

class DictionaryLookupService(
    private val asdClient: DictionaryLookupClient
) {
    suspend fun search(source: String?, query: String?): Either<DomainError, DictionaryLookupResponse> = either {
        val normalizedQuery = query?.trim().orEmpty()
        if (normalizedQuery.isBlank()) {
            raise(DomainError.ValidationError("query", "query is required for lookup"))
        }

        when (source?.trim()?.uppercase()) {
            Dictionary.ASD.name -> asdClient.search(normalizedQuery).bind()
            null, "" -> raise(DomainError.ValidationError("source", "needed source for lookup"))
            else -> raise(DomainError.ValidationError("source", "source not $source implemented yet"))
        }
    }
}
