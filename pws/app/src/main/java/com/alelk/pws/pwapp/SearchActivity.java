package com.alelk.pws.pwapp;

import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.source.PwsDataSource;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.database.table.PwsPsalmFtsTable;
import com.alelk.pws.pwapp.adapter.PsalmSuggestionCursorAdapter;
import com.alelk.pws.pwapp.adapter.SearchPsalmCursorAdapter;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;
import com.alelk.pws.pwapp.loader.PsalmSuggestionsLoaderCallback;

import java.util.List;


public class SearchActivity extends AppCompatActivity {

    private static final String LOG_TAG = SearchActivity.class.getSimpleName();

    Uri uri = Uri.parse("content://com.alelk.pws.database.provider/psalms");

    private ListView mListViewPsalms;
    private CursorAdapter mCursorAdapter;

    PwsDataSource pwsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mListViewPsalms = (ListView) findViewById(R.id.lv_search_psalm);

        mCursorAdapter = new SearchPsalmCursorAdapter(this, null, 0);
        mListViewPsalms.setAdapter(mCursorAdapter);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // TODO: 01.07.2015 search
            String query = intent.getStringExtra(SearchManager.QUERY);
            query = PwsPsalmFtsTable.TABLE_PSALMS_FTS + " MATCH '" + query + "'";
            Log.i("search action", "query " + query);
            Cursor cursor = getContentResolver().query(PwsDataProviderContract.CONTENT_URI_SEARCH, null, query, null, null);
            mCursorAdapter.swapCursor(cursor);
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
