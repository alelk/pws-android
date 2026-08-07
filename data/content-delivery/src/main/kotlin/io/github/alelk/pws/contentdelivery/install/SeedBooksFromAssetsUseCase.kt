package io.github.alelk.pws.contentdelivery.install

import android.content.Context
import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.domain.booklibrary.model.BookInstallSource
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.portable.serialization.BundleCrypto
import io.github.alelk.pws.portable.serialization.BundleSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Imports book bundles shipped inside the APK under `assets/seed-books/` on first launch.
 *
 * Only "preloaded" build variants carry these assets: a Gradle task downloads selected bundles from
 * the catalog at build time (see app-compose `generateSeedBundles<Variant>`). Universal/clean
 * variants have no such directory, so this use case is a fast no-op for them.
 *
 * Seeded books are marked [BookInstallSource.ASSET] → non-removable (enforced by
 * [BookUninstallerImpl]) and shown as "built-in" in the library UI.
 *
 * Idempotency is keyed by asset **file name**, which embeds the bundle version
 * (`{bookId}-{variant}-{version}.book.yaml.gz.enc`). A bundle is imported once and remembered in
 * [android.content.SharedPreferences]; it is therefore not re-imported on every launch, yet an APK
 * update that ships a **newer** bundle (new version → new file name) is picked up automatically.
 * The importer itself preserves user edits and skips non-newer songs, so re-import is safe.
 */
class SeedBooksFromAssetsUseCase(
    private val context: Context,
    private val db: PwsDatabase,
    private val importer: BookImporterImpl,
    private val keyProvider: ContentKeyProvider,
    private val telemetry: Telemetry = NoOpTelemetry,
) {
    /**
     * Ensures preloaded bundles are imported.
     *
     * @return `true` if, after seeding, at least one built-in ([BookInstallSource.ASSET]) book
     *   exists — i.e. this is a preloaded build with content ready, so onboarding can be skipped.
     *   `false` for clean variants, or if seeding produced nothing (e.g. every bundle failed to
     *   decode) — in which case the normal empty-DB onboarding flow proceeds.
     */
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val names = runCatching { context.assets.list(SEED_DIR)?.toList() }
            .getOrNull().orEmpty()
            .filter { it.endsWith(BUNDLE_SUFFIX) }
        seedBundles(names) { name -> context.assets.open("$SEED_DIR/$name").use { it.readBytes() } }
    }

    /**
     * Core seeding logic, decoupled from `AssetManager` for testability. Imports each not-yet-seeded
     * bundle (by [names]) as a built-in book, reading its bytes lazily via [readBytes] only when an
     * import is actually needed. Returns whether any built-in book exists afterwards.
     */
    internal suspend fun seedBundles(names: List<String>, readBytes: (String) -> ByteArray): Boolean {
        if (names.isEmpty()) {
            // Clean variant (no seed assets). Report whether built-in content already exists — it
            // may have been seeded by a previous launch of a preloaded build.
            return db.installedBookDao().existsBySource(BookInstallSource.ASSET)
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seeded = prefs.getStringSet(KEY_SEEDED, emptySet()).orEmpty().toMutableSet()
        val key = BundleCrypto.keyFromHex(keyProvider.keyHex())

        for (name in names) {
            if (name in seeded) continue
            runCatching {
                // decodeBookAuto auto-detects encrypted vs plain-gzip — works for both release
                // (.enc, encrypted with the release key) and debug/localSeed bundles.
                val bundle = BundleSerializer.decodeBookAuto(readBytes(name), key)
                importer.import(bundle, source = BookInstallSource.ASSET)
                Timber.i("Seeded built-in book ${bundle.book.id} from asset $name")
            }.onSuccess {
                seeded += name
            }.onFailure {
                Timber.e(it, "Failed to seed built-in book from asset $name")
                // A failed seed leaves a preloaded build with no content — the user sees an empty
                // app. Worth a non-fatal even though the flow degrades gracefully.
                telemetry.recordError(
                    it,
                    "seed_book_failed",
                    mapOf(TelemetryAttr.SOURCE to "asset", TelemetryAttr.STAGE to "decode_import"),
                )
            }
        }
        prefs.edit().putStringSet(KEY_SEEDED, seeded).apply()

        return db.installedBookDao().existsBySource(BookInstallSource.ASSET)
    }

    companion object {
        private const val SEED_DIR = "seed-books"
        private const val BUNDLE_SUFFIX = ".book.yaml.gz.enc"
        private const val PREFS = "pws_seed_books"
        private const val KEY_SEEDED = "seeded_asset_names"
    }
}
