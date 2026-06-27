package io.github.alelk.pws.android.compose

import android.app.Application
import android.content.Context
import cafe.adriel.voyager.core.registry.ScreenRegistry
import io.github.alelk.pws.android.compose.donation.SharedPrefsDonationPromptStateRepository
import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.contentdelivery.di.contentDeliveryModule
import io.github.alelk.pws.data.repository.room.di.repoRoomModule
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.database.pwsContentKeyHex
import io.github.alelk.pws.domain.donationprompt.config.DonationConfig
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateReadRepository
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateWriteRepository
import io.github.alelk.pws.features.app.PwsAppInfo
import io.github.alelk.pws.features.booklibrary.BookLibraryFirstLaunchState
import io.github.alelk.pws.features.di.appScreenModule
import io.github.alelk.pws.features.di.featuresModule
import io.github.alelk.pws.features.di.useCasesModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

class PwsComposeApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    // Register Voyager screen registry
    ScreenRegistry {
      appScreenModule()
    }

    val databaseModule = module {
      single<PwsDatabase> { PwsDatabaseProvider.getDatabase(androidContext()) }
    }

    val appInfoModule = module {
      val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
      single { PwsAppInfo(version) }
    }

    val firstLaunchModule = module {
      single<BookLibraryFirstLaunchState> { BookLibraryFirstLaunchStateImpl(androidContext()) }
    }

    val donationModule = module {
      single { DonationConfig(enabled = true, boostyUrl = "https://boosty.to/hymna") }
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
        firstLaunchModule,
        donationModule,
        repoRoomModule,
        contentDeliveryModule(
          catalogUrl = BuildConfig.CATALOG_URL,
          bundleVariant = BuildConfig.BUNDLE_VARIANT,
          keyProvider = ContentKeyProvider { pwsContentKeyHex() },
        ),
        useCasesModule,
        featuresModule,
      )
    }
  }
}
