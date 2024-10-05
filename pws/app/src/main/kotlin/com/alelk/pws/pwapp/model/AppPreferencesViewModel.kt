package com.alelk.pws.pwapp.model

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.AndroidViewModel
import com.alelk.pws.pwapp.dataStore
import com.alelk.pws.pwapp.model.AppPreferenceKeys.DEFAULT_SONG_TEXT_EXPANDED
import com.alelk.pws.pwapp.model.AppPreferenceKeys.SONG_TEXT_EXPANDED
import com.alelk.pws.pwapp.model.AppPreferenceKeys.SONG_TEXT_SIZE
import kotlinx.coroutines.flow.map

object AppPreferenceKeys {
  val SONG_TEXT_SIZE = floatPreferencesKey("song-text-size")
  val SONG_TEXT_EXPANDED = booleanPreferencesKey("song-text-expanded")
  const val DEFAULT_SONG_TEXT_EXPANDED = true
}

class AppPreferencesViewModel(application: Application) : AndroidViewModel(application) {

  private val datastore = application.dataStore

  val songTextSize = datastore.data.map { it[SONG_TEXT_SIZE] }
  val songTextExpanded = datastore.data.map { it[SONG_TEXT_EXPANDED] ?: DEFAULT_SONG_TEXT_EXPANDED }


  suspend fun setSongTextSize(value: Float) {
    datastore.edit { it[SONG_TEXT_SIZE] = value }
  }

  suspend fun setSongTextExpanded(value: Boolean) {
    datastore.edit { it[SONG_TEXT_EXPANDED] = value }
  }

}