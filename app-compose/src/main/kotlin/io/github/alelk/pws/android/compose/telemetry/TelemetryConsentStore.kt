package io.github.alelk.pws.android.compose.telemetry

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's "send crash reports and anonymous statistics" choice.
 *
 * Backed by [SharedPreferences] rather than the app's DataStore on purpose: the value is needed
 * **synchronously in `Application.onCreate()`**, before the telemetry SDK is activated, and DataStore
 * only offers suspending reads. Blocking the main thread on a DataStore read at startup would be
 * worse than a one-key SharedPreferences file.
 *
 * Default ([defaultEnabled]) is on for release builds — crash reporting is the low-risk, legitimate
 * -interest baseline, and analytics is opt-out via the settings toggle — and off for debug builds so
 * developer runs never pollute production statistics.
 */
class TelemetryConsentStore(
  context: Context,
  private val defaultEnabled: Boolean,
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, defaultEnabled))

  /** Current consent, observable by the settings screen. */
  val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

  /** Synchronous read for the startup path, before any coroutine is available. */
  fun isEnabled(): Boolean = _enabled.value

  /** Persists [value] and publishes it; the caller applies it to the telemetry provider. */
  fun setEnabled(value: Boolean) {
    prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    _enabled.value = value
  }

  private companion object {
    const val PREFS_NAME = "pws_telemetry"
    const val KEY_ENABLED = "data_sending_enabled"
  }
}
