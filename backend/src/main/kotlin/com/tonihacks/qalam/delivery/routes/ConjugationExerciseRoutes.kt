package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.conjugation.model.Tense
import com.tonihacks.qalam.domain.conjugation.model.Voice
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItem
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseItemResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseFormResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseLabelResponse
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseService
import com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSessionResponse
import com.tonihacks.qalam.domain.conjugationexercise.CreateConjugationExerciseSessionRequest
import com.tonihacks.qalam.domain.conjugationexercise.exerciseLabel
import com.tonihacks.qalam.domain.error.DomainError
import com.tonihacks.qalam.domain.training.TrainingMode
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.conjugationExerciseRoutes(service: ConjugationExerciseService) {
    route("/conjugation-exercise-sessions") {
        post {
            val request = call.receive<CreateConjugationExerciseSessionRequest>()
            val mode = parseEnum<TrainingMode>(request.mode, "mode")
            val tense = parseEnum<Tense>(request.tense, "tense")
            val voice = parseEnum<Voice>(request.voice, "voice")
            val wordListIds = request.wordListIds.map {
                runCatching { UUID.fromString(it) }.getOrElse {
                    call.respondError(DomainError.InvalidInput("Invalid word-list ID: $it"))
                    return@post
                }
            }.toSet()
            service.createMatchingSession(mode, request.size, wordListIds, tense, voice).fold(
                { call.respondError(it) },
                { (session, items) -> call.respond(HttpStatusCode.Created, session.toResponse(items)) },
            )
        }
    }
}

private inline fun <reified T : Enum<T>> parseEnum(value: String, field: String): T =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw IllegalArgumentException("Invalid $field: $value")

private fun com.tonihacks.qalam.domain.conjugationexercise.ConjugationExerciseSession.toResponse(
    items: List<ConjugationExerciseItem>,
) = ConjugationExerciseSessionResponse(
    id = id.value.toString(), mode = mode.name, status = status.name, createdAt = createdAt.toString(),
    items = items.map { item ->
        ConjugationExerciseItemResponse(
            itemId = item.id.value.toString(), wordId = item.wordId.toString(), lemma = item.lemmaSnapshot,
            translation = item.translationSnapshot, verbForm = item.verbFormSnapshot,
            tense = tense.name, voice = voice.name,
            forms = item.pairs.sortedBy { it.formPosition }.map { pair -> ConjugationExerciseFormResponse(pair.formId.toString(), pair.arabic) },
            labels = item.pairs.sortedBy { it.labelPosition }.map { pair -> ConjugationExerciseLabelResponse(pair.labelId.toString(), pair.person.code, pair.person.exerciseLabel()) },
        )
    },
)
