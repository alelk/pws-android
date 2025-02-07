package io.github.alelk.pws.android.app.fragment.preference

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alelk.pws.pwapp.R

class RootPreferenceFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.pref_root, rootKey)
    findPreference<Preference>("pref_general")?.setOnPreferenceClickListener {
      findNavController().navigate(R.id.generalPreferenceFragment)
      true
    }
    findPreference<Preference>("pref_backup")?.setOnPreferenceClickListener {
      findNavController().navigate(R.id.backupPreferenceFragment)
      true
    }
    findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
      findNavController().navigate(R.id.aboutPreferenceFragment)
      true
    }
  }
}