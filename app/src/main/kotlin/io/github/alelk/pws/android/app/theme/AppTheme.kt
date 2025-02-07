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
package io.github.alelk.pws.android.app.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.alelk.pws.pwapp.R

/**
 * App Theme
 *
 * Created by Alex Elkin on 24.08.17.
 */
enum class AppTheme(
  @field:StyleRes @param:StyleRes val themeResId: Int,
  @field:StyleRes @param:StyleRes val themeNoActionBarResId: Int,
  @field:StringRes @param:StringRes val themeKeyResId: Int,
  @field:StringRes @param:StringRes val themeNameResId: Int,
  val identifier: String
) {
  LIGHT(R.style.Theme_Light, R.style.Theme_Light_NoActionBar, R.string.pref_display_theme_light_value, R.string.pref_display_theme_light_title, "light"),
  DARK(R.style.Theme_Dark, R.style.Theme_Dark_NoActionBar, R.string.pref_display_theme_dark_value, R.string.pref_display_theme_dark_title, "dark"),
  BLACK(R.style.Theme_Black, R.style.Theme_Black_NoActionBar, R.string.pref_display_theme_black_value, R.string.pref_display_theme_black_title, "black");

  fun getThemeResId(themeType: ThemeType): Int = if (ThemeType.NO_ACTION_BAR == themeType) themeNoActionBarResId else themeResId

  companion object {
    fun byKey(context: Context, themeKey: String): AppTheme? = entries.find { theme -> context.getString(theme.themeKeyResId) == themeKey }
    fun byIdentifier(identifier: String): AppTheme? = entries.find { it.identifier == identifier }
    val DEFAULT = LIGHT
  }
}