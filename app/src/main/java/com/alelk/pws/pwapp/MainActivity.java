package com.alelk.pws.pwapp;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.source.PwsDataSource;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.pwapp.adapter.PsalmListAdapter;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;
import com.alelk.pws.pwapp.util.PwsUtils;
import com.alelk.pws.xmlengine.PwsXmlParser;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;


public class MainActivity extends ActionBarActivity {

    private final static Uri PSALMS_URI = Uri.parse("content://com.alelk.pws.database.provider/psalms/");
    private final static Uri SUGGEST_PSALMS_URI = Uri.parse("content://com.alelk.pws.database.provider/suggestions/psalms/");
    private final static String mPwsLibFilePath = "content.pwslib";
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ActionBar mActionBar;
    private TextView textView;
    private ListView mDrawerMenuList;
    private ListView mPsalmsList;
    private ArrayAdapter<Psalm> psalmListAdapter;
    PwsDataSource pwsDataSource;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActionBar = this.getActionBar();
        mPsalmsList = (ListView) findViewById(R.id.lv_main_psalmslist);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        mDrawerMenuList = (ListView) findViewById(R.id.lv_main_drawermenu);
        mDrawerMenuList.setAdapter(new ArrayAdapter<String>(this, R.layout.layout_main_drawer_item, Arrays.asList("Item 1", "Item 2")));

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mActionBar.setTitle("Title1");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mActionBar.setTitle("Open");
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        pwsDataSource = new PwsDataSourceImpl(this, "pws.db", 9);
        pwsDataSource.open();

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // TODO: 01.07.2015 search
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i("search action", "query " + query);
            return;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            BookEdition bookEdition = BookEdition.getInstanceBySignature(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
            long id = Long.parseLong(data.getLastPathSegment());
            try {
                Psalm psalm = pwsDataSource.getPsalm(id);
                if (bookEdition == null) {
                    bookEdition = psalm.getBookEditions().first();
                }
                Book bookInfo = pwsDataSource.getBookInfo(bookEdition);
                Intent intentPsalmView = new Intent(getApplicationContext(), PsalmActivity.class);
                intentPsalmView.putExtra("psalm", new PwsPsalmParcelable(psalm));
                intentPsalmView.putExtra("bookEdition", bookEdition.getSignature());
                intentPsalmView.putExtra("bookName", bookInfo.getDisplayName());
                startActivity(intentPsalmView);
            } catch (PwsDatabaseIncorrectValueException e) {
                e.printStackTrace();
            }
            return;
        }


        AssetManager am = this.getAssets();
        PwsXmlParser parser = new PwsXmlParser(am);
        try {
            String version = parser.parseLibraryVersion(mPwsLibFilePath);
            SharedPreferences sharedPreferences = getSharedPreferences("pwspref", Context.MODE_PRIVATE);
            String currentVersion = sharedPreferences.getString("libraryVersion", "0");
            Log.e("currentVersion", currentVersion);
            Log.e("version", version);
            if (!currentVersion.equals(version)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("libraryVersion", version);
                editor.commit();
                List<Book> books = parser.parseLibrary(mPwsLibFilePath);
                for (Book book : books) {
                    pwsDataSource.addBook(book);
                }
                for (Book book : books) {
                    for (Psalm psalm : book.getPsalms().values()) {
                        try {
                            pwsDataSource.addPsalm(psalm);
                        } catch (PwsDatabaseSourceIdExistsException e) {
                        } catch (PwsDatabaseIncorrectValueException e) {
                        }
                    }
                }
            }

            List<Psalm> psalms = new ArrayList<>();
            try {
                Map<Integer, Psalm> psalms1 = pwsDataSource.getPsalms(BookEdition.PV3055);
                if (psalms1 != null) {
                    psalms.addAll(psalms1.values());
                }
            } catch (PwsDatabaseIncorrectValueException e) {
                e.printStackTrace();
            }
            pwsDataSource.close();

            Collections.sort(psalms, Psalm.getNumberComparator(BookEdition.PV3055));

            psalmListAdapter = new PsalmListAdapter(this, R.layout.layout_psalms_list, psalms);
            mPsalmsList.setAdapter(psalmListAdapter);
            mPsalmsList.setOnItemClickListener(psalmListClickHandler);
        } catch (PwsXmlParserIncorrectSourceFormatException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
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

    private List<Psalm> loadData(String query) {
        PwsDataSourceImpl pwsDataSource = new PwsDataSourceImpl(this, "pws.db", 9);
        pwsDataSource.open();
        List<Psalm> psalms = new ArrayList<>();
        try {
            psalms.addAll(pwsDataSource.getPsalms(BookEdition.PV3055, "%" + query + "%").values());
        } catch (PwsDatabaseIncorrectValueException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        pwsDataSource.close();

        Collections.sort(psalms, Psalm.getNumberComparator(BookEdition.PV3055));
        return psalms;
    }

    private AdapterView.OnItemClickListener psalmListClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Psalm psalm = (Psalm) parent.getItemAtPosition(position);

            Intent intent = new Intent(getApplicationContext(), PsalmActivity.class);
            intent.putExtra("psalm", new PwsPsalmParcelable(psalm));
            intent.putExtra("bookEdition", BookEdition.PV3055.getSignature());
            startActivity(intent);
        }
    };
}
