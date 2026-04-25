package com.tonihacks.qalam.domain.text

import arrow.core.Either
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.word.Dialect
import com.tonihacks.qalam.domain.word.Difficulty

enum class TextSortField { CREATED_AT, UPDATED_AT, TITLE }

data class TextFilters(
    val q: String? = null,
    val dialect: Dialect? = null,
    val difficulty: Difficulty? = null,
    val tag: String? = null,
    val sortBy: TextSortField = TextSortField.CREATED_AT,
    val sortDesc: Boolean = true,
)

interface TextRepository {
    suspend fun findById(id: TextId): Either<DomainError, Text>
    suspend fun list(page: PageRequest, filters: TextFilters): Either<DomainError, PaginatedResponse<Text>>
    suspend fun save(text: Text): Either<DomainError, Text>
    suspend fun update(text: Text): Either<DomainError, Text>
    suspend fun delete(id: TextId): Either<DomainError, Unit>
}
