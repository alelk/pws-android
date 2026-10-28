@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.app.android.payment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

/*
 * TEMPLATE — app/src/test<Store>/kotlin/.../payment/PurchaseSyncServiceTest.kt.
 *
 * The lock of this skill's central invariant: syncing purchases can only grant or extend premium —
 * never revoke or shorten it, whatever the store returns. `writes shouldBe 0` is the sharp part: an
 * implementation that rewrites the same value "to be safe" is one refactor away from writing false.
 *
 * See it red: in PurchaseSyncService.sync, replace the lifetime line with
 *   `if (paid.none { it.productId == ProductIds.LIFETIME }) store.<a revoke you add for the experiment>()`
 * (the fork's syncDataStoreWithPurchases), or drop the `if (paid.isEmpty()) return`; revert.
 *
 * Evidence: pws-android app-compose/src/testRustore/.../payment/PurchaseSyncServiceTest.kt.
 */
class PurchaseSyncServiceTest :
    FunSpec({
        fun at(epochMs: Long) = Instant.fromEpochMilliseconds(epochMs)
        val earlierEnd = at(1_790_000_000_000)
        val laterEnd = at(1_822_000_000_000)

        fun paid(productId: String, expiresAt: Instant? = null) =
            ActivePurchase(productId = productId, storePurchaseId = "inv-$productId", title = productId, expiresAt = expiresAt, isPaid = true)

        fun unpaid(productId: String, expiresAt: Instant? = null) = paid(productId, expiresAt).copy(isPaid = false)

        fun fixture(prefs: Preferences = emptyPreferences()): Pair<FakePreferencesDataStore, PurchaseSyncService> {
            val dataStore = FakePreferencesDataStore(prefs)
            return dataStore to PurchaseSyncService(LocalEntitlementStore(dataStore))
        }

        val lifetime = mutablePreferencesOf(EntitlementKeys.LIFETIME to true)

        test("an empty purchase list keeps lifetime access and writes nothing") {
            val (store, sync) = fixture(lifetime)
            sync.sync(emptyList())
            store.state.value[EntitlementKeys.LIFETIME] shouldBe true
            store.writes shouldBe 0
        }

        test("a list without the lifetime product (other account / only a subscription) never writes false") {
            val (store, sync) = fixture(lifetime)
            sync.sync(listOf(paid(ProductIds.MONTHLY, earlierEnd)))
            store.state.value[EntitlementKeys.LIFETIME] shouldBe true
        }

        test("only unpaid purchases (invoice created, on hold, cancelled) write nothing") {
            val (store, sync) = fixture(lifetime)
            sync.sync(listOf(unpaid(ProductIds.LIFETIME), unpaid(ProductIds.YEARLY, laterEnd)))
            store.state.value[EntitlementKeys.LIFETIME] shouldBe true
            store.writes shouldBe 0
        }

        test("the pair: a paid lifetime product grants lifetime") {
            val (store, sync) = fixture()
            sync.sync(listOf(paid(ProductIds.LIFETIME)))
            store.state.value[EntitlementKeys.LIFETIME] shouldBe true
        }

        test("an unpaid lifetime product grants nothing") {
            val (store, sync) = fixture()
            sync.sync(listOf(unpaid(ProductIds.LIFETIME)))
            store.state.value[EntitlementKeys.LIFETIME] shouldBe null
            store.writes shouldBe 0
        }

        test("a later subscription end extends the stored one") {
            val (store, sync) = fixture(mutablePreferencesOf(EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS to earlierEnd.toEpochMilliseconds()))
            sync.sync(listOf(paid(ProductIds.YEARLY, laterEnd)))
            store.state.value[EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS] shouldBe laterEnd.toEpochMilliseconds()
        }

        test("an earlier subscription end never overwrites the stored one") {
            val (store, sync) = fixture(mutablePreferencesOf(EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS to laterEnd.toEpochMilliseconds()))
            sync.sync(listOf(paid(ProductIds.MONTHLY, earlierEnd)))
            store.state.value[EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS] shouldBe laterEnd.toEpochMilliseconds()
            store.writes shouldBe 0
        }

        test("the latest of several PAID subscriptions wins; an unpaid later one is ignored") {
            val (store, sync) = fixture()
            sync.sync(listOf(paid(ProductIds.MONTHLY, earlierEnd), paid(ProductIds.YEARLY, laterEnd), unpaid(ProductIds.YEARLY, at(4_000_000_000_000))))
            store.state.value[EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS] shouldBe laterEnd.toEpochMilliseconds()
        }

        test("a subscription without an end is ignored") {
            val (store, sync) = fixture()
            sync.sync(listOf(paid(ProductIds.MONTHLY, expiresAt = null)))
            store.writes shouldBe 0
        }

        test("after a successful purchase: lifetime is granted at once, a subscription is never marked lifetime") {
            val (store, sync) = fixture()
            sync.onPurchaseSucceeded(ProductIds.MONTHLY)
            store.state.value[EntitlementKeys.LIFETIME] shouldBe null
            sync.onPurchaseSucceeded(ProductIds.LIFETIME)
            store.state.value[EntitlementKeys.LIFETIME] shouldBe true
        }
    })

/** In-memory DataStore: synchronous, dispatcher-free (works under virtual time), counts real changes. */
internal class FakePreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    var writes = 0
        private set

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        if (updated != state.value) writes++
        state.value = updated
        return updated
    }
}
