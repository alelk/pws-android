package io.github.alelk.pws.android.compose.payment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import java.io.IOException

/**
 * The preferences file of the legacy RuStore fork (2.1.1–2.3.1): `files/datastore/pws-app-preferences.preferences_pb`.
 *
 * This is the SAME file the fork wrote the paid status to, so an in-place update reads the user's
 * already-paid status here — the unlock survives with no network and no Pay SDK.
 *
 * Invariant I6 (plan 2026-09-29): do NOT rename the file or the keys, do not delete or clear them,
 * never install a `ReplaceFileCorruptionHandler`, and never create a second DataStore on this file —
 * this single process-wide delegate is the only one (DataStore forbids two instances per file).
 */
private val Context.legacyRuStorePreferences: DataStore<Preferences> by
  preferencesDataStore(name = "pws-app-preferences")

/** Keys exactly as the fork wrote them. */
internal object LegacyPreferenceKeys {
  /** Lifetime purchase (`full_access_v1`). */
  val PURCHASE_FULL_ACCESS = booleanPreferencesKey("purchase_full_access")

  /** Subscription expiry, `yyyy-MM-dd` (`Locale.US`, device time zone at the time of writing). */
  val PURCHASE_SUBSCRIPTION_UNTIL = stringPreferencesKey("purchase_subscription_until")
}

/** What the legacy file says about the user's purchase. */
sealed interface LegacyEntitlementSnapshot {
  data class Read(
    /** `null` when the key is absent. */
    val fullAccess: Boolean?,
    /** `null` when absent or unparseable (see [rawSubscriptionUntil]). */
    val subscriptionUntil: LocalDate?,
    /** The stored string as is — for diagnostics only, never sent to telemetry. */
    val rawSubscriptionUntil: String?,
  ) : LegacyEntitlementSnapshot

  /** The file exists but could not be read (I/O error or corruption). It is left untouched. */
  data object ReadFailed : LegacyEntitlementSnapshot
}

/**
 * Typed access to the legacy RuStore entitlement stored in `pws-app-preferences`.
 *
 * Reads are error-safe: an I/O or corruption error becomes [LegacyEntitlementSnapshot.ReadFailed]
 * (and a non-fatal) instead of an exception — the file is never recreated or "repaired".
 *
 * Writes are monotonic (invariant I9): they can grant or extend premium, never revoke or shorten
 * it. They are used only when purchases are enabled (`PremiumSales`); in `PremiumComingSoon` nothing
 * calls them (invariant I8).
 */
class LegacyRuStoreEntitlementStore internal constructor(
  private val dataStore: DataStore<Preferences>,
  private val telemetry: Telemetry = NoOpTelemetry,
) {
  constructor(context: Context, telemetry: Telemetry = NoOpTelemetry) :
    this(context.applicationContext.legacyRuStorePreferences, telemetry)

  /** The raw preferences of the legacy file — for importing the fork's app settings (read only). */
  internal val preferences: Flow<Preferences> get() = dataStore.data

  val snapshot: Flow<LegacyEntitlementSnapshot> =
    dataStore.data
      .map<Preferences, LegacyEntitlementSnapshot> { prefs ->
        val raw = prefs[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL]
        LegacyEntitlementSnapshot.Read(
          fullAccess = prefs[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS],
          subscriptionUntil = raw?.let(::parseSubscriptionUntil),
          rawSubscriptionUntil = raw,
        )
      }
      .catch { e ->
        // CorruptionException is an IOException too. Anything else is a bug — let it surface.
        if (e !is IOException) throw e
        telemetry.recordError(e, "entitlement_read_failed")
        emit(LegacyEntitlementSnapshot.ReadFailed)
      }

  /** Grants lifetime access. No-op when it is already granted — the file is only touched on a change. */
  internal suspend fun grantLifetime() {
    dataStore.edit { prefs ->
      if (prefs[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] != true) prefs[LegacyPreferenceKeys.PURCHASE_FULL_ACCESS] = true
    }
  }

  /**
   * Extends the subscription to [until] if that is later than what is stored. An earlier or equal
   * date never overwrites the stored one; an unparseable stored value is replaced.
   *
   * @return true when the file was changed.
   */
  internal suspend fun extendSubscriptionUntil(until: LocalDate): Boolean {
    var changed = false
    dataStore.edit { prefs ->
      val current = prefs[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
      if (current == null || until > current) {
        prefs[LegacyPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] = until.toString() // ISO yyyy-MM-dd, as the fork wrote it
        changed = true
      }
    }
    return changed
  }

  private fun parseSubscriptionUntil(raw: String): LocalDate? =
    runCatching { LocalDate.parse(raw.trim()) }
      .onFailure { e ->
        // The value itself is not reported: only the fact that it did not parse.
        telemetry.recordError(IllegalArgumentException("unparseable subscription date", e), "entitlement_bad_subscription_date")
      }
      .getOrNull()
}
