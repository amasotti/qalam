package com.tonihacks.qalam.domain.error

// Domain layer — zero framework dependencies.
// HTTP mapping lives in delivery/ErrorMapping.kt as an extension function.
sealed class DomainError {

    /** Resource not found by ID. */
    data class NotFound(val resourceType: String, val id: String) : DomainError()

    /** Attempt to create a resource that already exists. */
    data class AlreadyExists(val resourceType: String, val detail: String) : DomainError()

    /** Field-level input validation failure. */
    data class ValidationError(val field: String, val message: String) : DomainError()

    /** Generic bad input that doesn't map to a single field. */
    data class InvalidInput(val message: String) : DomainError()

    /** AI endpoint called but OPENROUTER_API_KEY is not configured. */
    data object AiNotConfigured : DomainError()

    /** Unexpected database-layer failure. */
    data object DatabaseError : DomainError()
}
