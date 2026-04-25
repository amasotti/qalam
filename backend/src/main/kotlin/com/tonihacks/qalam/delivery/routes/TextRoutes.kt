package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.PaginatedResponse
import com.tonihacks.qalam.delivery.dto.text.CreateTextRequest
import com.tonihacks.qalam.delivery.dto.text.UpdateTextRequest
import com.tonihacks.qalam.delivery.dto.text.toResponse
import com.tonihacks.qalam.domain.text.TextService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

@Suppress("LongMethod")
fun Route.textRoutes(service: TextService) {
    route("/texts") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val q = call.request.queryParameters["q"]
            val dialect = call.request.queryParameters["dialect"]
            val difficulty = call.request.queryParameters["difficulty"]
            val tag = call.request.queryParameters["tag"]

            service.list(page, size, q, dialect, difficulty, tag).fold(
                { call.respondError(it) },
                { result ->
                    call.respond(
                        HttpStatusCode.OK,
                        PaginatedResponse(
                            items = result.items.map { it.toResponse() },
                            total = result.total,
                            page = result.page,
                            size = result.size,
                        ),
                    )
                },
            )
        }

        get("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.getById(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        post {
            val req = call.receive<CreateTextRequest>()
            service.create(
                title = req.title,
                body = req.body,
                transliteration = req.transliteration,
                translation = req.translation,
                difficulty = req.difficulty,
                dialect = req.dialect,
                comments = req.comments,
                tags = req.tags,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it.toResponse()) },
            )
        }

        put("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            val req = call.receive<UpdateTextRequest>()
            service.update(
                id = id,
                title = req.title,
                body = req.body,
                transliteration = req.transliteration,
                translation = req.translation,
                difficulty = req.difficulty,
                dialect = req.dialect,
                comments = req.comments,
                tags = req.tags,
            ).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it.toResponse()) },
            )
        }

        delete("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.delete(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }
    }
}
