package com.alelk.pws.pwapp.fragment;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
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
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.ReferredPsalmsRecyclerViewAdapter;
import com.alelk.pws.pwapp.holder.PsalmHolder;

import java.util.Locale;

/**
 * Created by Alex Elkin on 18.04.2015.
 *
 * The Activity who are the host of this fragment should implement Callbacks interface.
 */
public class PsalmTextFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int PWS_REFERRED_PSALMS_LOADER = 60;
    private final static String LOG_TAG = PsalmTextFragment.class.getSimpleName();
    public static final String KEY_PSALM_NUMBER_ID = "com.alelk.pws.pwapp.psalmNumberId";
    public static final String KEY_PSALM_TEXT_SIZE = "com.alelk.pws.pwapp.psalmTextSize";
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
    private ReferredPsalmsRecyclerViewAdapter mReferredPsalmsAdapter;
    private PsalmHolder mPsalmHolder;
    private long mPsalmNumberId = -1;
    private float mPsalmTextSize = -1;
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
        mReferredPsalmsAdapter = new ReferredPsalmsRecyclerViewAdapter(new ReferredPsalmsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(long psalmNumberId) {
                Intent intentPsalmView = new Intent(getActivity().getBaseContext(), PsalmActivity.class);
                intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
                startActivity(intentPsalmView);
            }
        }, mPsalmTextSize);
        final RecyclerView rvReferredPsalms =(RecyclerView) v.findViewById(R.id.rv_referred_psalms);
        rvReferredPsalms.setAdapter(mReferredPsalmsAdapter);
        rvReferredPsalms.setNestedScrollingEnabled(false);
        rvReferredPsalms.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        setPsalmTextSize();
        updateUi();
        setRetainInstance(true);
        getLoaderManager().initLoader(PWS_REFERRED_PSALMS_LOADER, null, this);
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
            Log.v(LOG_TAG, METHOD_NAME + ": The psalm data successfully loaded: " + "mPsalmHolder=" + mPsalmHolder.toString());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void updateUi() {
        if (mPsalmNumberId < 0 || mPsalmHolder == null) {
            return;
        }
        final String psalmTextHtml = PwsPsalmUtil.psalmTextToHtml(new Locale(mPsalmHolder.getPsalmLocale()), mPsalmHolder.getPsalmText());
        if (Build.VERSION.SDK_INT >= 24) {
            vPsalmText.setText(Html.fromHtml(psalmTextHtml, Html.FROM_HTML_MODE_LEGACY));
        } else {
            vPsalmText.setText(Html.fromHtml(psalmTextHtml));
        }
        final String psalmInfoHtml = PwsPsalmUtil.buildPsalmInfoHtml(new Locale(mPsalmHolder.getPsalmLocale()), mPsalmHolder.getPsalmAuthor(), mPsalmHolder.getPsalmTranslator(), mPsalmHolder.getPsalmComposer());
        if (psalmInfoHtml == null) {
            ((ViewManager) cvPsalmInfo.getParent()).removeView(cvPsalmInfo);
        } else if (Build.VERSION.SDK_INT >= 24) {
            vPsalmInfo.setText(Html.fromHtml(psalmInfoHtml, Html.FROM_HTML_MODE_LEGACY));
        } else {
            vPsalmInfo.setText(Html.fromHtml(psalmInfoHtml));
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
            mPsalmTextSize = getArguments().getFloat(KEY_PSALM_TEXT_SIZE, -1);
        }
        init();
        loadData();
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible && callbacks != null) {
            callbacks.onUpdatePsalmInfo(mPsalmHolder);
        }
    }

    /**
     * Create new instance of PsalmTextFragment with attached psalmNumberId argument
     * @param psalmNumberId psalmNumber Id
     * @param psalmTextSize psalm text size
     * @return instance of PsalmTextFragment
     */
    public static PsalmTextFragment newInstance(long psalmNumberId, float psalmTextSize) {
        final Bundle args = new Bundle();
        args.putLong(KEY_PSALM_NUMBER_ID, psalmNumberId);
        args.putFloat(KEY_PSALM_TEXT_SIZE, psalmTextSize);
        PsalmTextFragment fragment = new PsalmTextFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create new instance of PsalmTextFragment with attached psalmNumberId argument
     * @param psalmNumberId psalmNumber Id
     * @return instance of PsalmTextFragment
     */
    public static PsalmTextFragment newInstance(long psalmNumberId) {
        return newInstance(psalmNumberId, -1);
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

    public float getPsalmTextSize() {
        return vPsalmText.getTextSize();
    }

    public void setPsalmTextSize(float textSize) {
        mPsalmTextSize = textSize;
        setPsalmTextSize();
        mReferredPsalmsAdapter.setHeaderTextSize(mPsalmTextSize);
    }

    private void setPsalmTextSize() {
        if (mPsalmTextSize > 0 && vPsalmText != null && vPsalmTonalities != null && vPsalmInfo != null) {
            vPsalmText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPsalmTextSize);
            vPsalmTonalities.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPsalmTextSize/1.5f);
            vPsalmInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPsalmTextSize/1.5f);
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == PWS_REFERRED_PSALMS_LOADER)
                return new CursorLoader(getActivity().getBaseContext(), PwsDataProvider.PsalmNumbers.ReferencePsalms.getContentUri(mPsalmNumberId), null, null, null, null);
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == PWS_REFERRED_PSALMS_LOADER) {
            mReferredPsalmsAdapter.swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mReferredPsalmsAdapter.swapCursor(null);
    }

    /**
     * Interface which should be implemented by activity who are the host of this fragment.
     */
    public interface Callbacks {

        /**
         * This method is called when any psalm information is changed.
         * @param psalmHolder Psalm Holder
         */
        void onUpdatePsalmInfo(@Nullable PsalmHolder psalmHolder);

        void onRequestFullscreenMode();
    }
}
