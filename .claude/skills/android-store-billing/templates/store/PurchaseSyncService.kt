@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.app.android.payment

/*
 * TEMPLATE — store flavor source set. Reconciles the store's purchase list INTO the local entitlement.
 *
 * THE invariant of this skill (evidence: pws-android PurchaseSyncService.kt, "invariant I9"):
 * a purchase list can only GRANT or EXTEND, never revoke or shorten.
 *
 * - lifetime becomes true only for a PAID lifetime product; it is NEVER written false. An empty list,
 *   a signed-out store app, another store account, an SDK error or an outage must not erase a lifetime
 *   purchase. (The RuStore fork's syncDataStoreWithPurchases() wrote false whenever the product was
 *   absent from the list — and the first port copied it: plan finding C3, critical.)
 * - the subscription end only moves forward, to the latest end among PAID subscriptions; a
 *   subscription without an end is ignored.
 * - no paid purchase in the list -> nothing is written at all.
 *
 * Consequence, decided on purpose: a refund does not revoke access automatically. Revocation, if ever
 * wanted, needs an explicit "refunded" status from the store and its own plan — never "absent".
 *
 * Failure handling is the caller's: if fetching the list throws, sync() is simply not called.
 */
class PurchaseSyncService(
    private val store: LocalEntitlementStore,
) {
    /** Grants or extends the entitlement derived from [purchases]; never lowers it. */
    suspend fun sync(purchases: List<ActivePurchase>) {
        val paid = purchases.filter { it.isPaid }
        if (paid.isEmpty()) return

        if (paid.any { it.productId == ProductIds.LIFETIME }) store.grantLifetime()

        paid
            .filter { it.productId in ProductIds.SUBSCRIPTIONS }
            .mapNotNull { it.expiresAt }
            .maxOrNull()
            ?.let { store.extendSubscriptionUntil(it) }
    }

    /**
     * Right after the store reported a successful purchase of [productId]. Lifetime is granted at once
     * (a later purchases() hiccup must not lose a purchase the user just paid for). A subscription is
     * NOT marked lifetime — its end arrives with the next [sync] (the fork's bug was marking every
     * purchase as lifetime and relying on a sync to undo it).
     */
    suspend fun onPurchaseSucceeded(productId: String) {
        if (productId == ProductIds.LIFETIME) store.grantLifetime()
    }
}
