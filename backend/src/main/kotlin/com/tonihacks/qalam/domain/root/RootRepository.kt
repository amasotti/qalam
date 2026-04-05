package com.tonihacks.qalam.domain.root

import arrow.core.Either
import com.tonihacks.qalam.delivery.dto.PageRequest
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.domain.error.DomainError

interface RootRepository {
    suspend fun findById(id: RootId): Either<DomainError, ArabicRoot>
    suspend fun findByNormalizedForm(form: String): Either<DomainError, ArabicRoot?>
    suspend fun existsByNormalizedForm(form: String): Either<DomainError, Boolean>
    suspend fun list(page: PageRequest, letterCount: Int?): Either<DomainError, PaginatedResponse<ArabicRoot>>
    suspend fun create(root: ArabicRoot): Either<DomainError, ArabicRoot>
    suspend fun update(root: ArabicRoot): Either<DomainError, ArabicRoot>
    suspend fun delete(id: RootId): Either<DomainError, Unit>
}
