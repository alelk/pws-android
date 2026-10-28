package io.github.alelk.pws.android.compose.payment

import android.content.Context
import android.content.Intent
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

/**
 * [PaymentProvider] backed by the RuStore Pay SDK. This is the only class that references
 * `ru.rustore.sdk.*`; it maps the SDK types to the generic payment models.
 *
 * Created only when purchases are enabled (`MonetizationMode.PremiumSales`, Gradle flag
 * `pws.rustore.purchasesEnabled`); in `PremiumComingSoon` it is not registered in Koin at all, so the
 * SDK is never called. Callers must treat failures softly (see [PaymentController]) — the offline
 * entitlement in [RuStoreCompatEntitlementRepository] remains the source of truth regardless.
 */
class RuStorePaymentProvider(
  private val appContext: Context,
) : PaymentProvider {

  private val client get() = RuStorePayClient.instance
  private val productInteractor get() = client.getProductInteractor()
  private val purchaseInteractor get() = client.getPurchaseInteractor()
  private val userInteractor get() = client.getUserInteractor()

  override suspend fun authStatus(): AuthStatus =
    when (userInteractor.getUserAuthorizationStatus().coAwait()) {
      UserAuthorizationStatus.AUTHORIZED -> AuthStatus.AUTHORIZED
      UserAuthorizationStatus.UNAUTHORIZED -> AuthStatus.UNAUTHORIZED
    }

  override suspend fun products(ids: List<String>): List<PaymentProduct> =
    productInteractor.getProducts(ids.map { ProductId(it) }).coAwait().map { product ->
      PaymentProduct(
        id = product.productId.value,
        title = product.title.value,
        description = product.description?.value,
        priceLabel = product.amountLabel.value,
        type = if (product.productId.value in ProductIds.SUBSCRIPTIONS) {
          PaymentProductType.SUBSCRIPTION
        } else {
          PaymentProductType.NON_CONSUMABLE
        },
      )
    }

  override suspend fun purchases(): List<ActivePurchase> =
    purchaseInteractor.getPurchases(null, null, null).coAwait().mapNotNull { purchase ->
      when (purchase) {
        is SubscriptionPurchase -> ActivePurchase(
          productId = purchase.productId.value,
          invoiceId = purchase.invoiceId.value,
          title = purchase.description.value,
          expiration = purchase.expirationDate,
          isPaid = purchase.status in ACTIVE_SUBSCRIPTION_STATUSES,
        )

        is ProductPurchase -> ActivePurchase(
          productId = purchase.productId.value,
          invoiceId = purchase.invoiceId.value,
          title = purchase.description.value,
          expiration = null,
          isPaid = purchase.status in PAID_PRODUCT_STATUSES,
        )

        else -> null
      }
    }

  override suspend fun purchase(productId: String): PurchaseResult =
    try {
      purchaseInteractor.purchase(
        params = ProductPurchaseParams(productId = ProductId(productId)),
        preferredPurchaseType = PreferredPurchaseType.ONE_STEP,
      ).coAwait()
      PurchaseResult.Success
    } catch (_: RuStorePaymentException.ProductPurchaseCancelled) {
      PurchaseResult.Cancelled
    } catch (e: Throwable) {
      PurchaseResult.Failed(e)
    }

  override fun openAuthorization() {
    val intent = appContext.packageManager.getLaunchIntentForPackage("ru.vk.store")
      ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      ?: return
    appContext.startActivity(intent)
  }

  override fun proceedIntent(intent: Intent) {
    runCatching { client.getIntentInteractor().proceedIntent(intent) }
  }

  internal companion object {
    /**
     * Statuses that may grant premium (PurchaseSyncService only acts on isPaid purchases, and its
     * grants are permanent). Checked against Pay SDK 11.1.0 (BOM 2026.08.01). Purchases here are
     * ONE_STEP, whose final state is CONFIRMED. PAID is the funds-on-hold state of a two-step
     * payment that can still be REVERSED, so it does not count; neither do INVOICE_CREATED,
     * PROCESSING, EXECUTING, CANCELLED, REJECTED, EXPIRED, REFUNDING, REFUNDED, REVERSED.
     */
    val PAID_PRODUCT_STATUSES = setOf(ProductPurchaseStatus.CONFIRMED)

    /** Only a currently active subscription extends the expiry date (not PAUSED / TERMINATED / CLOSED / …). */
    val ACTIVE_SUBSCRIPTION_STATUSES = setOf(SubscriptionPurchaseStatus.ACTIVE)
  }
}
