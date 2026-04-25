package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.training.CreateSessionRequest
import com.tonihacks.qalam.domain.training.RecordResultRequest
import com.tonihacks.qalam.domain.training.TrainingService
import com.tonihacks.qalam.domain.training.TrainingSession
import com.tonihacks.qalam.domain.training.TrainingSessionResponse
import com.tonihacks.qalam.domain.training.TrainingSessionWord
import com.tonihacks.qalam.domain.training.TrainingSessionWordResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail

fun Route.trainingRoutes(service: TrainingService) {
    route("/training") {
        post("/sessions") {
            val req = call.receive<CreateSessionRequest>()
            service.createSession(req.mode, req.size).fold(
                { call.respondError(it) },
                { (session, words) -> call.respond(HttpStatusCode.Created, toSessionResponse(session, words)) },
            )
        }

        get("/sessions") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            service.listSessions(page, size).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/sessions/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.getSession(id).fold(
                { call.respondError(it) },
                { (session, words) -> call.respond(HttpStatusCode.OK, toSessionResponse(session, words)) },
            )
        }

        post("/sessions/{id}/results") {
            val id = call.pathParameters.getOrFail<String>("id")
            val req = call.receive<RecordResultRequest>()
            service.recordResult(id, req.wordId, req.result).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post("/sessions/{id}/complete") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.completeSession(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/stats") {
            service.getStats().fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}

private fun toSessionResponse(
    session: TrainingSession,
    words: List<TrainingSessionWord>,
): TrainingSessionResponse = TrainingSessionResponse(
    id          = session.id.value.toString(),
    mode        = session.mode.name,
    status      = session.status.name,
    words       = words.map { w ->
        TrainingSessionWordResponse(
            wordId          = w.wordId.value.toString(),
            arabicText      = w.arabicText,
            transliteration = w.transliteration,
            translation     = w.translation,
            frontSide       = w.frontSide.name,
            position        = w.position,
            result          = w.result?.name,
            masteryLevel    = w.masteryLevel,
        )
    },
    createdAt   = session.createdAt.toString(),
    completedAt = session.completedAt?.toString(),
)
