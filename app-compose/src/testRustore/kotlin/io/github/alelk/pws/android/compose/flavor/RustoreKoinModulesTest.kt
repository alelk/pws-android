package io.github.alelk.pws.android.compose.flavor

import io.github.alelk.pws.android.compose.BuildConfig
import io.github.alelk.pws.android.compose.payment.PaymentController
import io.github.alelk.pws.android.compose.payment.PaymentProvider
import io.github.alelk.pws.android.compose.payment.PurchaseSyncService
import io.github.alelk.pws.features.monetization.MonetizationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.koin.dsl.koinApplication
import java.io.File

/**
 * Guardrails for "Pro — coming soon" (plan 2026-09-29, T-C02/T-C03, invariant I8): with purchases
 * disabled nothing that could call the RuStore Pay SDK or write the legacy entitlement file can even
 * be resolved.
 */
class RustoreKoinModulesTest : FunSpec({

  test("a build without -Ppws.rustore.purchasesEnabled is PremiumComingSoon") {
    BuildConfig.PURCHASES_ENABLED shouldBe false
    MONETIZATION shouldBe MonetizationMode.PremiumComingSoon
  }

  test("PremiumComingSoon: no payment provider, purchase sync or paywall controller in the graph") {
    val koin = koinApplication { modules(rustoreKoinModules(MonetizationMode.PremiumComingSoon)) }.koin
    koin.getOrNull<PaymentProvider>() shouldBe null
    koin.getOrNull<PurchaseSyncService>() shouldBe null
    koin.getOrNull<PaymentController>() shouldBe null
  }

  test("PremiumSales adds the payment module on top of the entitlement module") {
    rustoreKoinModules(MonetizationMode.PremiumComingSoon) shouldHaveSize 1
    rustoreKoinModules(MonetizationMode.PremiumSales) shouldHaveSize 2
  }

  test("ru.rustore.sdk is referenced only by the payment provider (single SDK touch point)") {
    val sources = File("src/rustore/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    sources.isEmpty() shouldBe false
    sources.filter { "ru.rustore.sdk" in it.readText() }.map { it.name }.sorted() shouldBe
      listOf("RuStorePaymentProvider.kt", "RustoreSdkOpts.kt")
  }
})
