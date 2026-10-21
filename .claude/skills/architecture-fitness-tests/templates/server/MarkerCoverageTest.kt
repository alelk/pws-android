package io.github.myorg.myapp.app

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.routing.routingRoot
import io.ktor.server.testing.testApplication

/**
 * A closed domain list cross-checked against the live routing tree, BOTH ways.
 *
 * - every constant of the domain enum (read by name, PascalCase -> snake_case) stands on at least one
 *   route of the live tree, or is named in [KNOWN_UNROUTED_KINDS] with who writes it instead;
 * - every route of a bucket that must carry the marker (here: `credentials`, the routes that check a
 *   secret) carries it, or is named in [KNOWN_UNMARKED_CREDENTIALS] with its reason.
 *
 * A kind without a door and a door without a kind are equally red. The domain class is read by NAME,
 * so the test can be written before the class exists (red with `ClassNotFoundException` — the plan's
 * red) and does not make the app module depend on a domain package it otherwise would not import.
 *
 * Copy per marker. `module(...)`, `surfaceConfig` and the node rendering are your project's and must
 * match `ApiSurfaceTest.describe()`.
 */
class MarkerCoverageTest :
    FunSpec({
        test("guard: the live tree shows the credential routes, and the plan's list has its size") {
            val credentials = mountedMarkedRoutes().filter { it.isCredential }.map { it.route }
            withClue("credential routes: $credentials") {
                (credentials.size >= MIN_CREDENTIAL_ROUTES) shouldBe true
                credentials.containsAll(KNOWN_UNMARKED_CREDENTIALS.keys) shouldBe true
            }
            PLANNED_KINDS.size shouldBe PLANNED_KIND_COUNT
            (KNOWN_UNROUTED_KINDS.keys - PLANNED_KINDS).shouldBeEmpty()
        }

        test("the domain enum is the plan's closed list") {
            domainKinds() shouldBe PLANNED_KINDS
        }

        test("every kind is on a route of the live tree or named unrouted") {
            val routed = mountedMarkedRoutes().mapNotNull { it.marker }.toSet()
            withClue("routed kinds: ${routed.sorted()}") {
                routed + KNOWN_UNROUTED_KINDS.keys shouldBe domainKinds()
                (routed intersect KNOWN_UNROUTED_KINDS.keys).shouldBeEmpty()
            }
        }

        test("every route of the credentials bucket is marked, or named with its reason") {
            mountedMarkedRoutes()
                .filter { it.isCredential && it.marker == null }
                .map { it.route }
                .toSet() shouldBe KNOWN_UNMARKED_CREDENTIALS.keys
        }
    })

private data class MarkedRoute(
    val route: String,
    val buckets: List<String>,
    val marker: String?,
) {
    val isCredential: Boolean get() = "credentials" in buckets
}

private fun RoutingNode.toMarkedRoute(): MarkedRoute {
    val raw = toString()
    val method = Regex("""\(method:(\w+)\)""").find(raw)?.groupValues?.get(1) ?: "SSE"
    val buckets = Regex("""\(RateLimit ([\w-]+)\)""").findAll(raw).map { it.groupValues[1] }.toList()
    val markers = MARKER_NODE.findAll(raw).map { it.groupValues[1] }.toList()
    check(markers.size <= 1) { "a route wrapped twice: $raw" }
    val path =
        raw
            .replace(Regex("""/\(authenticate [\w-]+\)"""), "")
            .replace(Regex("""/\(RateLimit [\w-]+\)"""), "")
            .replace(MARKER_SEGMENT, "")
            .replace(Regex("""/\[[^\]]*\]"""), "")
            .replace(Regex("""/\(method:\w+\)"""), "")
    return MarkedRoute("$method $path", buckets, markers.singleOrNull())
}

private suspend fun mountedMarkedRoutes(): List<MarkedRoute> {
    lateinit var routes: List<MarkedRoute>
    testApplication {
        application {
            module(surfaceConfig, eagerDatabase = false, startBackgroundServices = false)
            routes = routingRoot.getAllRoutes().map { it.toMarkedRoute() }
        }
        startApplication()
    }
    return routes
}

private fun domainKinds(): Set<String> =
    Class
        .forName(KIND_CLASS)
        .enumConstants
        .map { (it as Enum<*>).name.snakeCase() }
        .toSet()

private fun String.snakeCase(): String = replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

private const val KIND_CLASS = "io.github.myorg.myapp.domain.common.audit.AuditKind"

private val MARKER_NODE = Regex("""\(audit ([a-z_]+)\)""")
private val MARKER_SEGMENT = Regex("""/\(audit [a-z_]+\)""")

/** Floor: today's count of credential routes; far fewer means the walk lost the tree. */
private const val MIN_CREDENTIAL_ROUTES = 10

/** The plan's closed list, pinned: a new kind is a decision of a plan, not of a stage. */
private const val PLANNED_KIND_COUNT = 3

private val PLANNED_KINDS =
    setOf(
        "sign_in",
        "password_changed",
        "ownership_transferred",
    )

/** Kinds no route carries — CLOSED; each says who writes it instead. */
private val KNOWN_UNROUTED_KINDS: Map<String, String> =
    mapOf(
        // "operator_reset_password" to "written by the operator CLI command itself",
    )

/** Credential routes without the marker — CLOSED; each with the reason no row is needed. */
private val KNOWN_UNMARKED_CREDENTIALS: Map<String, String> =
    mapOf(
        // "POST /v1/auth/verify-email/resend" to "sends a letter again; the proof itself is audited",
    )
