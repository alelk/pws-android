package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.alelk.pws.features.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app-settings")
private val appThemeKey = stringPreferencesKey("app-theme")
private val songTextScaleKey = floatPreferencesKey("song-text-scale")
private val songTextExpandedKey = booleanPreferencesKey("song-text-expanded")
private val favoritesSortModeKey = stringPreferencesKey("favorites-sort-mode")
private val favoritesAscendingKey = booleanPreferencesKey("favorites-ascending")

fun Context.themeModeFlow(): Flow<ThemeMode> =
  dataStore.data.map { prefs ->
    ThemeMode.byIdentifier(prefs[appThemeKey])
  }

suspend fun Context.setThemeMode(themeMode: ThemeMode) {
  dataStore.edit { prefs ->
    prefs[appThemeKey] = themeMode.identifier
  }
}

fun Context.songTextScaleFlow(): Flow<Float> =
  dataStore.data.map { prefs ->
    prefs[songTextScaleKey] ?: 1.0f
  }

suspend fun Context.setSongTextScale(value: Float) {
  dataStore.edit { prefs ->
    prefs[songTextScaleKey] = value
  }
}

fun Context.songTextExpandedFlow(): Flow<Boolean> =
  dataStore.data.map { prefs ->
    prefs[songTextExpandedKey] ?: true
  }

suspend fun Context.setSongTextExpanded(value: Boolean) {
  dataStore.edit { prefs ->
    prefs[songTextExpandedKey] = value
  }
}

fun Context.favoritesSortModeFlow(): Flow<String> =
  dataStore.data.map { prefs ->
    prefs[favoritesSortModeKey] ?: "ADDED_DATE"
  }

suspend fun Context.setFavoritesSortMode(value: String) {
  dataStore.edit { prefs ->
    prefs[favoritesSortModeKey] = value
  }
}

fun Context.favoritesAscendingFlow(): Flow<Boolean> =
  dataStore.data.map { prefs ->
    prefs[favoritesAscendingKey] ?: false
  }

suspend fun Context.setFavoritesAscending(value: Boolean) {
  dataStore.edit { prefs ->
    prefs[favoritesAscendingKey] = value
  }
}

fun Context.appSettingsDataStore(): DataStore<Preferences> = dataStore
