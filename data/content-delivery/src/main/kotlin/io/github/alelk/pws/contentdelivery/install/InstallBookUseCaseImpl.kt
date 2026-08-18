package io.github.alelk.pws.contentdelivery.install

import android.content.Context
import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.domain.booklibrary.model.BookCatalogEntry
import io.github.alelk.pws.domain.booklibrary.model.DownloadState
import io.github.alelk.pws.domain.booklibrary.usecase.InstallBookUseCase
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.portable.serialization.BundleCrypto
import io.github.alelk.pws.portable.serialization.BundleSerializer
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.readByteArray
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

class InstallBookUseCaseImpl(
  private val context: Context,
  private val importer: BookImporterImpl,
  private val keyProvider: ContentKeyProvider,
  private val httpClient: HttpClient,
  private val telemetry: Telemetry = NoOpTelemetry,
) : InstallBookUseCase {

  override fun invoke(entry: BookCatalogEntry): Flow<DownloadState> =
    channelFlow {
      val tmpFile = File(context.cacheDir, "books/${entry.bookId}.book.yaml.gz.enc.tmp")
      tmpFile.parentFile?.mkdirs()
      // Tracks which step failed, so a non-fatal says download vs checksum vs decode vs import
      // without ever carrying the bundle content itself.
      var stage = STAGE_DOWNLOAD
      try {
        send(DownloadState.Downloading(0L, entry.fileSizeBytes))

        val bytes = httpClient.prepareGet(entry.downloadUrl) {
          onDownload { downloaded, total ->
            val totalBytes = total ?: entry.fileSizeBytes
            send(DownloadState.Downloading(downloaded, totalBytes))
          }
        }.execute { response ->
          response.bodyAsChannel().readRemaining().readByteArray()
        }

        tmpFile.writeBytes(bytes)
        stage = STAGE_VERIFY
        verifyChecksum(bytes, entry.checksum)

        stage = STAGE_DECODE
        val key = BundleCrypto.keyFromHex(keyProvider.keyHex())
        val bundle = BundleSerializer.decodeBookGzipEncrypted(bytes, key)
        stage = STAGE_IMPORT
        importer.import(bundle)
        send(DownloadState.Done)
      } catch (e: Exception) {
        Timber.e(e, "Install failed for ${entry.bookId}")
        telemetry.recordError(
          e,
          "book_install_failed",
          mapOf(TelemetryAttr.BOOK_ID to entry.bookId.toString(), TelemetryAttr.STAGE to stage),
        )
        send(DownloadState.Error(e.message ?: "Unknown error"))
      } finally {
        tmpFile.delete()
      }
    }.catch { e ->
      Timber.e(e, "Unexpected error during install of ${entry.bookId}")
      telemetry.recordError(
        e,
        "book_install_unexpected_error",
        mapOf(TelemetryAttr.BOOK_ID to entry.bookId.toString()),
      )
      emit(DownloadState.Error(e.message ?: "Unknown error"))
    }.flowOn(Dispatchers.IO)

  private companion object {
    const val STAGE_DOWNLOAD = "download"
    const val STAGE_VERIFY = "verify_checksum"
    const val STAGE_DECODE = "decode"
    const val STAGE_IMPORT = "import"
  }

  private fun verifyChecksum(bytes: ByteArray, expected: String) {
    val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
      .joinToString("") { "%02x".format(it) }
    check(actual == expected) { "Checksum mismatch: expected $expected, got $actual" }
  }
}
