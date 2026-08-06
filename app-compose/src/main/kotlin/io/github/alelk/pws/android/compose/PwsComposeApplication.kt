package io.github.alelk.pws.android.compose

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cafe.adriel.voyager.core.registry.ScreenRegistry
import io.github.alelk.pws.android.compose.donation.SharedPrefsDonationPromptStateRepository
import io.github.alelk.pws.contentdelivery.di.contentDeliveryModule
import io.github.alelk.pws.data.repository.room.di.repoRoomModule
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.database.pwsContentKeyHex
import io.github.alelk.pws.domain.donationprompt.config.DonationConfig
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateReadRepository
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateWriteRepository
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

  private val applicationScope =
    CoroutineScope(
      SupervisorJob() +
        Dispatchers.IO +
        CoroutineExceptionHandler { _, e -> android.util.Log.e("PwsApp", "Background task failed", e) }
    )

  override fun onCreate() {
    super.onCreate()

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
      val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
      single { PwsAppInfo(version) }
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
        // Flavor overrides load last so they win (e.g. rustore overrides EntitlementRepository).
        *flavorKoinModules().toTypedArray(),
      )
    }

    applicationScope.launch {
      PwsDatabaseProvider.runLegacyMigration(this@PwsComposeApplication, get())
    }
  }
}
