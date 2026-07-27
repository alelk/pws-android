package io.github.alelk.pws.contentdelivery.catalog

import android.content.SharedPreferences

private const val KEY_PREFERRED_SOURCE = "preferred_catalog_source_url"

/**
 * SharedPreferences-backed [PreferredCatalogSourceStore] — a single scalar string, no schema.
 *
 * Instantiate with a dedicated preferences file: `"pws_catalog_source"`.
 */
class SharedPrefsPreferredCatalogSourceStore(
    private val prefs: SharedPreferences,
) : PreferredCatalogSourceStore {

    override fun get(): String? = prefs.getString(KEY_PREFERRED_SOURCE, null)

    override fun set(catalogUrl: String) {
        prefs.edit().putString(KEY_PREFERRED_SOURCE, catalogUrl).apply()
    }
}
