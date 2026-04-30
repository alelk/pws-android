package io.github.alelk.pws.android.compose

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import io.github.alelk.pws.backup.BackupService
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
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
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

      val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
          runCatching {
            val source = packageManager.getPackageInfo(packageName, 0).let { "${it.packageName}/${it.versionName}" }
            val backup = backupManager.exportBackup(source)
            val text = backupService.writeAsString(backup)
            withContext(Dispatchers.IO) {
              contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
            }
          }.onSuccess {
            Toast.makeText(context, "Export completed", Toast.LENGTH_SHORT).show()
          }.onFailure {
            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
          }
        }
      }

      val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
            val currentTime = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
            exportLauncher.launch("pws_backup_$currentTime.pws")
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

      val songDetailDisplaySettings = remember(songTextScale, songTextExpanded) {
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

      AppRoot(
        themeMode = themeMode,
        appVersion = appVersion,
        onThemeModeChange = { newMode ->
          lifecycleScope.launch {
            applicationContext.setThemeMode(newMode)
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
