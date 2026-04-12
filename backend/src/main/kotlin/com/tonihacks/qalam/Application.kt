package com.tonihacks.qalam

import com.tonihacks.qalam.delivery.configurePlugins
import com.tonihacks.qalam.delivery.configureRouting
import com.tonihacks.qalam.infrastructure.db.configureDatabase
import com.tonihacks.qalam.infrastructure.koin.appModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

// Entry point: io.ktor.server.netty.EngineMain (set in build.gradle.kts).
// EngineMain reads application.conf, binds to the configured port, and calls Application.module().
fun Application.module(vararg overrides: org.koin.core.module.Module) {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
        if (overrides.isNotEmpty()) {
            allowOverride(true)
            modules(*overrides)
        }
    }
    configureDatabase()
    configurePlugins()
    configureRouting()
}
