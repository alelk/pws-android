package com.alelk.pws.pwapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity;
import com.alelk.pws.pwapp.fragment.FavoritesFragment;
import com.alelk.pws.pwapp.fragment.HistoryFragment;
import com.alelk.pws.pwapp.fragment.ReadNowFragment;
import com.alelk.pws.pwapp.fragment.preference.DonatePreferenceFragment;
import com.alelk.pws.pwapp.theme.ThemeType;


public class MainActivity extends AppCompatThemedActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final static String KEY_NAVIGATION_ITEM_ID = "navigationItemId";

    private DrawerLayout mDrawerLayout;
    private int mNavigationItemId = R.id.drawer_main_home;
    private FloatingActionButton mFabSearchText;
    private FloatingActionButton mFabSearchNumber;
    private AppBarLayout mAppBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mNavigationItemId = savedInstanceState.getInt(KEY_NAVIGATION_ITEM_ID);
        }

        mFabSearchText = findViewById(R.id.fab_search_text);
        mFabSearchNumber = findViewById(R.id.fab_search_number);
        mFabSearchText.setOnClickListener(onButtonClick);
        mFabSearchNumber.setOnClickListener(onButtonClick);

        findViewById(R.id.btn_search_psalm_number).setOnClickListener(onButtonClick);
        findViewById(R.id.btn_search_psalm_text).setOnClickListener(onButtonClick);

        mDrawerLayout = findViewById(R.id.layout_main_drawer);
        mAppBar = findViewById(R.id.appbar_main);
        mAppBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (verticalOffset == 0) {
                mFabSearchText.hide();
                mFabSearchNumber.hide();
            } else {
                mFabSearchText.show();
                mFabSearchNumber.show();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar_main);
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
        Intent intent;
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
                intent = new Intent(this, MainSettingsActivity.class);
                startActivity(intent);
                result = true;
                break;
            case R.id.drawer_main_donate:
                intent = new Intent(this, MainSettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, DonatePreferenceFragment.class.getName());
                intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
                startActivity(intent);
                result = true;
                break;

        }
        DrawerLayout drawer = findViewById(R.id.layout_main_drawer);
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
        CollapsingToolbarLayout collapsingToolbarLayout= findViewById(R.id.collapsing_toolbar_main);
        collapsingToolbarLayout.setTitle(getString(titleResId));
        fragmentTransaction.replace(R.id.fragment_main_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private View.OnClickListener onButtonClick = view -> {
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
    };

    @Override
    protected ThemeType getThemeType() {
        return ThemeType.NO_ACTION_BAR;
    }
}
