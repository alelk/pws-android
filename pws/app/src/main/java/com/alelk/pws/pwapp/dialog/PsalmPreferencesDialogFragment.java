package com.alelk.pws.pwapp.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.alelk.pws.pwapp.R;

/**
 * Created by Alex Elkin on 26.12.2016.
 */

public class PsalmPreferencesDialogFragment extends DialogFragment {

    public interface OnPsalmPreferencesChangedCallbacks {
        void onPsalmTextSizeChanged(float textSize);
        void onApplyPsalmPreferences(float textSize);
        void onCancelPsalmPreferences(float previousTextSize);
    }
    public static final String KEY_TEXT_SIZE = "text_size";
    private static final float MIN_TEXT_SIZE = 10;
    private static final float MAX_TEXT_SIZE = 100;
    private final static String LOG_TAG = PsalmPreferencesDialogFragment.class.getSimpleName();
    private View mLayout;
    private SeekBar mSkBrTextSize;
    private OnPsalmPreferencesChangedCallbacks mCallbacks;
    private float mTextSizeDefault;
    private float mChangedTextSize;

    public static PsalmPreferencesDialogFragment newInstance(float currentTextSize) {
        Bundle args = new Bundle();
        args.putFloat(KEY_TEXT_SIZE, currentTextSize);
        PsalmPreferencesDialogFragment dialogFragment = new PsalmPreferencesDialogFragment();
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_psalm_preferences, null);
        mSkBrTextSize = (SeekBar) mLayout.findViewById(R.id.seek_bar_font_size);
        mSkBrTextSize.setProgress((int) ((MAX_TEXT_SIZE - MIN_TEXT_SIZE)/100 * (mTextSizeDefault - MIN_TEXT_SIZE)));
        mSkBrTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mChangedTextSize = i/(MAX_TEXT_SIZE - MIN_TEXT_SIZE) * 100 + MIN_TEXT_SIZE;
                mCallbacks.onPsalmTextSizeChanged(mChangedTextSize);
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
        builder.setPositiveButton(R.string.lbl_ok, (dialogInterface, i) -> mCallbacks.onApplyPsalmPreferences(mChangedTextSize));
        builder.setNegativeButton(R.string.lbl_cancel, (dialogInterface, i) -> mCallbacks.onCancelPsalmPreferences(mTextSizeDefault));
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
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
            mTextSizeDefault = getArguments().getFloat(KEY_TEXT_SIZE, 15);
            mChangedTextSize = mTextSizeDefault;
        }
    }
}
