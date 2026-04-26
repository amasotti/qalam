package com.tonihacks.qalam.delivery.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
)

/**
 * Parsed + validated page parameters extracted from query string.
 *
 * Usage in a route:
 *   val req = PageRequest.from(call.parameters["page"]?.toIntOrNull(), call.parameters["size"]?.toIntOrNull())
 *   val result = service.list(req)
 */
data class PageRequest(
    val page: Int,
    val size: Int,
) {
    val offset: Long get() = (page - 1).toLong() * size

    companion object {
        private const val DEFAULT_SIZE = 20
        private const val MAX_SIZE = 500

        fun from(page: Int?, size: Int?): PageRequest = PageRequest(
            page = (page ?: 1).coerceAtLeast(1),
            size = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE),
        )
    }
}
