package com.tonihacks.qalam

import com.tonihacks.qalam.delivery.configurePlugins
import com.tonihacks.qalam.delivery.configureRouting
import com.tonihacks.qalam.infrastructure.db.configureDatabase
import com.tonihacks.qalam.infrastructure.koin.appModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.core.module.Module

// Entry point: io.ktor.server.netty.EngineMain (set in build.gradle.kts).
// EngineMain reads application.conf, binds to the configured port, and calls this no-argument function.
fun Application.module() = configureApplication()

internal fun Application.configureApplication(additionalModules: List<Module> = emptyList()) {
    install(Koin) {
        slf4jLogger()
        modules(appModule, *additionalModules.toTypedArray())
    }
    configureDatabase()
    configurePlugins()
    configureRouting()
}
