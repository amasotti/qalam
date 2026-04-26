package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.delivery.dto.ai.InsightRequest
import com.tonihacks.qalam.delivery.dto.ai.InsightResponse
import com.tonihacks.qalam.domain.ai.AiInsightService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.aiInsightRoutes(service: AiInsightService) {
    route("/ai") {
        post("/insight") {
            val req = call.receive<InsightRequest>()
            service.generateInsight(req.entityType, req.entityId, req.mode).fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, InsightResponse(it)) },
            )
        }
    }
}
