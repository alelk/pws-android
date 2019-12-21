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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.activity.PsalmActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.adapter.SearchRecyclerViewAdapter;

/**
 * Search Result Fragment
 *
 * Created by Alex Elkin on 23.05.2016.
 */
public class SearchResultsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_QUERY = "com.alelk.pws.pwapp.query";
    public static final int PWS_SEARCH_RESULTS_LOADER = 4;

    private String mQuery;
    private RecyclerView mRecyclerView;
    private SearchRecyclerViewAdapter mSearchResultsAdapter;
    private View mLayoutSearchProgress;

    public static SearchResultsFragment newInstance(String query) {
        final Bundle args = new Bundle();
        args.putString(KEY_QUERY, query);
        final SearchResultsFragment fragment = new SearchResultsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getArguments() != null) {
            mQuery = getArguments().getString(KEY_QUERY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_search_results, null);
        mRecyclerView = v.findViewById(R.id.rv_search_results);
        mLayoutSearchProgress = v.findViewById(R.id.layout_search_progress);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mSearchResultsAdapter = new SearchRecyclerViewAdapter(psalmNumberId -> {
            Intent intentPsalmView = new Intent(getActivity().getBaseContext(), PsalmActivity.class);
            intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
            startActivity(intentPsalmView);
        });
        mRecyclerView.setAdapter(mSearchResultsAdapter);

        mLayoutSearchProgress.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(PWS_SEARCH_RESULTS_LOADER, null, this);

        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        if (TextUtils.isEmpty(mQuery)) return null;
        switch (loaderId) {
            case PWS_SEARCH_RESULTS_LOADER:
                return new CursorLoader(
                        getActivity().getBaseContext(),
                        PwsDataProvider.Psalms.Search.CONTENT_URI,
                        null,
                        null,
                        new String[]{ mQuery },
                        null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mSearchResultsAdapter.swapCursor(data);
        mLayoutSearchProgress.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mSearchResultsAdapter.swapCursor(null);
    }

    public void updateQuery(String query) {
        mQuery = query;
        getLoaderManager().restartLoader(PWS_SEARCH_RESULTS_LOADER, null, this);
        if (mLayoutSearchProgress != null)
            mLayoutSearchProgress.setVisibility(View.VISIBLE);
        if (mRecyclerView != null)
            mRecyclerView.setVisibility(View.INVISIBLE);
    }
}
