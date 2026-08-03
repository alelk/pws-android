package io.github.alelk.pws.android.compose.flavor

import android.content.Context
import org.koin.core.module.Module

/**
 * Free (Google Play) build: no paywall, no payment SDK. Entitlement stays at the default
 * always-active binding from `featuresModule`, so every premium gate is transparent.
 */
fun flavorKoinModules(): List<Module> = emptyList()

/** No paywall in the free builds — premium gates never fire. */
fun flavorShowPaywall(context: Context) = Unit

/** Whether this flavor sells premium features (controls the paywall settings entry). */
const val IS_PAYMENT_FLAVOR: Boolean = false
