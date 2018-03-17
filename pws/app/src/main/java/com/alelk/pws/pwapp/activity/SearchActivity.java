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

package com.alelk.pws.pwapp.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity;
import com.alelk.pws.pwapp.fragment.SearchResultsFragment;


public class SearchActivity extends AppCompatThemedActivity {

    public static final String KEY_INPUT_TYPE = "com.alelk.pws.pwapp.activity.SearchActivity.inputType";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        handleIntent();
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent();
    }

    private void handleIntent() {
        SearchResultsFragment resultsFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_search_results);

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            final String query = getIntent().getStringExtra(SearchManager.QUERY);
            if (resultsFragment != null) {
                resultsFragment.updateQuery(query);
            } else {
                resultsFragment = SearchResultsFragment.newInstance(query);
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_search_results, resultsFragment).commit();
            }
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            if (data == null) return;
            long psalmNumberId = Long.parseLong(data.getLastPathSegment());
            if (psalmNumberId != -1) {
                Intent intentPsalmView = new Intent(getApplicationContext(), PsalmActivity.class);
                intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
                startActivity(intentPsalmView);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        if (searchManager != null)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        final String query = getIntent().getStringExtra(SearchManager.QUERY);
        if (TextUtils.isEmpty(query)) {
            searchView.setIconified(false);
        } else {
            searchView.setIconified(true);
            searchView.setQuery(query, false);
        }
        searchView.setInputType(getIntent().getIntExtra(KEY_INPUT_TYPE, InputType.TYPE_CLASS_TEXT));
        if ((searchView.getInputType() & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER) {
            searchView.setQueryHint(getString(R.string.hint_enter_psalm_number));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
