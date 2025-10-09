package io.github.alelk.pws.android.app.feature.preference

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R

/**
 * About App
 *
 * Created by Alex Elkin on 07.11.2016.
 */
@AndroidEntryPoint
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