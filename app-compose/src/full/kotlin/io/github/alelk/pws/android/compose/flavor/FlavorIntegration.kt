package io.github.alelk.pws.android.compose.flavor

import android.content.Context
import io.github.alelk.pws.features.monetization.MonetizationMode
import io.github.alelk.pws.features.premium.PremiumFeature
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Free (Google Play) build: no paywall, no payment SDK. Entitlement stays at the default
 * always-active binding from `featuresModule`, so every premium gate is transparent. The donation
 * prompt is on — this is how the free builds ask for support.
 */
val MONETIZATION: MonetizationMode = MonetizationMode.Donations

fun flavorKoinModules(): List<Module> = emptyList()

/** No flavor-specific startup work in the free builds. */
@Suppress("UNUSED_PARAMETER")
suspend fun flavorStartupTasks(koin: Koin) = Unit

/** No paywall in the free builds — premium gates never fire. */
@Suppress("UNUSED_PARAMETER")
fun flavorShowPaywall(context: Context, feature: PremiumFeature?) = Unit
