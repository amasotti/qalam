package com.tonihacks.qalam

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Smoke test: every path+method declared in openapi/documentation.yaml must be registered
 * in the actual Ktor routing.
 *
 * Strategy:
 *   - Path params replaced with a zero UUID → route matches, resource is not found → 404 with
 *     body {"code":"NOT_FOUND"} (domain 404 vs routing 404 which has no body).
 *   - 405 MethodNotAllowed means the path exists but the method is not registered.
 *   - Anything else (200, 201, 400, 422, 503…) means the route exists and handled the request.
 */
class OpenApiSmokeTest : BaseIntegrationTest() {

    companion object {
        private const val DUMMY_UUID = "00000000-0000-0000-0000-000000000000"
        private val PATH_PARAM = Regex("\\{[^}]+\\}")
        private val METHOD_RE = Regex("^    (get|post|put|patch|delete):\\s*$")
        private val PATH_RE = Regex("^  (/api/[^:\\s]+):\\s*$")

        fun parseSpec(): List<Pair<HttpMethod, String>> {
            val yaml = object {}.javaClass.classLoader
                .getResourceAsStream("openapi/documentation.yaml")
                ?.bufferedReader()?.readText()
                ?: error("openapi/documentation.yaml not found on classpath")

            val result = mutableListOf<Pair<HttpMethod, String>>()
            var currentPath: String? = null

            for (line in yaml.lines()) {
                PATH_RE.find(line)?.let {
                    currentPath = it.groupValues[1]
                    return@let
                }
                METHOD_RE.find(line)?.let { match ->
                    val path = currentPath ?: return@let
                    val method = when (match.groupValues[1]) {
                        "get" -> HttpMethod.Get
                        "post" -> HttpMethod.Post
                        "put" -> HttpMethod.Put
                        "patch" -> HttpMethod.Patch
                        "delete" -> HttpMethod.Delete
                        else -> return@let
                    }
                    result.add(method to path)
                }
            }
            return result
        }
    }

    init {
        "OpenAPI spec — every declared route must be registered in Ktor routing" - {
            parseSpec().forEach { (method, pathTemplate) ->
                "$method $pathTemplate" {
                    val path = PATH_PARAM.replace(pathTemplate, DUMMY_UUID)
                    testApp { client ->
                        val response = client.request(path) {
                            this.method = method
                            if (method in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
                                contentType(ContentType.Application.Json)
                                setBody("{}")
                            }
                        }
                        // 404 is acceptable only if it's a domain NOT_FOUND (route exists, resource absent).
                        // A routing 404 (unregistered path) returns an empty body without "NOT_FOUND".
                        if (response.status == HttpStatusCode.NotFound) {
                            response.bodyAsText() shouldContain "NOT_FOUND"
                        }
                        response.status shouldNotBe HttpStatusCode.MethodNotAllowed
                    }
                }
            }
        }
    }
}
