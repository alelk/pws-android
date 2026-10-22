package io.github.alelk.pws.android.compose.di

import io.github.alelk.pws.android.compose.BuildConfig
import io.github.alelk.pws.android.compose.flavor.flavorKoinModules
import io.github.alelk.pws.contentdelivery.di.contentDeliveryModule
import io.github.alelk.pws.data.repository.room.di.repoRoomModule
import io.github.alelk.pws.database.pwsContentKeyHex
import io.github.alelk.pws.features.di.featuresModule
import io.github.alelk.pws.features.di.useCasesModule
import org.koin.core.module.Module

/**
 * All Koin modules of the app, in loading order. The order is significant — a later module overrides an
 * earlier one (README §6 p. 4 of the 2026-09-30 plan): featuresModule, then preferences, monetization and
 * telemetry (which override its defaults), then the flavor modules.
 */
internal fun appModules(inputs: AppModuleInputs): List<Module> = listOf(
  databaseModule,
  appInfoModule(inputs.appVersion),
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
  preferencesModule,
  startupModule(inputs.legacyMigrationDone),
  monetizationModule,
  // After featuresModule (overrides its NoOpTelemetry default), before the flavor modules so
  // a flavor could still substitute its own provider.
  telemetryModule(inputs.telemetry, inputs.telemetryConsent),
  // Flavor overrides load last so they win (e.g. rustore overrides EntitlementRepository).
) + flavorKoinModules()
