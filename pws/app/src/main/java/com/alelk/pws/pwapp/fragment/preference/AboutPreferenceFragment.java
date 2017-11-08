package com.alelk.pws.pwapp.fragment.preference;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.view.MenuItem;

import com.alelk.pws.database.BuildConfig;
import com.alelk.pws.pwapp.activity.MainSettingsActivity;
import com.alelk.pws.pwapp.R;

/**
 * About App
 *
 * Created by Alex Elkin on 07.11.2016.
 */

public class AboutPreferenceFragment extends PwsPreferenceFragment{

    private Preference mPrefAboutApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_about);
        mPrefAboutApp = findPreference(getString(R.string.pref_about_app_key));
        init();
        setHasOptionsMenu(true);
    }

    private void init() {
        mPrefAboutApp.setSummary(getString(R.string.pref_about_app_version_prefix, BuildConfig.VERSION_NAME));
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
}
