# Typed HTTP Client (`api:client`)

## Shape

- **One interface per API area, one per file, no default method bodies** (`TaskApi`, `BoardApi`).
  Screens/ViewModels depend on the interface; tests fake it in a few lines.
- Every method returns `Either<ApiError, T>` — errors are values. On a canvas web target an
  uncaught exception kills the whole scene; a returned error costs nothing.
- Lists return `CursorPage<T>`, never `PageDto` or `List`.
- Filters are typed values (`TaskListFilter`), not query strings.
- `api:client` does not see `domain`: it speaks DTOs; `features` maps them via `api:mapping`.

## The one HttpClient

```kotlin
fun createHttpClient(engine: HttpClientEngine, config: ApiClientConfig, tokens: TokenStore, session: SessionEvents) =
    HttpClient(engine) {
        expectSuccess = false                               // non-2xx is a value, not an exception
        install(ContentNegotiation) { json(ApiJson) }
        install(Resources)
        install(SSE) { showCommentEvents() }                 // keepalive comments are the liveness signal
        install(HttpTimeout) { requestTimeoutMillis = config.requestTimeoutMillis }
        install(DefaultRequest) {
            url.takeFrom(config.baseUrl)
            headers.append(ApiHeaders.REQUEST_ID, Uuid.generateV7().toString())   // per request
        }
        install(Auth) {
            bearer {
                loadTokens { tokens.bearerTokens() }
                refreshTokens { refresh(config, tokens, session) }   // single-flight, replays the call
            }
        }
    }
```

Use the stock `Auth`/`bearer` plugin rather than a hand-rolled interceptor: single-flight refresh
(parallel 401s wait for one refresh), replay of the original request and recursion protection
(`markAsRefreshTokenRequest()`) are already there. What stays yours is the **policy on failure**:
clear the store, emit `SessionEvent.Expired`.

## One call wrapper

```kotlin
internal suspend inline fun <reified T> apiCall(block: () -> HttpResponse): Either<ApiError, T> =
    try {
        val response = block()
        if (response.status.isSuccess()) response.body<T>().right() else response.toApiError().left()
    } catch (e: CancellationException) { throw e }
    catch (e: Throwable) { e.toTransportError().left() }     // timeout / offline / malformed
```

`toApiError()` is the **only** place that knows HTTP statuses and error codes
(template: [`../templates/api-client/ApiError.kt`](../templates/api-client/ApiError.kt)).
When reading the error body, don't wrap it in `runCatching` — it swallows cancellation; catch
`SerializationException`/`ContentConvertException` explicitly.

## Rules

- Never hardcode URLs — `@Resource` classes.
- Engine per platform lives in `api:client:di` (CIO on JVM, Js on wasm).
- The browser refresh token travels in an HttpOnly cookie on a dedicated path; the access token
  lives in memory only (see [`ktor-auth-sessions`](../../ktor-auth-sessions/SKILL.md)).
- Client tests use `ktor-client-mock`: each status/code → the expected `ApiError`, refresh on 401,
  no refresh loop on a failed refresh.
