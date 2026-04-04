package com.tonihacks.qalam

import io.kotest.core.spec.style.FreeSpec
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.testcontainers.containers.PostgreSQLContainer

abstract class BaseIntegrationTest : FreeSpec() {

    companion object {
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("qalam_test")
                .withUsername("qalam_test")
                .withPassword("qalam_test")
                .also { it.start() }
    }

    /**
     * Starts a full Ktor testApplication wired to the Testcontainers DB.
     * Flyway runs on first call; subsequent calls are instant (schema already applied).
     */
    protected fun testApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) {
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "database.url"                     to postgres.jdbcUrl,
                    "database.user"                    to postgres.username,
                    "database.password"                to postgres.password,
                    "database.pool.maximumPoolSize"    to "2",
                    "database.pool.minimumIdle"        to "1",
                    "database.pool.connectionTimeout"  to "5000",
                    "database.pool.idleTimeout"        to "60000",
                    "database.pool.maxLifetime"        to "120000",
                )
            }
            application { module() }

            val client = createClient {
                install(ContentNegotiation) { json() }
            }
            block(client)
        }
    }
}
