package com.tonihacks.qalam.delivery

import com.tonihacks.qalam.delivery.dto.ErrorResponse
import com.tonihacks.qalam.domain.error.DomainError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

// Delivery-layer concern: maps domain errors → HTTP status + ErrorResponse body.
// Kept here (not in domain) because HttpStatusCode is a Ktor type.

fun DomainError.toHttpResponse(): Pair<HttpStatusCode, ErrorResponse> = when (this) {
    is DomainError.NotFound ->
        HttpStatusCode.NotFound to ErrorResponse("$resourceType with id '$id' not found", "NOT_FOUND")
    is DomainError.Conflict ->
        HttpStatusCode.Conflict to ErrorResponse("$resourceType with id '$id' is available multiple times", "CONFLICT")
    is DomainError.AlreadyExists ->
        HttpStatusCode.Conflict to ErrorResponse(detail, "ALREADY_EXISTS")
    is DomainError.ValidationError ->
        HttpStatusCode.UnprocessableEntity to ErrorResponse("$field: $message", "VALIDATION_ERROR")
    is DomainError.InvalidInput ->
        HttpStatusCode.BadRequest to ErrorResponse(message, "INVALID_INPUT")
    is DomainError.AiNotConfigured ->
        HttpStatusCode.ServiceUnavailable to ErrorResponse("AI service not configured", "AI_NOT_CONFIGURED")
    is DomainError.DatabaseError ->
        HttpStatusCode.InternalServerError to ErrorResponse("A database error occurred", "DATABASE_ERROR")
}

/** Convenience extension for route handlers using Either.fold { error -> call.respondError(it) }. */
suspend fun ApplicationCall.respondError(error: DomainError) {
    val (status, body) = error.toHttpResponse()
    respond(status, body)
}
