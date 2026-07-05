package com.tonihacks.qalam.domain.dictionary

import arrow.core.Either
import com.tonihacks.qalam.delivery.dto.dictionary.DictionaryLookupResponse
import com.tonihacks.qalam.domain.error.DomainError


interface DictionaryLookupClient {
    suspend fun search(query: String): Either<DomainError, DictionaryLookupResponse>
}
