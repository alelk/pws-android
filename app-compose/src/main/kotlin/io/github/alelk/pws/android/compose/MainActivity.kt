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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import io.github.alelk.pws.portable.BackupService
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.features.app.AppRoot
import io.github.alelk.pws.features.settings.SettingsExternalActions
import io.github.alelk.pws.features.song.detail.FavoritesDisplaySettings
import io.github.alelk.pws.features.song.detail.SongDetailDisplaySettings
import io.github.alelk.pws.features.song.detail.SongDetailExternalActions
import io.github.alelk.pws.features.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    lifecycleScope.launch(Dispatchers.IO) {
      PwsBackupAgent.applyPendingRestoreIfNeeded(
        this@MainActivity,
        PwsDatabaseProvider.getDatabase(this@MainActivity),
        appSettingsDataStore(),
      )
    }
    setContent {
      val context = LocalContext.current
      val backupService = remember { BackupService() }
      val backupManager = remember {
        BackupManager(
          db = PwsDatabaseProvider.getDatabase(context),
          dataStore = context.appSettingsDataStore(),
        )
      }
      val scope = rememberCoroutineScope()

      val appVersion = remember {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
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

      val themeMode by applicationContext.themeModeFlow().collectAsState(initial = ThemeMode.DEFAULT)
      val songTextScale by applicationContext.songTextScaleFlow().collectAsState(initial = 1.0f)
      val songTextExpanded by applicationContext.songTextExpandedFlow().collectAsState(initial = true)
      val favoritesSortMode by applicationContext.favoritesSortModeFlow().collectAsState(initial = "ADDED_DATE")
      val favoritesAscending by applicationContext.favoritesAscendingFlow().collectAsState(initial = false)
      val useDynamicColor by applicationContext.useDynamicColorFlow().collectAsState(initial = false)
      val keepScreenOn by applicationContext.keepScreenOnFlow().collectAsState(initial = false)
      val songLineHeightMultiplier by applicationContext.songLineHeightMultiplierFlow().collectAsState(initial = 1.0f)

      // Window FLAG_KEEP_SCREEN_ON — обработка флага здесь, в shell.
      // iOS-analog: UIApplication.shared.isIdleTimerDisabled
      androidx.compose.runtime.DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
          window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
      }

      val songDetailDisplaySettings = remember(songTextScale, songTextExpanded, songLineHeightMultiplier) {
        SongDetailDisplaySettings(
          fontScale = songTextScale,
          expandedText = songTextExpanded,
          onFontScaleChange = { newScale ->
            lifecycleScope.launch {
              applicationContext.setSongTextScale(newScale)
            }
          },
          onExpandedTextChange = { expanded ->
            lifecycleScope.launch {
              applicationContext.setSongTextExpanded(expanded)
            }
          },
          lineHeightMultiplier = songLineHeightMultiplier,
          onLineHeightMultiplierChange = { multiplier ->
            lifecycleScope.launch {
              applicationContext.setSongLineHeightMultiplier(multiplier)
            }
          }
        )
      }

      val favoritesDisplaySettings = remember(favoritesSortMode, favoritesAscending) {
        FavoritesDisplaySettings(
          sortMode = favoritesSortMode,
          ascending = favoritesAscending,
          onSortModeChange = { newMode ->
            lifecycleScope.launch {
              applicationContext.setFavoritesSortMode(newMode)
            }
          },
          onAscendingChange = { ascending ->
            lifecycleScope.launch {
              applicationContext.setFavoritesAscending(ascending)
            }
          }
        )
      }

      @OptIn(ExperimentalComposeUiApi::class)
      Box(
        modifier = androidx.compose.ui.Modifier
          .fillMaxSize()
          .semantics { testTagsAsResourceId = true }
      ) {
        AppRoot(
          themeMode = themeMode,
          appVersion = appVersion,
          onThemeModeChange = { newMode ->
            lifecycleScope.launch {
              applicationContext.setThemeMode(newMode)
            }
          },
          useDynamicColor = useDynamicColor,
          onUseDynamicColorChange = { enabled ->
            lifecycleScope.launch {
              applicationContext.setUseDynamicColor(enabled)
            }
          },
          keepScreenOn = keepScreenOn,
          onKeepScreenOnChange = { enabled ->
            lifecycleScope.launch {
              applicationContext.setKeepScreenOn(enabled)
            }
          },
          settingsExternalActions = settingsExternalActions,
          songDetailExternalActions = songDetailExternalActions,
          songDetailDisplaySettings = songDetailDisplaySettings,
          favoritesDisplaySettings = favoritesDisplaySettings,
        )
      }
    }
  }
}
