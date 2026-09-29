@file:OptIn(kotlin.time.ExperimentalTime::class)

package io.github.alelk.pws.android.compose.payment

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.Date

/**
 * Invariant I9 (plan 2026-09-29, T-D01/T-D03): syncing purchases can only grant or extend premium —
 * never revoke or shorten it, whatever the store returns.
 */
class PurchaseSyncServiceTest : FunSpec({

  val utc = TimeZone.UTC
  fun dateOf(y: Int, m: Int, d: Int): Date = Date(LocalDateTime(y, m, d, 12, 0).toInstant(utc).toEpochMilliseconds())

  fun paid(productId: String, expiration: Date? = null) =
    ActivePurchase(productId = productId, invoiceId = "inv-$productId", title = productId, expiration = expiration, isPaid = true)

  fun unpaid(productId: String, expiration: Date? = null) = paid(productId, expiration).copy(isPaid = false)

  fun fixture(prefs: Preferences = emptyPreferences()): Pair<FakePreferencesDataStore, PurchaseSyncService> {
    val dataStore = FakePreferencesDataStore(prefs)
    return dataStore to PurchaseSyncService(LegacyRuStoreEntitlementStore(dataStore), timeZone = { utc })
  }

  val lifetime = mutablePreferencesOf(LegacyPreferenceKeys.PURCHASE_FULL_ACCESS to true)

  test("an empty purchase list keeps lifetime access and writes nothing (C3)") {
    val (store, sync) = fixture(lifetime)
    sync.sync(emptyList())
    store.state.value[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] shouldBe true
    store.writes shouldBe 0
  }

  test("a list without full_access (other account / only a subscription) never writes false") {
    val (store, sync) = fixture(lifetime)
    sync.sync(listOf(paid(ProductIds.MONTHLY_SUBSCRIPTION_V1, dateOf(2026, 10, 1))))
    store.state.value[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] shouldBe true
  }

  test("a paid full_access_v1 grants lifetime") {
    val (store, sync) = fixture()
    sync.sync(listOf(paid(ProductIds.FULL_ACCESS_V1)))
    store.state.value[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] shouldBe true
  }

  test("an unpaid full_access_v1 (e.g. invoice created, cancelled) grants nothing") {
    val (store, sync) = fixture()
    sync.sync(listOf(unpaid(ProductIds.FULL_ACCESS_V1)))
    store.state.value[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] shouldBe null
    store.writes shouldBe 0
  }

  test("a later subscription expiry extends the stored date") {
    val (store, sync) = fixture(mutablePreferencesOf(LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL to "2026-10-01"))
    sync.sync(listOf(paid(ProductIds.YEARLY_SUBSCRIPTION_V1, dateOf(2027, 10, 1))))
    store.state.value[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] shouldBe "2027-10-01"
  }

  test("an earlier subscription expiry never overwrites the stored date") {
    val (store, sync) = fixture(mutablePreferencesOf(LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL to "2027-10-01"))
    sync.sync(listOf(paid(ProductIds.MONTHLY_SUBSCRIPTION_V1, dateOf(2026, 11, 1))))
    store.state.value[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] shouldBe "2027-10-01"
    store.writes shouldBe 0
  }

  test("the latest of several active subscriptions wins") {
    val (store, sync) = fixture()
    sync.sync(
      listOf(
        paid(ProductIds.MONTHLY_SUBSCRIPTION_V1, dateOf(2026, 11, 1)),
        paid(ProductIds.YEARLY_SUBSCRIPTION_V1, dateOf(2027, 9, 30)),
        unpaid(ProductIds.YEARLY_SUBSCRIPTION_V1, dateOf(2030, 1, 1)),
      ),
    )
    store.state.value[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] shouldBe "2027-09-30"
  }

  test("a subscription without a date is ignored") {
    val (store, sync) = fixture()
    sync.sync(listOf(paid(ProductIds.MONTHLY_SUBSCRIPTION_V1, expiration = null)))
    store.writes shouldBe 0
  }

  test("an unparseable stored date is replaced by a valid later one") {
    val (store, sync) = fixture(mutablePreferencesOf(LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL to "garbage"))
    sync.sync(listOf(paid(ProductIds.MONTHLY_SUBSCRIPTION_V1, dateOf(2026, 11, 1))))
    store.state.value[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] shouldBe "2026-11-01"
  }

  test("a successful lifetime purchase is granted at once; a subscription purchase is not marked lifetime (T-D03)") {
    val (store, sync) = fixture()
    sync.onPurchaseSucceeded(ProductIds.MONTHLY_SUBSCRIPTION_V1)
    store.state.value[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] shouldBe null
    sync.onPurchaseSucceeded(ProductIds.FULL_ACCESS_V1)
    store.state.value[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] shouldBe true
  }
})
