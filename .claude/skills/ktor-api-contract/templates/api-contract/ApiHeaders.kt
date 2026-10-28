package io.github.myorg.myapp.contract.common

/**
 * Header and cookie names that are part of the contract, not of one side's implementation.
 * Both `api:client` and `server:transport` read them from here so a rename is a compile
 * error on both banks (same reason `@Resource` routes live in the contract).
 */
object ApiHeaders {
    /** Correlation id; echoed back on responses and present in every error envelope. */
    const val REQUEST_ID = "X-Request-Id"

    /**
     * A surface declaring how it wants the refresh token delivered. Absent → the token
     * pair comes in the response body (the path of every non-browser client and of all
     * pre-existing tests). Value [REFRESH_CHANNEL_COOKIE] → the refresh token is set as an
     * HttpOnly cookie and omitted from the body (browsers must not be able to read it from JS).
     */
    const val REFRESH_CHANNEL = "X-Refresh-Channel"

    const val REFRESH_CHANNEL_COOKIE = "cookie"

    /** Name of the refresh cookie; scoped to [REFRESH_COOKIE_PATH] so it rides only the auth routes. */
    const val REFRESH_COOKIE_NAME = "app_refresh"

    /**
     * `/v1/auth`, not `/v1/auth/refresh`: logout must receive the cookie to revoke the session, and a
     * second refresh route (a shared device) must too. A narrower path leaves logout unable to revoke
     * (ktor-auth-sessions, references/sessions.md).
     */
    const val REFRESH_COOKIE_PATH = "/v1/auth"
}
