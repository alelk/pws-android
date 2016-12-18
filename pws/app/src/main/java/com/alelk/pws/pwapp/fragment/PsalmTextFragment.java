package com.alelk.pws.pwapp.fragment;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.TextView;

import com.alelk.pws.database.data.Tonality;
import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.util.PwsPsalmUtil;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.holder.PsalmHolder;

import java.util.Locale;

/**
 * Created by Alex Elkin on 18.04.2015.
 *
 * The Activity who are the host of this fragment should implement Callbacks interface.
 */
public class PsalmTextFragment extends Fragment {

    private final static String LOG_TAG = PsalmTextFragment.class.getSimpleName();

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
    private PsalmHolder mPsalmHolder;
    private long mPsalmNumberId = -1;
    private boolean isAddedToHistory;

    public PsalmTextFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View v = inflater.inflate(R.layout.fragment_psalm_text, null);
        v.findViewById(R.id.ll_psalm_text_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callbacks.onRequestFullscreenMode();
            }
        });
        cvTonalities = (CardView) v.findViewById(R.id.cv_tonalities);
        cvPsalmInfo = (CardView) v.findViewById(R.id.cv_psalm_info);
        vPsalmText = (TextView) v.findViewById(R.id.txt_psalm_text);
        vPsalmInfo = (TextView) v.findViewById(R.id.txt_psalm_info);
        vPsalmTonalities = (TextView) v.findViewById(R.id.txt_psalm_tonalities);
        updateUi();
        setRetainInstance(true);
        return v;
    }

    private void init() {
        if (mPsalmNumberId < 0) return;
        SELECTION_ARGS[0] = String.valueOf(mPsalmNumberId);
        CONTENT_VALUES_FAVORITES.put(PwsFavoritesTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
        CONTENT_VALUES_HISTORY.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
    }

    private void loadData() {
        final String METHOD_NAME = "loadData";
        if (mPsalmNumberId < 0) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(PwsDataProvider.PsalmNumbers.Psalm.getContentUri(mPsalmNumberId), null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            mPsalmHolder = new PsalmHolder(cursor, isFavoritePsalm());
            Log.v(LOG_TAG, METHOD_NAME + (mPsalmHolder != null ? ": The psalm data successfully loaded: " : ": Error loading psalm data: ") + "mPsalmHolder=" + mPsalmHolder.toString());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void updateUi() {
        if (mPsalmNumberId < 0 || mPsalmHolder == null) {
            return;
        }
        if (mPsalmHolder == null) return;
        vPsalmText.setText(Html.fromHtml(PwsPsalmUtil.psalmTextToHtml(getActivity(), new Locale(mPsalmHolder.getPsalmLocale()), mPsalmHolder.getPsalmText())));
        final String psalmInfo = PwsPsalmUtil.buildPsalmInfoHtml(getActivity(), new Locale(mPsalmHolder.getPsalmLocale()), mPsalmHolder.getPsalmAuthor(), mPsalmHolder.getPsalmTranslator(), mPsalmHolder.getPsalmComposer());
        if (psalmInfo == null) {
            ((ViewManager) cvPsalmInfo.getParent()).removeView(cvPsalmInfo);
        }
        else {
            vPsalmInfo.setText(Html.fromHtml(psalmInfo));
        }
        String tonalities = null;
        final String[] tonsArray = mPsalmHolder.getPsalmTonalities();
        for (int i = 0; tonsArray != null && i < tonsArray.length; i++) {
            Tonality tonality = Tonality.getInstanceBySignature(tonsArray[i]);
            if (tonality == null) continue;
            tonalities = (tonalities == null ? "" : ", ") + tonality.getLabel(getActivity());
        }
        if (tonalities == null) {
            ((ViewManager) cvTonalities.getParent()).removeView(cvTonalities);
        } else {
            vPsalmTonalities.setText(tonalities);
        }
        callbacks.onUpdatePsalmInfo(mPsalmHolder);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (Callbacks) context;
        if (getArguments() != null) {
            mPsalmNumberId = getArguments().getLong(KEY_PSALM_NUMBER_ID, -10L);
        }
        init();
        loadData();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible == true && callbacks != null) {
            callbacks.onUpdatePsalmInfo(mPsalmHolder);
        }
    }

    /**
     * Create new instance of PsalmTextFragment with attached psalmNumberId argument
     * @param psalmNumberId psalmNumber Id
     * @return instance of PsalmTextFragment
     */
    public static PsalmTextFragment newInstance(long psalmNumberId) {
        final Bundle args = new Bundle();
        args.putLong(KEY_PSALM_NUMBER_ID, psalmNumberId);
        PsalmTextFragment fragment = new PsalmTextFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void addPsalmToHistory() {
        final String METHOD_NAME = "addPsalmToHistory";
        if (mPsalmNumberId < 0 || isAddedToHistory || getActivity() == null) return;
        final ContentResolver contentResolver = getActivity().getContentResolver();
        if (contentResolver == null) return;
        final Cursor cursor = contentResolver.query(PwsDataProvider.History.Last.CONTENT_URI, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.getLong(cursor.getColumnIndex(PwsDataProvider.History.Last.COLUMN_PSALMNUMBER_ID)) == mPsalmNumberId) {
                    Log.d(LOG_TAG, METHOD_NAME + ": The psalm already present in history table as a recent item");
                    isAddedToHistory = true;
                    return;
                }
            }
            getActivity().getContentResolver().insert(PwsDataProvider.History.CONTENT_URI, CONTENT_VALUES_HISTORY);
            isAddedToHistory = true;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Add psalm to favorites table
     */
    public void addPsalmToFavorites() {
        final String METHOD_NAME = "addPsalmToFavorites";
        if (mPsalmNumberId < 0) return;
        getActivity().getContentResolver().insert(PwsDataProvider.Favorites.CONTENT_URI, CONTENT_VALUES_FAVORITES);
        mPsalmHolder.setFavoritePsalm(isFavoritePsalm());
        callbacks.onUpdatePsalmInfo(mPsalmHolder);

        callbacks.onUpdatePsalmInfo(mPsalmHolder);
        Log.v(LOG_TAG, METHOD_NAME + ": Result: isFavoritePsalm=" + mPsalmHolder.isFavoritePsalm());
    }

    /**
     * Remove psalm from favorites table
     */
    public void removePsalmFromFavorites() {
        final String METHOD_NAME = "removePsalmFromFavorites";
        if (mPsalmNumberId < 0) return;
        getActivity().getContentResolver().delete(PwsDataProvider.Favorites.CONTENT_URI, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS);
        mPsalmHolder.setFavoritePsalm(false);
        callbacks.onUpdatePsalmInfo(mPsalmHolder);
        Log.v(LOG_TAG, METHOD_NAME + ": Result: isFavoritePsalm=" + mPsalmHolder.isFavoritePsalm());
    }

    /**
     * Check if it is favorite psalm
     * @return true if favorites table contains psalm, false otherwise
     */
    public boolean isFavoritePsalm() {
        if (mPsalmNumberId < 0) return false;
        Cursor cursor = getActivity().getContentResolver().query(PwsDataProvider.Favorites.CONTENT_URI, null, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS, null);
        try {
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
         * @param psalmHolder
         */
        void onUpdatePsalmInfo(@Nullable PsalmHolder psalmHolder);

        void onRequestFullscreenMode();
    }
}
