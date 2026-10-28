@file:OptIn(kotlin.time.ExperimentalTime::class)

package io.github.alelk.pws.android.compose.payment

import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.domain.telemetry.TelemetryEvent
import io.github.alelk.pws.features.premium.EntitlementInfo
import io.github.alelk.pws.features.premium.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * RuStore compatibility [EntitlementRepository]: the offline source of truth for premium status.
 *
 * Read straight from the legacy fork's preferences ([LegacyRuStoreEntitlementStore]) — no network,
 * no Pay SDK (invariant I7) — and never writes anything. Rules, in priority order:
 * - `purchase_full_access == true` → [EntitlementInfo.Lifetime] (also for a subscriber the fork
 *   wrongly marked as lifetime: what is written is honoured, as the fork did);
 * - `purchase_subscription_until` today or later → [EntitlementInfo.Subscription]. The file only
 *   holds a date, and the real expiry moment was somewhere within that day, so the whole day counts
 *   (the fork cut the user off at its start);
 * - an earlier date → [EntitlementInfo.Expired];
 * - otherwise, or when the file cannot be read → [EntitlementInfo.None].
 *
 * The status is re-evaluated at every local midnight while the process lives, and
 * [currentInfo] re-checks the date whenever a gate asks.
 */
class RuStoreCompatEntitlementRepository internal constructor(
  store: LegacyRuStoreEntitlementStore,
  scope: CoroutineScope,
  private val clock: Clock = Clock.System,
  private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
  private val telemetry: Telemetry = NoOpTelemetry,
) : EntitlementRepository {

  constructor(store: LegacyRuStoreEntitlementStore, telemetry: Telemetry = NoOpTelemetry) :
    this(store, CoroutineScope(SupervisorJob() + Dispatchers.IO), telemetry = telemetry)

  @Volatile
  private var lastSnapshot: LegacyEntitlementSnapshot? = null
  private val resolvedReported = AtomicBoolean(false)

  override val info: StateFlow<EntitlementInfo> =
    combine(store.snapshot.onEach { lastSnapshot = it }, midnightTicks()) { snapshot, _ ->
      resolve(snapshot).also { reportResolvedOnce(snapshot, it) }
    }
      // Eagerly so the first definite value is available as soon as the file is read; gates only
      // wait while the value is still Unknown (the very first read).
      .stateIn(scope, SharingStarted.Eagerly, EntitlementInfo.Unknown)

  override fun currentInfo(): EntitlementInfo = lastSnapshot?.let(::resolve) ?: info.value

  private fun today(): LocalDate = clock.todayIn(timeZone())

  private fun resolve(snapshot: LegacyEntitlementSnapshot): EntitlementInfo = when (snapshot) {
    LegacyEntitlementSnapshot.ReadFailed -> EntitlementInfo.None
    is LegacyEntitlementSnapshot.Read -> {
      val until = snapshot.subscriptionUntil
      when {
        snapshot.fullAccess == true -> EntitlementInfo.Lifetime
        until != null && today() <= until -> EntitlementInfo.Subscription(until)
        until != null -> EntitlementInfo.Expired(until)
        else -> EntitlementInfo.None
      }
    }
  }

  /** Emits now and then right after every local midnight, so an expiring subscription flips while the app runs. */
  private fun midnightTicks(): Flow<Unit> = flow {
    while (true) {
      emit(Unit)
      val zone = timeZone()
      val nextMidnight = clock.todayIn(zone).plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
      delay((nextMidnight - clock.now()).coerceAtLeast(1.seconds) + 1.seconds)
    }
  }

  /** T-B06: one aggregate event per process, to verify after release that paid statuses survived. */
  private fun reportResolvedOnce(snapshot: LegacyEntitlementSnapshot, info: EntitlementInfo) {
    if (!resolvedReported.compareAndSet(false, true)) return
    val kind = when {
      snapshot is LegacyEntitlementSnapshot.ReadFailed -> "read_failed"
      info is EntitlementInfo.Lifetime -> "lifetime"
      info is EntitlementInfo.Subscription -> "subscription"
      info is EntitlementInfo.Expired -> "expired"
      else -> "none"
    }
    telemetry.event(
      TelemetryEvent.ENTITLEMENT_RESOLVED,
      mapOf(TelemetryAttr.KIND to kind, TelemetryAttr.SOURCE to "legacy_rustore"),
    )
  }
}
