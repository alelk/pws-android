package io.github.myorg.myapp.app

import io.github.myorg.myapp.di.DbConfig
import io.github.myorg.myapp.transport.AuthConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.routing.routingRoot
import io.ktor.server.testing.testApplication

/**
 * The HTTP surface, pinned — and, column by column, the security matrix of the service.
 *
 * One line per mounted route: `METHOD /path auth=<provider|public> rl=<bucket[+bucket]|-> audit=<kind|->`.
 * Every column is read from a **marker node** the routing tree carries (`(authenticate <provider>)`,
 * `(RateLimit <bucket>)`, `(audit <kind>)` — the last one is your own transparent `RouteSelector`, see
 * `RouteMarker.kt`), so the file states what is mounted, not what someone remembers mounting.
 *
 * The API design says the tenant comes from the token, every `/v1` route but a named few sits behind
 * a session, every route stands in a rate bucket, path segments are kebab-case — this file is what
 * fails when one of those stops being true. A route mounted twice, outside `authenticate`, outside
 * every bucket, or lost in a refactor shows up here and nowhere else.
 */
private val surfaceConfig =
    AppConfig(
        server = ServerConfig(port = 0),
        db = DbConfig(jdbcUrl = "jdbc:postgresql://localhost:5432/unused", username = "unused", password = "unused"),
        auth = AuthConfig(issuer = "test", audience = "test", jwtSecret = "api-surface-secret-of-32-plus-characters"),
    )

class ApiSurfaceTest :
    FunSpec({
        test("the mounted route table matches api-surface.txt") {
            mountedRoutes().sorted() shouldBe expectedSurface()
        }

        test("every /v1 route is authenticated except the named public ones") {
            mountedRoutes()
                .filter { it.path().startsWith("/v1/") && " auth=public " in it }
                .map { it.substringBefore(" auth=") }
                .toSet() shouldBe PUBLIC_V1_ROUTES.keys
        }

        // A second principal kind (a shared device, a service account) gets a closed list of its own:
        // describing its routes as "public" would hide them among the public ones.
        test("every route under the device provider is one of the named device routes") {
            mountedRoutes()
                .filter { " auth=device " in it }
                .map { it.substringBefore(" auth=") }
                .toSet() shouldBe DEVICE_ROUTES.keys
        }

        // A route without a bucket is a decision, not a default: the list below is the whole of it.
        test("rl=- only on the closed list of unlimited routes") {
            mountedRoutes()
                .filter { " rl=- " in it }
                .map { it.substringBefore(" auth=") }
                .toSet() shouldBe UNLIMITED_ROUTES.keys
        }

        test("every route with a session but the live channels stands in the default bucket first") {
            mountedRoutes()
                .filter { PROVIDERS.any { p -> " auth=$p " in it } && !it.startsWith("SSE ") }
                .filterNot { it.substringAfter(" rl=").substringBefore(' ').split('+').first() == "default" } shouldBe
                emptyList()
        }

        test("no method+path pair is mounted twice") {
            val pairs = mountedRoutes().map { it.substringBefore(" auth=") }
            pairs.size shouldBe pairs.toSet().size
        }

        test("every path segment is kebab-case or a parameter") {
            badSegments(mountedRoutes()) shouldBe KNOWN_BAD_SEGMENTS
        }

        test("one segment position has one parameter name") {
            parameterNameClashes(mountedRoutes()) shouldBe KNOWN_PARAM_NAME_CLASHES
        }
    })

/**
 * `/(authenticate jwt)/v1/items/{id}/(RateLimit default)/[limit?]/(method:GET)`
 *   -> `GET /v1/items/{id} auth=jwt rl=default audit=-`.
 * Every limiter on the way is kept, outermost first, joined by `+`: pinning only the first would let
 * the second be lost unnoticed. A route wrapped by two audit markers is a bug, not a line.
 */
private fun RoutingNode.describe(): String {
    val raw = toString()
    val method = Regex("""\(method:(\w+)\)""").find(raw)?.groupValues?.get(1) ?: "SSE"
    val auth = PROVIDERS.firstOrNull { "(authenticate $it)" in raw } ?: "public"
    val rateLimit =
        Regex("""\(RateLimit ([\w-]+)\)""")
            .findAll(raw)
            .joinToString("+") { it.groupValues[1] }
            .ifEmpty { "-" }
    val audits = AUDIT_MARKER.findAll(raw).map { it.groupValues[1] }.toList()
    check(audits.size <= 1) { "a route wrapped twice: $raw" }
    val path =
        raw
            .replace(Regex("""/\(authenticate [\w-]+\)"""), "")
            .replace(Regex("""/\(RateLimit [\w-]+\)"""), "")
            .replace(Regex("""/\(audit [a-z_]+\)"""), "")
            .replace(Regex("""/\[[^\]]*\]"""), "")
            .replace(Regex("""/\(method:\w+\)"""), "")
    return "$method $path auth=$auth rl=$rateLimit audit=${audits.singleOrNull() ?: "-"}"
}

