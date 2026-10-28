package io.github.alelk.pws.android.compose.di

import io.github.alelk.pws.android.compose.telemetry.TelemetryConsentStore
import io.github.alelk.pws.domain.telemetry.Telemetry
import org.koin.dsl.module

internal fun telemetryModule(telemetry: Telemetry, consent: TelemetryConsentStore) = module {
  single<Telemetry> { telemetry }
  single { consent }
}
