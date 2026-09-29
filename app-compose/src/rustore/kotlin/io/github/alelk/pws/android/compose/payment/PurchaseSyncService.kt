@file:OptIn(kotlin.time.ExperimentalTime::class)

package io.github.alelk.pws.android.compose.payment

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Date
import kotlin.time.Instant

/**
 * Reconciles online purchases into the offline legacy entitlement ([LegacyRuStoreEntitlementStore])
 * that [RuStoreCompatEntitlementRepository] reads — so once refreshed online, the unlock keeps
 * working entirely offline afterwards. Used only when purchases are enabled (`PremiumSales`).
 *
 * Writes are strictly monotonic (invariant I9) — a list of purchases can only grant or extend:
 * - `purchase_full_access` becomes `true` only for a paid `full_access_v1`; it is **never** written
 *   `false` — an empty list, another store account or a store outage must not erase a lifetime
 *   purchase (the fork's `syncDataStoreWithPurchases()` did exactly that);
 * - `purchase_subscription_until` only moves forward, to the latest expiry of the active paid
 *   subscriptions; a subscription without a date is ignored;
 * - an empty list (not signed in, SDK error, nothing bought) writes nothing.
 *
 * Refunds therefore do not revoke access automatically — a deliberate choice (plan §4.3).
 */
class PurchaseSyncService(
  private val store: LegacyRuStoreEntitlementStore,
  private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

  /** Grants or extends the entitlement derived from [purchases]; never lowers it. */
  suspend fun sync(purchases: List<ActivePurchase>) {
    val paid = purchases.filter { it.isPaid }
    if (paid.isEmpty()) return

    if (paid.any { it.productId == ProductIds.FULL_ACCESS_V1 }) store.grantLifetime()

    paid
      .filter { it.productId in ProductIds.SUBSCRIPTIONS }
      .mapNotNull { it.expiration }
      .maxByOrNull { it.time }
      ?.let { store.extendSubscriptionUntil(it.toLocalDate()) }
  }

  /**
   * Called right after the store reported a successful purchase of [productId]. The lifetime
   * product is granted immediately, as the fork did. A subscription is **not** marked as lifetime
   * (the fork's bug, plan §2.2): its expiry date arrives with the next [sync].
   */
  suspend fun onPurchaseSucceeded(productId: String) {
    if (productId == ProductIds.FULL_ACCESS_V1) store.grantLifetime()
  }

  /** The expiry day in the device time zone — the fork stored it the same way. */
  private fun Date.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(time).toLocalDateTime(timeZone()).date
}
