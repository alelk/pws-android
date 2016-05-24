package com.alelk.pws.pwapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import com.alelk.pws.pwapp.fragment.SearchResultsFragment;


public class SearchActivity extends AppCompatActivity {

    private static final String LOG_TAG = SearchActivity.class.getSimpleName();
    private SearchResultsFragment mResultsFragment;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mResultsFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_search_results);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            if (mResultsFragment != null) {
                mResultsFragment.updateQuery(mQuery);
            } else {
                mResultsFragment = SearchResultsFragment.newInstance(mQuery);
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_search_results, mResultsFragment).commit();
            }
            return;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            long psalmNumberId = Long.parseLong(data.getLastPathSegment());
            if (psalmNumberId != -1) {
                Intent intentPsalmView = new Intent(getApplicationContext(), PsalmActivity.class);
                intentPsalmView.putExtra("psalmNumberId", psalmNumberId);
                startActivity(intentPsalmView);
            }
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        if (TextUtils.isEmpty(mQuery)) {
            searchView.setIconified(false);
        } else {
            searchView.setIconified(true);
            searchView.setQuery(mQuery, false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
