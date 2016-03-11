package com.alelk.pws.pwapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;

/**
 * Created by Alex Elkin on 18.04.2015.
 */
public class PsalmActivity extends AppCompatActivity {

    // TODO: 03.03.2016 check why 'fv.' is needed
    private static final String SELECTION_FAVORITES_PSALM_NUMBER_MATCH = "fv." + PwsFavoritesTable.COLUMN_PSALMNUMBERID + " = ?";
    private static final String SELECTION_FAVORITES_PSALM_NUMBER_MATCH_2 = PwsFavoritesTable.COLUMN_PSALMNUMBERID + " = ?";
    private final String[] SELECTION_ARGS = new String[1];
    private final ContentValues CONTENT_VALUES_FAVORITES = new ContentValues(1);
    private final ContentValues CONTENT_VALUES_HISTORY = new ContentValues(1);

    private FloatingActionButton fabFavorite;
    private long mPsalmNumberId;
    private Psalm mPsalm;
    private PwsDataSourceImpl mPwsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psalm);

        mPsalmNumberId = getIntent().getLongExtra("psalmNumberId", -1);
        SELECTION_ARGS[0] = String.valueOf(mPsalmNumberId);
        CONTENT_VALUES_FAVORITES.put(PwsFavoritesTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
        CONTENT_VALUES_HISTORY.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));

        addPsalmToHistory();

        mPwsDataSource = new PwsDataSourceImpl(getBaseContext(), "pws.db", PwsDataProviderContract.DATABASE_VERSION);
        mPwsDataSource.open();

        try {
            mPsalm = mPwsDataSource.getPsalmByPsalmNumberId(mPsalmNumberId);
        } catch (PwsDatabaseIncorrectValueException e) {
            e.printStackTrace();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        setSupportActionBar(toolbar);

        fabFavorite = (FloatingActionButton) findViewById(R.id.fab_psalm);
        fabFavorite.setOnClickListener(new FabFavoritesOnClick());
        drawFavoriteFabIcon();

        setTitle(mPsalm.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPwsDataSource.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPwsDataSource.close();
    }

    public class FabFavoritesOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(isFavoritePsalm()) {
                removePsalmFromFavorites();
                Snackbar.make(v, "Removed from favorites.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            } else {
                addPsalmToFavorites();
                Snackbar.make(v, "Added to favorites.", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
            drawFavoriteFabIcon();
        }
    }

    public void addPsalmToFavorites() {
        getContentResolver().insert(PwsDataProviderContract.Favorites.CONTENT_URI, CONTENT_VALUES_FAVORITES);
    }

    public void removePsalmFromFavorites() {
        getContentResolver().delete(PwsDataProviderContract.Favorites.CONTENT_URI, SELECTION_FAVORITES_PSALM_NUMBER_MATCH_2, SELECTION_ARGS);
    }

    public void addPsalmToHistory() {
        getContentResolver().insert(PwsDataProviderContract.History.CONTENT_URI, CONTENT_VALUES_HISTORY);
    }

    public boolean isFavoritePsalm() {
        Cursor cursor = getContentResolver().query(PwsDataProviderContract.Favorites.CONTENT_URI, null, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS, null);
        return cursor.moveToFirst();
    }

    public void drawFavoriteFabIcon() {
        if (isFavoritePsalm()) {
            fabFavorite.setImageDrawable(getApplicationContext().getDrawable(R.drawable.ic_favorite));
        } else {
            fabFavorite.setImageDrawable(getApplicationContext().getDrawable(R.drawable.ic_favorite_border));
        }
    }
}
