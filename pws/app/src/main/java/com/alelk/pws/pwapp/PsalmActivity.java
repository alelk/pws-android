package com.alelk.pws.pwapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.util.PwsPsalmUtil;
import com.alelk.pws.pwapp.fragment.PsalmHeaderFragment;
import com.alelk.pws.pwapp.fragment.PsalmTextFragment;

import java.util.Locale;

/**
 * Created by Alex Elkin on 25.03.2016.
 */
public class PsalmActivity extends AppCompatActivity implements PsalmTextFragment.Callbacks {

    private Long mPsalmNumberId;
    private PsalmTextFragment mPsalmTextFragment;
    private PsalmHeaderFragment mPsalmHeaderFragment;
    private FloatingActionButton mFabFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psalm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        mFabFavorite = (FloatingActionButton) findViewById(R.id.fab_psalm);
        mFabFavorite.setOnClickListener(new FabFavoritesOnClick());
        mPsalmNumberId = getIntent().getLongExtra("psalmNumberId", -10L);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPsalmTextFragment = (PsalmTextFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_text);
        mPsalmHeaderFragment = (PsalmHeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_header);
        if (mPsalmTextFragment == null) {
            mPsalmTextFragment = PsalmTextFragment.newInstance(mPsalmNumberId);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_text, mPsalmTextFragment).commit();
            mPsalmTextFragment.addPsalmToHistory();
        }
        if (mPsalmHeaderFragment == null) {
            mPsalmHeaderFragment = new PsalmHeaderFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_header, mPsalmHeaderFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_psalm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUpdatePsalmInfo(int psalmNumber, String psalmName, String bookName, String bibleRef, boolean isFavoritePsalm) {
        getSupportActionBar().setTitle("№ " + psalmNumber);
        mPsalmHeaderFragment.updateUi(psalmName, bookName, bibleRef);
        drawFavoriteFabIcon(isFavoritePsalm);
    }

    public class FabFavoritesOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(mPsalmTextFragment.isFavoritePsalm()) {
                mPsalmTextFragment.removePsalmFromFavorites();
                Snackbar.make(v, "Removed from favorites.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            } else {
                mPsalmTextFragment.addPsalmToFavorites();
                Snackbar.make(v, "Added to favorites.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }
    }

    private void drawFavoriteFabIcon(boolean isFavoritePsalm) {
        if (isFavoritePsalm) {
            mFabFavorite.setImageDrawable(getDrawable(R.drawable.ic_favorite));
        } else {
            mFabFavorite.setImageDrawable(getDrawable(R.drawable.ic_favorite_border));
        }
    }
}
