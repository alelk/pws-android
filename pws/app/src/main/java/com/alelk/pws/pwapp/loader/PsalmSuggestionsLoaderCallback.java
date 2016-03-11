package com.alelk.pws.pwapp.loader;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CursorAdapter;

import com.alelk.pws.database.provider.PwsDataProvider;

/**
 * Created by Alex Elkin on 22.05.2015.
 */
public class PsalmSuggestionsLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {

    private Context mContext;
    private Uri mUri;
    private String[] mProjection;
    private String mSelection;
    private String mSortOrder;
    private CursorAdapter mCursorAdapter;

    public PsalmSuggestionsLoaderCallback(Context mContext, Uri mUri, String[] mProjection, String mSelection, String mSortOrder, CursorAdapter mCursorAdapter) {
        this.mContext = mContext;
        this.mUri = mUri;
        this.mProjection = mProjection;
        this.mSelection = mSelection;
        this.mSortOrder = mSortOrder;
        this.mCursorAdapter = mCursorAdapter;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext, mUri, mProjection, mSelection, null, mSortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
