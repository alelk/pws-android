package io.github.myorg.myapp.server.transport

import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/*
 * A cross-cutting per-route policy (an audit kind, a feature flag, a "re-authentication required",
 * a cost class) that the API-surface test can SEE.
 *
 * Ktor already leaves visible nodes for its own scoping — `(authenticate jwt)`, `(RateLimit default)` —
 * and `ApiSurfaceTest.describe()` turns them into the `auth=` and `rl=` columns. A policy you add by a
 * helper function leaves nothing in the tree, so nothing fails when a new route forgets it. Wrapping
 * the policy in a TRANSPARENT selector gives it a node of its own; the surface test reads it as one
 * more column, and a second test can cross-check that column against the closed domain list it comes
 * from (`MarkerCoverageTest`).
 *
 * Mine it prevents: "a kind added to the enum and no route wrapped" — every test of the kinds that
 * exist stays green while the new door writes nothing.
 */

/** Matches nothing and consumes nothing; exists only to show `(audit <code>)` in the routing tree. */
class AuditRouteSelector(
    val code: String,
) : RouteSelector() {
    override suspend fun evaluate(
        context: RoutingResolveContext,
        segmentIndex: Int,
    ): RouteSelectorEvaluation = RouteSelectorEvaluation.Transparent

    override fun toString(): String = "(audit $code)"
}

/**
 * `audited(AuditKind.OwnershipTransferred) { post<Owner> { … } }` — every route [build] mounts carries
 * the marker and the route-scoped plugin that implements the policy.
 */
fun Route.audited(
    kind: AuditKind,
    build: Route.() -> Unit,
): Route {
    val route = createChild(AuditRouteSelector(kind.code))
    route.install(AuditDoor) { this.kind = kind }
    route.build()
    return route
}

/** Your domain's closed list; `code` is the snake_case wire/storage name of the constant. */
enum class AuditKind {
    SignIn,
    PasswordChanged,
    OwnershipTransferred,
    ;

    val code: String get() = name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}

class AuditDoorConfig {
    lateinit var kind: AuditKind
}

/** The policy itself (for an audit journal: write one row after the answer is formed, in its own transaction). */
val AuditDoor: RouteScopedPlugin<AuditDoorConfig> =
    createRouteScopedPlugin("AuditDoor", ::AuditDoorConfig) {
        // Hook the send pipeline at ApplicationSendPipeline.Before (not ResponseBodyReadyForSend: by then
        // the ApiErrorDto is serialized) and read the error `respondError` kept in a call attribute.
        // Full implementation: audit-journal-at-the-door, templates/transport/Audited.kt.
    }
