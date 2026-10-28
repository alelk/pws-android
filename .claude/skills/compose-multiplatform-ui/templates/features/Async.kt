package io.github.myorg.myapp.features.common

import io.github.myorg.myapp.client.ApiError

/**
 * The three fields every screen state shares — and only those. No generic `UiState<T>` wrapper:
 * screen states differ; what they have in common is exactly this. One name per concept across the
 * module (`busy`, never `submitting`/`saving`).
 */
interface Async {
    /** Loading what the screen was opened for. Drawn as a loader. */
    val loading: Boolean

    /** An action the person started is in flight. Drawn as a disabled button, not a loader. */
    val busy: Boolean get() = false

    val error: ApiError?
}
