package io.github.alelk.pws.android.compose.flavor

import android.content.Context
import android.content.Intent
import io.github.alelk.pws.android.compose.BuildConfig
import io.github.alelk.pws.android.compose.appSettingsDataStore
import io.github.alelk.pws.android.compose.payment.LegacyRuStoreEntitlementStore
import io.github.alelk.pws.android.compose.payment.LegacySettingsImporter
import io.github.alelk.pws.android.compose.payment.PaymentActivity
import io.github.alelk.pws.android.compose.payment.PaymentController
import io.github.alelk.pws.android.compose.payment.PaymentProvider
import io.github.alelk.pws.android.compose.payment.PurchaseSyncService
import io.github.alelk.pws.android.compose.payment.RuStoreCompatEntitlementRepository
import io.github.alelk.pws.android.compose.payment.RuStorePaymentProvider
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.features.monetization.MonetizationMode
import io.github.alelk.pws.features.premium.EntitlementRepository
import io.github.alelk.pws.features.premium.PremiumFeature
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * RuStore: premium features are gated. Purchases are switched on by the Gradle property
 * `pws.rustore.purchasesEnabled` (default `false`) — while RuStore monetisation is off the build is
 * [MonetizationMode.PremiumComingSoon]: existing entitlements are honoured, a blocked gate shows
 * "Pro — coming soon", and the Pay SDK is never called. No donation prompt in either mode.
 */
val MONETIZATION: MonetizationMode =
  if (BuildConfig.PURCHASES_ENABLED) MonetizationMode.PremiumSales else MonetizationMode.PremiumComingSoon

/**
 * RuStore Koin wiring for [mode]. Loaded after `featuresModule`, so its [EntitlementRepository]
 * overrides the default always-active one — premium status comes from the offline, read-only
 * [RuStoreCompatEntitlementRepository] in both modes.
 *
 * The payment stack (Pay SDK provider, purchase sync, paywall controller) is registered only when
 * purchases are enabled: in [MonetizationMode.PremiumComingSoon] nothing that could call the SDK or
 * write the legacy entitlement file can even be resolved (invariant I8).
 */
internal fun rustoreKoinModules(mode: MonetizationMode): List<Module> = buildList {
  add(
    module {
      // The one owner of the legacy `pws-app-preferences` file in this process (invariant I6).
      single { LegacyRuStoreEntitlementStore(androidContext(), get<Telemetry>()) }
      single<EntitlementRepository> { RuStoreCompatEntitlementRepository(get<LegacyRuStoreEntitlementStore>(), get<Telemetry>()) }
      single {
        LegacySettingsImporter(
          legacyPreferences = get<LegacyRuStoreEntitlementStore>().preferences,
          appSettings = androidContext().appSettingsDataStore(),
          telemetry = get<Telemetry>(),
        )
      }
    },
  )
  if (mode.purchasesEnabled) {
    add(
      module {
        single<PaymentProvider> { RuStorePaymentProvider(androidContext()) }
        single { PurchaseSyncService(get<LegacyRuStoreEntitlementStore>()) }
        single { PaymentController(get(), get(), get()) }
      },
    )
  }
}

fun flavorKoinModules(): List<Module> = rustoreKoinModules(MONETIZATION)

/** Imports the RuStore fork's theme / text settings once (before the UI leaves the loading screen). */
suspend fun flavorStartupTasks(koin: Koin) {
  koin.get<LegacySettingsImporter>().importOnce()
}

/**
 * Opens the store paywall. Only reachable when purchases are enabled — in `PremiumComingSoon` the
 * shell never passes a paywall action, and this is a no-op as a second line of defence.
 */
@Suppress("UNUSED_PARAMETER")
fun flavorShowPaywall(context: Context, feature: PremiumFeature?) {
  if (!MONETIZATION.purchasesEnabled) return
  context.startActivity(
    Intent(context, PaymentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
  )
}
