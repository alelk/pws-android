package io.github.alelk.pws.android.compose.payment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.alelk.pws.android.compose.appThemeKey
import io.github.alelk.pws.android.compose.songTextExpandedKey
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.features.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * One-time import of the RuStore fork's display settings into the new `app-settings` store, so the
 * user's theme does not "reset" on the update (plan 2026-09-29, T-A05).
 *
 * - `app-theme` → `app-theme` (the fork's ids `light` / `dark` / `black` are the [ThemeMode] ids);
 * - `song-text-expanded` → `song-text-expanded`;
 * - `song-text-size` is skipped: the fork stored an absolute size, the new app a scale factor.
 *
 * Runs once (marker `legacy_settings_imported` in `app-settings`), so a theme the user changes in the
 * new app is never overwritten. The legacy file is only read, never written (invariant I8).
 */
class LegacySettingsImporter(
  private val legacyPreferences: Flow<Preferences>,
  private val appSettings: DataStore<Preferences>,
  private val telemetry: Telemetry = NoOpTelemetry,
) {
  suspend fun importOnce() {
    if (appSettings.data.first()[IMPORTED] == true) return
    // A legacy file that cannot be read is simply not imported (retried on the next start).
    val legacy = runCatching { legacyPreferences.first() }
      .onFailure { e -> telemetry.recordError(e, "legacy_settings_read_failed") }
      .getOrNull() ?: return
    val theme = legacy[LEGACY_APP_THEME]?.takeIf { id -> ThemeMode.entries.any { it.identifier == id } }
    val expanded = legacy[LEGACY_SONG_TEXT_EXPANDED]
    appSettings.edit { prefs ->
      if (prefs[IMPORTED] == true) return@edit
      if (theme != null && prefs[appThemeKey] == null) prefs[appThemeKey] = theme
      if (expanded != null && prefs[songTextExpandedKey] == null) prefs[songTextExpandedKey] = expanded
      prefs[IMPORTED] = true
    }
  }

  private companion object {
    val IMPORTED = booleanPreferencesKey("legacy_settings_imported")
    val LEGACY_APP_THEME = stringPreferencesKey("app-theme")
    val LEGACY_SONG_TEXT_EXPANDED = booleanPreferencesKey("song-text-expanded")
  }
}
