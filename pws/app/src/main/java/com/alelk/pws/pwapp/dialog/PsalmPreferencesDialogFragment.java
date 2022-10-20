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

package com.alelk.pws.pwapp.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;

import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.preference.PsalmPreferences;

/**
 * Psalm Preferences Dialog Fragment
 *
 * Created by Alex Elkin on 26.12.2016.
 */

public class PsalmPreferencesDialogFragment extends DialogFragment {

    public interface OnPsalmPreferencesChangedCallbacks {
        void onPreferencesChanged(PsalmPreferences preferences);
        void onApplyPreferences(PsalmPreferences preferences);
        void onCancelPreferences(PsalmPreferences previousPreferences);
    }
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_EXPANDED_PSALM_TEXT = "text_is_expanded";
    private static final float MIN_TEXT_SIZE = 10;
    private static final float MAX_TEXT_SIZE = 100;
    private final static String LOG_TAG = PsalmPreferencesDialogFragment.class.getSimpleName();
    private View mLayout;
    private OnPsalmPreferencesChangedCallbacks mCallbacks;

    private PsalmPreferences mDefaultPreferences;
    private PsalmPreferences mChangedPreferences;

    public static PsalmPreferencesDialogFragment newInstance(PsalmPreferences preferences) {
        Bundle args = new Bundle();
        args.putFloat(KEY_TEXT_SIZE, preferences.getTextSize());
        args.putBoolean(KEY_EXPANDED_PSALM_TEXT, preferences.isExpandPsalmText());
        PsalmPreferencesDialogFragment dialogFragment = new PsalmPreferencesDialogFragment();
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() == null) return;
        mLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_psalm_preferences, null);
        SeekBar skBrTextSize = mLayout.findViewById(R.id.seek_bar_font_size);
        Switch switchIsPsalmTextExpanded = mLayout.findViewById(R.id.swtch_expand_psalm_text);
        switchIsPsalmTextExpanded.setChecked(mDefaultPreferences.isExpandPsalmText());
        skBrTextSize.setProgress((int) ((MAX_TEXT_SIZE - MIN_TEXT_SIZE)/100 * (mDefaultPreferences.getTextSize() - MIN_TEXT_SIZE)));
        switchIsPsalmTextExpanded.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mChangedPreferences.setExpandPsalmText(isChecked);
            mCallbacks.onPreferencesChanged(mChangedPreferences);
        });
        skBrTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mChangedPreferences.setTextSize(i/(MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100 + MIN_TEXT_SIZE);
                mCallbacks.onPreferencesChanged(mChangedPreferences);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(mLayout);
        builder.setPositiveButton(R.string.lbl_ok, (dialog, which) -> mCallbacks.onApplyPreferences(mChangedPreferences));
        builder.setNegativeButton(R.string.lbl_cancel, (dialog, which) -> mCallbacks.onCancelPreferences(mDefaultPreferences));
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        final String METHOD_NAME = "onAttach";
        super.onAttach(context);
        try {
            mCallbacks = (OnPsalmPreferencesChangedCallbacks) context;
            init();
        } catch (ClassCastException ex) {
            final String message = context.toString() + " must implement " + OnPsalmPreferencesChangedCallbacks.class.getCanonicalName();
            Log.e(LOG_TAG, METHOD_NAME + ": " + message);
            throw new ClassCastException(message);
        }
    }

    private void init() {
        if (getArguments() != null) {
            mDefaultPreferences = new PsalmPreferences(
                    getArguments().getFloat(KEY_TEXT_SIZE, 15),
                    getArguments().getBoolean(KEY_EXPANDED_PSALM_TEXT, false)
            );
            mChangedPreferences = new PsalmPreferences(mDefaultPreferences.getTextSize(), mDefaultPreferences.isExpandPsalmText());
        }
    }
}
