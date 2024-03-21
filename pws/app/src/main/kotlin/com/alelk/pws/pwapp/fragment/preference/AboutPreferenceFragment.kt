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
package com.alelk.pws.pwapp.fragment.preference

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.MainSettingsActivity

/**
 * About App
 *
 * Created by Alex Elkin on 07.11.2016.
 */
class AboutPreferenceFragment : PreferenceFragmentCompat() {
  private var mPrefAboutApp: Preference? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.pref_about)
    mPrefAboutApp = findPreference(getString(R.string.pref_about_app_key))
    init()
    setHasOptionsMenu(true)
  }

  private fun init() {
    try {
      val pi = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
      mPrefAboutApp!!.summary =
        getString(R.string.pref_about_app_version_prefix, pi.versionName)
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == android.R.id.home) {
      startActivity(Intent(activity, MainSettingsActivity::class.java))
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}