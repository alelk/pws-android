package com.alelk.pws.pwapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SearchViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import com.alelk.pws.pwapp.fragment.SearchResultsFragment;


public class SearchActivity extends AppCompatActivity {

    public static final String KEY_INPUT_TYPE = "com.alelk.pws.pwapp.SearchActivity.inputType";
    private static final String LOG_TAG = SearchActivity.class.getSimpleName();
    private SearchResultsFragment mResultsFragment;

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
        mResultsFragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_search_results);

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            final String query = getIntent().getStringExtra(SearchManager.QUERY);
            if (mResultsFragment != null) {
                mResultsFragment.updateQuery(query);
            } else {
                mResultsFragment = SearchResultsFragment.newInstance(query);
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_search_results, mResultsFragment).commit();
            }
            return;
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            long psalmNumberId = Long.parseLong(data.getLastPathSegment());
            if (psalmNumberId != -1) {
                Intent intentPsalmView = new Intent(getApplicationContext(), PsalmActivity.class);
                intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId);
                startActivity(intentPsalmView);
            }
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
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
