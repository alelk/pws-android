package io.github.alelk.pws.android.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.github.alelk.pws.android.compose.telemetry.AppMetricaTelemetry
import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.features.telemetry.TelemetrySettings

/**
 * Public privacy policy, linked from Settings → Privacy and from the store listings. Must stay
 * in sync with docs/privacy-policy.md and with the Play Data Safety / RuStore declarations.
 */
const val PRIVACY_POLICY_URL = "https://github.com/alelk/pws-android/blob/master/docs/privacy-policy.md"

/** The telemetry consent state of [consent] as the [TelemetrySettings] that pws-core's UI shows. */
@Composable
fun rememberTelemetrySettings(consent: TelemetryConsentStore): TelemetrySettings {
  val enabled by consent.enabled.collectAsState()
  val pending by consent.pending.collectAsState()
  val applyConsent = remember<(Boolean) -> Unit>(consent) {
    { value ->
      consent.setEnabled(value)
      AppMetricaTelemetry.setDataSendingEnabled(value)
    }
  }
  return remember(enabled, pending) {
    TelemetrySettings(
      dataSendingEnabled = enabled,
      onDataSendingEnabledChange = applyConsent,
      privacyPolicyUrl = PRIVACY_POLICY_URL,
      // Non-null only until the first-launch disclosure in onboarding is answered; nothing is
      // transmitted while it is. Paths that never show onboarding get this default applied by
      // pws-core's AppStartupModel once the app opens.
      pendingConsentDefault = if (pending) consent.defaultConsent else null,
    )
  }
}
