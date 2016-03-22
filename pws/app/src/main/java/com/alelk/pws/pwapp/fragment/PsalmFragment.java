package com.alelk.pws.pwapp.fragment;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.database.table.PwsFavoritesTable;
import com.alelk.pws.database.table.PwsHistoryTable;
import com.alelk.pws.database.util.PwsPsalmUtil;
import com.alelk.pws.pwapp.R;

import java.util.Locale;

/**
 * Created by Alex Elkin on 18.04.2015.
 */
public class PsalmFragment extends Fragment {

    // TODO: 03.03.2016 check why 'fv.' is needed
    private static final String SELECTION_FAVORITES_PSALM_NUMBER_MATCH = "fv." + PwsFavoritesTable.COLUMN_PSALMNUMBERID + " = ?";
    private static final String SELECTION_FAVORITES_PSALM_NUMBER_MATCH_2 = PwsFavoritesTable.COLUMN_PSALMNUMBERID + " = ?";
    private final String[] SELECTION_ARGS = new String[1];
    private final ContentValues CONTENT_VALUES_FAVORITES = new ContentValues(1);
    private final ContentValues CONTENT_VALUES_HISTORY = new ContentValues(1);

    private FloatingActionButton fabFavorite;
    private TextView vPsalmText;
    private ScrollView mScrollView;
    private long mPsalmNumberId;
    private Cursor mCursor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View v = inflater.inflate(R.layout.fragment_psalm, null);
        vPsalmText = (TextView) v.findViewById(R.id.txt_psalmtext);
        mScrollView = (ScrollView) v.findViewById(R.id.scrlv_psalmtext);
        mPsalmNumberId = getActivity().getIntent().getLongExtra("psalmNumberId", -1);
        if (mPsalmNumberId == -10) {
            mCursor = getActivity().getContentResolver()
                    .query(PwsDataProvider.History.Last.CONTENT_URI, null, null, null, null);
        } else {

            mCursor = getActivity().getContentResolver()
                    .query(PwsDataProvider.PsalmNumbers.Psalm.getContentUri(mPsalmNumberId), null, null, null, null);
        }


        if (mCursor != null && mCursor.moveToFirst()) {
            String stringText = mCursor.getString(mCursor.getColumnIndex(PwsDataProvider.PsalmNumbers.Psalm.COLUMN_PSALMTEXT));
            vPsalmText.setText(stringText);

        }

        SELECTION_ARGS[0] = String.valueOf(mPsalmNumberId);
        CONTENT_VALUES_FAVORITES.put(PwsFavoritesTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));
        CONTENT_VALUES_HISTORY.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, String.valueOf(mPsalmNumberId));

        addPsalmToHistory();

        //Toolbar toolbar = (Toolbar) v.findViewById(R.id.toolbar_psalm);
        //toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        //getActivity().setSupportActionBar(toolbar);



        //fabFavorite = (FloatingActionButton) v.findViewById(R.id.fab_psalm);
        //fabFavorite.setOnClickListener(new FabFavoritesOnClick());
        //drawFavoriteFabIcon();

        //getActivity().setTitle(mPsalm.getName());
        return v;
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
        getActivity().getContentResolver().insert(PwsDataProvider.Favorites.CONTENT_URI, CONTENT_VALUES_FAVORITES);
    }

    public void removePsalmFromFavorites() {
        getActivity().getContentResolver().delete(PwsDataProvider.Favorites.CONTENT_URI, SELECTION_FAVORITES_PSALM_NUMBER_MATCH_2, SELECTION_ARGS);
    }

    public void addPsalmToHistory() {
        getActivity().getContentResolver().insert(PwsDataProvider.History.CONTENT_URI, CONTENT_VALUES_HISTORY);
    }

    public boolean isFavoritePsalm() {
        Cursor cursor = getActivity().getContentResolver().query(PwsDataProvider.Favorites.CONTENT_URI, null, SELECTION_FAVORITES_PSALM_NUMBER_MATCH, SELECTION_ARGS, null);
        return cursor.moveToFirst();
    }

    public void drawFavoriteFabIcon() {
        if (isFavoritePsalm()) {
            fabFavorite.setImageDrawable(getActivity().getApplicationContext().getDrawable(R.drawable.ic_favorite));
        } else {
            fabFavorite.setImageDrawable(getActivity().getApplicationContext().getDrawable(R.drawable.ic_favorite_border));
        }
    }
}
