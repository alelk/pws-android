package com.alelk.pws.pwapp.fragment.preference;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.provider.PwsDataProviderContract;
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.pwapp.MainSettingsActivity;
import com.alelk.pws.pwapp.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alex Elkin on 06.08.2016.
 */
public class GeneralPreferenceFragment extends PwsPreferenceFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public final static int PWS_BOOKS_STATISTIC_LOADER = 44;

    private PreferenceCategory booksCategory = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        booksCategory = (PreferenceCategory) findPreference(getString(R.string.pref_books_key));
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(PWS_BOOKS_STATISTIC_LOADER, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), MainSettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == PWS_BOOKS_STATISTIC_LOADER) {
            return new CursorLoader(getActivity().getBaseContext(), PwsDataProvider.BookStatistic.CONTENT_URI, null, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (booksCategory == null || cursor == null || cursor.isAfterLast()) return;
        if (!cursor.moveToFirst()) return;
        do {
            final String bookEdition = cursor.getString(cursor.getColumnIndex(PwsDataProvider.BookStatistic.COLUMN_BOOKEDITION));
            final String bookDisplayName = cursor.getString(cursor.getColumnIndex(PwsDataProvider.BookStatistic.COLUMN_BOOKDISPLAYNAME));
            final int bookStatisticPref = cursor.getInt(cursor.getColumnIndex(PwsDataProvider.BookStatistic.COLUMN_BOOKSTATISTIC_PREFERENCE));
            final String key = "bookstatistic." + bookEdition + ".pref";
            PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext())
                    .edit().putInt(key, bookStatisticPref).commit();
            Preference pref = booksCategory.findPreference(bookEdition);
            if (pref == null) {
                pref = new SwitchPreference(getActivity().getBaseContext());
                pref.setKey(bookEdition);
                booksCategory.addPreference(pref);
            }
            pref.setTitle(bookDisplayName);
            ((SwitchPreference) pref).setChecked(bookStatisticPref > 0);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    final Uri uri = PwsDataProvider.BookStatistic.getBookStatisticBookEditionUri(preference.getKey());
                    final int defaultPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext())
                            .getInt("bookstatistic." + preference.getKey() + ".pref", 1);
                    int newPref = (Boolean) o ? defaultPref : 0;
                    final ContentValues values = new ContentValues();
                    values.put(PwsBookStatisticTable.COLUMN_USERPREFERENCE, newPref);
                    getActivity().getContentResolver().update(uri, values, null, null);
                    return true;
                }
            });
        } while (cursor.moveToNext());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
