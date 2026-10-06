package io.github.alelk.pws.android.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
 * Key names (declared in `ThemePreferences.kt`), value formats and defaults are those the app has always
 * used (G3): users' stored settings are read with them. `app-theme` is also read and written by
 * [BackupManager].
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
    themeMode = ThemeMode.byIdentifier(this[appThemeKey]),
    useDynamicColor = this[useDynamicColorKey] ?: defaults.useDynamicColor,
    keepScreenOn = this[keepScreenOnKey] ?: defaults.keepScreenOn,
    songText = SongTextPreferences(
      scale = this[songTextScaleKey] ?: defaults.songText.scale,
      expanded = this[songTextExpandedKey] ?: defaults.songText.expanded,
      lineHeightMultiplier = this[songLineHeightMultiplierKey] ?: defaults.songText.lineHeightMultiplier,
      serifFont = this[songSerifFontKey] ?: defaults.songText.serifFont,
      showNavButtons = this[showSongNavButtonsKey] ?: defaults.songText.showNavButtons,
    ),
    favorites = FavoritesPreferences(
      sortMode = FavoritesSortMode.byIdentifier(this[favoritesSortModeKey]),
      ascending = this[favoritesAscendingKey] ?: defaults.favorites.ascending,
    ),
  )

  /**
   * Writes only the keys whose value changed, as the old per-setting setters did: untouched settings
   * stay absent (and keep following the defaults) and the backup exports exactly what the user set.
   */
  private fun MutablePreferences.writeChanged(old: AppPreferences, new: AppPreferences) {
    if (new.themeMode != old.themeMode) this[appThemeKey] = new.themeMode.identifier
    if (new.useDynamicColor != old.useDynamicColor) this[useDynamicColorKey] = new.useDynamicColor
    if (new.keepScreenOn != old.keepScreenOn) this[keepScreenOnKey] = new.keepScreenOn
    if (new.songText.scale != old.songText.scale) this[songTextScaleKey] = new.songText.scale
    if (new.songText.expanded != old.songText.expanded) this[songTextExpandedKey] = new.songText.expanded
    if (new.songText.lineHeightMultiplier != old.songText.lineHeightMultiplier) {
      this[songLineHeightMultiplierKey] = new.songText.lineHeightMultiplier
    }
    if (new.songText.serifFont != old.songText.serifFont) this[songSerifFontKey] = new.songText.serifFont
    if (new.songText.showNavButtons != old.songText.showNavButtons) {
      this[showSongNavButtonsKey] = new.songText.showNavButtons
    }
    if (new.favorites.sortMode != old.favorites.sortMode) this[favoritesSortModeKey] = new.favorites.sortMode.identifier
    if (new.favorites.ascending != old.favorites.ascending) this[favoritesAscendingKey] = new.favorites.ascending
  }
}
