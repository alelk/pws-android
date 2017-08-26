package com.alelk.pws.pwapp.activity.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.alelk.pws.pwapp.theme.AppTheme;
import com.alelk.pws.pwapp.theme.OnThemeChangeListener;
import com.alelk.pws.pwapp.theme.ThemePreferences;
import com.alelk.pws.pwapp.theme.ThemeType;

/**
 * AppCompat Themed Activity
 *
 * Created by Alex Elkin on 24.08.17.
 */

public class AppCompatThemedActivity extends AppCompatActivity {

    private ThemePreferences mThemePreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemePreferences = new ThemePreferences(this);
        setTheme(mThemePreferences.getAppTheme().getThemeResId(getThemeType()));
        mThemePreferences.registerThemeChangeListener(onThemeChange);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThemePreferences.unregisterThemeChangeListener(onThemeChange);
    }

    private void restartActivity() {
        Intent intent = getIntent();
        intent.removeCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(intent);
        finish();
    }

    private final OnThemeChangeListener onThemeChange = new OnThemeChangeListener() {
        @Override
        public void onThemeChange(AppTheme appTheme) {
            restartActivity();
        }
    };

    protected ThemeType getThemeType() {
        return ThemeType.NORMAL;
    }

}
