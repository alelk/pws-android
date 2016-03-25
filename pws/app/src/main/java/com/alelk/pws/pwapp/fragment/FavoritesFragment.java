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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.pwapp.MainActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.FavoritesCursorAdapter;

/**
 * Created by Alex Elkin on 18.02.2016.
 */
public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public final static int PWS_FAVORITES_LOADER = 1;

    private ListView mListViewFavorites;
    private FavoritesCursorAdapter mCursorAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_favorite, null);
        mListViewFavorites = (ListView) v.findViewById(R.id.lv_favorites_list);

        getLoaderManager().initLoader(PWS_FAVORITES_LOADER, null, this);

        mCursorAdapter = new FavoritesCursorAdapter(getActivity().getBaseContext(), null, 0);

        mListViewFavorites.setAdapter(mCursorAdapter);
        mListViewFavorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: 03.03.2016 incorrect method
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                long psalmNumberId = cursor.getLong(cursor.getColumnIndex(PwsFavoritesTable.COLUMN_PSALMNUMBERID));
                Intent intentPsalmView = new Intent(getActivity().getBaseContext(), MainActivity.class);
                intentPsalmView.setAction(Intent.ACTION_VIEW);
                intentPsalmView.putExtra("psalmNumberId", psalmNumberId);
                startActivity(intentPsalmView);
            }
        });
        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case PWS_FAVORITES_LOADER:
                CursorLoader cursorLoader = new CursorLoader(getActivity().getBaseContext(), Uri.parse("content://com.alelk.pws.database.provider/favorites"), null, null, null, null);
                return cursorLoader;
            default:
        }
        return null;
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
