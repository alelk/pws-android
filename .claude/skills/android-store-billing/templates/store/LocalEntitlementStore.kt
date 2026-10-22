@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.app.android.payment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.app.domain.telemetry.BillingTelemetry
import com.example.app.domain.telemetry.NoOpBillingTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.time.Instant

/*
 * TEMPLATE — store flavor source set. The persisted entitlement: the offline source of truth.
 *
 * Invariants (evidence: pws-android LegacyRuStoreEntitlementStore.kt, plan invariants I6, I9):
 * - ONE process-wide DataStore delegate for this file (DataStore forbids two instances per file) and
 *   ONE owner class (this one). Pin the file name in a StorageNamesPinned-style test.
 * - NO corruptionHandler / ReplaceFileCorruptionHandler: "repairing" the file replaces the paid flag
 *   with defaults. A read error becomes Snapshot.ReadFailed + a non-fatal, the file stays as it is.
 * - Never rename the file or the keys once shipped; never delete or clear them.
 * - Writes are MONOTONIC: grant or extend only. There is deliberately no revoke/clear/setFalse API —
 *   a method that does not exist cannot be called from a sync branch.
 * - Writes are `internal`: only PurchaseSyncService (same module) calls them.
 *
 * Adopting an older app's file? Use ITS file name, key names and value encoding verbatim
 * (references/legacy-entitlement-compat.md) — the evidence stores a `yyyy-MM-dd` string, not millis.
 */
private val Context.entitlementPreferences: DataStore<Preferences> by preferencesDataStore(name = "app-entitlement")

internal object EntitlementKeys {
    val LIFETIME = booleanPreferencesKey("entitlement_lifetime")
    val SUBSCRIPTION_UNTIL_EPOCH_MS = longPreferencesKey("entitlement_subscription_until_epoch_ms")
}

sealed interface EntitlementSnapshot {
    data class Read(
        /** `null` when the key is absent. */
        val lifetime: Boolean?,
        /** `null` when absent. */
        val subscriptionUntil: Instant?,
    ) : EntitlementSnapshot

    /** The file exists but could not be read (I/O error or corruption). It is left untouched. */
    data object ReadFailed : EntitlementSnapshot
}

class LocalEntitlementStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val telemetry: BillingTelemetry = NoOpBillingTelemetry,
) {
    constructor(context: Context, telemetry: BillingTelemetry = NoOpBillingTelemetry) :
        this(context.applicationContext.entitlementPreferences, telemetry)

    val snapshot: Flow<EntitlementSnapshot> =
        dataStore.data
            .map<Preferences, EntitlementSnapshot> { prefs ->
                EntitlementSnapshot.Read(
                    lifetime = prefs[EntitlementKeys.LIFETIME],
                    subscriptionUntil = prefs[EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS]?.let(Instant::fromEpochMilliseconds),
                )
            }.catch { e ->
                // CorruptionException is an IOException too. Anything else is a bug: let it surface.
                if (e !is IOException) throw e
                telemetry.recordError(e, "entitlement_read_failed")
                emit(EntitlementSnapshot.ReadFailed)
            }

    /** Grants lifetime access. Touches the file only on a change. There is no inverse. */
    internal suspend fun grantLifetime() {
        dataStore.edit { prefs ->
            if (prefs[EntitlementKeys.LIFETIME] != true) prefs[EntitlementKeys.LIFETIME] = true
        }
    }

    /**
     * Moves the subscription end to [until] if it is LATER than what is stored; an earlier or equal
     * value never overwrites. @return true when the file changed.
     */
    internal suspend fun extendSubscriptionUntil(until: Instant): Boolean {
        var changed = false
        dataStore.edit { prefs ->
            val current = prefs[EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS]
            val candidate = until.toEpochMilliseconds()
            if (current == null || candidate > current) {
                prefs[EntitlementKeys.SUBSCRIPTION_UNTIL_EPOCH_MS] = candidate
                changed = true
            }
        }
        return changed
    }
}
