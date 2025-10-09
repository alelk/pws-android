package io.github.alelk.pws.android.app.feature.preference

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
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.feature.books.BookStatisticViewModel
import io.github.alelk.pws.android.app.core.theme.AppTheme
import io.github.alelk.pws.database.entity.BookStatisticWithBookEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * General Preference Fragment
 *
 * Created by Alex Elkin on 06.08.2016.
 */
@AndroidEntryPoint
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
    Timber.Forest.d("current theme: $currentThemeKey")
    themePreference.setDefaultValue(getString(currentTheme.themeKeyResId))
    themePreference.summary = getString(currentTheme.themeNameResId)
    themePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
      val newAppTheme = AppTheme.Companion.byKey(requireActivity(), newValue.toString()) ?: AppTheme.LIGHT
      viewLifecycleOwner.lifecycleScope.launch {
        appPreferencesViewModel.setAppTheme(newAppTheme)
      }
      true
    }
  }

  private fun initBookPreferences(bookPreferences: PreferenceCategory, books: List<BookStatisticWithBookEntity>) {
    Timber.Forest.d("book preferences: ${books.joinToString(", ") { "${it.book.id}->${it.bookStatistic.priority}" }}")
    bookPreferences.removeAll()
    books.forEach { book ->
      val pref = SwitchPreference(requireContext()).apply {
        key = book.book.id.toString()
        title = book.book.displayName
        isChecked = (book.bookStatistic.priority ?: 0) > 0
        setOnPreferenceChangeListener { _, _ ->
          viewLifecycleOwner.lifecycleScope.launch {
            bookStatisticViewModel.update(book.book.id) {
              it.bookStatistic.copy(priority = if ((it.bookStatistic.priority ?: 0) > 0) 0 else 1)
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