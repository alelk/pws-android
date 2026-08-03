package io.github.alelk.pws.android.compose.flavor

import android.content.Context
import android.content.Intent
import io.github.alelk.pws.android.compose.payment.PaymentActivity
import io.github.alelk.pws.android.compose.payment.PaymentController
import io.github.alelk.pws.android.compose.payment.PaymentProvider
import io.github.alelk.pws.android.compose.payment.PurchaseSyncService
import io.github.alelk.pws.android.compose.payment.RuStoreCompatEntitlementRepository
import io.github.alelk.pws.android.compose.payment.RuStorePaymentProvider
import io.github.alelk.pws.features.premium.EntitlementRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * RuStore build wiring. Loaded after `featuresModule`, so its [EntitlementRepository] definition
 * overrides the default always-active one — premium status now comes from the offline
 * [RuStoreCompatEntitlementRepository].
 */
private val rustorePaymentModule: Module = module {
  single<PaymentProvider> { RuStorePaymentProvider(androidContext()) }
  single { PurchaseSyncService(androidContext()) }
  single { PaymentController(get(), get()) }
  single<EntitlementRepository> { RuStoreCompatEntitlementRepository(androidContext()) }
}

fun flavorKoinModules(): List<Module> = listOf(rustorePaymentModule)

/** Opens the paywall in response to a blocked premium gate. */
fun flavorShowPaywall(context: Context) {
  context.startActivity(
    Intent(context, PaymentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
  )
}

/** This flavor sells premium features — the paywall settings entry is shown. */
const val IS_PAYMENT_FLAVOR: Boolean = true
