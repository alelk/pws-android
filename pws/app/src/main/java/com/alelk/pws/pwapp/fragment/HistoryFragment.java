package com.alelk.pws.pwapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter;

/**
 * Created by Alex Elkin on 18.02.2016.
 */
public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_ITEMS_LIMIT = "com.alelk.pws.pwapp.historyItemsLimit";
    public final static int PWS_HISTORY_LOADER = 2;

    private final static int DEFAULT_ITEMS_LIMIT = 100;

    private RecyclerView mRecyclerView;
    private HistoryRecyclerViewAdapter mHistoryAdapter;
    private int mItemsLimit;

    public static HistoryFragment newInstance(int itemsLimit) {
        final Bundle args = new Bundle();
        args.putInt(KEY_ITEMS_LIMIT, itemsLimit);
        HistoryFragment fragment = new HistoryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_history, null);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.rv_history);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mHistoryAdapter = new HistoryRecyclerViewAdapter(new HistoryRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(long psalmNumberId) {
                Intent intentPsalmView = new Intent(getActivity(), PsalmActivity.class);
                intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
                startActivity(intentPsalmView);
            }
        });
        mRecyclerView.setAdapter(mHistoryAdapter);
        getLoaderManager().initLoader(PWS_HISTORY_LOADER, null, this);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mItemsLimit = DEFAULT_ITEMS_LIMIT;
        if (getArguments() != null) {
            mItemsLimit = getArguments().getInt(KEY_ITEMS_LIMIT, DEFAULT_ITEMS_LIMIT);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case PWS_HISTORY_LOADER:
                return new CursorLoader(getActivity(), PwsDataProvider.History.getContentUri(mItemsLimit), null, null, null, null);
            default:
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mHistoryAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mHistoryAdapter.swapCursor(null);
    }
}
