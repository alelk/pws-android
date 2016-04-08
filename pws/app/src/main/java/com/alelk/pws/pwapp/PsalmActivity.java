package com.alelk.pws.pwapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.util.PwsPsalmUtil;

import java.util.Locale;

/**
 * Created by Alex Elkin on 25.03.2016.
 */
public class PsalmActivity extends AppCompatActivity {

    private Long mPsalmNumberId;
    private Cursor mCursor;
    private TextView vPsalmText;
    private TextView vPsalmNumber;
    private TextView vPsalmName;
    private TextView vBookName;
    private TextView vBibleRef;
    private TextView vPsalmInfo;
    private FloatingActionButton mFabFavorite;

    private static final String SELECTION_FAVORITES_PSALM_NUMBER_MATCH = PwsDataProvider.Favorites.COLUMN_PSALMNUMBER_ID + " = ?";
    private final String[] SELECTION_ARGS = new String[1];
    private final ContentValues CONTENT_VALUES_FAVORITES = new ContentValues(1);
    private final ContentValues CONTENT_VALUES_HISTORY = new ContentValues(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psalm);
        vPsalmText = (TextView) findViewById(R.id.txt_psalm_text);
        vPsalmNumber = (TextView) findViewById(R.id.txt_psalm_number);
        vPsalmName = (TextView) findViewById(R.id.txt_psalm_name);
        vBookName = (TextView) findViewById(R.id.txt_book_name);
        vBibleRef = (TextView) findViewById(R.id.txt_bible_ref);
        vPsalmInfo = (TextView) findViewById(R.id.txt_psalm_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        mFabFavorite = (FloatingActionButton) findViewById(R.id.fab_psalm);
        mPsalmNumberId = getIntent().getLongExtra("psalmNumberId", -10L);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mFabFavorite.setOnClickListener(new FabFavoritesOnClick());

        if (mPsalmNumberId < 0) {
            mCursor = getContentResolver()
                    .query(PwsDataProvider.History.Last.CONTENT_URI, null, null, null, null);
        } else {
            mCursor = getContentResolver()
                    .query(PwsDataProvider.PsalmNumbers.Psalm.getContentUri(mPsalmNumberId), null, null, null, null);
        }

        if (mCursor != null && mCursor.moveToFirst()) {
            mPsalmNumberId = mCursor.getLong(mCursor.getColumnIndex(PwsDataProvider.History.COLUMN_PSALMNUMBER_ID));
            swapCursor();
        }

        SELECTION_ARGS[0] = String.valueOf(mPsalmNumberId);
        CONTENT_VALUES_FAVORITES.put(PwsFavoritesTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
        CONTENT_VALUES_HISTORY.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));

        addPsalmToHistory();
        drawFavoriteFabIcon();
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

    private void swapCursor() {
        String title = "№ " + mCursor.getInt(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMNUMBER));
        String psalmAuthor = mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMAUTHOR));
        String psalmComposer = mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMCOMPOSER));
        String psalmTranslator = mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMTRANSLATOR));
        StringBuilder builder = new StringBuilder();
        if (psalmAuthor != null) builder.append("<b>Автор:</b> " + psalmAuthor);
        if (psalmTranslator != null) builder.append("<br><b>Перевод:</b> " + psalmTranslator);
        if (psalmComposer != null) builder.append("<br><b>Музыка:</b> " + psalmComposer);
        getSupportActionBar().setTitle(title);
        String stringText = mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMTEXT));
        vPsalmText.setText(Html.fromHtml(PwsPsalmUtil.psalmTextToHtml(this, new Locale("ru"), stringText)));
        //vPsalmNumber.setText(Long.toString(mCursor.getLong(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMNUMBER))));
        vPsalmName.setText(mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMNAME)));
        vBookName.setText(mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_BOOKDISPLAYNAME)));
        String bibleRef = mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMANNOTATION));
        vBibleRef.setText(bibleRef);
        if (builder.toString() != null) vPsalmInfo.setText(Html.fromHtml(builder.toString()));
    }

    public void addPsalmToHistory() {
        getContentResolver().insert(PwsDataProvider.History.CONTENT_URI, CONTENT_VALUES_HISTORY);
    }

    public void addPsalmToFavorites() {
        getContentResolver().insert(PwsDataProvider.Favorites.CONTENT_URI, CONTENT_VALUES_FAVORITES);
    }

    public void removePsalmFromFavorites() {
        getContentResolver().delete(PwsDataProvider.Favorites.CONTENT_URI, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS);
    }

    public boolean isFavoritePsalm() {
        Cursor cursor = getContentResolver().query(PwsDataProvider.Favorites.CONTENT_URI, null, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS, null);
        return cursor.moveToFirst();
    }

    public void drawFavoriteFabIcon() {
        if (isFavoritePsalm()) {
            mFabFavorite.setImageDrawable(getDrawable(R.drawable.ic_favorite));
        } else {
            mFabFavorite.setImageDrawable(getDrawable(R.drawable.ic_favorite_border));
        }
    }
}
