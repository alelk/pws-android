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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.activity.PsalmActivity;
import com.alelk.pws.pwapp.adapter.HistoryRecyclerViewAdapter;


/**
 * Read Now Fragment
 *
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
        mRecentPsalmsAdapter = new HistoryRecyclerViewAdapter(psalmNumberId -> {
            Intent intentPsalmView = new Intent(getActivity().getBaseContext(), PsalmActivity.class);
            intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
            startActivity(intentPsalmView);
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
        rvRecentPsalms = v.findViewById(R.id.rv_recent);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        rvRecentPsalms.setLayoutManager(layoutManager);
        rvRecentPsalms.setAdapter(mRecentPsalmsAdapter);
        rvRecentPsalms.setNestedScrollingEnabled(true);
        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case PWS_RECENT_PSALM_LOADER:
                return new CursorLoader(getActivity().getBaseContext(), PwsDataProvider.History.getContentUri(DEFAULT_RECENT_LIMIT), null, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mRecentPsalmsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecentPsalmsAdapter.swapCursor(null);
    }
}
