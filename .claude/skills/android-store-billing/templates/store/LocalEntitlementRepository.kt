@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.app.android.payment

import com.example.app.domain.telemetry.BillingTelemetry
import com.example.app.domain.telemetry.NoOpBillingTelemetry
import com.example.app.features.premium.EntitlementInfo
import com.example.app.features.premium.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/*
 * TEMPLATE — store flavor source set. The offline, READ-ONLY EntitlementRepository.
 *
 * - No network, no store SDK: premium works offline, without the store app, and in a build that never
 *   calls the SDK (PremiumComingSoon). The store only reconciles INTO LocalEntitlementStore.
 * - Never writes. Writing is PurchaseSyncService's job, through the store's monotonic API.
 * - Rules, in priority order: lifetime == true -> Lifetime; subscription end in the future ->
 *   Subscription; in the past -> Expired; otherwise, or ReadFailed -> None.
 * - Time is injected (Clock) so expiry is testable; the state flips AT the expiry moment while the
 *   process lives, and currentInfo() re-checks whenever a gate asks.
 *
 * Evidence: pws-android RuStoreCompatEntitlementRepository.kt (there the file holds a date, so it
 * ticks at local midnight and counts the whole expiry day; here the end is an instant).
 */
class LocalEntitlementRepository internal constructor(
    store: LocalEntitlementStore,
    scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val telemetry: BillingTelemetry = NoOpBillingTelemetry,
) : EntitlementRepository {
    constructor(store: LocalEntitlementStore, telemetry: BillingTelemetry = NoOpBillingTelemetry) :
        this(store, CoroutineScope(SupervisorJob() + Dispatchers.IO), telemetry = telemetry)

    @Volatile
    private var lastSnapshot: EntitlementSnapshot? = null
    private val resolvedReported = AtomicBoolean(false)

    override val info: StateFlow<EntitlementInfo> =
        store.snapshot
            .onEach { lastSnapshot = it }
            .transformLatest { snapshot ->
                val now = resolve(snapshot)
                reportResolvedOnce(snapshot, now)
                emit(now)
                // Flip to Expired at the expiry moment, without waiting for a new file emission.
                if (now is EntitlementInfo.Subscription) {
                    delay((now.validUntil - clock.now()).coerceAtLeast(0.seconds) + 1.seconds)
                    emit(resolve(snapshot))
                }
            }
            // Eagerly: the first definite value is ready as soon as the file is read; gates wait only
            // while it is still Unknown.
            .stateIn(scope, SharingStarted.Eagerly, EntitlementInfo.Unknown)

    override fun currentInfo(): EntitlementInfo = lastSnapshot?.let(::resolve) ?: info.value

    private fun resolve(snapshot: EntitlementSnapshot): EntitlementInfo =
        when (snapshot) {
            EntitlementSnapshot.ReadFailed -> EntitlementInfo.None
            is EntitlementSnapshot.Read -> {
                val until = snapshot.subscriptionUntil
                when {
                    snapshot.lifetime == true -> EntitlementInfo.Lifetime
                    until != null && clock.now() < until -> EntitlementInfo.Subscription(until)
                    until != null -> EntitlementInfo.Expired(until)
                    else -> EntitlementInfo.None
                }
            }
        }

    /** One aggregate event per process: after a release, verify that paid statuses survived. No dates, no ids. */
    private fun reportResolvedOnce(snapshot: EntitlementSnapshot, info: EntitlementInfo) {
        if (!resolvedReported.compareAndSet(false, true)) return
        val kind =
            when {
                snapshot is EntitlementSnapshot.ReadFailed -> "read_failed"
                info is EntitlementInfo.Lifetime -> "lifetime"
                info is EntitlementInfo.Subscription -> "subscription"
                info is EntitlementInfo.Expired -> "expired"
                else -> "none"
            }
        telemetry.event("entitlement_resolved", mapOf("kind" to kind, "source" to "local"))
    }
}
