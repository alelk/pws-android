package io.github.myorg.myapp.client

import io.github.myorg.myapp.contract.error.ApiErrorCodes
import io.github.myorg.myapp.contract.error.ValidationReasons

/**
 * The machine-readable part of a non-2xx response. `message` is for logs, never for the UI —
 * the UI localises by [code] and tells one refusal from another by [reason] (compared with
 * `ValidationReasons` constants). A fitness test keeps `info.message` out of the screens.
 */
data class ApiErrorInfo(
    val code: String,
    val message: String,
    val correlationId: String,
    val details: Map<String, String> = emptyMap(),
) {
    /** `details.reason`: which wall this is, when a screen needs to know. */
    val reason: String? get() = details[ValidationReasons.DETAILS_KEY]

    companion object {
        fun unparsable(status: Int) = ApiErrorInfo("UNKNOWN", "unparsable error body (HTTP $status)", "")
    }
}

enum class ConflictKind { StaleVersion, AlreadyExists }

/**
 * Every way a call can fail, as a value. The only place that knows HTTP statuses is the mapping
 * below; everything above branches on this type.
 */
sealed interface ApiError {
    sealed interface Protocol : ApiError { val info: ApiErrorInfo }

    data class BadRequest(override val info: ApiErrorInfo) : Protocol
    data class Unauthorized(override val info: ApiErrorInfo) : Protocol
    data class Forbidden(override val info: ApiErrorInfo) : Protocol
    data class NoActiveTenant(override val info: ApiErrorInfo) : Protocol
    data class NotFound(override val info: ApiErrorInfo) : Protocol
    data class Conflict(val kind: ConflictKind, override val info: ApiErrorInfo) : Protocol
    data class Gone(override val info: ApiErrorInfo) : Protocol
    data class Validation(override val info: ApiErrorInfo) : Protocol
    data class RateLimited(val retryAfterSeconds: Long?, override val info: ApiErrorInfo) : Protocol
    data class Server(val status: Int, override val info: ApiErrorInfo) : Protocol

    /** Transport failures — no envelope. */
    data object Timeout : ApiError
    data object Offline : ApiError
    data class Malformed(val detail: String?) : ApiError
}

// --- the single status/code → ApiError mapping ------------------------------------------------

internal suspend fun io.ktor.client.statement.HttpResponse.toApiError(): ApiError {
    val info =
        try {
            io.ktor.client.call.body<io.github.myorg.myapp.contract.error.ApiErrorDto>(this).let {
                ApiErrorInfo(it.code, it.message, it.correlationId, it.details)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {        // not runCatching: it would swallow cancellation
            ApiErrorInfo.unparsable(status.value)
        }
    return when (status.value) {
        400 -> ApiError.BadRequest(info)
        401 -> ApiError.Unauthorized(info)
        403 -> if (info.code == ApiErrorCodes.NO_ACTIVE_TENANT) ApiError.NoActiveTenant(info) else ApiError.Forbidden(info)
        404 -> ApiError.NotFound(info)
        409 -> ApiError.Conflict(if (info.code == ApiErrorCodes.CONFLICT) ConflictKind.StaleVersion else ConflictKind.AlreadyExists, info)
        410 -> ApiError.Gone(info)
        422 -> ApiError.Validation(info)
        429 -> ApiError.RateLimited(headers["Retry-After"]?.trim()?.toLongOrNull(), info)
        else -> ApiError.Server(status.value, info)
    }
}
