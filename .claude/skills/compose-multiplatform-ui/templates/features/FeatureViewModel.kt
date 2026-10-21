package io.github.myorg.myapp.features.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import io.github.myorg.myapp.client.ApiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one "raise flag → call → fold Either → lower flag" block. Written by hand per call it has one
 * recurring bug: the failure branch forgets to lower the flag and the screen spins forever even
 * though the error arrived. [startedRequest]/[finishedRequest] are the `copy` over the three
 * [Async] fields that the interface itself cannot do.
 */
abstract class FeatureViewModel<S : Async> : ViewModel() {
    protected abstract val mutableState: MutableStateFlow<S>

    /** Raises `busy` (a person's action) or `loading` (the screen's load) — a one-line `copy`. */
    protected abstract fun S.startedRequest(busy: Boolean): S

    /** Lowers the same flag and records the outcome — a one-line `copy`. */
    protected abstract fun S.finishedRequest(
        busy: Boolean,
        error: ApiError?,
    ): S

    /**
     * Runs [call] with the flag raised and lowers it on BOTH outcomes. [onSuccess] gets the state
     * (flag already lowered) and the value. Override [onFailure] only where a failure is not a
     * plain `error` (a 401 on sign-in is "wrong credentials") — it receives the state BEFORE the
     * flag is lowered and must lower it via [finishedRequest]. Returns the [Job] so a caller can
     * cancel an earlier, still running attempt (search as you type).
     */
    protected fun <T> launchRequest(
        busy: Boolean = false,
        call: suspend () -> Either<ApiError, T>,
        onFailure: (S, ApiError) -> S = { s, error -> s.finishedRequest(busy, error) },
        onSuccess: (S, T) -> S,
    ): Job =
        viewModelScope.launch {
            mutableState.update { it.startedRequest(busy) }
            call().fold(
                { failure -> mutableState.update { onFailure(it, failure) } },
                { value -> mutableState.update { onSuccess(it.finishedRequest(busy, null), value) } },
            )
        }
}
