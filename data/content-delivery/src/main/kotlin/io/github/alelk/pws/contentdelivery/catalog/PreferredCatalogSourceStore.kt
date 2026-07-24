package io.github.alelk.pws.contentdelivery.catalog

/**
 * Persists which catalog source last served a successful fetch, so it can be tried first next time.
 *
 * The stored value is the source's `catalogUrl` (a stable identity from BuildConfig). When it no
 * longer matches any configured source — e.g. after a catalog major-version bump — the repository
 * treats the situation as a first launch and re-races all sources.
 */
interface PreferredCatalogSourceStore {
    fun get(): String?
    fun set(catalogUrl: String)

    /** In-memory no-op used by tests and as a safe default. */
    object NoOp : PreferredCatalogSourceStore {
        override fun get(): String? = null
        override fun set(catalogUrl: String) = Unit
    }
}
