package io.github.alelk.pws.android.compose.di

import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.domain.telemetry.Telemetry
import kotlinx.coroutines.CompletableDeferred

/** What the application hands to the Koin modules that cannot build it themselves. */
internal class AppModuleInputs(
  val appVersion: String,
  val telemetry: Telemetry,
  val telemetryConsent: TelemetryConsentStore,
  val legacyMigrationDone: CompletableDeferred<Unit>,
)