/** Your authentication providers by name; anything outside every provider is `public`. */
private val PROVIDERS = listOf("jwt", "device")

/** The text of your marker selector (`RouteMarker.kt`). */
private val AUDIT_MARKER = Regex("""\(audit ([a-z_]+)\)""")

private suspend fun mountedRoutes(): List<String> {
    lateinit var routes: List<String>
    testApplication {
        application {
            module(surfaceConfig, eagerDatabase = false, startBackgroundServices = false)
            routes = routingRoot.getAllRoutes().map { it.describe() }
        }
        startApplication()
    }
    return routes
}

private fun expectedSurface(): List<String> =
    checkNotNull(ApiSurfaceTest::class.java.getResourceAsStream("/api-surface.txt"))
        .bufferedReader()
        .readLines()
        .filter { it.isNotBlank() }
        .sorted()

private fun String.path(): String = substringAfter(' ').substringBefore(" auth=")

private fun badSegments(routes: List<String>): Set<String> =
    routes
        .flatMap { it.path().split('/') }
        .filter { it.isNotEmpty() }
        .filterNot { it.matches(KEBAB_SEGMENT) || it.matches(PARAMETER_SEGMENT) }
        .toSet()

private fun parameterNameClashes(routes: List<String>): Set<String> {
    val parameterPrefixes =
        routes
            .map { it.path() }
            .distinct()
            .flatMap { path ->
                val segments = path.trim('/').split('/')
                segments.indices
                    .filter { segments[it].matches(PARAMETER_SEGMENT) }
                    .map { index -> "/" + segments.take(index + 1).joinToString("/") }
            }

    return parameterPrefixes
        .groupBy { prefix ->
            prefix
                .trim('/')
                .split('/')
                .joinToString("/", prefix = "/") { segment ->
                    if (segment.matches(PARAMETER_SEGMENT)) "{}" else segment
                }
        }.values
        .flatMap { prefixes ->
            prefixes
                .distinct()
                .sorted()
                .let { sorted ->
                    sorted.flatMapIndexed { index, left ->
                        sorted.drop(index + 1).map { right -> "$left vs $right" }
                    }
                }
        }.toSet()
}

private val KEBAB_SEGMENT = Regex("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$")
private val PARAMETER_SEGMENT = Regex("^\\{[A-Za-z][A-Za-z0-9]*}$")

/**
 * Routes that answer without a session, each with the reason it has to. Health is not here: it
 * is not `/v1`. This map is PINNED, not shrinking: a new name is a decision, not an edit.
 */
private val PUBLIC_V1_ROUTES: Map<String, String> =
    mapOf(
        "POST /v1/accounts" to "registration answers the same either way (no enumeration)",
        "POST /v1/auth/login" to "the door itself",
        "POST /v1/auth/refresh" to "the access token has expired; the refresh cookie is the credential",
        "POST /v1/auth/logout" to "idempotent; the cookie may be all there is",
        "GET /v1/capabilities" to "what this deployment can do; nothing personal",
        // "GET /v1/exports/{token}" to "a one-time link opened by a browser that carries no token",
    )

/** The routes a device token opens — CLOSED, the mirror of [PUBLIC_V1_ROUTES]; every other route answers it 401. */
private val DEVICE_ROUTES: Map<String, String> =
    mapOf(
        // "GET /v1/device/board" to "the shared screen's view of the day",
    )

/**
 * Routes outside every bucket — CLOSED. Health answers the orchestrator, which must never meet a 429;
 * a live channel is one long request, capped by connections, not by a rate.
 */
private val UNLIMITED_ROUTES: Map<String, String> =
    mapOf(
        "GET /health" to "the orchestrator's probe",
        "GET /health/live" to "the orchestrator's probe",
        "GET /health/ready" to "the orchestrator's probe",
        "SSE /v1/sync" to "one long-lived request, capped by connections per tenant",
    )

/** Closed today: every segment already reads as kebab-case. The list exists so it stays that way. */
private val KNOWN_BAD_SEGMENTS = emptySet<String>()

/** One segment position, one parameter name. */
private val KNOWN_PARAM_NAME_CLASHES = emptySet<String>()
