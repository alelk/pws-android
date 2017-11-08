package com.alelk.pws.pwapp.fragment.preference;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import com.alelk.pws.database.provider.PwsDataProvider;
import com.alelk.pws.database.table.PwsBookStatisticTable;
import com.alelk.pws.pwapp.activity.MainSettingsActivity;
import com.alelk.pws.pwapp.R;
import com.alelk.pws.pwapp.theme.AppTheme;
import com.alelk.pws.pwapp.theme.ThemePreferences;

/**
 * General Preference Fragment
 *
 * Created by Alex Elkin on 06.08.2016.
 */
public class GeneralPreferenceFragment extends PwsPreferenceFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static String LOG_TAG = GeneralPreferenceFragment.class.getSimpleName();
    public final static int PWS_BOOKS_STATISTIC_LOADER = 44;

    private PreferenceCategory booksCategory = null;
    private ThemePreferences mThemePreferences;
    private ListPreference mThemeListPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        booksCategory = (PreferenceCategory) findPreference(getString(R.string.pref_books_key));
        mThemeListPreference = (ListPreference) findPreference(getString(R.string.pref_themes_key));
        mThemePreferences = new ThemePreferences(getActivity());
        initThemePreference();
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(PWS_BOOKS_STATISTIC_LOADER, null, this);
    }

    private void initThemePreference() {
        if (mThemeListPreference == null) return;
        setupThemeListPreference(mThemePreferences.getAppTheme());
        mThemeListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            AppTheme newAppTheme;
            try {
                newAppTheme = AppTheme.forThemeKey(getActivity(), newValue.toString());
            } catch (IllegalArgumentException exc) {
                Log.w(LOG_TAG, "Unable to get app theme for the key '" + newValue + '\'');
                newAppTheme = AppTheme.LIGHT;
            }
            setupThemeListPreference(newAppTheme);
            mThemePreferences.persistAppTheme(newAppTheme);
            return true;
        });
    }

    private void setupThemeListPreference(AppTheme appTheme) {
        mThemeListPreference.setDefaultValue(getString(appTheme.getThemeKeyResId()));
        mThemeListPreference.setSummary(getString(appTheme.getThemeNameResId()));
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
            final String key = "bookstatistic110." + bookEdition + ".userPref";
            if (bookStatisticPref > 0) {
                PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext())
                        .edit().putInt(key, bookStatisticPref).apply();
            }
            Preference pref = booksCategory.findPreference(bookEdition);
            if (pref == null) {
                pref = new SwitchPreference(getActivity().getBaseContext());
                pref.setKey(bookEdition);
                booksCategory.addPreference(pref);
            }
            pref.setTitle(bookDisplayName);
            ((SwitchPreference) pref).setChecked(bookStatisticPref > 0);
            pref.setOnPreferenceChangeListener((preference, o) -> {
                final Uri uri = PwsDataProvider.BookStatistic.getBookStatisticBookEditionUri(preference.getKey());
                final int defaultPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext())
                        .getInt("bookstatistic110." + preference.getKey() + ".userPref", 1);
                int newPref = (Boolean) o ? defaultPref : 0;
                final ContentValues values = new ContentValues();
                values.put(PwsBookStatisticTable.COLUMN_USERPREFERENCE, newPref);
                getActivity().getContentResolver().update(uri, values, null, null);
                return true;
            });
        } while (cursor.moveToNext());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
