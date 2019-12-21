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

package com.alelk.pws.pwapp.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alelk.pws.pwapp.R;

/**
 * Psalm Header Fragment
 *
 * Created by Alex Elkin on 11.05.2016.
 */
public class PsalmHeaderFragment extends Fragment {

    public static final String KEY_PSALM_NAME = "com.alelk.pws.pwapp.psalmName";
    public static final String KEY_BOOK_NAME = "com.alelk.pws.pwapp.bookName";
    public static final String KEY_BIBLE_REF = "com.alelk.pws.pwapp.bibleRef";

    private String mPsalmName;
    private String mBookName;
    private String mBibleRef;
    private TextView vPsalmName;
    private TextView vBookName;
    private TextView vBibleRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View v = inflater.inflate(R.layout.fragment_psalm_header, null);
        vPsalmName = v.findViewById(R.id.txt_psalm_name);
        vBookName = v.findViewById(R.id.txt_book_name);
        vBibleRef = v.findViewById(R.id.txt_bible_ref);
        updateUi();
        return v;
    }

    public void updateUi(String psalmName, String bookName, String bibleRef) {
        mPsalmName = psalmName;
        mBookName = bookName;
        mBibleRef = bibleRef;
        updateUi();
    }

    private void updateUi() {
        vPsalmName.setText(mPsalmName == null ? "" : mPsalmName);
        vBookName.setText(mBookName == null ? "" : mBookName);
        vBibleRef.setText(mBibleRef == null ? "" : mBibleRef);
    }

    public static PsalmHeaderFragment newInstance(String psalmName, String bookName, String bibleRef) {
        Bundle args = new Bundle();
        args.putString(KEY_PSALM_NAME, psalmName);
        args.putString(KEY_BOOK_NAME, bookName);
        args.putString(KEY_BIBLE_REF, bibleRef);
        PsalmHeaderFragment fragment = new PsalmHeaderFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
