package io.github.alelk.pws.android.compose.telemetry

import android.app.Application
import android.util.Log
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import io.appmetrica.analytics.profile.Attribute
import io.appmetrica.analytics.profile.UserProfile
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.domain.telemetry.TelemetryPrivacy

/**
 * [Telemetry] on top of Yandex AppMetrica.
 *
 * AppMetrica installs the global crash and ANR handlers itself when the SDK is activated, so this
 * class only carries the explicit surface: non-fatals, breadcrumbs, product events and user
 * properties. Everything it sends goes through [TelemetryPrivacy] first — no song text, user edit or
 * search query can reach the network even if a call site passes one by mistake.
 */
class AppMetricaTelemetry : Telemetry {

  override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) {
    val safe = TelemetryPrivacy.sanitizeAttributes(
      attributes + (TelemetryAttr.ERROR_TYPE to (throwable::class.simpleName ?: "Throwable")),
    )
    // Error-environment values are attached to every subsequent report, so they are set right
    // before reporting and describe this error's context.
    safe.forEach { (key, value) -> AppMetrica.putErrorEnvironmentValue(key, value) }
    // The group id must be stable — a variable message would create a new error group per
    // occurrence and make the console useless.
    val groupId = message ?: (throwable::class.simpleName ?: "throwable")
    AppMetrica.reportError(groupId, groupId, throwable)
  }

  override fun log(message: String) {
    val safe = TelemetryPrivacy.sanitizeMessage(message) ?: return
    AppMetrica.reportEvent(BREADCRUMB_EVENT, mapOf(BREADCRUMB_PARAM to safe))
  }

  override fun event(name: String, params: Map<String, Any?>) {
    // Unknown names are dropped rather than sent: the event vocabulary is what the store
    // data-safety declarations describe, and it must not drift silently.
    if (!TelemetryPrivacy.isKnownEvent(name)) {
      Log.w(TAG, "Dropped undeclared telemetry event '$name' — add it to TelemetryEvent first")
      return
    }
    val safe = TelemetryPrivacy.sanitize(params)
    if (safe.isEmpty()) AppMetrica.reportEvent(name) else AppMetrica.reportEvent(name, safe)
  }

  override fun setUserProperty(key: String, value: String?) {
    if (key !in TelemetryAttr.keys) {
      Log.w(TAG, "Dropped undeclared telemetry user property '$key'")
      return
    }
    val attribute = Attribute.customString(key)
    val update = value
      ?.let { TelemetryPrivacy.sanitizeMessage(it) }
      ?.let { attribute.withValue(it) }
      ?: attribute.withValueReset()
    AppMetrica.reportUserProfile(UserProfile.newBuilder().apply(update).build())
  }

  companion object {
    private const val TAG = "PwsTelemetry"

    /** Breadcrumbs are ordinary events in AppMetrica; one reserved name keeps them out of the funnels. */
    private const val BREADCRUMB_EVENT = "breadcrumb"
    private const val BREADCRUMB_PARAM = "message"

    /**
     * Activates the SDK and returns the provider to bind, or [NoOpTelemetry] when [apiKey] is blank
     * (no key configured for this build — see `appmetrica.apiKey` in app-compose/build.gradle.kts).
     *
     * Must be called as the very first thing in `Application.onCreate()`: activation installs the
     * crash/ANR handlers, and anything that runs before it is invisible to the reports.
     *
     * [dataSendingEnabled] is the user's stored consent; passing it into the config means nothing is
     * transmitted before the choice is applied, rather than "send first, disable a moment later".
     */
    fun activate(
      application: Application,
      apiKey: String,
      dataSendingEnabled: Boolean,
      appVersion: String,
      environment: Map<String, String> = emptyMap(),
    ): Telemetry {
      if (apiKey.isBlank()) {
        Log.i(TAG, "AppMetrica API key is not configured — telemetry disabled for this build")
        return NoOpTelemetry
      }
      return try {
        val config = AppMetricaConfig.newConfigBuilder(apiKey)
          .withAppVersion(appVersion)
          .withCrashReporting(true)
          .withAnrMonitoring(true)
          .withSessionsAutoTrackingEnabled(true)
          .withDataSendingEnabled(dataSendingEnabled)
          .withLocationTracking(false)   // the app has no location feature; never ask for one
          .withAdvIdentifiersTracking(false) // no ads, no advertising identifiers
          .apply {
            TelemetryPrivacy.sanitizeAttributes(environment)
              .forEach { (key, value) -> withAppEnvironmentValue(key, value) }
          }
          .build()
        AppMetrica.activate(application, config)
        AppMetricaTelemetry()
      } catch (e: Throwable) {
        // Telemetry must never be the reason the app fails to start.
        Log.e(TAG, "AppMetrica activation failed — continuing without telemetry", e)
        NoOpTelemetry
      }
    }

    /** Applies a consent change to the running SDK. Safe to call when the SDK was never activated. */
    fun setDataSendingEnabled(enabled: Boolean) {
      runCatching { AppMetrica.setDataSendingEnabled(enabled) }
        .onFailure { Log.w(TAG, "Cannot apply data-sending consent", it) }
    }
  }
}
