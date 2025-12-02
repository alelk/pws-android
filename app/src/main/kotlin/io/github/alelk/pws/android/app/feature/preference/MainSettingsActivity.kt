package io.github.alelk.pws.android.app.feature.preference

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.AppCompatThemedActivity

/**
 * A PreferenceActivity that contains main PWS App settings.
 *
 * Created by Alex Elkin on 18.02.2016.
 */
@AndroidEntryPoint
class MainSettingsActivity : AppCompatThemedActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
  }
}