package io.github.myorg.myapp.client

import io.github.myorg.myapp.contract.common.ApiHeaders
import io.github.myorg.myapp.contract.common.ApiJson
import io.github.myorg.myapp.contract.identity.RefreshRequest
import io.github.myorg.myapp.contract.identity.TokenPairDto
import io.github.myorg.myapp.contract.route.AuthRefresh
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Builds the one `HttpClient` the whole app uses.
 *
 * Auto-refresh is the stock `Auth`/`bearer` plugin rather than a hand-rolled interceptor:
 * single-flight (parallel 401s wait for one refresh), replay of the original request and
 * recursion protection (`markAsRefreshTokenRequest`) already live there. What stays ours is
 * the *policy* on failure — clear the store and tell the UI the session is gone.
 */
@OptIn(ExperimentalUuidApi::class)
fun createHttpClient(
    engine: HttpClientEngine,
    config: ApiClientConfig,
    tokens: TokenStore,
    sessionEvents: SessionEvents,
): HttpClient =
    HttpClient(engine) {
        // Errors are values here (Either), so Ktor must not throw on non-2xx.
        expectSuccess = false

        install(ContentNegotiation) { json(ApiJson) }
        install(Resources)
        // The live channel (SyncClient) streams over this same client — so the Auth plugin
        // supplies Authorization and refreshes on a 401 the reconnect hits (the native
        // EventSource can't send headers, which is the whole reason for a Ktor-client SSE).
        install(SSE) {
            // Comment-only events are dropped by default, and the server's liveness signal IS
            // a comment (`: keepalive`, every 15 s). Without this the client cannot tell a
            // healthy quiet channel from a dead socket — the stream simply never says anything.
            // `SyncClient` ignores their content; what it uses is that they arrived.
            showCommentEvents()
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
        }

        install(DefaultRequest) {
            url.takeFrom(config.baseUrl)
            // Correlation id travels with every request so a UI failure can be found in the
            // server log. The server must retrieveFromHeader(REQUEST_ID) for this to work; it then
            // echoes it in X-Request-Id and in every error body. DefaultRequest runs per request.
            headers.append(ApiHeaders.REQUEST_ID, Uuid.generateV7().toString())
            if (config.refreshChannel == RefreshChannel.Cookie) {
                headers.append(ApiHeaders.REFRESH_CHANNEL, ApiHeaders.REFRESH_CHANNEL_COOKIE)
            }
        }

        install(Auth) {
            bearer {
                loadTokens { tokens.bearerTokens() }
                refreshTokens { performRefresh(config, tokens, sessionEvents) }
            }
        }
    }

/**
 * Runs the rotation and applies our failure policy. Returning `null` tells the plugin the
 * request cannot be retried; the original 401 then reaches the caller as
 * [ApiError.Unauthorized] while the UI has already been told the session expired.
 */
@Suppress("ReturnCount") // the early exits (no token, failed rotate, bad body) each mean "expire"
private suspend fun io.ktor.client.plugins.auth.providers.RefreshTokensParams.performRefresh(
    config: ApiClientConfig,
    tokens: TokenStore,
    sessionEvents: SessionEvents,
): BearerTokens? {
    val presented = tokens.refreshToken()
    // Body channel with nothing to present: there is no session to rotate.
    if (config.refreshChannel == RefreshChannel.Body && presented.isNullOrBlank()) {
        return expire(tokens, sessionEvents)
    }
    val response: HttpResponse =
        client.post(AuthRefresh()) {
            markAsRefreshTokenRequest()
            if (presented != null) {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(presented))
            }
        }
    if (!response.status.isSuccess()) return expire(tokens, sessionEvents)
    val pair =
        try {
            response.body<TokenPairDto>()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return expire(tokens, sessionEvents)
    tokens.update(pair.accessToken, pair.refreshToken)
    return tokens.bearerTokens()
}

private fun expire(
    tokens: TokenStore,
    sessionEvents: SessionEvents,
): BearerTokens? {
    tokens.clear()
    sessionEvents.publish(SessionEvent.Expired)
    return null
}

/**
 * The plugin's token holder. In the cookie channel there is no refresh value to hand over —
 * an empty string is the honest representation: the plugin never uses it, our
 * [performRefresh] reads the store, and the server reads the cookie.
 */
private fun TokenStore.bearerTokens(): BearerTokens? =
    accessToken()?.let { access -> BearerTokens(access, refreshToken() ?: "") }
