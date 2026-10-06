package io.github.alelk.pws.android.compose.platform

import android.content.Context
import android.net.Uri
import io.github.alelk.pws.android.compose.BackupManager
import io.github.alelk.pws.android.compose.R
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.portable.BackupService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup export / import through the system file picker.
 *
 * Export is two-phase: [prepareExport] writes the backup text to a temporary file in the cache
 * directory, the picker returns a target [Uri], [completeExport] copies the file there and deletes it.
 * Only the temp file's name travels through the Activity's saved state, so a recreated Activity between
 * the two phases does not lose the backup (the text used to live in a `remember`).
 */
class BackupFileActions internal constructor(
  private val cacheDir: File,
  private val streams: ContentStreams,
  private val operations: Operations,
  private val telemetry: Telemetry,
  private val toaster: ShellToaster,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  constructor(
    context: Context,
    backupManager: BackupManager,
    backupService: BackupService,
    telemetry: Telemetry,
    toaster: ShellToaster = AndroidShellToaster(context),
  ) : this(
    cacheDir = context.cacheDir,
    streams = ContentStreams(
      openInput = { context.contentResolver.openInputStream(it) },
      openOutput = { context.contentResolver.openOutputStream(it) },
    ),
    operations = Operations(
      exportText = {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val backup = backupManager.exportBackup("${info.packageName}/${info.versionName}")
        withContext(Dispatchers.IO) { backupService.writeAsString(backup) }
      },
      restoreFromText = { text ->
        // Parsing a large backup is CPU work: never on the caller's (main) thread.
        val backup = withContext(Dispatchers.IO) { backupService.readFromString(text) }
        backupManager.restoreBackup(backup)
      },
    ),
    telemetry = telemetry,
    toaster = toaster,
  )

  /** Opens streams of the document the user picked. */
  internal class ContentStreams(val openInput: (Uri) -> InputStream?, val openOutput: (Uri) -> OutputStream?)

  internal class Operations(val exportText: suspend () -> String, val restoreFromText: suspend (String) -> Unit)

  /** [tempFileName] is what to hand back to [completeExport]; [suggestedFileName] goes to the picker. */
  class ExportRequest(val tempFileName: String, val suggestedFileName: String)

  /** Builds the backup into a temp file. Null (and a message to the user) when it could not be built. */
  suspend fun prepareExport(): ExportRequest? = runCatching {
    val text = operations.exportText()
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
    val tempName = "$TEMP_PREFIX$timestamp$TEMP_SUFFIX"
    withContext(ioDispatcher) { File(cacheDir, tempName).writeText(text) }
    ExportRequest(tempName, "pws_backup_$timestamp.pws")
  }.onFailure {
    telemetry.recordError(it, "backup_export_failed")
    toaster.show(R.string.backup_export_failed)
  }.getOrNull()

  /** Copies the temp file to [target] and deletes it; a null [target] (picker cancelled) only deletes it. */
  suspend fun completeExport(tempFileName: String, target: Uri?) {
    val file = tempFile(tempFileName) ?: return
    try {
      if (target == null) return
      runCatching {
        withContext(ioDispatcher) {
          val output = streams.openOutput(target) ?: error("Cannot open output stream")
          output.use { out -> file.inputStream().use { it.copyTo(out) } }
        }
      }.onSuccess {
        toaster.show(R.string.backup_saved)
      }.onFailure {
        telemetry.recordError(it, "backup_export_failed")
        toaster.show(R.string.backup_export_failed)
      }
    } finally {
      file.delete()
    }
  }

  suspend fun import(uri: Uri) {
    runCatching {
      val text = withContext(ioDispatcher) {
        streams.openInput(uri)?.bufferedReader()?.use { it.readText() } ?: error("File cannot be read")
      }
      operations.restoreFromText(text)
    }.onSuccess {
      toaster.show(R.string.backup_import_done)
    }.onFailure {
      telemetry.recordError(it, "backup_import_failed")
      toaster.show(R.string.backup_import_failed)
    }
  }

  /** The name comes from saved state: accept only a plain file name of our own temp files. */
  private fun tempFile(name: String): File? = File(cacheDir, name).takeIf {
    name.startsWith(TEMP_PREFIX) && name.endsWith(TEMP_SUFFIX) && File(name).name == name
  }

  private companion object {
    const val TEMP_PREFIX = "pws_backup_export_"
    const val TEMP_SUFFIX = ".tmp"
  }
}
