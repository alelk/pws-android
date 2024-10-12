package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alelk.pws.pwapp.dataStore
import com.alelk.pws.pwapp.model.AppPreferenceKeys.APP_THEME
import com.alelk.pws.pwapp.model.AppPreferenceKeys.SONG_TEXT_EXPANDED
import com.alelk.pws.pwapp.model.AppPreferenceKeys.SONG_TEXT_SIZE
import com.alelk.pws.pwapp.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

object AppPreferenceKeys {
  val SONG_TEXT_SIZE = floatPreferencesKey("song-text-size")
  val SONG_TEXT_EXPANDED = booleanPreferencesKey("song-text-expanded")
  val APP_THEME = stringPreferencesKey("app-theme")
}

class AppPreferencesViewModel(application: Application) : AndroidViewModel(application) {

  private val datastore = application.dataStore

  val songTextSize: Flow<Float?> = datastore.data.map { it[SONG_TEXT_SIZE] }
  val songTextExpanded: Flow<Boolean> = datastore.data.map { it[SONG_TEXT_EXPANDED] ?: true }
  val appTheme: StateFlow<AppTheme> =
    datastore.data.map { it[APP_THEME]?.let(AppTheme::byIdentifier) ?: AppTheme.DEFAUL }
      .distinctUntilChanged()
      .let { prefs ->
        prefs.stateIn(viewModelScope, SharingStarted.Eagerly, runBlocking { prefs.first() })
      }


  suspend fun setSongTextSize(value: Float) {
    datastore.edit { it[SONG_TEXT_SIZE] = value }
  }

  suspend fun setSongTextExpanded(value: Boolean) {
    datastore.edit { it[SONG_TEXT_EXPANDED] = value }
  }

  suspend fun setAppTheme(value: AppTheme) {
    datastore.edit { it[APP_THEME] = value.identifier }
  }
}