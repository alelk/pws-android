package io.github.alelk.pws.android.compose.payment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The paid-status DataStore. This is the SAME file the legacy RuStore fork (2.3.1) wrote to, so an
 * in-place update reads the user's already-paid status here — the unlock survives with no network
 * and no Pay SDK. Do NOT rename the file or the keys.
 *
 * A single process-wide delegate is required (DataStore forbids two instances for one file), so both
 * the read side ([RuStoreCompatEntitlementRepository]) and the write side ([PurchaseSyncService]) go
 * through this one accessor.
 */
internal val Context.paymentPreferencesDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "pws-app-preferences")

internal object PaymentPreferenceKeys {
  val PURCHASE_FULL_ACCESS = booleanPreferencesKey("purchase_full_access")
  val PURCHASE_SUBSCRIPTION_UNTIL = stringPreferencesKey("purchase_subscription_until")
}

/** `yyyy-MM-dd`, `Locale.US` — the exact format the fork stores the subscription expiry in. */
internal val subscriptionUntilFormat: SimpleDateFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

internal suspend fun DataStore<Preferences>.setFullAccessPaid(value: Boolean) {
  edit { it[PaymentPreferenceKeys.PURCHASE_FULL_ACCESS] = value }
}

internal suspend fun DataStore<Preferences>.setSubscriptionUntil(date: Date) {
  edit { it[PaymentPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] = subscriptionUntilFormat.format(date) }
}
