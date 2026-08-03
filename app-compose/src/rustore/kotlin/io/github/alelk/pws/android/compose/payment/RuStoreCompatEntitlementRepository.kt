package io.github.alelk.pws.android.compose.payment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.features.premium.EntitlementRepository
import io.github.alelk.pws.features.premium.PremiumStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date

/**
 * RuStore compatibility [EntitlementRepository]: the offline source of truth for premium status.
 *
 * Premium is active when either full access was purchased or an unexpired subscription date is
 * stored — read straight from [paymentPreferencesDataStore], so it works with no network and no Pay
 * SDK. Online refresh (when monetisation is live) is handled by [PurchaseSyncService], which only
 * writes back into this same DataStore.
 */
class RuStoreCompatEntitlementRepository internal constructor(
  private val dataStore: DataStore<Preferences>,
  scope: CoroutineScope,
) : EntitlementRepository {

  constructor(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
  ) : this(context.applicationContext.paymentPreferencesDataStore, scope)

  override val status: StateFlow<PremiumStatus> =
    dataStore.data
      .map { prefs ->
        val fullAccess = prefs[PaymentPreferenceKeys.PURCHASE_FULL_ACCESS] == true
        val subscriptionActive = prefs[PaymentPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL]
          ?.let { runCatching { subscriptionUntilFormat.parse(it) }.getOrNull() }
          ?.let { it.after(Date()) } == true
        if (fullAccess || subscriptionActive) PremiumStatus.Active else PremiumStatus.Inactive
      }
      // Eagerly so the first definite value is available as soon as DataStore is read; the gate only
      // blocks while the value is still Unknown (the very first read).
      .stateIn(scope, SharingStarted.Eagerly, PremiumStatus.Unknown)
}
