package com.example.app.android.flavor

import android.content.Context
import com.example.app.features.premium.MonetizationMode
import com.example.app.features.premium.PremiumFeature
import org.koin.core.module.Module

/*
 * TEMPLATE — app/src/<flavor-without-store-sdk>/kotlin/com/example/app/android/flavor/FlavorIntegration.kt.
 *
 * A flavor that links no store SDK: the same public names as the store flavor, all no-ops. Entitlement
 * stays at the shared module's AlwaysActiveEntitlementRepository, so every premium gate is transparent.
 *
 * Evidence: pws-android app-compose/src/{ru,uk,full}/.../flavor/FlavorIntegration.kt.
 */
val MONETIZATION: MonetizationMode = MonetizationMode.Donations

fun flavorKoinModules(): List<Module> = emptyList()

/** No paywall in this build — premium gates never fire. */
@Suppress("UNUSED_PARAMETER")
fun flavorShowPaywall(context: Context, feature: PremiumFeature?) = Unit
