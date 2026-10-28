package io.github.alelk.pws.android.compose.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

private val ANY_FILE = arrayOf("application/octet-stream", "*/*")

/** Entry points of the backup flow, wired to the system file picker. */
class BackupLaunchers(val exportBackup: () -> Unit, val importBackup: () -> Unit)

/**
 * Registers the document pickers for [actions]. The name of the pending export's temp file survives
 * Activity re-creation (`rememberSaveable`), so the backup is not lost between "export" and the file choice.
 */
@Composable
fun rememberBackupLaunchers(actions: BackupFileActions): BackupLaunchers {
  val scope = rememberCoroutineScope()
  var pendingExportFile by rememberSaveable { mutableStateOf<String?>(null) }

  val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/octet-stream")) { uri: Uri? ->
    val tempFileName = pendingExportFile ?: return@rememberLauncherForActivityResult
    pendingExportFile = null
    scope.launch { actions.completeExport(tempFileName, uri) }
  }
  val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
    if (uri != null) scope.launch { actions.import(uri) }
  }

  return remember(exportLauncher, importLauncher) {
    BackupLaunchers(
      exportBackup = {
        scope.launch {
          val request = actions.prepareExport() ?: return@launch
          pendingExportFile = request.tempFileName
          exportLauncher.launch(request.suggestedFileName)
        }
      },
      importBackup = { importLauncher.launch(ANY_FILE) },
    )
  }
}

/** Returns the action that opens the picker for a book bundle and imports the chosen file. */
@Composable
fun rememberBundleImportLauncher(actions: BundleImportActions): () -> Unit {
  val scope = rememberCoroutineScope()
  val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
    if (uri != null) scope.launch { actions.import(uri) }
  }
  return remember(launcher) { { launcher.launch(ANY_FILE) } }
}
