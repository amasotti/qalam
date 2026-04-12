package com.tonihacks.qalam.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val db = environment.config.config("database")
    val pool = db.config("pool")

    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl         = db.property("url").getString()
            username        = db.property("user").getString()
            password        = db.property("password").getString()
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize   = pool.property("maximumPoolSize").getString().toInt()
            minimumIdle       = pool.property("minimumIdle").getString().toInt()
            connectionTimeout = pool.property("connectionTimeout").getString().toLong()
            idleTimeout       = pool.property("idleTimeout").getString().toLong()
            maxLifetime       = pool.property("maxLifetime").getString().toLong()
        }
    )

    // Migrations run before Exposed gets the connection — startup fails hard if they can't.
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(dataSource)

    monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }
}
