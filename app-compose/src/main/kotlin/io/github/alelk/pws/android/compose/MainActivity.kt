package io.github.alelk.pws.android.compose

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.android.compose.flavor.flavorShowPaywall
import io.github.alelk.pws.android.compose.platform.AndroidShareActions
import io.github.alelk.pws.android.compose.platform.AndroidShellToaster
import io.github.alelk.pws.android.compose.platform.AndroidUrlActions
import io.github.alelk.pws.android.compose.platform.BackupFileActions
import io.github.alelk.pws.android.compose.platform.BundleImportActions
import io.github.alelk.pws.android.compose.platform.rememberBackupLaunchers
import io.github.alelk.pws.android.compose.platform.rememberBundleImportLauncher
import io.github.alelk.pws.android.compose.platform.rememberTelemetrySettings
import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.features.app.AppRoot
import io.github.alelk.pws.features.booklibrary.BookLibraryExternalActions
import io.github.alelk.pws.features.settings.SettingsExternalActions
import io.github.alelk.pws.features.song.detail.SongDetailExternalActions
import io.github.alelk.pws.portable.BackupService
import org.koin.android.ext.android.get

/** Shell wiring only: platform actions, telemetry consent and the pws-core [AppRoot]. */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val toaster = AndroidShellToaster(this)
    val urlActions = AndroidUrlActions(this, get(), toaster)
    val shareActions = AndroidShareActions(this, get(), toaster)
    val backupActions = BackupFileActions(this, get<BackupManager>(), BackupService(), get(), toaster)
    val bundleActions = BundleImportActions(get(), get(), toaster)
    val telemetryConsent = get<TelemetryConsentStore>()
    val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"

    // Blocked premium gates are handled by UpsellHost in AppRoot (paywall or "Pro — coming soon");
    // the shell only supplies the paywall action below, when purchases are enabled.
    setContent {
      val backup = rememberBackupLaunchers(backupActions)
      val importBundle = rememberBundleImportLauncher(bundleActions)
      val settingsActions = remember(backup) {
        SettingsExternalActions(
          openUrl = urlActions::openUrl,
          sendEmail = urlActions::sendEmail,
          exportBackup = backup.exportBackup,
          importBackup = backup.importBackup,
          // Only a build that can sell premium right now gets a paywall; otherwise a blocked gate
          // shows "Pro — coming soon" (rustore) or never fires (free builds).
          openPaywall = if (MONETIZATION.purchasesEnabled) {
            { feature -> flavorShowPaywall(this@MainActivity, feature) }
          } else {
            null
          },
        )
      }

      @OptIn(ExperimentalComposeUiApi::class)
      Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        AppRoot(
          appVersion = appVersion,
          onKeepScreenOnChanged = ::setKeepScreenOn,
          settingsExternalActions = settingsActions,
          songDetailExternalActions = remember { SongDetailExternalActions(shareText = shareActions::shareText) },
          bookLibraryExternalActions = remember(importBundle) { BookLibraryExternalActions(importBundle) },
          telemetrySettings = rememberTelemetrySettings(telemetryConsent),
        )
      }
    }
  }

  // Window FLAG_KEEP_SCREEN_ON is handled here, in the shell; AppRoot reports the preference.
  // iOS analog: UIApplication.shared.isIdleTimerDisabled
  private fun setKeepScreenOn(keepScreenOn: Boolean) {
    if (keepScreenOn) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }
}
