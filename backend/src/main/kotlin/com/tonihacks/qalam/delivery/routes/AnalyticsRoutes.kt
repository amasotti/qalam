package com.tonihacks.qalam.delivery.routes

import com.tonihacks.qalam.delivery.respondError
import com.tonihacks.qalam.domain.analytics.AnalyticsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.analyticsRoutes(service: AnalyticsService) {
    route("/analytics") {
        get("/overview") {
            service.getOverview().fold(
                { call.respondError(it) },
                { call.respond(HttpStatusCode.OK, it) },
            )
        }
    }
}
