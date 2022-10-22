package com.alelk.pws.pwapp.fragment.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.alelk.pws.pwapp.R

class RootPreferenceFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.pref_root, rootKey)
  }
}