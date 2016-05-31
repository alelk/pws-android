package com.alelk.pws.pwapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.adapter.PsalmTextFragmentStatePagerAdapter;
import com.alelk.pws.pwapp.fragment.PsalmHeaderFragment;
import com.alelk.pws.pwapp.fragment.PsalmTextFragment;
import com.alelk.pws.pwapp.holder.PsalmHolder;

import java.util.ArrayList;

/**
 * Created by Alex Elkin on 25.03.2016.
 */
public class PsalmActivity extends AppCompatActivity implements PsalmTextFragment.Callbacks {

    private static final int REQUEST_CODE_FULLSCREEN_ACTIVITY = 1;
    private Long mPsalmNumberId = -1L;
    private ViewPager mPagerPsalmText;
    private PsalmHeaderFragment mPsalmHeaderFragment;
    private Toolbar mToolbar;
    private FloatingActionButton mFabFavorite;
    private ArrayList<Long> mBookPsalmNumberIds;
    private PsalmTextFragmentStatePagerAdapter mPsalmTextPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_psalm);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        mFabFavorite = (FloatingActionButton) findViewById(R.id.fab_psalm);
        mFabFavorite.setOnClickListener(new FabFavoritesOnClick());

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPsalmHeaderFragment = (PsalmHeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_header);
        if (mPsalmHeaderFragment == null) {
            mPsalmHeaderFragment = new PsalmHeaderFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_header, mPsalmHeaderFragment).commit();
        }

        mPagerPsalmText = (ViewPager) findViewById(R.id.pager_psalm_text);
        mPsalmTextPagerAdapter = new PsalmTextFragmentStatePagerAdapter(getSupportFragmentManager(), mBookPsalmNumberIds);
        mPagerPsalmText.setAdapter(mPsalmTextPagerAdapter);
        mPagerPsalmText.setCurrentItem(mBookPsalmNumberIds.indexOf(mPsalmNumberId));
    }

    private void init() {
        if (mPsalmNumberId >= 0) return;
        mPsalmNumberId = getIntent().getLongExtra("psalmNumberId", -10L);

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri(mPsalmNumberId), null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final String[] psalmNumberIdsList = cursor.getString(
                        cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBERID_LIST))
                        .split(",");
                mBookPsalmNumberIds = new ArrayList<>(psalmNumberIdsList.length);
                for (String id : psalmNumberIdsList) {
                    try {
                        mBookPsalmNumberIds.add(Long.parseLong(id));
                    } catch (NumberFormatException ex) {
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
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
    public void onUpdatePsalmInfo(PsalmHolder psalmHolder) {
        if (psalmHolder == null ||
                mBookPsalmNumberIds.get(mPagerPsalmText.getCurrentItem()) != psalmHolder.getPsalmNumberId()) return;

        CollapsingToolbarLayout collapsingToolbarLayout= (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_psalm);
        collapsingToolbarLayout.setTitle("№ " + psalmHolder.getPsalmNumber());
        getSupportActionBar().setTitle("№ " + psalmHolder.getPsalmNumber());
        mPsalmHeaderFragment.updateUi(psalmHolder.getPsalmName(), psalmHolder.getBookName(), psalmHolder.getBibleRef());
        drawFavoriteFabIcon(psalmHolder.isFavoritePsalm());
    }

    @Override
    public void onRequestFullscreenMode() {
        Intent intent = new Intent(this, PsalmFullscreenActivity.class);
        intent.putExtra(PsalmFullscreenActivity.KEY_PSALM_NUMBER_ID, mBookPsalmNumberIds.get(mPagerPsalmText.getCurrentItem()));
        startActivityForResult(intent, REQUEST_CODE_FULLSCREEN_ACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_FULLSCREEN_ACTIVITY:
                if (resultCode != RESULT_OK || data == null) return;
                mPsalmNumberId = data.getLongExtra(PsalmFullscreenActivity.KEY_PSALM_NUMBER_ID, -1);
                mPagerPsalmText.setCurrentItem(mBookPsalmNumberIds.indexOf(mPsalmNumberId));
                break;
        }
    }

    public class FabFavoritesOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
            if(fragment.isFavoritePsalm()) {
                fragment.removePsalmFromFavorites();
                Snackbar.make(v, "Removed from favorites.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            } else {
                fragment.addPsalmToFavorites();
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
