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
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.alelk.pws.database.dao.BookStatisticWithBook
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.MainSettingsActivity
import com.alelk.pws.pwapp.model.AppPreferencesViewModel
import com.alelk.pws.pwapp.model.BookStatisticViewModel
import com.alelk.pws.pwapp.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * General Preference Fragment
 *
 * Created by Alex Elkin on 06.08.2016.
 */

// fixme not working
class GeneralPreferenceFragment : PreferenceFragmentCompat() {

  private val bookStatisticViewModel: BookStatisticViewModel by viewModels()
  private val appPreferencesViewModel: AppPreferencesViewModel by viewModels()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.pref_general, rootKey)
    setHasOptionsMenu(true)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val bookPreference = checkNotNull(findPreference<PreferenceCategory>(getString(R.string.pref_books_key)))
    val themePreference = checkNotNull(findPreference<ListPreference>(getString(R.string.pref_themes_key)))
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          bookStatisticViewModel.bookStatistic.collectLatest { bookWithStatisticList ->
            initBookPreferences(bookPreference, bookWithStatisticList)
          }
        }
        launch {
          appPreferencesViewModel.appTheme.collectLatest { appTheme ->
            initThemePreference(themePreference, appTheme)
          }
        }
      }
    }
  }

  private fun initThemePreference(themePreference: ListPreference, currentTheme: AppTheme) {
    val currentThemeKey = getString(currentTheme.themeKeyResId)
    Timber.d("current theme: $currentThemeKey")
    themePreference.setDefaultValue(getString(currentTheme.themeKeyResId))
    themePreference.summary = getString(currentTheme.themeNameResId)
    themePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
      val newAppTheme = AppTheme.byKey(requireActivity(), newValue.toString()) ?: AppTheme.LIGHT
      viewLifecycleOwner.lifecycleScope.launch {
        appPreferencesViewModel.setAppTheme(newAppTheme)
      }
      true
    }
  }

  private fun initBookPreferences(bookPreferences: PreferenceCategory, books: List<BookStatisticWithBook>) {
    Timber.d("book preferences: ${books.joinToString(", ") { "${it.book.externalId}->${it.bookStatistic.userPreference}" }}")
    bookPreferences.removeAll()
    books.forEach { book ->
      val pref = SwitchPreference(requireContext()).apply {
        key = book.book.externalId.toString()
        title = book.book.displayName
        isChecked = (book.bookStatistic.userPreference ?: 0) > 0
        setOnPreferenceChangeListener { _, _ ->
          viewLifecycleOwner.lifecycleScope.launch {
            bookStatisticViewModel.update(book.book.externalId) {
              it.bookStatistic.copy(userPreference = if ((it.bookStatistic.userPreference ?: 0) > 0) 0 else 1)
            }
          }
          true
        }
      }
      bookPreferences.addPreference(pref)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return if (item.itemId == android.R.id.home) {
      startActivity(Intent(activity, MainSettingsActivity::class.java))
      true
    } else {
      super.onOptionsItemSelected(item)
    }
  }
}