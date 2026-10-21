package com.example.app.features.premium

import com.example.app.domain.telemetry.BillingTelemetry
import com.example.app.domain.telemetry.NoOpBillingTelemetry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
 * TEMPLATE — shared (KMP commonMain). Evidence: pws-core features/premium/PremiumGate.kt.
 *
 * Exactly ONE consumer collects upsellRequests (an UpsellHost at the app root) and shows either the
 * paywall (mode.purchasesEnabled) or a coming-soon sheet. Two collectors of a SharedFlow = the upsell
 * shown twice.
 */

/** What is gated. Keep the list closed; gate every entry point of a feature, not just the main one. */
enum class PremiumFeature(val id: String) {
    Favorites("favorites"),
    Export("export"),
    Theme("theme"),
}

data class UpsellRequest(val feature: PremiumFeature)

interface PremiumGate {
    val upsellRequests: SharedFlow<UpsellRequest>

    /** Runs [action] if premium is active; otherwise emits an upsell request and returns `null`. */
    suspend fun <T> requirePremium(feature: PremiumFeature, action: suspend () -> T): T?
}

class DefaultPremiumGate(
    private val entitlements: EntitlementRepository,
    private val telemetry: BillingTelemetry = NoOpBillingTelemetry,
    private val unknownTimeout: Duration = 3.seconds,
) : PremiumGate {
    private val _upsellRequests = MutableSharedFlow<UpsellRequest>(extraBufferCapacity = 1)
    override val upsellRequests: SharedFlow<UpsellRequest> = _upsellRequests.asSharedFlow()

    override suspend fun <T> requirePremium(feature: PremiumFeature, action: suspend () -> T): T? {
        val resolved = withTimeoutOrNull(unknownTimeout) { entitlements.info.first { it != EntitlementInfo.Unknown } }
        if (resolved == null) {
            // Still unknown: drop the action WITHOUT an upsell. Telling a user who may well have paid
            // that they have no premium is worse than an ignored tap. Make it visible to us, though.
            telemetry.recordError(
                IllegalStateException("premium entitlement still unknown after $unknownTimeout"),
                "entitlement_unknown_timeout",
                mapOf("feature" to feature.id),
            )
            return null
        }
        // Re-evaluated at call time: a subscription may have expired since the last emission.
        val current = entitlements.currentInfo().takeIf { it != EntitlementInfo.Unknown } ?: resolved
        return if (current.status == PremiumStatus.Active) {
            action()
        } else {
            _upsellRequests.emit(UpsellRequest(feature))
            null
        }
    }
}
