package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItem
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItemResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseFormResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseLabelResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseMappingResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSegmentResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseService
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSessionResponse
import com.tonihacks.qalam.domain.conjugationexercise.AnswerConjugationExerciseItemRequest
import com.tonihacks.qalam.domain.conjugationexercise.CreateConjugationExerciseSessionRequest
import com.tonihacks.qalam.domain.conjugationexercise.exerciseLabel
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.training.TrainingMode
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

fun Route.conjugationExerciseRoutes(service: ConjugationExerciseService) {
    route("/conjugation-exercise-sessions") {
        post {
            val request = call.receive<CreateConjugationExerciseSessionRequest>()
            val mode = parseEnum<TrainingMode>(request.mode) ?: return@post
                call.respondError(DomainError.InvalidInput("Invalid mode: ${request.mode}"))
            val tense = parseEnum<Tense>(request.tense) ?: return@post
                call.respondError(DomainError.InvalidInput("Invalid tense: ${request.tense}"))
            val voice = parseEnum<Voice>(request.voice) ?: return@post
                call.respondError(DomainError.InvalidInput("Invalid voice: ${request.voice}"))
            val wordListIds = request.wordListIds.map { raw ->
                runCatching { java.util.UUID.fromString(raw) }.getOrElse {
                    call.respondError(DomainError.InvalidInput("Invalid word-list ID: $raw"))
                    return@post
                }
            }.toSet()
            service.createMatchingSession(mode, request.size, wordListIds, tense, voice).fold(
                { call.respondError(it) },
                { (session, items) -> call.respond(HttpStatusCode.Created, session.toResponse(items)) },
            )
        }

        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            service.listSessions(page, size).fold(
                { call.respondError(it) }, { call.respond(HttpStatusCode.OK, it) },
            )
        }

        get("/{id}") {
            service.getSession(call.pathParameters.getOrFail("id")).fold(
                { call.respondError(it) }, { (session, items) -> call.respond(HttpStatusCode.OK, session.toResponse(items)) },
            )
        }

        post("/{id}/answers") {
            val request = call.receive<AnswerConjugationExerciseItemRequest>()
            service.answerItem(call.pathParameters.getOrFail("id"), request.itemId, request.mappings).fold(
                { call.respondError(it) }, { call.respond(HttpStatusCode.OK, it) },
            )
        }

        post("/{id}/complete") {
            service.completeSession(call.pathParameters.getOrFail("id")).fold(
                { call.respondError(it) }, { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}

private inline fun <reified T : Enum<T>> parseEnum(value: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }

private fun com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSession.toResponse(
    items: List<ConjugationExerciseItem>,
) = ConjugationExerciseSessionResponse(
    id = id.value.toString(), mode = mode.name, status = status.name, createdAt = createdAt.toString(), completedAt = completedAt?.toString(),
    items = items.map { item ->
        ConjugationExerciseItemResponse(
            itemId = item.id.value.toString(), wordId = item.wordId.toString(), lemma = item.lemmaSnapshot,
            translation = item.translationSnapshot, verbForm = item.verbFormSnapshot,
            tense = tense.name, voice = voice.name,
            forms = item.pairs.sortedBy { it.formPosition }.map { pair ->
                ConjugationExerciseFormResponse(
                    pair.formId.toString(), pair.arabic,
                    pair.segments.map { ConjugationExerciseSegmentResponse(it.text, it.type.name) },
                )
            },
            labels = item.pairs.sortedBy { it.labelPosition }.map { pair -> ConjugationExerciseLabelResponse(pair.labelId.toString(), pair.person.code, pair.person.exerciseLabel()) },
            result = item.result?.name,
            submittedMappings = item.answers.takeIf { item.result != null }?.map { answer ->
                ConjugationExerciseMappingResponse(answer.formId.toString(), answer.selectedLabelId.toString(), answer.isCorrect)
            },
            correctMappings = item.pairs.takeIf { item.result != null }?.map { pair ->
                ConjugationExerciseMappingResponse(pair.formId.toString(), pair.labelId.toString())
            },
        )
    },
)
