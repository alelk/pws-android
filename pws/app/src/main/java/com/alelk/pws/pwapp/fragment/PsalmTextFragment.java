package com.alelk.pws.pwapp.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.TextView;

import com.alelk.pws.database.data.Tonality;
import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.util.PwsPsalmUtil;
import com.alelk.pws.pwapp.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Alex Elkin on 18.04.2015.
 *
 * The Activity who are the host of this fragment should implement Callbacks interface.
 */
public class PsalmTextFragment extends Fragment {

    public static final String KEY_PSALM_NUMBER_ID = "com.alelk.pws.pwapp.psalmNumberId";

    private static final String SELECTION_FAVORITES_PSALM_NUMBER_MATCH = PwsDataProvider.Favorites.COLUMN_PSALMNUMBER_ID + " = ?";
    private final String[] SELECTION_ARGS = new String[1];
    private final ContentValues CONTENT_VALUES_FAVORITES = new ContentValues(1);
    private final ContentValues CONTENT_VALUES_HISTORY = new ContentValues(1);
    private Callbacks callbacks;
    private CardView cvTonalities;
    private CardView cvPsalmInfo;
    private TextView vPsalmText;
    private TextView vPsalmInfo;
    private TextView vPsalmTonalities;
    private long mPsalmNumberId = -1;
    private int mPsalmNumber;
    private String mPsalmName;
    private String mPsalmText;
    private String mPsalmAuthor;
    private String mPsalmTranslator;
    private String mPsalmComposer;
    private String mPsalmLocale;
    private String[] mPsalmTonalities;
    private String mBibleRef;
    private String mBookName;
    private boolean isFavoritePsalm;

    public PsalmTextFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View v = inflater.inflate(R.layout.fragment_psalm_text, null);
        cvTonalities = (CardView) v.findViewById(R.id.cv_tonalities);
        cvPsalmInfo = (CardView) v.findViewById(R.id.cv_psalm_info);
        vPsalmText = (TextView) v.findViewById(R.id.txt_psalm_text);
        vPsalmInfo = (TextView) v.findViewById(R.id.txt_psalm_info);
        vPsalmTonalities = (TextView) v.findViewById(R.id.txt_psalm_tonalities);
        updateUi();
        return v;
    }

    private void init() {
        if (mPsalmNumberId < 0) return;
        SELECTION_ARGS[0] = String.valueOf(mPsalmNumberId);
        CONTENT_VALUES_FAVORITES.put(PwsFavoritesTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
        CONTENT_VALUES_HISTORY.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
    }

    private void loadData() {
        if (mPsalmNumberId < 0) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver()
                    .query(PwsDataProvider.PsalmNumbers.Psalm.getContentUri(mPsalmNumberId), null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            mPsalmText = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMTEXT));
            mPsalmAuthor = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMAUTHOR));
            mPsalmComposer = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMCOMPOSER));
            mPsalmTranslator = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMTRANSLATOR));
            mPsalmNumber = cursor.getInt(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMNUMBER));
            mPsalmName = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMNAME));
            mPsalmLocale = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMLOCALE));
            String tons = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMTONALITIES));
            if (tons != null) mPsalmTonalities = tons.split("\\|");
            mBookName = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_BOOKDISPLAYNAME));
            mBibleRef = cursor.getString(cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMANNOTATION));
            isFavoritePsalm = isFavoritePsalm();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void updateUi() {
        if (mPsalmNumberId < 0) {
            return;
        }
        loadData();
        vPsalmText.setText(Html.fromHtml(PwsPsalmUtil.psalmTextToHtml(getActivity(), new Locale(mPsalmLocale), mPsalmText)));
        final String psalmInfo = PwsPsalmUtil.buildPsalmInfoHtml(getActivity(), new Locale(mPsalmLocale), mPsalmAuthor, mPsalmTranslator, mPsalmComposer);
        if (psalmInfo == null) {
            ((ViewManager) cvPsalmInfo.getParent()).removeView(cvPsalmInfo);
        }
        else {
            vPsalmInfo.setText(Html.fromHtml(psalmInfo));
        }
        String tonalities = null;
        for (int i = 0; mPsalmTonalities != null && i < mPsalmTonalities.length; i++) {
            Tonality tonality = Tonality.getInstanceBySignature(mPsalmTonalities[i]);
            if (tonality == null) continue;
            tonalities = (tonalities == null ? "" : ", ") + tonality.getLabel(getActivity());
        }
        if (tonalities == null) {
            ((ViewManager) cvTonalities.getParent()).removeView(cvTonalities);
        } else {
            vPsalmTonalities.setText(tonalities);
        }
        callbacks.onUpdatePsalmInfo(mPsalmNumber, mPsalmName, mBookName, mBibleRef, isFavoritePsalm);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (Callbacks) context;
        if (getArguments() != null) {
            mPsalmNumberId = getArguments().getLong(KEY_PSALM_NUMBER_ID, -10L);
        }
        init();
        addPsalmToHistory();
    }

    /**
     * Create new instance of PsalmTextFragment with attached psalmNumberId argument
     * @param psalmNumberId psalmNumber Id
     * @return instance of PsalmTextFragment
     */
    public static PsalmTextFragment newInstance(long psalmNumberId) {
        Bundle args = new Bundle();
        args.putLong(KEY_PSALM_NUMBER_ID, psalmNumberId);
        PsalmTextFragment fragment = new PsalmTextFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void addPsalmToHistory() {
        if (mPsalmNumberId < 0) return;
        getActivity().getContentResolver().insert(PwsDataProvider.History.CONTENT_URI, CONTENT_VALUES_HISTORY);
    }

    /**
     * Add psalm to favorites table
     */
    public void addPsalmToFavorites() {
        if (mPsalmNumberId < 0) return;
        getActivity().getContentResolver().insert(PwsDataProvider.Favorites.CONTENT_URI, CONTENT_VALUES_FAVORITES);
        isFavoritePsalm = isFavoritePsalm();
        callbacks.onUpdatePsalmInfo(mPsalmNumber, mPsalmName, mBookName, mBibleRef, isFavoritePsalm);
    }

    /**
     * Remove psalm from favorites table
     */
    public void removePsalmFromFavorites() {
        if (mPsalmNumberId < 0) return;
        getActivity().getContentResolver().delete(PwsDataProvider.Favorites.CONTENT_URI, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS);
        isFavoritePsalm = isFavoritePsalm();
        callbacks.onUpdatePsalmInfo(mPsalmNumber, mPsalmName, mBookName, mBibleRef, isFavoritePsalm);
    }

    /**
     * Check if it is favorite psalm
     * @return true if favorites table contains psalm, false otherwise
     */
    public boolean isFavoritePsalm() {
        if (mPsalmNumberId < 0) return false;
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(PwsDataProvider.Favorites.CONTENT_URI, null, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS, null);
            return cursor != null && cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Interface which should be implemented by activity who are the host of this fragment.
     */
    public interface Callbacks {

        /**
         * This method is called when any psalm information is changed.
         * @param psalmNumber psalm number
         * @param psalmName psalm name
         * @param bookName name of book which contains the psalm
         * @param bibleRef bible reference
         * @param isFavorite if the psalm exists in the favorites table
         */
        void onUpdatePsalmInfo(int psalmNumber, String psalmName, String bookName, String bibleRef, boolean isFavorite);
    }
}
