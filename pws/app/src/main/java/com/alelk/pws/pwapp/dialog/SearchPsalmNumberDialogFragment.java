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

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.R;

import java.util.HashMap;

/**
 * Search Psalm Number Dialog Fragment
 *
 * Created by Alex Elkin on 12.06.2016.
 */
public class SearchPsalmNumberDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_CURRENT_PSALM_NUMBER_ID = "com.alelk.pws.pwapp.dialog.currentPsalmNumberId";
    public final static int PWS_PSALM_NUMBER_LOADER = 20;
    private static final String LOG_TAG = SearchPsalmNumberDialogFragment.class.getSimpleName();

    public interface SearchPsalmNumberDialogListener {
        void onPositiveButtonClick(long psalmNumberId);
        void onNegativeButtonClick();
    }

    private long mCurrentPsalmNumberId;
    private long mPsalmNumberId = -1;
    private int mMinNumber;
    private int mMaxNumber;
    private HashMap<Integer, Long> mPsalmNumberIdMap;
    private SearchPsalmNumberDialogListener mListener;
    private EditText mTxtPsalmNumber;
    private View mView;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case PWS_PSALM_NUMBER_LOADER:
                return new CursorLoader(getActivity().getBaseContext(),
                        PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri(mCurrentPsalmNumberId),
                        PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.PROJECTION,
                        null, null, null);
            default:
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            mMinNumber = cursor.getInt(cursor.getColumnIndex(
                    PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_MIN_PSALMNUMBER));
            mMaxNumber = cursor.getInt(cursor.getColumnIndex(
                    PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_MAX_PSALMNUMBER));
            final String[] psalmNumberList = cursor.getString(
                    cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBER_LIST))
                    .split(",");
            final String[] psalmNumberIdList = cursor.getString(
                    cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBERID_LIST))
                    .split(",");
            mPsalmNumberIdMap = new HashMap<>(psalmNumberList.length);
            for (int i = 0; i < psalmNumberList.length; i++) {
                try {
                    mPsalmNumberIdMap.put(Integer.parseInt(psalmNumberList[i]), Long.parseLong(psalmNumberIdList[i]));
                } catch (NumberFormatException ex) {
                    Log.d(LOG_TAG, "onLoadFinished: cannot parse int (long) value: " + psalmNumberList[i] + " " + Long.parseLong(psalmNumberIdList[i]));
                }
            }
        }
        updateUi();
    }

    private void updateUi() {
        if (mMinNumber > 0 && mMaxNumber > 0 && mTxtPsalmNumber != null) {
            mTxtPsalmNumber.setHint(mMinNumber + " - " + mMaxNumber);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public static SearchPsalmNumberDialogFragment newInstance(long currentPsalmNumberId) {
        Bundle args = new Bundle();
        args.putLong(KEY_CURRENT_PSALM_NUMBER_ID, currentPsalmNumberId);
        SearchPsalmNumberDialogFragment dialog = new SearchPsalmNumberDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        final String METHOD_NAME = "onAttach";
        super.onAttach(context);
        try {
            mListener = (SearchPsalmNumberDialogListener) context;
            init();
        } catch (ClassCastException ex) {
            final String message = context.toString() + " must implement " + SearchPsalmNumberDialogListener.class.getCanonicalName();
            Log.e(LOG_TAG, METHOD_NAME + ": " + message);
            throw new ClassCastException(message);
        }
    }

    private void init() {
        if (getArguments() != null) {
            mCurrentPsalmNumberId = getArguments().getLong(KEY_CURRENT_PSALM_NUMBER_ID);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = getActivity().getLayoutInflater().inflate(R.layout.dialog_search_psalm_number, null);
        mTxtPsalmNumber = mView.findViewById(R.id.edittxt_psalm_number);
        mTxtPsalmNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int number = Integer.parseInt(s.toString());
                    mPsalmNumberId = mPsalmNumberIdMap.get(number) == null ? -1 : mPsalmNumberIdMap.get(number);
                } catch (NumberFormatException ex) {
                    mPsalmNumberId = -1;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getLoaderManager().initLoader(PWS_PSALM_NUMBER_LOADER, null, this);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(mView);
        builder.setPositiveButton(R.string.lbl_ok, (dialog, which) -> {
            if (mPsalmNumberId == -1) {
                Snackbar.make(mView, R.string.msg_no_psalm_number_found, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                dialog.dismiss();
                return;
            }
            mListener.onPositiveButtonClick(mPsalmNumberId);
        });
        builder.setNegativeButton(R.string.lbl_cancel, (dialog, which) -> mListener.onNegativeButtonClick());
        return builder.create();
    }
}
