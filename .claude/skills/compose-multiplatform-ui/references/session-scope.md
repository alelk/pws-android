# Session-scoped client state

## Problem

Stores that cache tenant data (the board snapshot, the roster, the wallet, the live-sync
connection) are convenient as Koin singletons — and wrong: after logout, token expiry or a switch
to another tenant, the old tenant's data and its SSE stream survive in memory and flash on the
next screen.

## Decision

Two graphs:

| Root graph (singletons)                                  | Session graph (`scope(SESSION)`)                          |
|----------------------------------------------------------|-----------------------------------------------------------|
| HTTP client, token store, platform seams, capabilities   | every store holding tenant data, the SSE hub, timers      |
| anonymous/account ViewModels (login, choose tenant)      | tenant screens' collaborators                             |

```kotlin
data class SessionKey(val accountId: String, val tenantId: String)

class ClientSession(private val koin: Koin) {
    private val lock = Mutex()
    private val mutableCurrent = MutableStateFlow<SessionScope?>(null)
    val current: StateFlow<SessionScope?> = mutableCurrent.asStateFlow()
    private var epoch = 0L

    suspend fun switchTo(key: SessionKey?) = lock.withLock {
        if (mutableCurrent.value?.key == key) return@withLock        // token refresh: same tenant, keep caches
        mutableCurrent.value?.scope?.close()                         // closes every scoped store
        mutableCurrent.value = null
        key ?: return@withLock                                       // logout
        val id = "session-${++epoch}"                                // EPOCH, not tenant id:
        mutableCurrent.value = SessionScope(id, key, koin.createScope(id, SESSION)) // returning never revives a cache
    }
}
```

```kotlin
scope(SESSION) {
    scoped { BoardStore(get(), get()) } withOptions { onClose { it?.close() } }
    scoped { SyncHub(get()) } withOptions { onClose { it?.close() } }   // closing Koin does NOT cancel SSE by itself
}
```

## Rules

- Every scoped store implements `AutoCloseable` and cancels **its own** `CoroutineScope` in
  `close()`; Koin's `onClose` calls it.
- The authenticated navigation tree is keyed by the session id (`key(session.id) { … }`), so a new
  session recomposes a fresh tree instead of reusing entries that resolved from a closed scope.
- The session ViewModel publishes `Loading` **before** closing the old scope, then swaps the token,
  re-reads `me()` and opens the new scope — no frame renders from a closed scope.
- A same-tenant refresh keeps the scope (closing it mid-frame breaks the tree being drawn).
- Tests: after `switchTo(otherTenant)` no store of the old scope emits, and the SSE job is cancelled.

Server-side, the old access token stays valid until `exp`; revocation is separate server work.

## Related

- A shared device (wall tablet) is **not** a session of this kind: it gets its own Koin scope that
  never raises `ClientSession` → [`shared-device-zone.md`](shared-device-zone.md).
- The live channel's "Reconnecting…" decision (grace period, `transformLatest`) lives in a scoped
  store → [`interaction-patterns.md`](interaction-patterns.md#live-channel-loss-tell-but-not-too-early).
- Offline command queues are out of scope (a seam) — same file, § Offline.
