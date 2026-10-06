package io.github.alelk.pws.android.compose.platform

import android.net.Uri
import io.github.alelk.pws.android.compose.R
import io.github.alelk.pws.contentdelivery.install.ImportBundleFromFileUseCase
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.domain.telemetry.TelemetryEvent
import io.github.alelk.pws.domain.telemetry.TelemetryResult

/** Imports a book bundle chosen by the user (a file picked through the Activity Result API). */
class BundleImportActions internal constructor(
  private val importBundleFromFile: ImportBundleFromFileUseCase,
  private val telemetry: Telemetry,
  private val toaster: ShellToaster,
) {
  suspend fun import(uri: Uri) {
    runCatching {
      importBundleFromFile.invoke(uri)
    }.onSuccess {
      telemetry.event(
        TelemetryEvent.BOOK_IMPORT,
        mapOf(TelemetryAttr.RESULT to TelemetryResult.OK, TelemetryAttr.SOURCE to "file"),
      )
      toaster.show(R.string.bundle_imported)
    }.onFailure {
      telemetry.event(
        TelemetryEvent.BOOK_IMPORT,
        mapOf(TelemetryAttr.RESULT to TelemetryResult.ERROR, TelemetryAttr.SOURCE to "file"),
      )
      telemetry.recordError(it, "book_import_from_file_failed")
      toaster.show(R.string.bundle_import_failed)
    }
  }
}
