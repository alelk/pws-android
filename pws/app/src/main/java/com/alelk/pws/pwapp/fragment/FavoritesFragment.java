package com.alelk.pws.pwapp.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.pwapp.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.FavoritesRecyclerViewAdapter;

/**
 * Created by Alex Elkin on 18.02.2016.
 */
public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public final static int PWS_FAVORITES_LOADER = 1;

    private RecyclerView mRecyclerView;
    private Cursor cursor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_favorite, null);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.rv_favorites);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(layoutManager);

        getLoaderManager().initLoader(PWS_FAVORITES_LOADER, null, this);

        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case PWS_FAVORITES_LOADER:
                CursorLoader cursorLoader = new CursorLoader(getActivity().getBaseContext(), PwsDataProvider.Favorites.CONTENT_URI, null, null, null, null);
                return cursorLoader;
            default:
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursor = data;
        mRecyclerView.setAdapter(
                new FavoritesRecyclerViewAdapter(cursor,
                        new FavoritesRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(long psalmNumberId) {
                Intent intentPsalmView = new Intent(getActivity().getBaseContext(), PsalmActivity.class);
                intentPsalmView.putExtra("psalmNumberId", psalmNumberId);
                startActivity(intentPsalmView);
            }
        }));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursor = null;
    }
}
