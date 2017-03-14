package com.alelk.pws.pwapp.fragment.preference;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alelk.pws.pwapp.MainSettingsActivity;
import com.alelk.pws.pwapp.R;

/**
 * Created by Alex Elkin on 06.08.2016.
 */
public class DonatePreferenceFragment extends PwsPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_donate);
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
