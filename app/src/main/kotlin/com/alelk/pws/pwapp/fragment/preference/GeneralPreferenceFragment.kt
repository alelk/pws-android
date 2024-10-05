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

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.provider.PwsDataProviderContract.BookStatistic.getBookStatisticBookEditionUri
import com.alelk.pws.database.table.PwsBookStatisticTable
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.MainSettingsActivity
import com.alelk.pws.pwapp.theme.AppTheme
import com.alelk.pws.pwapp.theme.ThemePreferences

// fixme: reimplement this fragment
/**
 * General Preference Fragment
 *
 * Created by Alex Elkin on 06.08.2016.
 */
class GeneralPreferenceFragment : PreferenceFragmentCompat(), LoaderManager.LoaderCallbacks<Cursor?> {
  private var booksCategory: PreferenceCategory? = null
  private var mThemePreferences: ThemePreferences? = null
  private var mThemeListPreference: ListPreference? = null
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.pref_general)
    booksCategory = findPreference(getString(R.string.pref_books_key)) as PreferenceCategory?
    mThemeListPreference = findPreference(getString(R.string.pref_themes_key)) as ListPreference?
    mThemePreferences = ThemePreferences(activity)
    initThemePreference()
    setHasOptionsMenu(true)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    LoaderManager.getInstance(this).initLoader(PWS_BOOKS_STATISTIC_LOADER, null, this)
  }

  private fun initThemePreference() {
    if (mThemeListPreference == null) return
    setupThemeListPreference(mThemePreferences!!.appTheme)
    mThemeListPreference!!.onPreferenceChangeListener =
      Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
        val newAppTheme: AppTheme = try {
          AppTheme.forThemeKey(requireActivity(), newValue.toString())
        } catch (exc: IllegalArgumentException) {
          Log.w(LOG_TAG, "Unable to get app theme for the key '$newValue'")
          AppTheme.LIGHT
        }
        setupThemeListPreference(newAppTheme)
        mThemePreferences!!.persistAppTheme(newAppTheme)
        true
      }
  }

  private fun setupThemeListPreference(appTheme: AppTheme) {
    mThemeListPreference!!.setDefaultValue(getString(appTheme.themeKeyResId))
    mThemeListPreference!!.summary = getString(appTheme.themeNameResId)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == android.R.id.home) {
      startActivity(Intent(activity, MainSettingsActivity::class.java))
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor?> {
    return if (id == PWS_BOOKS_STATISTIC_LOADER) {
      CursorLoader(
        requireActivity().baseContext,
        PwsDataProviderContract.BookStatistic.CONTENT_URI,
        null,
        null,
        null,
        null
      )
    } else throw java.lang.IllegalStateException("Unable to load cursor")
  }

  override fun onLoadFinished(loader: Loader<Cursor?>, cursor: Cursor?) {
    if (booksCategory == null || cursor == null || cursor.isAfterLast) return
    if (!cursor.moveToFirst()) return
    do {
      val bookEdition =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.BookStatistic.COLUMN_BOOKEDITION))
      val bookDisplayName =
        cursor.getString(cursor.getColumnIndex(PwsDataProviderContract.BookStatistic.COLUMN_BOOKDISPLAYNAME))
      val bookStatisticPref =
        cursor.getInt(cursor.getColumnIndex(PwsDataProviderContract.BookStatistic.COLUMN_BOOKSTATISTIC_PREFERENCE))
      val key = "bookstatistic110.$bookEdition.userPref"
      if (bookStatisticPref > 0) {
        PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
          .edit().putInt(key, bookStatisticPref).apply()
      }
      var pref = booksCategory!!.findPreference<SwitchPreference>(bookEdition)
      if (pref == null) {
        pref = SwitchPreference(requireActivity().baseContext)
        pref.key = bookEdition
        booksCategory!!.addPreference(pref)
      }
      pref.title = bookDisplayName
      pref.isChecked = bookStatisticPref > 0
      pref.setOnPreferenceChangeListener { preference: Preference, o: Any ->
        val uri: Uri =
          getBookStatisticBookEditionUri(preference.key)
        val defaultPref =
          PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)
            .getInt("bookstatistic110." + preference.key + ".userPref", 1)
        val newPref = if (o as Boolean) defaultPref else 0
        val values = ContentValues()
        values.put(PwsBookStatisticTable.COLUMN_USERPREFERENCE, newPref)
        requireActivity().contentResolver.update(uri, values, null, null)
        true
      }
    } while (cursor.moveToNext())
  }

  override fun onLoaderReset(loader: Loader<Cursor?>) {}

  companion object {
    private val LOG_TAG = GeneralPreferenceFragment::class.java.simpleName
    const val PWS_BOOKS_STATISTIC_LOADER = 44
  }
}