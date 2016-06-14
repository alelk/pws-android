package com.alelk.pws.pwapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.Toolbar;


import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.pwapp.fragment.FavoritesFragment;
import com.alelk.pws.pwapp.fragment.HistoryFragment;
import com.alelk.pws.pwapp.fragment.ReadNowFragment;

import java.io.File;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private Intent mIntent;
    private Toolbar mToolbar;
    private FloatingActionButton mFActionButton;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private FragmentTransaction mFragmentTransaction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIntent = getIntent();


        if (!Intent.ACTION_VIEW.equals(mIntent.getAction()) && savedInstanceState == null) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            ReadNowFragment readNowFragment = new ReadNowFragment();
            mFragmentTransaction.add(R.id.fragment_main_container, readNowFragment);
            mFragmentTransaction.commit();
        } else if (Intent.ACTION_VIEW.equals(mIntent.getAction())) {
            Intent intent = new Intent(this, PsalmActivity.class);
            intent.putExtra("psalmNumberId", mIntent.getLongExtra("psalmNumberId", -10L));
            startActivity(intent);
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(mToolbar);

        mFActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                startActivity(intent);
            }
        });
        mFActionButton.setVisibility(View.GONE);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
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

        if (id == R.id.drawer_main_psalm) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            ReadNowFragment readNowFragment = new ReadNowFragment();
            mFragmentTransaction.replace(R.id.fragment_main_container, readNowFragment);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
            setTitle(getString(R.string.lbl_drawer_main_psalm));
            mFActionButton.setVisibility(View.GONE);
        } else if (id == R.id.drawer_main_history) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            HistoryFragment historyFragment = new HistoryFragment();
            mFragmentTransaction.replace(R.id.fragment_main_container, historyFragment);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
            setTitle(getString(R.string.lbl_drawer_main_history));
            mFActionButton.setVisibility(View.VISIBLE);
        } else if (id == R.id.drawer_main_favorite) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            FavoritesFragment favoritesFragment = new FavoritesFragment();
            mFragmentTransaction.replace(R.id.fragment_main_container, favoritesFragment);
            mFragmentTransaction.addToBackStack(null);
            mFragmentTransaction.commit();
            setTitle(getString(R.string.lbl_drawer_main_favorite));
            mFActionButton.setVisibility(View.VISIBLE);
        } else if (id == R.id.drawer_main_settings) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.layout_main_drawer);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
