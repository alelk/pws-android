package com.example.app.features.premium

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/*
 * TEMPLATE — shared (KMP commonMain) module, e.g. `features/src/commonMain/.../premium/`.
 *
 * Payment-agnostic on purpose: these types say WHETHER premium is unlocked and WHAT the build
 * monetizes, never HOW it was paid for. No store SDK type, product id or purchase token lives here.
 * A store flavor overrides the EntitlementRepository binding with its own implementation.
 *
 * Evidence: pws-core features/premium/EntitlementRepository.kt, features/monetization/MonetizationMode.kt.
 */

/**
 * How a given build monetizes — the single axis a product flavor picks once; everything else
 * (paywall entry, store SDK, gating, donation prompt) derives from it. Mutually exclusive by design:
 * a paying user should not also see donation prompts. If a build ever needs both, replace the enum
 * with independent booleans in a recorded decision.
 */
enum class MonetizationMode {
    /** Free build: donation prompt on, no paywall, gates transparent. */
    Donations,

    /** Premium features are gated and sold through a store SDK. */
    PremiumSales,

    /**
     * Premium features are gated and existing entitlements honoured, but nothing can be bought yet:
     * a blocked gate shows "coming soon" and the store SDK is never called.
     */
    PremiumComingSoon,

    /** No donations, no sales: every gate is transparent. */
    None,
    ;

    val donationsEnabled: Boolean get() = this == Donations
    val premiumGatingEnabled: Boolean get() = this == PremiumSales || this == PremiumComingSoon

    /** True only when premium can actually be bought: controls the paywall entry AND whether the SDK is wired. */
    val purchasesEnabled: Boolean get() = this == PremiumSales
}

enum class PremiumStatus { Active, Inactive, Unknown }

/**
 * Why premium is (or is not) unlocked. `Unknown` is NOT `None`: it means "not read yet", and a gate
 * must wait for a definite value rather than tell a possibly-paying user they have nothing.
 */
@OptIn(ExperimentalTime::class)
sealed interface EntitlementInfo {
    data object Unknown : EntitlementInfo

    /** Nothing unlocks premium (never paid, or the local state could not be read). */
    data object None : EntitlementInfo

    data object Lifetime : EntitlementInfo

    /** Unlocked until [validUntil] (exclusive). */
    data class Subscription(val validUntil: Instant) : EntitlementInfo

    /** A subscription used to unlock premium and ended at [expiredAt] — for "expired on …" copy. */
    data class Expired(val expiredAt: Instant) : EntitlementInfo

    val status: PremiumStatus
        get() =
            when (this) {
                Lifetime, is Subscription -> PremiumStatus.Active
                None, is Expired -> PremiumStatus.Inactive
                Unknown -> PremiumStatus.Unknown
            }
}

/** Single source of truth for the current premium entitlement — the port every gate reads. */
interface EntitlementRepository {
    /** Starts at [EntitlementInfo.Unknown] until the local state has been read. */
    val info: StateFlow<EntitlementInfo>

    /**
     * The entitlement as of *now*. A time-bound source overrides this to re-check expiry at the
     * moment a gate asks, instead of trusting the last emission.
     */
    fun currentInfo(): EntitlementInfo = info.value
}

val EntitlementRepository.status: Flow<PremiumStatus>
    get() = info.map { it.status }.distinctUntilChanged()

/** Default binding for builds without a paywall: everything unlocked, gates never block. */
class AlwaysActiveEntitlementRepository : EntitlementRepository {
    override val info: StateFlow<EntitlementInfo> = MutableStateFlow<EntitlementInfo>(EntitlementInfo.Lifetime).asStateFlow()
}
