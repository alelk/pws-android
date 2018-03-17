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

package com.alelk.pws.pwapp.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Theme Preferences
 *
 * Created by Alex Elkin on 24.08.17.
 */

public class ThemePreferences {

    private static final String KEY_APP_THEME = "com.alelk.pws.pwapp.appTheme";
    private static final int DEFAULT_THEME_KEY_RES_ID = AppTheme.LIGHT.getThemeKeyResId();

    private final SharedPreferences mPreferences;
    private final Map<OnThemeChangeListener, SharedPreferences.OnSharedPreferenceChangeListener> mPreferenceMap = new LinkedHashMap<>();

    public ThemePreferences(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void registerThemeChangeListener(final OnThemeChangeListener listener) {
        final SharedPreferences.OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> {
            if (KEY_APP_THEME.equals(key))
                listener.onThemeChange(AppTheme.forThemeKeyResId(sharedPreferences.getInt(KEY_APP_THEME, DEFAULT_THEME_KEY_RES_ID)));
        };
        mPreferences.registerOnSharedPreferenceChangeListener(changeListener);
        mPreferenceMap.put(listener, changeListener);
    }

    public void unregisterThemeChangeListener(final OnThemeChangeListener listener) {
        if (!mPreferenceMap.containsKey(listener)) return;
        mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferenceMap.get(listener));
        mPreferenceMap.remove(listener);
    }

    public AppTheme getAppTheme() {
        return AppTheme.forThemeKeyResId(mPreferences.getInt(KEY_APP_THEME, DEFAULT_THEME_KEY_RES_ID));
    }

    public void persistAppTheme(AppTheme appTheme) {
        mPreferences.edit().putInt(KEY_APP_THEME, appTheme.getThemeKeyResId()).apply();
    }
}
