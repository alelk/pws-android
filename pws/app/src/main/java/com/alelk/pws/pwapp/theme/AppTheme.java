package com.alelk.pws.pwapp.theme;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;

import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity;

/**
 * App Theme
 *
 * Created by Alex Elkin on 24.08.17.
 */

public enum AppTheme {

    LIGHT(R.style.Theme_Light,
            R.style.Theme_Light_NoActionBar,
            R.string.pref_display_theme_light_value,
            R.string.pref_display_theme_light_title
    ),
    DARK(R.style.Theme_Dark,
            R.style.Theme_Dark_NoActionBar,
            R.string.pref_display_theme_dark_value,
            R.string.pref_display_theme_dark_title
    ),
    BLACK(R.style.Theme_Black,
            R.style.Theme_Black_NoActionBar,
            R.string.pref_display_theme_black_value,
            R.string.pref_display_theme_black_title
    );

    @StyleRes private final int themeNoActionBarResId;
    @StyleRes private final int themeResId;
    @StringRes private final int themeKeyResId;
    @StringRes private final int themeNameResId;

    AppTheme(@StyleRes int themeResId,
             @StyleRes int themeNoActionBarResId,
             @StringRes int themeKeyResId,
             @StringRes int themeNameResId) {
        this.themeResId = themeResId;
        this.themeNoActionBarResId = themeNoActionBarResId;
        this.themeKeyResId = themeKeyResId;
        this.themeNameResId = themeNameResId;
    }

    public int getThemeResId() {
        return themeResId;
    }

    public int getThemeResId(ThemeType themeType) {
        return ThemeType.NO_ACTION_BAR.equals(themeType) ? themeNoActionBarResId : themeResId;
    }

    public int getThemeNoActionBarResId() {
        return themeNoActionBarResId;
    }

    public int getThemeKeyResId() {
        return themeKeyResId;
    }

    public int getThemeNameResId() {
        return themeNameResId;
    }

    public static AppTheme forThemeKeyResId(@StringRes int themeKeyResId) {
        for (AppTheme appTheme : values())
            if (appTheme.themeKeyResId == themeKeyResId) return appTheme;
        return LIGHT;
    }

    public static AppTheme forThemeKey(Context context, String themeKey) {
        for (AppTheme appTheme : values())
            if (context.getString(appTheme.themeKeyResId).equals(themeKey)) return appTheme;
        throw new IllegalArgumentException("No theme found for theme key '" + themeKey + '\'');
    }
}
