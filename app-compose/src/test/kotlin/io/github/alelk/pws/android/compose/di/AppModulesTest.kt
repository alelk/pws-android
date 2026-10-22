package io.github.alelk.pws.android.compose.di

import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.android.compose.DataStoreUserPreferencesRepository
import io.github.alelk.pws.android.compose.LegacyMigrationGate
import io.github.alelk.pws.android.compose.PwsComposeApplication
import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.domain.donationprompt.config.DonationConfig
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateReadRepository
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateWriteRepository
import io.github.alelk.pws.domain.preferences.repository.UserPreferencesRepository
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.features.app.PwsAppInfo
import io.github.alelk.pws.features.monetization.MonetizationMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named

/**
 * Plan 2026-09-30, 05.3: the Koin graph the real [PwsComposeApplication] starts (Robolectric creates it)
 * resolves the key types of every module in `di/`, and the module order is intact (README §6 p. 4): the
 * later overriding modules (preferences, monetization, telemetry) win over pws-core's defaults.
 * Types behind the database (repositories, use cases, AppStartupTasks) are not resolved:
 * the native SQLite of the Robolectric sandbox is unavailable here.
 *
 * Red check: move `preferencesModule` before `featuresModule` in [appModules] and run
 * `./gradlew :app-compose:testRuDebugUnitTest --tests '*AppModulesTest*'`, then revert.
 */
@RobolectricTest(sdk = [34])
class AppModulesTest :
  FeatureSpec({

    afterTest { runCatching { stopKoin() } }

    fun koin(): org.koin.core.Koin {
      ApplicationProvider.getApplicationContext<PwsComposeApplication>()
      return GlobalContext.get()
    }

    feature("application graph") {
      // One scenario: Robolectric starts the Application (and Koin) once per test, afterTest stops it.
      scenario("key types resolve and later modules override pws-core defaults") {
        val koin = koin()
        koin.get<PwsAppInfo>() shouldNotBe null
        koin.get<String>(named("deviceLanguage")) shouldNotBe null
        koin.get<DonationConfig>().enabled shouldBe MONETIZATION.donationsEnabled
        koin.get<DonationPromptStateReadRepository>() shouldNotBe null
        koin.get<DonationPromptStateWriteRepository>() shouldNotBe null
        koin.get<LegacyMigrationGate>() shouldNotBe null
        koin.get<TelemetryConsentStore>() shouldNotBe null

        koin.get<UserPreferencesRepository>().shouldBeInstanceOf<DataStoreUserPreferencesRepository>()
        koin.get<MonetizationMode>() shouldBe MONETIZATION
        koin.get<Telemetry>() shouldNotBe NoOpTelemetry
      }
    }
  })
