package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.root.CreateRootRequest
import com.tonihacks.qalam.delivery.dto.root.NormalizeRequest
import com.tonihacks.qalam.delivery.dto.root.UpdateRootRequest
import com.tonihacks.qalam.domain.root.RootService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

fun Route.rootRoutes(service: RootService) {
    route("/roots") {
        get {

            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val letterCount = call.request.queryParameters["letterCount"]?.toIntOrNull()

            service.list(page, size, letterCount).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.getById(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post {
            val req = call.receive<CreateRootRequest>()
            service.create(req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.Created, it) },
            )
        }

        put("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            val req = call.receive<UpdateRootRequest>()
            service.update(id, req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        delete("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.delete(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.NoContent) },
            )
        }

        post("/normalize") {
            val req = call.receive<NormalizeRequest>()
            service.normalize(req).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}
