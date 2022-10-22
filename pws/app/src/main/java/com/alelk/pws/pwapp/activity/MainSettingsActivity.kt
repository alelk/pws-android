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
package com.alelk.pws.pwapp.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.fragment.preference.AboutPreferenceFragment
import com.alelk.pws.pwapp.fragment.preference.GeneralPreferenceFragment

/**
 * A [PreferenceActivity] that contains main PWS App settings.
 *
 * Created by Alex Elkin on 18.02.2016.
 */
class MainSettingsActivity : AppCompatPreferenceActivity() {
  @Deprecated("Deprecated in Java")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupActionBar()
  }

  /**
   * Set up the [android.app.ActionBar].
   */
  private fun setupActionBar() {
    val actionBar = supportActionBar
    actionBar?.setDisplayHomeAsUpEnabled(true)
  }

  @Deprecated("Deprecated in Java")
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    startActivity(Intent(this, MainActivity::class.java))
    return super.onOptionsItemSelected(item)
  }

//  /**
//   * {@inheritDoc}
//   */
//  @Deprecated("Deprecated in Java")
//  override fun onIsMultiPane(): Boolean {
//    return isXLarge(this)
//  }
//
//  /**
//   * {@inheritDoc}
//   */
//  @Deprecated("Deprecated in Java")
//  override fun onBuildHeaders(target: List<Header>) {
//    loadHeadersFromResource(R.xml.pref_headers, target)
//  }
//
//  /**
//   * This method stops fragment injection in malicious applications.
//   * Make sure to deny any unknown fragments here.
//   */
//  @Deprecated("Deprecated in Java")
//  override fun isValidFragment(fragmentName: String): Boolean {
//    return PreferenceFragmentCompat::class.java.name == fragmentName || GeneralPreferenceFragment::class.java.name == fragmentName || AboutPreferenceFragment::class.java.name == fragmentName
//  }

  companion object {
    /**
     * Determine if the extra-large screen.
     */
    private fun isXLarge(context: Context): Boolean {
      return (context.resources.configuration.screenLayout
        and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }
  }
}