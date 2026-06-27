package io.github.alelk.pws.android.compose

import android.content.Context
import io.github.alelk.pws.features.booklibrary.BookLibraryFirstLaunchState

class BookLibraryFirstLaunchStateImpl(context: Context) : BookLibraryFirstLaunchState {
    private val prefs = context.getSharedPreferences("pws_book_library", Context.MODE_PRIVATE)
    private val key = "shown_for_version_${BuildConfig.VERSION_CODE}"

    override suspend fun shouldShow(): Boolean = !prefs.getBoolean(key, false)

    override fun markShown() {
        prefs.edit().putBoolean(key, true).apply()
    }
}
