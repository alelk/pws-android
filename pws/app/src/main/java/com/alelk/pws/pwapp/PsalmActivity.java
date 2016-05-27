package com.alelk.pws.pwapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alelk.pws.pwapp.fragment.PsalmHeaderFragment;
import com.alelk.pws.pwapp.fragment.PsalmTextFragment;
import com.alelk.pws.pwapp.holder.PsalmHolder;

/**
 * Created by Alex Elkin on 25.03.2016.
 */
public class PsalmActivity extends AppCompatActivity implements PsalmTextFragment.Callbacks {

    private static final int REQUEST_CODE_FULLSCREEN_ACTIVITY = 1;
    private Long mPsalmNumberId = -1L;
    private PsalmTextFragment mPsalmTextFragment;
    private PsalmHeaderFragment mPsalmHeaderFragment;
    private FloatingActionButton mFabFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_psalm);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        mFabFavorite = (FloatingActionButton) findViewById(R.id.fab_psalm);
        mFabFavorite.setOnClickListener(new FabFavoritesOnClick());

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPsalmTextFragment = (PsalmTextFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_text);
        mPsalmHeaderFragment = (PsalmHeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_header);
        if (mPsalmTextFragment == null) {
            mPsalmTextFragment = PsalmTextFragment.newInstance(mPsalmNumberId);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_text, mPsalmTextFragment).commit();
        }
        if (mPsalmHeaderFragment == null) {
            mPsalmHeaderFragment = new PsalmHeaderFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_header, mPsalmHeaderFragment).commit();
        }
    }

    private void init() {
        if (mPsalmNumberId >= 0) return;
        mPsalmNumberId = getIntent().getLongExtra("psalmNumberId", -10L);
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
    public void onUpdatePsalmInfo(PsalmHolder psalmHolder) {
        getSupportActionBar().setTitle("№ " + psalmHolder.getPsalmNumber());
        mPsalmHeaderFragment.updateUi(psalmHolder.getPsalmName(), psalmHolder.getBookName(), psalmHolder.getBibleRef());
        drawFavoriteFabIcon(psalmHolder.isFavoritePsalm());
    }

    @Override
    public void onRequestFullscreenMode() {
        Intent intent = new Intent(this, PsalmFullscreenActivity.class);
        intent.putExtra(PsalmFullscreenActivity.KEY_PSALM_NUMBER_ID, mPsalmNumberId);
        startActivityForResult(intent, REQUEST_CODE_FULLSCREEN_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_FULLSCREEN_ACTIVITY:
                if (resultCode != RESULT_OK || data == null) return;
                mPsalmNumberId = data.getLongExtra(PsalmFullscreenActivity.KEY_PSALM_NUMBER_ID, -1);
                mPsalmTextFragment = PsalmTextFragment.newInstance(mPsalmNumberId);
                if (getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_text) != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_psalm_text, mPsalmTextFragment).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_text, mPsalmTextFragment).commit();
                }
                break;
        }
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
