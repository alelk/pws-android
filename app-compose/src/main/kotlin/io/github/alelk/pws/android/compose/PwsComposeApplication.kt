package io.github.alelk.pws.android.compose

import android.app.Application
import cafe.adriel.voyager.core.registry.ScreenRegistry
import io.github.alelk.pws.data.repository.room.di.repoRoomModule
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.features.di.appScreenModule
import io.github.alelk.pws.features.di.featuresModule
import io.github.alelk.pws.features.di.useCasesModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
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

    startKoin {
      androidContext(this@PwsComposeApplication)
      modules(
        databaseModule,
        repoRoomModule,
        useCasesModule,
        featuresModule,
      )
    }
  }
}
