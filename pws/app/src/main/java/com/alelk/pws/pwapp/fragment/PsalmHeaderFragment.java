package com.alelk.pws.pwapp.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
