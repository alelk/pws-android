package io.github.alelk.pws.android.compose.payment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.github.alelk.pws.features.premium.PremiumStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Verifies the offline unlock (invariant I4): premium status is derived purely from the local
 * DataStore, with no network and no Pay SDK involved.
 */
class RuStoreCompatEntitlementRepositoryTest : FunSpec({

  fun tempStore(): DataStore<Preferences> {
    val file = File.createTempFile("pws-app-preferences", ".preferences_pb").apply {
      delete()
      deleteOnExit()
    }
    return PreferenceDataStoreFactory.create(produceFile = { file })
  }

  fun repo(store: DataStore<Preferences>) =
    RuStoreCompatEntitlementRepository(store, CoroutineScope(SupervisorJob() + Dispatchers.IO))

  suspend fun DataStore<Preferences>.firstDefiniteStatus(): PremiumStatus {
    val store = this
    return withTimeout(5_000) { repo(store).status.first { it != PremiumStatus.Unknown } }
  }

  test("full access purchased resolves to Active with no SDK/network") {
    val store = tempStore()
    store.edit { it[PaymentPreferenceKeys.PURCHASE_FULL_ACCESS] = true }
    store.firstDefiniteStatus() shouldBe PremiumStatus.Active
  }

  test("no purchase resolves to Inactive") {
    tempStore().firstDefiniteStatus() shouldBe PremiumStatus.Inactive
  }

  test("unexpired subscription date resolves to Active") {
    val store = tempStore()
    val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      .format(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))
    store.edit { it[PaymentPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] = tomorrow }
    store.firstDefiniteStatus() shouldBe PremiumStatus.Active
  }

  test("expired subscription date resolves to Inactive") {
    val store = tempStore()
    val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      .format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
    store.edit { it[PaymentPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] = yesterday }
    store.firstDefiniteStatus() shouldBe PremiumStatus.Inactive
  }
})
