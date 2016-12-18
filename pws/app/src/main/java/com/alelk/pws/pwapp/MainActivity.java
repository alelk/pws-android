package com.alelk.pws.pwapp;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
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
    private Toolbar mToolbar;
    private FloatingActionButton mFActionButton;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mNavigationItemId = savedInstanceState.getInt(KEY_NAVIGATION_ITEM_ID);
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(mToolbar);

        mFActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFActionButton.setVisibility(View.GONE);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

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
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean result = false;
        switch (id) {
            case R.id.drawer_main_home:
            case R.id.drawer_main_history:
            case R.id.drawer_main_favorite:
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
        switch (mNavigationItemId) {
            case R.id.drawer_main_home:
                fragment = new ReadNowFragment();
                setTitle(getString(R.string.lbl_drawer_main_home));
                mFActionButton.hide();
                break;
            case R.id.drawer_main_history:
                fragment = new HistoryFragment();
                setTitle(getString(R.string.lbl_drawer_main_history));
                mFActionButton.show();
                break;
            case R.id.drawer_main_favorite:
                fragment = new FavoritesFragment();
                setTitle(getString(R.string.lbl_drawer_main_favorite));
                mFActionButton.show();
                break;
        }
        fragmentTransaction.replace(R.id.fragment_main_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void onClickSearchFab(View v) {
        Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
        startActivity(intent);
    }
}
