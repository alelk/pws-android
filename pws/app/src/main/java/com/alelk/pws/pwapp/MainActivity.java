package com.alelk.pws.pwapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.Toolbar;


import com.alelk.pws.pwapp.fragment.FavoritesFragment;
import com.alelk.pws.pwapp.fragment.HistoryFragment;
import com.alelk.pws.pwapp.fragment.ReadNowFragment;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final static String KEY_NAVIGATION_ITEM_ID = "navigationItemId";

    private DrawerLayout mDrawerLayout;
    private int mNavigationItemId = R.id.drawer_main_home;
    private FloatingActionButton mFabSearchText;
    private FloatingActionButton mFabSearchNumber;
    private AppBarLayout mAppBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mNavigationItemId = savedInstanceState.getInt(KEY_NAVIGATION_ITEM_ID);
        }

        mFabSearchText = (FloatingActionButton) findViewById(R.id.fab_search_text);
        mFabSearchNumber = (FloatingActionButton) findViewById(R.id.fab_search_number);
        mFabSearchText.setOnClickListener(onButtonClick);
        mFabSearchNumber.setOnClickListener(onButtonClick);

        findViewById(R.id.btn_search_psalm_number).setOnClickListener(onButtonClick);
        findViewById(R.id.btn_search_psalm_text).setOnClickListener(onButtonClick);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        mAppBar = (AppBarLayout) findViewById(R.id.appbar_main);
        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset == 0) {
                    mFabSearchText.hide();
                    mFabSearchNumber.hide();
                } else {
                    mFabSearchText.show();
                    mFabSearchNumber.show();
                }
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        ((NavigationView) findViewById(R.id.nav_view)).setNavigationItemSelectedListener(this);

        displayFragment();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_NAVIGATION_ITEM_ID, mNavigationItemId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, MainSettingsActivity.class);
            startActivity(intent);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        boolean result = false;
        switch (id) {
            case R.id.drawer_main_history:
            case R.id.drawer_main_favorite:
                mAppBar.setExpanded(false, true);
                mNavigationItemId = id;
                displayFragment();
                result = true;
                break;
            case R.id.drawer_main_home:
                mAppBar.setExpanded(true, true);
                mNavigationItemId = id;
                displayFragment();
                result = true;
                break;
            case R.id.drawer_main_settings:
                Intent intent = new Intent(this, MainSettingsActivity.class);
                startActivity(intent);
                result = true;
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        drawer.closeDrawer(GravityCompat.START);
        return result;
    }

    private void displayFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;
        int titleResId = R.string.app_name;
        switch (mNavigationItemId) {
            case R.id.drawer_main_home:
                fragment = new ReadNowFragment();
                titleResId = R.string.lbl_drawer_main_home;
                break;
            case R.id.drawer_main_history:
                fragment = new HistoryFragment();
                titleResId = R.string.lbl_drawer_main_history;
                break;
            case R.id.drawer_main_favorite:
                fragment = new FavoritesFragment();
                titleResId = R.string.lbl_drawer_main_favorite;
                break;
        }
        CollapsingToolbarLayout collapsingToolbarLayout= (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_main);
        collapsingToolbarLayout.setTitle(getString(titleResId));
        fragmentTransaction.replace(R.id.fragment_main_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_search_psalm_number:
                case R.id.fab_search_number:
                    Intent intentSearchNumber = new Intent(getBaseContext(), SearchActivity.class);
                    intentSearchNumber.putExtra(SearchActivity.KEY_INPUT_TYPE, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    startActivity(intentSearchNumber);
                    break;
                case R.id.btn_search_psalm_text:
                case R.id.fab_search_text:
                    Intent intentSearchText = new Intent(getBaseContext(), SearchActivity.class);
                    intentSearchText.putExtra(SearchActivity.KEY_INPUT_TYPE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
                    startActivity(intentSearchText);
                    break;
            }
        }
    };
}
