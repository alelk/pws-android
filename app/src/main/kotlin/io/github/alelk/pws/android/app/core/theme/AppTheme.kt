package io.github.alelk.pws.android.app.core.theme

import android.content.Context
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.core.theme.ThemeType

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