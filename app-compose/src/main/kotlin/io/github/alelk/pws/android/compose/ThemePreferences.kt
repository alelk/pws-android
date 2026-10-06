package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * The app settings DataStore and the names of its keys (G3: never rename). The values are read and
 * written through `UserPreferencesRepository` ([DataStoreUserPreferencesRepository]); `app-theme` is also
 * read and written by the backup ([DataStoreBackupSettings]) and the legacy settings importer.
 */
private val Context.dataStore by preferencesDataStore(name = "app-settings")
internal val appThemeKey = stringPreferencesKey("app-theme")
internal val songTextScaleKey = floatPreferencesKey("song-text-scale")
internal val songTextExpandedKey = booleanPreferencesKey("song-text-expanded")
internal val favoritesSortModeKey = stringPreferencesKey("favorites-sort-mode")
internal val favoritesAscendingKey = booleanPreferencesKey("favorites-ascending")
internal val useDynamicColorKey = booleanPreferencesKey("use-dynamic-color")
internal val keepScreenOnKey = booleanPreferencesKey("keep-screen-on")
internal val songLineHeightMultiplierKey = floatPreferencesKey("song-line-height-multiplier")
internal val songSerifFontKey = booleanPreferencesKey("song-serif-font")
internal val showSongNavButtonsKey = booleanPreferencesKey("show-song-nav-buttons")

fun Context.appSettingsDataStore(): DataStore<Preferences> = dataStore
