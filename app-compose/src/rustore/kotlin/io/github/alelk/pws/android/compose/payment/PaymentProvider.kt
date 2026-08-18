package io.github.alelk.pws.android.compose.payment

import android.content.Intent
import java.util.Date

/**
 * Store product ids. These MUST match the ids configured in the RuStore console and the ones the
 * legacy fork used, otherwise existing purchases would not be recognised.
 */
object ProductIds {
  const val FULL_ACCESS_V1 = "full_access_v1"
  const val MONTHLY_SUBSCRIPTION_V1 = "monthly_subscription_v1"
  const val YEARLY_SUBSCRIPTION_V1 = "yearly_subscription_v1"
  val ALL = listOf(FULL_ACCESS_V1, MONTHLY_SUBSCRIPTION_V1, YEARLY_SUBSCRIPTION_V1)
  val SUBSCRIPTIONS = listOf(MONTHLY_SUBSCRIPTION_V1, YEARLY_SUBSCRIPTION_V1)
}

enum class PaymentProductType { NON_CONSUMABLE, SUBSCRIPTION }

/** A purchasable product, decoupled from any store SDK type. */
data class PaymentProduct(
  val id: String,
  val title: String,
  val description: String?,
  val priceLabel: String,
  val type: PaymentProductType,
)

/** An owned purchase, decoupled from any store SDK type. */
data class ActivePurchase(
  val productId: String,
  val invoiceId: String,
  val title: String,
  /** Non-null only for subscriptions. */
  val expiration: Date?,
)

enum class AuthStatus { AUTHORIZED, UNAUTHORIZED }

/** Result of a purchase attempt. */
sealed interface PurchaseResult {
  data object Success : PurchaseResult
  data object Cancelled : PurchaseResult
  data class Failed(val cause: Throwable) : PurchaseResult
}

/**
 * The single point that knows about a concrete payment SDK. Everything above it (paywall UI,
 * entitlement, gates) speaks only in the generic models here, so swapping the store means writing a
 * new [PaymentProvider] — nothing else changes.
 */
interface PaymentProvider {
  /** Whether the user is signed in to the store account. */
  suspend fun authStatus(): AuthStatus

  /** Loads product metadata for [ids]. Does not require authorization. */
  suspend fun products(ids: List<String>): List<PaymentProduct>

  /** Loads the user's owned purchases. Requires authorization. */
  suspend fun purchases(): List<ActivePurchase>

  /** Starts the purchase flow for [productId]. */
  suspend fun purchase(productId: String): PurchaseResult

  /** Opens the store app so the user can sign in / restore purchases. */
  fun openAuthorization()

  /** Feeds a deeplink [intent] back into the SDK (payment return). No-op if unsupported. */
  fun proceedIntent(intent: Intent)
}
