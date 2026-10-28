@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.app.android.payment

import android.content.Intent
import kotlin.time.Instant

/*
 * TEMPLATE — store flavor source set: app/src/<store>/kotlin/com/example/app/android/payment/.
 *
 * The port between the app and ONE store SDK. Everything above it (paywall UI, sync, entitlement,
 * gates) speaks only these generic models, so switching RuStore -> Google Play -> Huawei means writing
 * one new PaymentProvider implementation; nothing else changes.
 *
 * Evidence: pws-android app-compose/src/rustore/.../payment/PaymentProvider.kt.
 */

/**
 * Store product ids. MUST equal the ids configured in the store console (and those any previous app
 * used), otherwise existing purchases are not recognised. Never user data — safe in telemetry.
 */
object ProductIds {
    const val LIFETIME = "full_access_v1"
    const val MONTHLY = "monthly_subscription_v1"
    const val YEARLY = "yearly_subscription_v1"
    val ALL = listOf(LIFETIME, MONTHLY, YEARLY)
    val SUBSCRIPTIONS = listOf(MONTHLY, YEARLY)
}

enum class PaymentProductType { NON_CONSUMABLE, SUBSCRIPTION }

/** A purchasable product, decoupled from any store SDK type. */
data class PaymentProduct(
    val id: String,
    val title: String,
    val description: String?,
    /** Localised price string from the store; never format prices yourself. */
    val priceLabel: String,
    val type: PaymentProductType,
)

/** An owned purchase, decoupled from any store SDK type. */
data class ActivePurchase(
    val productId: String,
    /** Store-side id (invoice / order / purchase token). Diagnostics only; never sent to telemetry. */
    val storePurchaseId: String,
    val title: String,
    /** Non-null only for subscriptions, when the store reports an end. */
    val expiresAt: Instant?,
    /**
     * True ONLY when the store reports the purchase as final and paid (a product) or the subscription
     * as currently active. The adapter owns the mapping from SDK statuses; only `isPaid` purchases may
     * grant or extend premium (see PurchaseSyncService), and those grants are permanent.
     */
    val isPaid: Boolean,
)

enum class AuthStatus { AUTHORIZED, UNAUTHORIZED }

sealed interface PurchaseResult {
    data object Success : PurchaseResult

    data object Cancelled : PurchaseResult

    data class Failed(val cause: Throwable) : PurchaseResult
}

/**
 * The single point that knows about a concrete store SDK. Implementations may throw from any suspend
 * function; callers (PaymentController) guard every call. `purchase` never throws: it maps the
 * cancel exception to [PurchaseResult.Cancelled] and everything else to [PurchaseResult.Failed].
 */
interface PaymentProvider {
    /** Whether the user is signed in to the store account. Stores without a sign-in concept return AUTHORIZED. */
    suspend fun authStatus(): AuthStatus

    /** Product metadata for [ids]. Must not require sign-in. */
    suspend fun products(ids: List<String>): List<PaymentProduct>

    /** The user's owned purchases, every status — the caller filters on [ActivePurchase.isPaid]. */
    suspend fun purchases(): List<ActivePurchase>

    /** Runs the purchase flow for [productId] to its final state (incl. acknowledgement where the store needs it). */
    suspend fun purchase(productId: String): PurchaseResult

    /** Opens the store app so the user can sign in / restore purchases. Throws if there is no store app. */
    fun openAuthorization()

    /** Feeds a payment-return deeplink [intent] back into the SDK. No-op when the store has none. */
    fun proceedIntent(intent: Intent)
}
