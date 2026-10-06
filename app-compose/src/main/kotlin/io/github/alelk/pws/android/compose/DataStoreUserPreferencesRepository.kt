package io.github.alelk.pws.android.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.alelk.pws.domain.preferences.model.AppPreferences
import io.github.alelk.pws.domain.preferences.model.FavoritesPreferences
import io.github.alelk.pws.domain.preferences.model.FavoritesSortMode
import io.github.alelk.pws.domain.preferences.model.SongTextPreferences
import io.github.alelk.pws.domain.preferences.model.ThemeMode
import io.github.alelk.pws.domain.preferences.repository.UserPreferencesRepository
import io.github.alelk.pws.features.song.detail.PlatformDefaultShowSongNavButtons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [UserPreferencesRepository] over the app settings DataStore — the single instance from
 * [appSettingsDataStore] (registered in Koin); never open a second DataStore on the same file.
 *
 * Key names, value formats and defaults are those of `ThemePreferences.kt` (G3): users' stored settings
 * are read with them. `app-theme` is also read and written by [BackupManager].
 */
class DataStoreUserPreferencesRepository(private val dataStore: DataStore<Preferences>) : UserPreferencesRepository {
  // Mobile default: songs are paged by swipe, so the header arrows stay hidden unless enabled.
  private val defaults = AppPreferences.defaults(showNavButtons = PlatformDefaultShowSongNavButtons)

  override val preferences: Flow<AppPreferences> =
    dataStore.data.map { it.toAppPreferences() }.distinctUntilChanged()

  override suspend fun update(transform: (AppPreferences) -> AppPreferences) {
    dataStore.edit { prefs ->
      val current = prefs.toAppPreferences()
      prefs.writeChanged(current, transform(current))
    }
  }

  private fun Preferences.toAppPreferences(): AppPreferences = AppPreferences(
    themeMode = ThemeMode.byIdentifier(this[THEME]),
    useDynamicColor = this[USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
    keepScreenOn = this[KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
    songText = SongTextPreferences(
      scale = this[SONG_TEXT_SCALE] ?: defaults.songText.scale,
      expanded = this[SONG_TEXT_EXPANDED] ?: defaults.songText.expanded,
      lineHeightMultiplier = this[SONG_LINE_HEIGHT_MULTIPLIER] ?: defaults.songText.lineHeightMultiplier,
      serifFont = this[SONG_SERIF_FONT] ?: defaults.songText.serifFont,
      showNavButtons = this[SHOW_SONG_NAV_BUTTONS] ?: defaults.songText.showNavButtons,
    ),
    favorites = FavoritesPreferences(
      sortMode = FavoritesSortMode.byIdentifier(this[FAVORITES_SORT_MODE]),
      ascending = this[FAVORITES_ASCENDING] ?: defaults.favorites.ascending,
    ),
  )

  /**
   * Writes only the keys whose value changed, as the old per-setting setters did: untouched settings
   * stay absent (and keep following the defaults) and the backup exports exactly what the user set.
   */
  private fun MutablePreferences.writeChanged(old: AppPreferences, new: AppPreferences) {
    if (new.themeMode != old.themeMode) this[THEME] = new.themeMode.identifier
    if (new.useDynamicColor != old.useDynamicColor) this[USE_DYNAMIC_COLOR] = new.useDynamicColor
    if (new.keepScreenOn != old.keepScreenOn) this[KEEP_SCREEN_ON] = new.keepScreenOn
    if (new.songText.scale != old.songText.scale) this[SONG_TEXT_SCALE] = new.songText.scale
    if (new.songText.expanded != old.songText.expanded) this[SONG_TEXT_EXPANDED] = new.songText.expanded
    if (new.songText.lineHeightMultiplier != old.songText.lineHeightMultiplier) {
      this[SONG_LINE_HEIGHT_MULTIPLIER] = new.songText.lineHeightMultiplier
    }
    if (new.songText.serifFont != old.songText.serifFont) this[SONG_SERIF_FONT] = new.songText.serifFont
    if (new.songText.showNavButtons != old.songText.showNavButtons) {
      this[SHOW_SONG_NAV_BUTTONS] = new.songText.showNavButtons
    }
    if (new.favorites.sortMode != old.favorites.sortMode) this[FAVORITES_SORT_MODE] = new.favorites.sortMode.identifier
    if (new.favorites.ascending != old.favorites.ascending) this[FAVORITES_ASCENDING] = new.favorites.ascending
  }

  private companion object {
    val THEME = stringPreferencesKey("app-theme")
    val SONG_TEXT_SCALE = floatPreferencesKey("song-text-scale")
    val SONG_TEXT_EXPANDED = booleanPreferencesKey("song-text-expanded")
    val FAVORITES_SORT_MODE = stringPreferencesKey("favorites-sort-mode")
    val FAVORITES_ASCENDING = booleanPreferencesKey("favorites-ascending")
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use-dynamic-color")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep-screen-on")
    val SONG_LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("song-line-height-multiplier")
    val SONG_SERIF_FONT = booleanPreferencesKey("song-serif-font")
    val SHOW_SONG_NAV_BUTTONS = booleanPreferencesKey("show-song-nav-buttons")
  }
}
