package io.github.alelk.pws.android.compose.di

import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.features.monetization.MonetizationMode
import org.koin.dsl.module

/** The build's monetization mode for pws-core (UpsellHost, Settings). Loaded after featuresModule. */
internal val monetizationModule = module {
  single<MonetizationMode> { MONETIZATION }
}
