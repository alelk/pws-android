package com.alelk.pws.pwapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.Toolbar;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.source.PwsDataSource;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.pwapp.fragment.FavoritesFragment;
import com.alelk.pws.pwapp.fragment.HistoryFragment;
import com.alelk.pws.pwapp.fragment.ReadNowFragment;
import com.alelk.pws.xmlengine.PwsXmlParser;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import java.util.List;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private final static Uri PSALMS_URI = Uri.parse("content://com.alelk.pws.database.provider/psalms/");
    private final static Uri SUGGEST_PSALMS_URI = Uri.parse("content://com.alelk.pws.database.provider/suggestions/psalms/");
    private final static String mPwsLibFilePath = "content.pwslib";
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private FloatingActionButton mFActionButton;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private FragmentTransaction mFragmentTransaction;
    PwsDataSource pwsDataSource;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragmentTransaction = getSupportFragmentManager().beginTransaction();
        ReadNowFragment readNowFragment= new ReadNowFragment();
        mFragmentTransaction.add(R.id.fragment_main_container, readNowFragment);
        mFragmentTransaction.commit();
        setTitle(getString(R.string.lbl_drawer_main_readnow));

        mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(mToolbar);

        mFActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                startActivity(intent);
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);


        pwsDataSource = new PwsDataSourceImpl(this, "pws.db", 9);
        pwsDataSource.open();

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
        } catch (PwsXmlParserIncorrectSourceFormatException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.drawer_main_readnow) {
            Log.e("item selected", "readnow");
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            ReadNowFragment readNowFragment= new ReadNowFragment();
            mFragmentTransaction.replace(R.id.fragment_main_container, readNowFragment);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
            setTitle(getString(R.string.lbl_drawer_main_readnow));
        } else if (id == R.id.drawer_main_history) {
            Log.e("item selected", "history");
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            HistoryFragment historyFragment = new HistoryFragment();
            mFragmentTransaction.replace(R.id.fragment_main_container, historyFragment);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
            setTitle(getString(R.string.lbl_drawer_main_history));
        } else if (id == R.id.drawer_main_favorite) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            FavoritesFragment favoritesFragment = new FavoritesFragment();
            mFragmentTransaction.replace(R.id.fragment_main_container, favoritesFragment);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
            setTitle(getString(R.string.lbl_drawer_main_favorite));
        } else if (id == R.id.drawer_main_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
