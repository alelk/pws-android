# Pinning the HTTP surface (route-surface pinning)

## Source of truth: the live routing tree

Boot the real module DB-free and walk the routes:

```kotlin
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
```

`describe()` turns a node's text into one line. Each column comes from a **marker node** the tree
carries:

```
/(authenticate jwt)/v1/items/{id}/(RateLimit default)/(audit item_deleted)/(method:DELETE)
  -> DELETE /v1/items/{id} auth=jwt rl=default audit=item_deleted
```

| Column  | Marker node                    | Values                                                              |
|---------|--------------------------------|---------------------------------------------------------------------|
| `auth`  | `(authenticate <provider>)`    | each provider by name (`jwt`, `device`, …) or `public` = no provider |
| `rl`    | `(RateLimit <bucket>)`         | every bucket on the way, outermost first, joined by `+`; `-` = none  |
| `audit` | your own `(audit <kind>)`      | the kind written, `-` = none                                         |

Template: [`../templates/server/ApiSurfaceTest.kt`](../templates/server/ApiSurfaceTest.kt). Take the
providers from a list (`PROVIDERS = listOf("jwt", "device")`) — a template that only knows
`(authenticate jwt)` describes a second provider's routes as `public`, and the shortest repair of the
red test is to add them to the public list.

The route's `toString()` format is Ktor-internal — if an upgrade changes it, the test fails loudly
(good) and `describe()` needs a tweak.

## A policy that leaves no node is invisible — give it one

`authenticate` and `rateLimit` scope routes with nodes of their own; a policy applied by a helper
function (`audit(kind)` inside the handler, a "needs re-auth" check) does not, so nothing fails when
a new route forgets it. Wrap the policy in a **transparent `RouteSelector`** whose `toString()` is
the marker, and install the route-scoped plugin under it:

```kotlin
class AuditRouteSelector(val code: String) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Transparent
    override fun toString() = "(audit $code)"
}
fun Route.audited(kind: AuditKind, build: Route.() -> Unit): Route =
    createChild(AuditRouteSelector(kind.code)).apply { install(AuditDoor) { this.kind = kind }; build() }
```

Template: [`../templates/server/RouteMarker.kt`](../templates/server/RouteMarker.kt). The surface
gains a column; the diff of `api-surface.txt` now shows which routes gained or lost the policy.

## Assertions

| Test                                                         | Why                                                          |
|--------------------------------------------------------------|--------------------------------------------------------------|
| sorted routes == `api-surface.txt`                            | a route lost in a refactor, or added without a decision      |
| public `/v1` routes == `PUBLIC_V1_ROUTES` (pinned, with reasons) | a route mounted outside `authenticate` by accident        |
| each extra provider's routes == its own closed map            | a device/service token reaching a route nobody decided on    |
| `rl=-` only on `UNLIMITED_ROUTES` (closed: probes, live channels) | a route mounted with a bare `authenticate` and no bucket |
| every session route stands in the `default` bucket **first**  | a route whose only limiter is a narrow one (or none)         |
| no method+path twice; one marker of a kind per route          | double mounting / double wrapping                            |
| every segment kebab-case or `{param}`                         | URL style                                                    |
| one parameter name per path position                          | `{id}` vs `{itemId}` at the same position                    |

`api-surface.txt` (test resource), one line per route:

```
GET /health auth=public rl=- audit=-
POST /v1/accounts auth=public rl=credentials audit=registered
POST /v1/auth/login auth=public rl=credentials audit=sign_in
GET /v1/items auth=jwt rl=default audit=-
GET /v1/device/board auth=device rl=default audit=-
SSE /v1/sync auth=jwt rl=- audit=-
```

Every pinned map (`PUBLIC_V1_ROUTES`, `DEVICE_ROUTES`, `UNLIMITED_ROUTES`) is `Map<route, reason>`.
They are **pinned, not shrinking**: changing one is a reviewed decision; the diff of
`api-surface.txt` is the API and security changelog of the PR.

## Cross-check a closed domain list against the tree

When a column's values come from a domain enum (audit kinds, permission names), pin the
correspondence **both ways** ([`../templates/server/MarkerCoverageTest.kt`](../templates/server/MarkerCoverageTest.kt)):

- every constant (read by name, `PascalCase` -> `snake_case`) appears in the column at least once, or
  is in `KNOWN_UNROUTED_KINDS` (map: kind -> who writes it instead);
- every route of a bucket that must carry the marker (e.g. `credentials`) carries it, or is in
  `KNOWN_UNMARKED_CREDENTIALS` (map: route -> why no row is needed).

"A kind without a door and a door without a kind are equally red." Mine it prevents: a kind added to
the enum, its tests written, and no route wrapped — the new door writes nothing and every test is green.

## The wall test walks the same tree

The surface proves which provider guards each route; it does not prove the providers refuse each
other's tokens. That is a behaviour test over the same live tree — every route of provider A sent a
token of provider B must answer `401` — see `kotlin-testing-strategy`
([`security-boundaries.md`](../../kotlin-testing-strategy/references/security-boundaries.md)) and the
`PrincipalWallTest` template in `ktor-auth-sessions`.

Evidence: family-planner `ApiSurfaceTest` (166 lines, `auth=jwt|kiosk|public`, `rl=` joined, `audit=`),
`AuditSurfaceTest` (ADR 0096), `Audited.kt` (`AuditRouteSelector`), step 78 (`rl=-` closed list of
five, `default` bucket first), step 73 (`KIOSK_ROUTES`, ADR 0082).
