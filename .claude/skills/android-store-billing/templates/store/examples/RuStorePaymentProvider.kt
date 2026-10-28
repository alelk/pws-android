@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.app.android.payment

import android.content.Context
import android.content.Intent
import ru.rustore.sdk.core.tasks.Task
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchase
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductPurchaseStatus
import ru.rustore.sdk.pay.model.RuStorePaymentException
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus
import ru.rustore.sdk.pay.model.UserAuthorizationStatus
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Instant

/*
 * WORKED EXAMPLE — the adapter for RuStore Pay SDK (`ru.rustore.sdk:pay`, checked against 11.1.0 via
 * BOM 2026.08.01). Lives in app/src/rustore/ only. This file (and nothing else in main code) imports
 * the SDK package; StoreFlavorWiringTest pins that.
 *
 * Writing an adapter for another store (Google Play Billing, Huawei IAP, …): keep the shape — map SDK
 * types to the generic models, decide `isPaid` from an EXPLICIT allow-set of final statuses (never a
 * deny-set: a status added in an SDK update must not grant by default), map the cancel exception to
 * PurchaseResult.Cancelled. See references/purchase-flow.md for the Play mapping.
 *
 * SDK init: RuStore auto-initialises from manifest meta-data (`console_app_id_value`,
 * `sdk_pay_scheme_value`) in the rustore manifest overlay; the deeplink scheme on the payment-return
 * activity MUST equal `sdk_pay_scheme_value`. Keep the console app id in a string resource, not here.
 *
 * Evidence: pws-android RuStorePaymentProvider.kt + RustoreSdkOpts.kt.
 */
class RuStorePaymentProvider(
    private val appContext: Context,
) : PaymentProvider {
    private val client get() = RuStorePayClient.instance
    private val productInteractor get() = client.getProductInteractor()
    private val purchaseInteractor get() = client.getPurchaseInteractor()
    private val userInteractor get() = client.getUserInteractor()

    override suspend fun authStatus(): AuthStatus =
        when (userInteractor.getUserAuthorizationStatus().await()) {
            UserAuthorizationStatus.AUTHORIZED -> AuthStatus.AUTHORIZED
            UserAuthorizationStatus.UNAUTHORIZED -> AuthStatus.UNAUTHORIZED
        }

    override suspend fun products(ids: List<String>): List<PaymentProduct> =
        productInteractor.getProducts(ids.map { ProductId(it) }).await().map { product ->
            PaymentProduct(
                id = product.productId.value,
                title = product.title.value,
                description = product.description?.value,
                priceLabel = product.amountLabel.value,
                type = if (product.productId.value in ProductIds.SUBSCRIPTIONS) PaymentProductType.SUBSCRIPTION else PaymentProductType.NON_CONSUMABLE,
            )
        }

    override suspend fun purchases(): List<ActivePurchase> =
        purchaseInteractor.getPurchases(null, null, null).await().mapNotNull { purchase ->
            when (purchase) {
                is SubscriptionPurchase ->
                    ActivePurchase(
                        productId = purchase.productId.value,
                        storePurchaseId = purchase.invoiceId.value,
                        title = purchase.description.value,
                        expiresAt = purchase.expirationDate?.let { Instant.fromEpochMilliseconds(it.time) },
                        isPaid = purchase.status in ACTIVE_SUBSCRIPTION_STATUSES,
                    )

                is ProductPurchase ->
                    ActivePurchase(
                        productId = purchase.productId.value,
                        storePurchaseId = purchase.invoiceId.value,
                        title = purchase.description.value,
                        expiresAt = null,
                        isPaid = purchase.status in PAID_PRODUCT_STATUSES,
                    )

                else -> null
            }
        }

    override suspend fun purchase(productId: String): PurchaseResult =
        try {
            purchaseInteractor
                .purchase(
                    params = ProductPurchaseParams(productId = ProductId(productId)),
                    // ONE_STEP: funds are captured immediately; the final state is CONFIRMED and there
                    // is no separate confirm call to forget. TWO_STEP would need a confirm (or cancel)
                    // after PAID — and a PAID-but-unconfirmed purchase must never grant.
                    preferredPurchaseType = PreferredPurchaseType.ONE_STEP,
                ).await()
            PurchaseResult.Success
        } catch (_: RuStorePaymentException.ProductPurchaseCancelled) {
            PurchaseResult.Cancelled
        } catch (e: Throwable) {
            PurchaseResult.Failed(e)
        }

    override fun openAuthorization() {
        val intent =
            appContext.packageManager
                .getLaunchIntentForPackage(RUSTORE_APP_PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: throw IllegalStateException("RuStore app is not installed")
        appContext.startActivity(intent)
    }

    override fun proceedIntent(intent: Intent) {
        runCatching { client.getIntentInteractor().proceedIntent(intent) }
    }

    internal companion object {
        private const val RUSTORE_APP_PACKAGE = "ru.vk.store"

        /**
         * Allow-set of statuses that may grant (grants are permanent). ONE_STEP purchases end in
         * CONFIRMED. NOT included: PAID (funds on hold in a two-step payment, can still be REVERSED),
         * INVOICE_CREATED, PROCESSING, EXECUTING, CANCELLED, REJECTED, EXPIRED, REFUNDING, REFUNDED,
         * REVERSED. Re-check against the SDK docs on every SDK upgrade.
         */
        val PAID_PRODUCT_STATUSES = setOf(ProductPurchaseStatus.CONFIRMED)

        /** Only a currently active subscription extends the end (not PAUSED / TERMINATED / CLOSED / …). */
        val ACTIVE_SUBSCRIPTION_STATUSES = setOf(SubscriptionPurchaseStatus.ACTIVE)
    }
}

/** Bridges a RuStore SDK [Task] into a coroutine. */
private suspend fun <T> Task<T>.await(): T =
    suspendCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
