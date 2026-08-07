package io.github.alelk.pws.android.compose

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cafe.adriel.voyager.core.registry.ScreenRegistry
import io.github.alelk.pws.android.compose.donation.SharedPrefsDonationPromptStateRepository
import io.github.alelk.pws.android.compose.telemetry.AppMetricaTelemetry
import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.contentdelivery.di.contentDeliveryModule
import io.github.alelk.pws.data.repository.room.di.repoRoomModule
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.database.pwsContentKeyHex
import io.github.alelk.pws.domain.donationprompt.config.DonationConfig
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateReadRepository
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateWriteRepository
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.features.app.PwsAppInfo
import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.android.compose.flavor.flavorKoinModules
import io.github.alelk.pws.features.di.appScreenModule
import io.github.alelk.pws.features.di.featuresModule
import io.github.alelk.pws.features.di.useCasesModule
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

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
        }
    )

  override fun onCreate() {
    super.onCreate()

    val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"

    // Telemetry first: activation installs the crash/ANR handlers, so anything initialised before
    // it would crash invisibly. Debug builds default to not sending, to keep dev runs out of the
    // production statistics (flip the settings toggle to test the pipeline).
    val telemetryConsent = TelemetryConsentStore(this, defaultEnabled = !BuildConfig.DEBUG)
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
    )
    telemetry.setUserProperty(TelemetryAttr.FLAVOR, BuildConfig.FLAVOR)
    telemetry.setUserProperty(TelemetryAttr.BUNDLE_VARIANT, BuildConfig.BUNDLE_VARIANT)
    telemetry.setUserProperty(TelemetryAttr.DEVICE_LANGUAGE, java.util.Locale.getDefault().language)

    // Register Voyager screen registry
    ScreenRegistry {
      appScreenModule()
    }

    val databaseModule = module {
      single<PwsDatabase> { PwsDatabaseProvider.getDatabase(androidContext()) }
      single<DataStore<Preferences>> { androidContext().appSettingsDataStore() }
      single { BackupManager(get<PwsDatabase>(), get<DataStore<Preferences>>()) }
    }

    val appInfoModule = module {
      single { PwsAppInfo(appVersion) }
    }

    val telemetryModule = module {
      single<Telemetry> { telemetry }
      single { telemetryConsent }
    }

    val deviceLanguageModule = module {
      single(named("deviceLanguage")) { java.util.Locale.getDefault().language }
    }

    val donationModule = module {
      // Donation prompt is on only for donation-mode builds; premium-selling builds suppress it.
      single { DonationConfig(enabled = MONETIZATION.donationsEnabled, boostyUrl = "https://boosty.to/hymna") }
      single {
        SharedPrefsDonationPromptStateRepository(
          androidContext().getSharedPreferences("pws_donation", Context.MODE_PRIVATE)
        )
      } binds arrayOf(DonationPromptStateReadRepository::class, DonationPromptStateWriteRepository::class)
    }

    startKoin {
      androidContext(this@PwsComposeApplication)
      modules(
        databaseModule,
        appInfoModule,
        deviceLanguageModule,
        donationModule,
        repoRoomModule,
        contentDeliveryModule(
          catalogUrls = BuildConfig.CATALOG_URLS.split(",").map { it.trim() },
          bundleVariant = BuildConfig.BUNDLE_VARIANT,
          keyProvider = { pwsContentKeyHex() },
        ),
        useCasesModule,
        featuresModule,
        // After featuresModule (overrides its NoOpTelemetry default), before the flavor modules so
        // a flavor could still substitute its own provider.
        telemetryModule,
        // Flavor overrides load last so they win (e.g. rustore overrides EntitlementRepository).
        *flavorKoinModules().toTypedArray(),
      )
    }

    applicationScope.launch {
      PwsDatabaseProvider.runLegacyMigration(this@PwsComposeApplication, get())
    }
  }
}
