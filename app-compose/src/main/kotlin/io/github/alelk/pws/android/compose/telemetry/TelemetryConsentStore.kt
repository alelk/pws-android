package io.github.alelk.pws.android.compose.telemetry

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's "send crash reports and statistics" choice.
 *
 * Backed by [SharedPreferences] rather than the app's DataStore on purpose: the value is needed
 * **synchronously in `Application.onCreate()`**, before the telemetry SDK is activated, and DataStore
 * only offers suspending reads. Blocking the main thread on a DataStore read at startup would be
 * worse than a one-key SharedPreferences file.
 *
 * Until the user has answered the first-launch disclosure the store is [pending]: [enabled] reports
 * `false`, so **nothing is transmitted before the user has been told what is collected**. The shell
 * resolves the pending state exactly once — from the onboarding disclosure when it is shown, or by
 * committing [defaultConsent] on the paths that never show onboarding (an existing install being
 * updated, a build with preloaded songbooks). [defaultConsent] is on for release builds — crash
 * reporting is the low-risk, legitimate-interest baseline and analytics stays opt-out via the
 * settings toggle — and off for debug builds so developer runs never pollute production statistics.
 */
class TelemetryConsentStore(
  context: Context,
  /** The value proposed to the user by the disclosure, and used when no disclosure can be shown. */
  val defaultConsent: Boolean,
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  // Absent key = no answer yet. Note this is also the state of an existing install updating to the
  // first build with telemetry; that path is resolved by the shell, not by defaulting here.
  private val _pending = MutableStateFlow(!prefs.contains(KEY_ENABLED))

  private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))

  /** Current consent, observable by the settings screen. `false` while [pending]. */
  val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

  /** True until an explicit choice has been stored. */
  val pending: StateFlow<Boolean> = _pending.asStateFlow()

  /** Synchronous read for the startup path, before any coroutine is available. */
  fun isEnabled(): Boolean = _enabled.value

  /** Synchronous read for the startup path. */
  fun isPending(): Boolean = _pending.value

  /** Persists [value] and publishes it; the caller applies it to the telemetry provider. */
  fun setEnabled(value: Boolean) {
    prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    _enabled.value = value
    _pending.value = false
  }

  private companion object {
    const val PREFS_NAME = "pws_telemetry"
    const val KEY_ENABLED = "data_sending_enabled"
  }
}
