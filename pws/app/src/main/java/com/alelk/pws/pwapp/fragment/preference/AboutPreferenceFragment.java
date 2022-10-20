/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alelk.pws.pwapp.fragment.preference;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
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
        try {
            PackageInfo pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            mPrefAboutApp.setSummary(getString(R.string.pref_about_app_version_prefix, pi.versionName));
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
