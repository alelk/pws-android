package io.github.alelk.pws.android.compose.payment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.util.Date

/**
 * Reconciles online purchases into the offline [paymentPreferencesDataStore].
 *
 * When monetisation is live and the user is authorized, this reads the owned purchases and writes
 * the derived full-access / subscription-until values back into the DataStore that
 * [RuStoreCompatEntitlementRepository] reads. That way, once refreshed online, the unlock keeps
 * working entirely offline afterwards.
 */
class PurchaseSyncService internal constructor(
  private val dataStore: DataStore<Preferences>,
) {
  constructor(context: Context) : this(context.applicationContext.paymentPreferencesDataStore)

  /** Writes the entitlement derived from [purchases] into the DataStore. */
  suspend fun sync(purchases: List<ActivePurchase>) {
    val hasFullAccess = purchases.any { it.productId == ProductIds.FULL_ACCESS_V1 }
    dataStore.setFullAccessPaid(hasFullAccess)

    val subscriptionUntil: Date? = purchases
      .filter { it.productId in ProductIds.SUBSCRIPTIONS }
      .mapNotNull { it.expiration }
      .maxByOrNull { it.time }
    subscriptionUntil?.let { dataStore.setSubscriptionUntil(it) }
  }
}
