package com.example.app.android.flavor

import android.content.Context
import android.content.Intent
import com.example.app.android.BuildConfig
import com.example.app.android.payment.LocalEntitlementRepository
import com.example.app.android.payment.LocalEntitlementStore
import com.example.app.android.payment.PaymentActivity
import com.example.app.android.payment.PaymentController
import com.example.app.android.payment.PaymentProvider
import com.example.app.android.payment.PurchaseSyncService
import com.example.app.android.payment.RuStorePaymentProvider
import com.example.app.domain.telemetry.BillingTelemetry
import com.example.app.features.premium.EntitlementRepository
import com.example.app.features.premium.MonetizationMode
import com.example.app.features.premium.PremiumFeature
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/*
 * TEMPLATE — app/src/<store>/kotlin/com/example/app/android/flavor/FlavorIntegration.kt.
 *
 * Every flavor source set has a file with this name declaring the SAME public names (MONETIZATION,
 * flavorKoinModules, flavorShowPaywall); `main` calls them without knowing the flavor. Pin the contract
 * with a FlavorContract-style source test so a rename in one flavor fails on any flavor's build.
 *
 * Evidence: pws-android app-compose/src/rustore/.../flavor/FlavorIntegration.kt.
 */

/**
 * Sales are switched on by the Gradle property `app.store.purchasesEnabled` (default false) →
 * BuildConfig.PURCHASES_ENABLED. While off, the build is PremiumComingSoon: existing entitlements are
 * honoured, a blocked gate says "coming soon", and the store SDK is never called.
 */
val MONETIZATION: MonetizationMode =
    if (BuildConfig.PURCHASES_ENABLED) MonetizationMode.PremiumSales else MonetizationMode.PremiumComingSoon

/**
 * Loaded AFTER the shared features module, so its EntitlementRepository overrides the default
 * AlwaysActive one. The payment stack (SDK provider, sync, controller) is registered only when
 * purchases are enabled: otherwise nothing that could call the SDK or write the entitlement can even
 * be resolved. `internal` + parameterised by mode so a test can build both graphs.
 */
internal fun storeKoinModules(mode: MonetizationMode): List<Module> =
    buildList {
        add(
            module {
                // The one owner of the entitlement file in this process.
                single { LocalEntitlementStore(androidContext(), get<BillingTelemetry>()) }
                single<EntitlementRepository> { LocalEntitlementRepository(get<LocalEntitlementStore>(), get<BillingTelemetry>()) }
            },
        )
        if (mode.purchasesEnabled) {
            add(
                module {
                    single<PaymentProvider> { RuStorePaymentProvider(androidContext()) }
                    single { PurchaseSyncService(get<LocalEntitlementStore>()) }
                    single { PaymentController(get(), get(), get<BillingTelemetry>()) }
                },
            )
        }
    }

fun flavorKoinModules(): List<Module> = storeKoinModules(MONETIZATION)

/**
 * Opens the store paywall. The shell never offers a paywall action when purchases are off; this is
 * the second line of defence. PaymentActivity itself, when started by the payment-return deeplink
 * with purchases off, forwards to the main activity without resolving PaymentController.
 */
@Suppress("UNUSED_PARAMETER")
fun flavorShowPaywall(context: Context, feature: PremiumFeature?) {
    if (!MONETIZATION.purchasesEnabled) return
    context.startActivity(Intent(context, PaymentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
