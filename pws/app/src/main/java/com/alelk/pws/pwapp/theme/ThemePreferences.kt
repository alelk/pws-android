/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alelk.pws.pwapp.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import com.alelk.pws.pwapp.theme.AppTheme.Companion.forThemeKeyResId

/**
 * Theme Preferences
 *
 * Created by Alex Elkin on 24.08.17.
 */
class ThemePreferences(context: Context?) {
  private val mPreferences: SharedPreferences
  private val mPreferenceMap: MutableMap<(AppTheme) -> Unit, OnSharedPreferenceChangeListener> =
    LinkedHashMap()

  init {
    mPreferences = PreferenceManager.getDefaultSharedPreferences(context)
  }

  fun registerThemeChangeListener(listener: (AppTheme) -> Unit) {
    val changeListener =
      OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String? ->
        if (KEY_APP_THEME == key) listener(
          forThemeKeyResId(
            sharedPreferences.getInt(
              KEY_APP_THEME,
              DEFAULT_THEME_KEY_RES_ID
            )
          )
        )
      }
    mPreferences.registerOnSharedPreferenceChangeListener(changeListener)
    mPreferenceMap[listener] = changeListener
  }

  fun unregisterThemeChangeListener(listener: (AppTheme) -> Unit) {
    if (!mPreferenceMap.containsKey(listener)) return
    mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferenceMap[listener])
    mPreferenceMap.remove(listener)
  }

  val appTheme: AppTheme
    get() = forThemeKeyResId(mPreferences.getInt(KEY_APP_THEME, DEFAULT_THEME_KEY_RES_ID))

  fun persistAppTheme(appTheme: AppTheme) {
    mPreferences.edit().putInt(KEY_APP_THEME, appTheme.themeKeyResId).apply()
  }

  companion object {
    private const val KEY_APP_THEME = "com.alelk.pws.pwapp.appTheme"
    private val DEFAULT_THEME_KEY_RES_ID = AppTheme.LIGHT.themeKeyResId
  }
}