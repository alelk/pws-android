@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.alelk.pws.android.compose.payment

import androidx.datastore.preferences.core.mutablePreferencesOf
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.features.premium.EntitlementInfo
import io.github.alelk.pws.features.premium.PremiumStatus
import io.github.alelk.pws.features.premium.status
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The offline unlock (invariants I6, I7): premium status is derived purely from the fork's own
 * preferences file — no network, no Pay SDK — and the file is never modified.
 *
 * Golden files: `src/testRustore/resources/legacy-entitlement/` (tools/make-legacy-entitlement-fixtures.py),
 * read with a clock fixed at 2026-09-29 12:00 UTC.
 */
class RuStoreCompatEntitlementRepositoryTest : FunSpec({

  val utc = TimeZone.UTC
  fun fixedClock(at: Instant) = object : Clock {
    override fun now(): Instant = at
  }
  val sep29 = fixedClock(LocalDateTime(2026, 9, 29, 12, 0).toInstant(utc))

  class RecordingTelemetry : Telemetry {
    val events = mutableListOf<Pair<String, Map<String, Any?>>>()
    val errors = mutableListOf<String?>()
    override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) {
      errors += message
    }
    override fun log(message: String) = Unit
    override fun event(name: String, params: Map<String, Any?>) {
      events += name to params
    }
    override fun setUserProperty(key: String, value: String?) = Unit
  }

  data class Golden(val fixture: String, val expected: EntitlementInfo, val kind: String)

  context("golden fork files (T-B05)") {
    listOf(
      Golden("none.preferences_pb", EntitlementInfo.None, "none"),
      Golden("lifetime.preferences_pb", EntitlementInfo.Lifetime, "lifetime"),
      Golden("lifetime_false_sub.preferences_pb", EntitlementInfo.Subscription(LocalDate(2026, 10, 15)), "subscription"),
      Golden("sub_future.preferences_pb", EntitlementInfo.Subscription(LocalDate(2026, 10, 15)), "subscription"),
      Golden("sub_past.preferences_pb", EntitlementInfo.Expired(LocalDate(2026, 9, 1)), "expired"),
      Golden("sub_and_full.preferences_pb", EntitlementInfo.Lifetime, "lifetime"),
      Golden("bad_date.preferences_pb", EntitlementInfo.None, "none"),
      Golden("corrupted.preferences_pb", EntitlementInfo.None, "read_failed"),
    ).forEach { (fixture, expected, kind) -> test("$fixture → $expected") {
      val file = legacyPreferencesFile(fixture)
      val hashBefore = file.sha256()
      val telemetry = RecordingTelemetry()
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
      try {
        val repo = RuStoreCompatEntitlementRepository(
          store = LegacyRuStoreEntitlementStore(fileDataStore(file), telemetry),
          scope = scope,
          clock = sep29,
          timeZone = { utc },
          telemetry = telemetry,
        )

        withTimeout(10.seconds) { repo.info.first { it != EntitlementInfo.Unknown } } shouldBe expected
        repo.currentInfo() shouldBe expected
        // T-B06: one aggregate event, no dates, no identifiers.
        telemetry.events shouldContainExactly listOf(
          "entitlement_resolved" to mapOf("kind" to kind, "source" to "legacy_rustore"),
        )
      } finally {
        scope.cancel()
      }
      // I6: reading never rewrites, repairs or recreates the file.
      file.sha256() shouldBe hashBefore
    } }
  }

  test("a corrupted file is reported as a non-fatal and resolves to Inactive without a crash (T-B02)") {
    val telemetry = RecordingTelemetry()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    try {
      val store = LegacyRuStoreEntitlementStore(fileDataStore(legacyPreferencesFile("corrupted.preferences_pb")), telemetry)
      val repo = RuStoreCompatEntitlementRepository(store, scope, sep29, { utc }, telemetry)
      withTimeout(10.seconds) { repo.status.first { it != PremiumStatus.Unknown } } shouldBe PremiumStatus.Inactive
      telemetry.errors shouldContainExactly listOf("entitlement_read_failed")
    } finally {
      scope.cancel()
    }
  }

  test("an unparseable date is a non-fatal without the raw value") {
    val telemetry = RecordingTelemetry()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    try {
      val store = LegacyRuStoreEntitlementStore(fileDataStore(legacyPreferencesFile("bad_date.preferences_pb")), telemetry)
      store.snapshot.first() shouldBe LegacyEntitlementSnapshot.Read(fullAccess = null, subscriptionUntil = null, rawSubscriptionUntil = "garbage")
      telemetry.errors shouldContainExactly listOf("entitlement_bad_subscription_date")
    } finally {
      scope.cancel()
    }
  }

  context("subscription expiry (T-B03)") {
    fun repoAt(until: String, clock: Clock, scope: CoroutineScope) = RuStoreCompatEntitlementRepository(
      store = LegacyRuStoreEntitlementStore(
        FakePreferencesDataStore(mutablePreferencesOf(LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL to until)),
      ),
      scope = scope,
      clock = clock,
      timeZone = { utc },
    )

    test("the whole expiry day still counts: today == D → Active") {
      runTest {
        val repo = repoAt("2026-09-29", fixedClock(LocalDateTime(2026, 9, 29, 23, 59, 59).toInstant(utc)), backgroundScope)
        runCurrent()
        repo.info.value shouldBe EntitlementInfo.Subscription(LocalDate(2026, 9, 29))
      }
    }

    test("the day after: D+1 → Expired") {
      runTest {
        val repo = repoAt("2026-09-29", fixedClock(LocalDateTime(2026, 9, 30, 0, 0, 1).toInstant(utc)), backgroundScope)
        runCurrent()
        repo.info.value shouldBe EntitlementInfo.Expired(LocalDate(2026, 9, 29))
        repo.info.value.status shouldBe PremiumStatus.Inactive
      }
    }

    test("midnight passes while the process lives → Expired, without any new file emission") {
      runTest {
        val start = LocalDateTime(2026, 9, 29, 23, 59, 0).toInstant(utc)
        val clock = object : Clock {
          override fun now(): Instant = start + testScheduler.currentTime.milliseconds
        }
        val repo = repoAt("2026-09-29", clock, backgroundScope)
        runCurrent()
        repo.info.value shouldBe EntitlementInfo.Subscription(LocalDate(2026, 9, 29))

        // Half a second past midnight: the midnight tick (00:00:01) has not fired yet, but a gate
        // asking right now already gets the fresh answer.
        advanceTimeBy(60.seconds + 500.milliseconds)
        runCurrent()
        repo.info.value shouldBe EntitlementInfo.Subscription(LocalDate(2026, 9, 29))
        repo.currentInfo() shouldBe EntitlementInfo.Expired(LocalDate(2026, 9, 29))

        advanceTimeBy(2.minutes)
        runCurrent()
        repo.info.value shouldBe EntitlementInfo.Expired(LocalDate(2026, 9, 29))
      }
    }
  }
})
