package io.github.alelk.pws.contentdelivery.install

import android.content.Context
import android.net.Uri
import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.portable.serialization.BundleCrypto
import io.github.alelk.pws.portable.serialization.BundleSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImportBundleFromFileUseCase(
    private val context: Context,
    private val importer: BookImporterImpl,
    private val keyProvider: ContentKeyProvider,
    private val telemetry: Telemetry = NoOpTelemetry,
) {
    suspend fun invoke(uri: Uri) = withContext(Dispatchers.IO) {
        Timber.i("Importing bundle from file: $uri")
        // The stage tells apart "unreadable file" from "bad bundle" from "import failed". The Uri is
        // never reported — a document path can contain the user's name.
        var stage = STAGE_READ
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Cannot open file: $uri")
            stage = STAGE_DECODE
            val key = BundleCrypto.keyFromHex(keyProvider.keyHex())
            // decodeBookAuto auto-detects encrypted vs plain-gzip — works for both prod and localSeed
            val bundle = BundleSerializer.decodeBookAuto(bytes, key)
            stage = STAGE_IMPORT
            importer.import(bundle)
            Timber.i("Bundle imported from file: ${bundle.book.id}")
        } catch (e: Exception) {
            telemetry.recordError(e, "book_import_from_file_failed", mapOf(TelemetryAttr.STAGE to stage))
            throw e
        }
    }

    private companion object {
        const val STAGE_READ = "read_file"
        const val STAGE_DECODE = "decode"
        const val STAGE_IMPORT = "import"
    }
}
