package io.github.myorg.myapp.contract.common

import kotlinx.serialization.json.Json

/**
 * The one JSON policy the contract is read and written with — on the server, in the error
 * envelope, on the live channel and in every client.
 *
 * It lives in `api:contract` because that is the only module both banks can see. A second
 * policy anywhere always drifts — typically Ktor's `DefaultJson` on the server (does NOT ignore
 * unknown keys, IS lenient) against a client that ignores them, which silently turns rule one
 * of additive evolution into "a new field is a 400".
 *
 * - [Json.ignoreUnknownKeys] — the additive-evolution rule itself. A newer client's extra
 *   field must cost nothing.
 * - [Json.encodeDefaults] — without it `{"type":"resync"}` loses every field that happens to
 *   hold its default, and `@EncodeDefault(NEVER)` stops meaning anything.
 * - [Json.explicitNulls] `= false` — an absent field and a null field mean the same thing in a
 *   **response**; the three-state PATCH semantics live in `OptionalFieldDto`, on requests, and
 *   are unaffected.
 * - `isLenient` is deliberately **not** set: ktor's default turns it on, and a contract this
 *   strict has no reason to accept unquoted literals.
 * - `classDiscriminator` is deliberately **not** set: it stays `"type"`, and every polymorphic
 *   hierarchy says so itself with `@JsonClassDiscriminator("type")`, checked by
 *   a contract conventions test. A rule that holds by coincidence does not hold.
 */
val ApiJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
