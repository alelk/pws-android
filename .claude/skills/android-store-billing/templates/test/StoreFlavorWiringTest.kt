package com.example.app.android.flavor

import com.example.app.android.BuildConfig
import com.example.app.android.payment.LocalEntitlementStore
import com.example.app.android.payment.PaymentController
import com.example.app.android.payment.PaymentProvider
import com.example.app.android.payment.PurchaseSyncService
import com.example.app.features.premium.MonetizationMode
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.koin.dsl.koinApplication
import java.io.File

/*
 * TEMPLATE — app/src/test<Store>/kotlin/.../flavor/StoreFlavorWiringTest.kt (flavor-scoped: it
 * references flavor classes, so it compiles only in that flavor's unit-test task).
 *
 * Evidence: pws-android app-compose/src/testRustore/.../flavor/RustoreKoinModulesTest.kt
 * (plan 2026-09-29 invariant I8: with purchases off, nothing that could call the SDK or write the
 * entitlement file can be resolved).
 */

/** Same placeholder rule as PaymentSdkIsolationTest: set it per project (e.g. `ru.rustore.sdk`). */
private const val STORE_SDK_PACKAGE_PREFIX = "com.example.storesdk"

/** The only files of the store flavor that may import the SDK. Adding one is a reviewed decision. */
private val SDK_TOUCH_POINTS: Map<String, String> =
    mapOf(
        "RuStorePaymentProvider.kt" to "the adapter: SDK types -> generic payment models",
    )

class StoreFlavorWiringTest :
    FunSpec({
        test("a build without -Papp.store.purchasesEnabled does not sell") {
            BuildConfig.PURCHASES_ENABLED shouldBe false
            MONETIZATION shouldBe MonetizationMode.PremiumComingSoon
        }

        test("PremiumComingSoon: no payment provider, purchase sync or paywall controller in the graph") {
            val koin = koinApplication { modules(storeKoinModules(MonetizationMode.PremiumComingSoon)) }.koin
            koin.getOrNull<PaymentProvider>() shouldBe null
            koin.getOrNull<PurchaseSyncService>() shouldBe null
            koin.getOrNull<PaymentController>() shouldBe null
        }

        test("the pair: PremiumSales adds the payment module on top of the entitlement module") {
            storeKoinModules(MonetizationMode.PremiumComingSoon) shouldHaveSize 1
            storeKoinModules(MonetizationMode.PremiumSales) shouldHaveSize 2
        }

        test("the SDK is imported only by the adapter files (single touch point)") {
            // cwd = the app module directory.
            val sources = File("src/rustore/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            withClue("no sources under src/rustore/kotlin — wrong cwd?") { sources.isEmpty() shouldBe false }
            val touching = sources.filter { STORE_SDK_PACKAGE_PREFIX in it.readText() }.map { it.name }.toSet()
            // Exact equality: fails on a new SDK user AND on the adapter vanishing (a vacuous pass).
            touching shouldBe SDK_TOUCH_POINTS.keys
        }

        test("the entitlement store has no revoking API") {
            // Monotonic by construction: a method that does not exist cannot be called from a sync branch.
            val names = LocalEntitlementStore::class.java.declaredMethods.map { it.name.substringBefore('$') }
            withClue(names) {
                names.none { Regex("""(?i)revoke|clear|remove|reset|setLifetime|setSubscription""").containsMatchIn(it) } shouldBe true
            }
        }
    })
