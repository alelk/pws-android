package com.alelk.pws.pwapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.pwapp.adapter.PsalmTextFragmentStatePagerAdapter;
import com.alelk.pws.pwapp.dialog.PsalmPreferencesDialogFragment;
import com.alelk.pws.pwapp.dialog.SearchPsalmNumberDialogFragment;
import com.alelk.pws.pwapp.fragment.PsalmHeaderFragment;
import com.alelk.pws.pwapp.fragment.PsalmTextFragment;
import com.alelk.pws.pwapp.holder.PsalmHolder;

import java.util.ArrayList;

/**
 * Psalm Activity
 *
 * Created by Alex Elkin on 25.03.2016.
 */
public class PsalmActivity extends AppCompatActivity implements PsalmTextFragment.Callbacks,
        SearchPsalmNumberDialogFragment.SearchPsalmNumberDialogListener,
        PsalmPreferencesDialogFragment.OnPsalmPreferencesChangedCallbacks{

    public static final String KEY_PSALM_NUMBER_ID = "psalmNumberId";
    private static final int REQUEST_CODE_FULLSCREEN_ACTIVITY = 1;
    private static final int ADD_TO_HISTORY_DELAY = 5000;
    private Long mPsalmNumberId = -1L;
    private float mPsalmTextSize = -1;
    private ViewPager mPagerPsalmText;
    private PsalmHeaderFragment mPsalmHeaderFragment;
    private PsalmTextFragment mCurrentPsalmTextFragment;
    private Toolbar mToolbar;
    private FloatingActionButton mFabFavorite;
    private ArrayList<Long> mBookPsalmNumberIds;
    private PsalmTextFragmentStatePagerAdapter mPsalmTextPagerAdapter;
    private Handler mAddToHistoryHandler = new Handler();
    private Runnable mAddToHistoryRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPsalmTextPagerAdapter == null || mPagerPsalmText == null) return;
            final PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
            fragment.addPsalmToHistory();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_psalm);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        mFabFavorite = (FloatingActionButton) findViewById(R.id.fab_psalm);
        mFabFavorite.setOnClickListener(new FabFavoritesOnClick());

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPsalmHeaderFragment = (PsalmHeaderFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_psalm_header);
        if (mPsalmHeaderFragment == null) {
            mPsalmHeaderFragment = new PsalmHeaderFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_psalm_header, mPsalmHeaderFragment).commit();
        }

        mPagerPsalmText = (ViewPager) findViewById(R.id.pager_psalm_text);
        mPsalmTextPagerAdapter = new PsalmTextFragmentStatePagerAdapter(getSupportFragmentManager(), mBookPsalmNumberIds, mPsalmTextSize);
        mPagerPsalmText.setAdapter(mPsalmTextPagerAdapter);
        mPagerPsalmText.setCurrentItem(mBookPsalmNumberIds.indexOf(mPsalmNumberId));
    }

    private void init() {
        if (mPsalmNumberId >= 0) return;
        mPsalmNumberId = getIntent().getLongExtra(KEY_PSALM_NUMBER_ID, -10L);
        mPsalmTextSize = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, -1);

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri(mPsalmNumberId), null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String[] psalmNumberIdsList = cursor.getString(
                        cursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBERID_LIST))
                        .split(",");
                mBookPsalmNumberIds = new ArrayList<>(psalmNumberIdsList.length);
                for (String id : psalmNumberIdsList) {
                    try {
                        mBookPsalmNumberIds.add(Long.parseLong(id));
                    } catch (NumberFormatException ignored) {
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
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_jump) {
            DialogFragment searchNumberDialog = SearchPsalmNumberDialogFragment.newInstance(mPsalmNumberId);
            searchNumberDialog.show(getSupportFragmentManager(), SearchPsalmNumberDialogFragment.class.getSimpleName());
            return true;
        } else if (id == R.id.menu_text_size ){
            if (mPsalmTextSize < 0) {
                PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
                mPsalmTextSize = fragment.getPsalmTextSize();
            }
            DialogFragment psalmPreferencesDialog = PsalmPreferencesDialogFragment.newInstance(mPsalmTextSize);
            psalmPreferencesDialog.show(getSupportFragmentManager(), PsalmPreferencesDialogFragment.class.getSimpleName());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUpdatePsalmInfo(PsalmHolder psalmHolder) {
        if (psalmHolder == null ||
                mBookPsalmNumberIds.get(mPagerPsalmText.getCurrentItem()) != psalmHolder.getPsalmNumberId()) return;

        CollapsingToolbarLayout collapsingToolbarLayout= (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_psalm);
        collapsingToolbarLayout.setTitle("№ " + psalmHolder.getPsalmNumber());
        mPsalmHeaderFragment.updateUi(psalmHolder.getPsalmName(), psalmHolder.getBookName(), psalmHolder.getBibleRef());
        drawFavoriteFabIcon(psalmHolder.isFavoritePsalm());
        mAddToHistoryHandler.removeCallbacks(mAddToHistoryRunnable);
        mAddToHistoryHandler.postDelayed(mAddToHistoryRunnable, ADD_TO_HISTORY_DELAY);
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

    @Override
    public void onPositiveButtonClick(long psalmNumberId) {
        mPagerPsalmText.setCurrentItem(mBookPsalmNumberIds.indexOf(psalmNumberId));
    }

    @Override
    public void onNegativeButtonClick() {

    }

    @Override
    public void onPsalmTextSizeChanged(float textSize) {
        mPsalmTextSize = textSize;
        PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
        fragment.setPsalmTextSize(textSize);
    }

    @Override
    public void onApplyPsalmPreferences(float textSize) {
        mPsalmTextSize = textSize;
        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit()
                .putFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, textSize).apply();
        PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
        fragment.setPsalmTextSize(textSize);
        mPsalmTextPagerAdapter.applyPsalmTextPreferences(textSize);
    }

    @Override
    public void onCancelPsalmPreferences(float previousTextSize) {
        mPsalmTextSize = previousTextSize;
        PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
        fragment.setPsalmTextSize(previousTextSize);
    }

    private class FabFavoritesOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final PsalmTextFragment fragment = (PsalmTextFragment) mPsalmTextPagerAdapter.getRegisteredFragments().get(mPagerPsalmText.getCurrentItem());
            if(fragment.isFavoritePsalm()) {
                fragment.removePsalmFromFavorites();
                Snackbar.make(v, R.string.msg_removed_from_favorites, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            } else {
                fragment.addPsalmToFavorites();
                Snackbar.make(v, R.string.msg_added_to_favorites, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }
    }

    private void drawFavoriteFabIcon(boolean isFavoritePsalm) {
        if (isFavoritePsalm) {
            mFabFavorite.setImageResource(R.drawable.ic_favorite_white);
        } else {
            mFabFavorite.setImageResource(R.drawable.ic_favorite_border_white);
        }
    }
}
