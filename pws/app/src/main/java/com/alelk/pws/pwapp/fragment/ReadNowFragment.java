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
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.SearchActivity;
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter;


/**
 * Created by Alex Elkin on 17.02.2016.
 */
public class ReadNowFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public final static int PWS_RECENT_PSALM_LOADER = 30;
    public final static int DEFAULT_RECENT_LIMIT = 10;

    private RecyclerView rvRecentPsalms;
    private HistoryRecyclerViewAdapter mRecentPsalmsAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRecentPsalmsAdapter = new HistoryRecyclerViewAdapter(new HistoryRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(long psalmNumberId) {
                Intent intentPsalmView = new Intent(getActivity().getBaseContext(), PsalmActivity.class);
                intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
                startActivity(intentPsalmView);
            }
        });
        getLoaderManager().initLoader(PWS_RECENT_PSALM_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecentPsalmsAdapter.notifyDataSetChanged();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_readnow, null);
        rvRecentPsalms = (RecyclerView) v.findViewById(R.id.rv_recent);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        rvRecentPsalms.setLayoutManager(layoutManager);
        rvRecentPsalms.setAdapter(mRecentPsalmsAdapter);
        rvRecentPsalms.setNestedScrollingEnabled(false);
        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case PWS_RECENT_PSALM_LOADER:
                CursorLoader cursorLoader = new CursorLoader(getActivity().getBaseContext(), PwsDataProvider.History.getContentUri(DEFAULT_RECENT_LIMIT), null, null, null, null);
                return cursorLoader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        rvRecentPsalms.setNestedScrollingEnabled(false);
        mRecentPsalmsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecentPsalmsAdapter.swapCursor(null);
    }
}