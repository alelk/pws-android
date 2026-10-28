# A shared device: a screen that belongs to no one

A wall tablet in a kitchen or an office, a reception screen: it shows the tenant's shared board and
lets whoever stands in front of it act on what is theirs. It is a **device, not a member** — no
account, no member row, no role. This file is the client half; the server half (device principal,
its own token audience, a closed list of device routes with a wall both ways) is
[ktor-auth-sessions](../../ktor-auth-sessions/SKILL.md).

**The mine:** the screen quietly turns into a member — it gets a role "because the use-case needs
one", reaches a store that knows a role (badges, wallet, notifications), or the client decides what
to hide. Every rule below exists to keep that from happening without a test turning red.

## Session: a fourth state, never a flavour of the third

```kotlin
sealed interface SessionState {
    data object Loading : SessionState
    data class Anonymous(val expired: Boolean = false, val deviceOff: Boolean = false) : SessionState
    data class Authenticated(/* account, memberships, active tenant */) : SessionState
    data class Device(val tenantName: String, val timezone: TimeZone) : SessionState
}

when (state) {                       // in AppRoot — no `else`: a fifth state does not compile unanswered
    SessionState.Loading -> Splash()
    is SessionState.Anonymous -> AuthZone(state)
    is SessionState.Authenticated -> AppZone(state)
    is SessionState.Device -> DeviceZone(state)
}
```

- Start order on a browser is written down and tested (`StartOrderTest`): personal session →
  device session → a bookmarked sign-in link → sign-in screen.
- **One HTTP client.** Token refresh picks its route by the session kind (person refresh vs device
  refresh); there is no second client with its own interceptors to drift. Cookie paths must let both
  refresh routes see their cookie (a person's refresh cookie scoped to the auth prefix, the device
  secret in its own cookie).
- Revocation is a typed event (`SessionEvent.DeviceRevoked`) that lands on the sign-in screen with
  "This screen was switched off" — **not** "your session expired", and without a reason: the server
  does not say why (an admin's switch-off or a tenant being deleted), so neither does the client.

## The zone

```kotlin
@Composable
internal fun DeviceZone(state: SessionState.Device) {
    val koin = getKoin()
    val scope = remember { koin.createScope("device-${newClientId()}", DEVICE) }   // NOT the person's ClientSession
    DisposableEffect(scope) { onDispose { scope.close() } }
    val history = koinInject<BrowserHistory>()
    LaunchedEffect(history) {
        history.sync("", push = false)   // the address a person's zone left in the tab means nothing here
        history.bindBack { }             // browser-back on a wall tablet leads nowhere
    }
    UnboundKoinScope(scope) { DeviceBoardEntry(state.tenantName, state.timezone) }
}
```

- No rail, no tabs, no navigation, no address in history; **one** entry composable.
- Its own Koin scope (`DEVICE`): the person's session scope is never raised here, so no store that
  knows a role is even resolvable. (Worth a Koin-graph test that resolves the device scope and
  asserts the role-bearing stores are absent; the source project relies on the scope split alone.)
- A sheet opened on the shared screen closes itself after an idle period (e.g. two minutes without a
  touch) and releases the microphone; an unfinished recording is discarded, a sent one stays sent;
  the timer does not run while someone is recording. The next person must not inherit the last
  one's half-done action.
- Default skin and the system theme (no person to have a preference).
- No first-time hints on the shared screen — nobody to remember them for
  ([compose-design-system / first-time-hints.md](../../compose-design-system/references/first-time-hints.md)).

## What shows is not the client's decision

- The server filters: one pure function on the server decides which items are "shared" vs
  "personal" (by the owner of the goal/project the item belongs to; doubt hides). The client never
  re-derives it and has no predicate to drift.
- The projection is **narrow by signature**: the device card DTO carries title, description,
  checklist, assignee, time, column, the return reason and the available action — no price, no
  balance, no report text, no conversation. A field that is not in the type cannot leak.
- The device acts *as the item's assignee* server-side (a constant "hand" object, not a role), and
  the trace is a device id on the fact (`closed_via`, `via_device`) — the client sends nothing that
  claims a role.

## Layout

One breakpoint for the device too (e.g. ≥ 840 dp: a strip on top + three columns; narrower: one
column with a column switcher). Large touch targets come from `AppTheme.sizing`, not a second skin.

Evidence: family-planner step 73, ADR 0082 (device, not a member), ADR 0083 (one circle function,
narrow projection), `docs/40-frontend/platform-shells.md` §Step 73; locks `KioskWallTest` (server,
both directions), `StartOrderTest`, `KioskIdleTest`, `TraceCarriedTest`, `HintRulesTest` ("the
shared screen draws no hint").
