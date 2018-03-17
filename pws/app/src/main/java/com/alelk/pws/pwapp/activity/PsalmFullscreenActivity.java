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

package com.alelk.pws.pwapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity;
import com.alelk.pws.pwapp.adapter.PsalmTextFragmentStatePagerAdapter;
import com.alelk.pws.pwapp.fragment.PsalmTextFragment;
import com.alelk.pws.pwapp.holder.PsalmHolder;
import com.alelk.pws.pwapp.preference.PsalmPreferences;

import java.util.ArrayList;

public class PsalmFullscreenActivity extends AppCompatThemedActivity implements PsalmTextFragment.Callbacks {

    public static final String KEY_PSALM_NUMBER_ID = PsalmTextFragment.KEY_PSALM_NUMBER_ID;

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int ADD_TO_HISTORY_DELAY = 5000;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = this::hide;
    private Handler mAddToHistoryHandler = new Handler();
    private Runnable mAddToHistoryRunnable = new Runnable() {
        @Override
        public void run() {
            if (mFragmentStatePagerAdapter == null || mPagerPsalmText == null) return;
            final PsalmTextFragment fragment = (PsalmTextFragment) mFragmentStatePagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
            fragment.addPsalmToHistory();
        }
    };

    private long mPsalmNumberId;
    private Button mBtnFavorites;
    private ArrayList<Long> mBookPsalmNumberIds;
    private ViewPager mPagerPsalmText;
    private PsalmTextFragmentStatePagerAdapter mFragmentStatePagerAdapter;
    private PsalmPreferences mPsalmPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_psalm_fullscreen);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.pager_psalm_text);
        mBtnFavorites = findViewById(R.id.btn_favorites);
        mBtnFavorites.setOnClickListener(v -> {
            PsalmTextFragment fragment = (PsalmTextFragment) mFragmentStatePagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
            if (fragment.isFavoritePsalm()) {
                fragment.removePsalmFromFavorites();
            } else {
                fragment.addPsalmToFavorites();
            }
        });
        mBtnFavorites.setOnTouchListener((v, event) -> {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            if (MotionEvent.ACTION_UP == event.getAction())
                v.performClick();
            return true;
        });

        mPagerPsalmText = findViewById(R.id.pager_psalm_text);
        float psalmTextSize = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, -1);
        mFragmentStatePagerAdapter = new PsalmTextFragmentStatePagerAdapter(getSupportFragmentManager(), mBookPsalmNumberIds, mPsalmPreferences);
        mPagerPsalmText.setAdapter(mFragmentStatePagerAdapter);
        mPagerPsalmText.setCurrentItem(mBookPsalmNumberIds.indexOf(mPsalmNumberId));
    }

    private void init() {
        mPsalmNumberId = getIntent().getLongExtra(KEY_PSALM_NUMBER_ID, -10);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mPsalmPreferences = new PsalmPreferences(
                preferences.getFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, -1),
                preferences.getBoolean(PsalmTextFragment.KEY_PSALM_TEXT_EXPANDED, true)
        );
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri(mPsalmNumberId), null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final String[] psalmNumberIdsList = cursor.getString(
                        cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBERID_LIST))
                        .split(",");
                mBookPsalmNumberIds = new ArrayList<>(psalmNumberIdsList.length);
                for (String id : psalmNumberIdsList) {
                    try {
                        mBookPsalmNumberIds.add(Long.parseLong(id));
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        hide();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onUpdatePsalmInfo(PsalmHolder psalmHolder) {
        if (psalmHolder == null ||
                mBookPsalmNumberIds.get(mPagerPsalmText.getCurrentItem()) != psalmHolder.getPsalmNumberId()) return;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("№ " + psalmHolder.getPsalmNumber() + " " + psalmHolder.getBookName());
        }
        if (psalmHolder.isFavoritePsalm()) {
            mBtnFavorites.setText(R.string.lbl_remove_from_favorites);
        } else {
            mBtnFavorites.setText(R.string.lbl_add_to_favorites);
        }
        mPsalmNumberId = psalmHolder.getPsalmNumberId();
        Intent intent = new Intent();
        intent.putExtra(KEY_PSALM_NUMBER_ID, mPsalmNumberId);
        setResult(RESULT_OK, intent);
        mAddToHistoryHandler.removeCallbacks(mAddToHistoryRunnable);
        mAddToHistoryHandler.postDelayed(mAddToHistoryRunnable, ADD_TO_HISTORY_DELAY);
    }

    @Override
    public void onRequestFullscreenMode() {
        toggle();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
