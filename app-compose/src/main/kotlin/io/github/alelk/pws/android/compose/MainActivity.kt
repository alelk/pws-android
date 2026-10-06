package io.github.alelk.pws.android.compose

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.android.compose.telemetry.AppMetricaTelemetry
import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.android.compose.flavor.flavorShowPaywall
import io.github.alelk.pws.contentdelivery.install.ImportBundleFromFileUseCase
import io.github.alelk.pws.features.booklibrary.BookLibraryExternalActions
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.domain.telemetry.TelemetryEvent
import io.github.alelk.pws.domain.telemetry.TelemetryResult
import io.github.alelk.pws.features.telemetry.TelemetrySettings
import org.koin.android.ext.android.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import io.github.alelk.pws.portable.BackupService
import io.github.alelk.pws.features.app.AppRoot
import io.github.alelk.pws.features.settings.SettingsExternalActions
import io.github.alelk.pws.features.song.detail.SongDetailExternalActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

  companion object {
    /**
     * Public privacy policy, linked from Settings → Privacy and from the store listings. Must stay
     * in sync with docs/privacy-policy.md and with the Play Data Safety / RuStore declarations.
     */
    const val PRIVACY_POLICY_URL = "https://github.com/alelk/pws-android/blob/master/docs/privacy-policy.md"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    // Blocked premium gates are handled by UpsellHost in AppRoot (paywall or "Pro — coming soon");
    // the shell only supplies the paywall action below, when purchases are enabled.

    setContent {
      val context = LocalContext.current
      val backupService = remember { BackupService() }
      val backupManager = remember { get<BackupManager>() }
      val scope = rememberCoroutineScope()

      val appVersion = remember {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
      }

      val telemetry = remember { get<Telemetry>() }
      val telemetryConsent = remember { get<TelemetryConsentStore>() }
      val telemetryEnabled by telemetryConsent.enabled.collectAsState()
      val telemetryPending by telemetryConsent.pending.collectAsState()
      val applyTelemetryConsent = remember<(Boolean) -> Unit> {
        { enabled ->
          telemetryConsent.setEnabled(enabled)
          AppMetricaTelemetry.setDataSendingEnabled(enabled)
        }
      }
      val telemetrySettings = remember(telemetryEnabled, telemetryPending) {
        TelemetrySettings(
          dataSendingEnabled = telemetryEnabled,
          onDataSendingEnabledChange = applyTelemetryConsent,
          privacyPolicyUrl = PRIVACY_POLICY_URL,
          // Non-null only until the first-launch disclosure in onboarding is answered; nothing is
          // transmitted while it is. Paths that never show onboarding get this default applied by
          // pws-core's AppStartupModel once the app opens.
          pendingConsentDefault = if (telemetryPending) telemetryConsent.defaultConsent else null,
        )
      }

      var pendingBackupText by remember { mutableStateOf<String?>(null) }

      val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/octet-stream")) { uri ->
        val text = pendingBackupText ?: return@rememberLauncherForActivityResult
        pendingBackupText = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
          runCatching {
            withContext(Dispatchers.IO) {
              contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                ?: error("Cannot open output stream")
            }
          }.onSuccess {
            Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
          }.onFailure {
            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
          }
        }
      }

      val importLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
          runCatching {
            val backup = withContext(Dispatchers.IO) {
              val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("File cannot be read")
              backupService.readFromString(text)
            }
            backupManager.restoreBackup(backup)
          }.onSuccess {
            Toast.makeText(context, "Import completed", Toast.LENGTH_SHORT).show()
          }.onFailure {
            Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
          }
        }
      }

      val importBundleFromFile = remember { get<ImportBundleFromFileUseCase>() }
      val importBundleLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
          runCatching {
            importBundleFromFile.invoke(uri)
          }.onSuccess {
            telemetry.event(
              TelemetryEvent.BOOK_IMPORT,
              mapOf(TelemetryAttr.RESULT to TelemetryResult.OK, TelemetryAttr.SOURCE to "file"),
            )
            Toast.makeText(context, "Bundle imported", Toast.LENGTH_SHORT).show()
          }.onFailure {
            telemetry.event(
              TelemetryEvent.BOOK_IMPORT,
              mapOf(TelemetryAttr.RESULT to TelemetryResult.ERROR, TelemetryAttr.SOURCE to "file"),
            )
            telemetry.recordError(it, "book_import_from_file_failed")
            Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_SHORT).show()
          }
        }
      }

      val bookLibraryExternalActions = remember(importBundleLauncher) {
        BookLibraryExternalActions(
          onImportFromFile = {
            importBundleLauncher.launch(arrayOf("application/octet-stream", "*/*"))
          },
        )
      }

      val settingsExternalActions = remember(exportLauncher, importLauncher) {
        SettingsExternalActions(
          openUrl = { url ->
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
          },
          sendEmail = { mailto ->
            startActivity(Intent(Intent.ACTION_SENDTO, android.net.Uri.parse(mailto)))
          },
          exportBackup = {
            scope.launch {
              runCatching {
                val source = packageManager.getPackageInfo(packageName, 0).let { "${it.packageName}/${it.versionName}" }
                val backup = backupManager.exportBackup(source)
                pendingBackupText = backupService.writeAsString(backup)
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                exportLauncher.launch("pws_backup_$timestamp.pws")
              }.onFailure {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
              }
            }
          },
          importBackup = {
            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
          },
          // Only a build that can sell premium right now gets a paywall; otherwise a blocked gate
          // shows "Pro — coming soon" (rustore) or never fires (free builds).
          openPaywall = if (MONETIZATION.purchasesEnabled) {
            { feature -> flavorShowPaywall(this@MainActivity, feature) }
          } else {
            null
          },
        )
      }

      val songDetailExternalActions = remember {
        SongDetailExternalActions(
          shareText = { text ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, null))
          }
        )
      }

      // Window FLAG_KEEP_SCREEN_ON is handled here, in the shell; AppRoot reports the preference.
      // iOS analog: UIApplication.shared.isIdleTimerDisabled
      val onKeepScreenOnChanged = remember<(Boolean) -> Unit> {
        { keepScreenOn ->
          if (keepScreenOn) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
        }
      }

      @OptIn(ExperimentalComposeUiApi::class)
      Box(
        modifier = androidx.compose.ui.Modifier
          .fillMaxSize()
          .semantics { testTagsAsResourceId = true }
      ) {
        AppRoot(
          appVersion = appVersion,
          onKeepScreenOnChanged = onKeepScreenOnChanged,
          settingsExternalActions = settingsExternalActions,
          songDetailExternalActions = songDetailExternalActions,
          bookLibraryExternalActions = bookLibraryExternalActions,
          telemetrySettings = telemetrySettings,
        )
      }
    }
  }
}
