# Carrying an existing paid status across an update

Applies when users already paid in an earlier app with the same `applicationId` (a fork, an older
architecture, a previous SDK) and the new build replaces it by an in-place update. The paid flag is on
the device; the update must keep reading it, offline, with the SDK possibly switched off.

Evidence: pws-android `rustore` flavor replacing the RuStore fork 2.1.1–2.3.1 (plan
`2026-09-29_rustore-release-compat-pro-coming-soon_plan.md`, invariants I6–I9).

## Decision: the old file stays the source of truth

Plan §4.4: "keep the old file as the source of truth instead of migrating to a new store. Zero
migration = zero chance to lose the flag during migration; the file is already on the device; the new
build only reads it." Writes (when sales open) go into the same file, monotonically.

The fork's format, adopted verbatim:

| | Value |
|---|---|
| file | `files/datastore/pws-app-preferences.preferences_pb` (Jetpack DataStore Preferences) |
| lifetime | `purchase_full_access: Boolean` |
| subscription | `purchase_subscription_until: String` — `yyyy-MM-dd`, `Locale.US`, device time zone at write time |
| active rule (fork) | `full_access == true` → Pro; else `subscription_until` after now → Pro |

## Invariant I6 — the file is untouchable

- Do not rename the file or the keys; do not delete or clear them.
- **Never install a `ReplaceFileCorruptionHandler`** — it would replace a file that failed to parse
  with defaults, i.e. erase the paid flag on one bad read.
- **One DataStore per file per process** — one top-level `preferencesDataStore(name = …)` delegate,
  owned by one class (`LegacyRuStoreEntitlementStore`); DataStore forbids two instances on one file.
- Read errors: `IOException` (incl. `CorruptionException`) → `ReadFailed` + non-fatal; the file is not
  "repaired"; the next start tries again.
- Pin the store name in a source test (evidence `StorageNamesPinnedTest`: the literal
  `"pws-app-preferences"` may occur in exactly `LegacyRuStoreEntitlementStore.kt`; "changing a name is
  a decision of the owner, not a refactoring").

Invariant I8: in the build that does not sell (`PremiumComingSoon`), the app writes **nothing** to the
file and never calls the SDK. The acceptance check is byte-level: the file's SHA-256 is unchanged after
the upgrade and a week of use.

## Parse defensively, report without data

- Unparseable date → `subscriptionUntil = null` + non-fatal `entitlement_bad_subscription_date`; the
  raw value is kept in the snapshot for diagnostics but **never** sent to telemetry.
- A later valid date replaces an unparseable stored one (the monotonic compare treats unparseable as
  absent).
- Day granularity: the whole stored day counts (`today <= until`), and the status flips at local
  midnight — see [`entitlement-sync.md`](entitlement-sync.md#reading-resolution-and-time).

## Honour what was written, even the fork's bugs

The fork set `purchase_full_access = true` for any purchase (subscriptions included) and relied on a
later sync to undo it. A subscriber whose sync failed has `true` on disk. Decision (plan §2.2): honour
it — the fork did — but never write that again.

## Golden files, not files written by the code under test

Evidence `src/testRustore/resources/legacy-entitlement/*.preferences_pb`: `none`, `lifetime`,
`lifetime_false_sub`, `sub_future`, `sub_past`, `sub_and_full`, `bad_date`, `corrupted`. They were
produced by an **independent** protobuf encoder (`tools/make-legacy-entitlement-fixtures.py`), not by
the app's DataStore code — a fixture written by the code under test cannot catch a key or encoding
mismatch. Each test:

1. copies the fixture to a temp dir under the real file name;
2. builds the store on a real file-backed `PreferenceDataStoreFactory`;
3. asserts the resolved `EntitlementInfo` and the single `entitlement_resolved` event;
4. asserts the file's **SHA-256 is unchanged** (reading never rewrites).

## Other things the same update must keep

Out of this skill's scope but on the same release checklist (evidence plan): the signing certificate
(CI verifies the artifact's cert), `applicationId`, monotonically growing `versionCode`, `minSdk`, the
content-provider authority, the launcher component name (an `activity-alias` with the old name keeps
pinned icons working), the old database migrated before seeding, and the old app's display settings
imported once (`LegacySettingsImporter`: reads the same legacy file, never writes it, marker key in the
new store so a user's later choice is never overwritten).

## Upgrade test (manual + script)

Evidence `tools/rustore-upgrade-test.md`, `e2e/scripts/rustore-entitlement.sh`:

- install the old release (or a debuggable build signed with the same key, so `run-as` works);
- plant a fixture with the app stopped (`rustore-entitlement.sh <kind>`), record `sha256`
  (`rustore-entitlement.sh show`);
- `adb install -r` the new build; check status in Settings, gated actions, offline cold start, no SDK
  calls in logcat (sales off), and the same `sha256` afterwards;
- Maestro flows (`e2e/flows/compose/rustore/`): free user sees "coming soon", Pro user's actions work,
  Settings shows the right status — never `clearState` in the Pro flows (it deletes the planted file).
