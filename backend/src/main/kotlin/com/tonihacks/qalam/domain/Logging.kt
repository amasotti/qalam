package com.tonihacks.qalam.domain

import arrow.core.Either
import com.tonihacks.qalam.domain.error.DomainError
import io.github.oshai.kotlinlogging.KLogger

internal fun <A> Either<DomainError, A>.logDomainFailure(
    log: KLogger,
    message: (DomainError) -> String,
): Either<DomainError, A> {
    if (this is Either.Left) {
        log.warn { message(value) }
    }
    return this
}
