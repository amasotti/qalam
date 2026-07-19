package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.exercise.AnswerExerciseItemRequest
import com.tonihacks.qalam.domain.exercise.CreateExerciseSessionRequest
import com.tonihacks.qalam.domain.exercise.ExerciseOptionResponse
import com.tonihacks.qalam.domain.exercise.ExercisePromptResponse
import com.tonihacks.qalam.domain.exercise.ExerciseService
import com.tonihacks.qalam.domain.exercise.ExerciseSession
import com.tonihacks.qalam.domain.exercise.ExerciseSessionItem
import com.tonihacks.qalam.domain.exercise.ExerciseSessionItemResponse
import com.tonihacks.qalam.domain.exercise.ExerciseSessionResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

fun Route.exerciseRoutes(service: ExerciseService) {
    route("/exercise-sessions") {
        post {
            val req = call.receive<CreateExerciseSessionRequest>()
            service.createSession(
                modeStr          = req.mode,
                size             = req.size,
                wordListIds      = req.wordListIds,
                exerciseTypeStrs = req.exerciseTypes,
                optionCount      = req.optionCount,
            ).fold(
                { call.respondError(it) },
                { (session, items) -> call.respond(HttpStatusCode.Created, toExerciseSessionResponse(session, items)) },
            )
        }

        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            service.listSessions(page, size).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/{id}") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.getSession(id).fold(
                { call.respondError(it) },
                { (session, items) -> call.respond(HttpStatusCode.OK, toExerciseSessionResponse(session, items)) },
            )
        }

        post("/{id}/answers") {
            val id = call.pathParameters.getOrFail<String>("id")
            val req = call.receive<AnswerExerciseItemRequest>()
            service.answerItem(id, req.itemId, req.selectedOptionId).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post("/{id}/complete") {
            val id = call.pathParameters.getOrFail<String>("id")
            service.completeSession(id).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}

private fun toExerciseSessionResponse(
    session: ExerciseSession,
    items: List<ExerciseSessionItem>,
): ExerciseSessionResponse = ExerciseSessionResponse(
    id          = session.id.value.toString(),
    mode        = session.mode.name,
    status      = session.status.name,
    items       = items.map { item ->
        ExerciseSessionItemResponse(
            itemId           = item.id.value.toString(),
            wordId           = item.wordId.value.toString(),
            type             = item.type.name,
            prompt           = ExercisePromptResponse(
                kind = item.promptKind.name,
                text = item.promptText,
            ),
            options          = item.options.map { option ->
                ExerciseOptionResponse(
                    optionId        = option.id.value.toString(),
                    wordId          = option.wordId.value.toString(),
                    arabicText      = option.arabicText,
                    transliteration = option.transliteration,
                    translation     = option.translation,
                )
            },
            result           = item.result?.name,
            selectedOptionId = item.selectedOptionId?.value?.toString(),
            answeredAt       = item.answeredAt?.toString(),
        )
    },
    createdAt   = session.createdAt.toString(),
    completedAt = session.completedAt?.toString(),
)
