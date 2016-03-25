package com.alelk.pws.pwapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.source.PwsDataSource;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsPsalmFtsTable;
import com.alelk.pws.database.table.PwsPsalmTable;
import com.alelk.pws.pwapp.adapter.SearchPsalmCursorAdapter;
import com.alelk.pws.pwapp.fragment.PsalmFragment;


public class SearchActivity extends AppCompatActivity {

    private static final String LOG_TAG = SearchActivity.class.getSimpleName();

    private ListView mListViewPsalms;
    private CursorAdapter mCursorAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String METHOD_NAME = "onCreate";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mListViewPsalms = (ListView) findViewById(R.id.lv_search_psalm);
        mCursorAdapter = new SearchPsalmCursorAdapter(this, null, 0);
        mListViewPsalms.setAdapter(mCursorAdapter);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Cursor cursor = getContentResolver().query(
                    PwsDataProvider.Psalms.Search.CONTENT_URI, null,
                    PwsDataProvider.Psalms.Search.SELECTION,
                    PwsDataProvider.Psalms.Search.getSelectionArgs(query), null);
            mCursorAdapter.swapCursor(cursor);
            Log.d(LOG_TAG, METHOD_NAME + ": Action=" + Intent.ACTION_SEARCH + " query='" + query +
                    "' results: " + (cursor == null ? 0 : cursor.getCount()));
            mListViewPsalms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                    long psalmNumberId = cursor.getLong(cursor.getColumnIndex(PwsFavoritesTable.COLUMN_PSALMNUMBERID));
                    Intent intentPsalmView = new Intent(getBaseContext(), MainActivity.class);
                    intentPsalmView.setAction(Intent.ACTION_VIEW);
                    intentPsalmView.putExtra("psalmNumberId", psalmNumberId);
                    startActivity(intentPsalmView);
                }
            });
            return;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            long psalmNumberId = Long.parseLong(data.getLastPathSegment());
            if (psalmNumberId != -1) {
                Intent intentPsalmView = new Intent(getApplicationContext(), MainActivity.class);
                intentPsalmView.setAction(Intent.ACTION_VIEW);
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
