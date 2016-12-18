package com.alelk.pws.pwapp.fragment.preference;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.alelk.pws.pwapp.MainSettingsActivity;
import com.alelk.pws.pwapp.R;

/**
 * Created by Alex Elkin on 07.11.2016.
 */

public class AboutPreferenceFragment extends PwsPreferenceFragment{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_about);
        setHasOptionsMenu(true);
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
