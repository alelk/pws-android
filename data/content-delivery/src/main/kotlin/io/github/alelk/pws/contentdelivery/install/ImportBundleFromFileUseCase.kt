package io.github.alelk.pws.contentdelivery.install

import android.content.Context
import android.net.Uri
import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.portable.serialization.BundleCrypto
import io.github.alelk.pws.portable.serialization.BundleSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ImportBundleFromFileUseCase(
    private val context: Context,
    private val importer: BookImporterImpl,
    private val keyProvider: ContentKeyProvider,
) {
    suspend fun invoke(uri: Uri) = withContext(Dispatchers.IO) {
        Timber.i("Importing bundle from file: $uri")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot open file: $uri")
        val key = BundleCrypto.keyFromHex(keyProvider.keyHex())
        // decodeBookAuto auto-detects encrypted vs plain-gzip — works for both prod and localSeed
        val bundle = BundleSerializer.decodeBookAuto(bytes, key)
        importer.import(bundle)
        Timber.i("Bundle imported from file: ${bundle.book.id}")
    }
}
