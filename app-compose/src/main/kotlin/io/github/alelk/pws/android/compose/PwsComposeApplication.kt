package io.github.alelk.pws.android.compose

import android.app.Application
import cafe.adriel.voyager.core.registry.ScreenRegistry
import io.github.alelk.pws.android.compose.di.AppModuleInputs
import io.github.alelk.pws.android.compose.di.appModules
import io.github.alelk.pws.android.compose.flavor.flavorStartupTasks
import io.github.alelk.pws.android.compose.telemetry.AppMetricaTelemetry
import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.features.di.appScreenModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import timber.log.Timber

class PwsComposeApplication : Application() {

  /**
   * Set once telemetry is activated in [onCreate]. Read (not captured) by the exception handler
   * below, which is constructed before activation happens.
   */
  @Volatile
  private var telemetry: Telemetry = NoOpTelemetry

  private val applicationScope =
    CoroutineScope(
      SupervisorJob() +
        Dispatchers.IO +
        CoroutineExceptionHandler { _, e ->
          android.util.Log.e("PwsApp", "Background task failed", e)
          // Background failures used to die in logcat only; now they surface as non-fatals.
          telemetry.recordError(e, "background_task_failed")
        },
    )

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

    val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"

    // Telemetry first: activation installs the crash/ANR handlers, so anything initialised before
    // it would crash invisibly. Debug builds default to not sending, to keep dev runs out of the
    // production statistics (flip the settings toggle to test the pipeline).
    //
    // On a first launch the store is still "pending" — isEnabled() is false — so the SDK activates
    // without transmitting anything until the user has seen the disclosure (AppStartupModel resolves it).
    val telemetryConsent = TelemetryConsentStore(this, defaultConsent = !BuildConfig.DEBUG)
    telemetry = AppMetricaTelemetry.activate(
      application = this,
      apiKey = BuildConfig.APPMETRICA_API_KEY,
      dataSendingEnabled = telemetryConsent.isEnabled(),
      appVersion = appVersion,
      environment = mapOf(
        TelemetryAttr.FLAVOR to BuildConfig.FLAVOR,
        TelemetryAttr.BUNDLE_VARIANT to BuildConfig.BUNDLE_VARIANT,
        TelemetryAttr.APP_VERSION to appVersion,
      ),
      verboseLogs = BuildConfig.DEBUG,
    )
    telemetry.setUserProperty(TelemetryAttr.FLAVOR, BuildConfig.FLAVOR)
    telemetry.setUserProperty(TelemetryAttr.BUNDLE_VARIANT, BuildConfig.BUNDLE_VARIANT)
    telemetry.setUserProperty(TelemetryAttr.DEVICE_LANGUAGE, java.util.Locale.getDefault().language)

    // Register Voyager screen registry
    ScreenRegistry {
      appScreenModule()
    }

    // Opens once the legacy-database migration has finished (see LegacyMigrationGate, di/StartupModule).
    val legacyMigrationDone = CompletableDeferred<Unit>()

    // Defensive: guards against a stray already-started Koin instance (e.g. Robolectric
    // re-instantiating the Application without a clean process restart between test runs).
    if (GlobalContext.getOrNull() != null) stopKoin()

    startKoin {
      androidContext(this@PwsComposeApplication)
      modules(appModules(AppModuleInputs(appVersion, telemetry, telemetryConsent, legacyMigrationDone)))
    }

    // Legacy data first, strictly before anything installs books (C2 in the 2026-09-29 plan). The
    // gate opens in `finally` — a failed migration must never keep the app on the loading screen.
    applicationScope.launch {
      try {
        runCatching { flavorStartupTasks(GlobalContext.get()) }
          .onFailure { telemetry.recordError(it, "flavor_startup_failed") }
        val outcomes = PwsDatabaseProvider.runLegacyMigration(this@PwsComposeApplication, get())
        telemetry.reportLegacyMigration(outcomes)
      } finally {
        legacyMigrationDone.complete(Unit)
      }
    }
  }
}
