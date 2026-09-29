# RuStore upgrade test: fork 2.x → new app (`rustore` flavor)

Manual verification before the first RuStore release of the new app (plan
[`2026-09-29_rustore-release-compat-pro-coming-soon_plan.md`](../docs/ai/plans/2026-09-29_rustore-release-compat-pro-coming-soon_plan.md),
Phase G). It proves invariants I6–I10 on a real device: paid users keep Pro, user data survives,
nothing touches the fork's entitlement file, no RuStore Pay SDK calls while purchases are off.

Automated coverage that this procedure complements:

| What | Test |
|---|---|
| fork DB `pws.2.3.0.db` found, migrated, quarantined; partial → retry; give-up after 5 | `:data:db-android` `MigrateLegacyDatabaseFileTest` (fixture `test-db/v11-rustore-2.3.1`, built from the published 2.3.1 APK by `tools/make-rustore-fork-db-fixture.py`) |
| migration strictly before seeding | `:app-compose` `LegacyMigrationGateTest` |
| entitlement from golden fork files, file never modified | `:app-compose` `RuStoreCompatEntitlementRepositoryTest` (fixtures by `tools/make-legacy-entitlement-fixtures.py`) |
| monotonic purchase sync | `PurchaseSyncServiceTest` |
| no Pay SDK in `PremiumComingSoon` | `RustoreKoinModulesTest` |
| gates show "Pro — coming soon" / work with Pro | Maestro `e2e/flows/compose/rustore/` |

## T-G01 — matrix

| From (APK in `pws-android-rustore/output/`) | Pro state (fixture kind) | Network | RuStore app |
|---|---|---|---|
| **2.3.1** (main), 2.2.0, 2.1.1; 1.1.0 if an APK is found | `none` · `lifetime` · `sub_future` · `sub_past` · `sub_and_full` | on / off | installed / not installed |

Minimum for the release DoD: 2.3.1 × every Pro state × offline, and 2.2.0 / 2.1.1 × `lifetime`.

Record each run:

| From | Pro | Net | RuStore | data ok | status in Settings | coming soon | sha256 same | quarantine | theme | icon | result |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2.3.1 | lifetime | off | no | | | | | | | | |

## T-G02 — procedure

Tools: `adb` (with `exec-in`), Python 3, a device/emulator with Google Play-free image is fine.

1. **Install the fork.** Either the published APK (`adb install pws-android-rustore/output/pws-app-release-2.3.1-ru.apk`)
   — then use the fork's debug build only to prepare files — or a debug fork build signed with the
   RuStore key (`pws-android-rustore`: `./gradlew :app:assembleRuDebug`), which allows `run-as`.
2. **Create user data** in the fork: favorites in ≥ 2 books (one of them **not** PV3300), history,
   one edited song, one custom tag with songs, dark theme.
3. **Put the Pro state** (app stopped):
   ```shell
   e2e/scripts/rustore-entitlement.sh lifetime        # or none / sub_future / sub_past / sub_and_full
   e2e/scripts/rustore-entitlement.sh show > before.sha256
   ```
   (The script writes `files/datastore/pws-app-preferences.preferences_pb` exactly like the fork, with
   dates relative to today, and prints its SHA-256. It needs `run-as` — a debuggable build.)
4. **Update in place** with the new build:
   ```shell
   # release candidate from CI (purchases disabled), or rustoreDebug signed with the RuStore key:
   ./gradlew :app-compose:assembleRustoreDebug \
     -Pandroid.release.keystorePathRustore=... -Pandroid.release.keyAliasRuRustore=... \
     -Pandroid.release.keyPasswordRustore=... -Pandroid.release.storePasswordRustore=...
   adb install -r app-compose/build/outputs/apk/rustore/debug/app-compose-rustore-debug.apk
   ```
   (`rustoreDebug` is signed with the RuStore key when those properties are set — T-F04.)
5. **Checks** (cold start; for "offline" enable airplane mode before the first start):
   - [ ] no crash; the loading screen ends (migration + seeding), no onboarding for PV3300 users;
   - [ ] favorites / history / edited song / custom tag present — **including the non-PV3300 book**;
   - [ ] Settings → "Pro-версия" shows the fixture's status (`Pro активна навсегда`, `Pro активна до …`,
         `Подписка истекла … Продление станет доступно позже`, `Pro-версия появится позже`);
   - [ ] without Pro: favorite / edit / tags / share / theme → "Pro-версия — скоро" sheet naming the
         feature, the action is not performed; with Pro: all work, no sheet;
   - [ ] `adb logcat | grep -i -E "rustore|RuStorePay"` — no Pay SDK network calls;
   - [ ] `adb exec-out run-as io.github.alelk.pws.app ls databases/legacy-migrated/` → `pws.2.3.0.db`
         (and no `databases/pws.2.3.0.db` left);
   - [ ] theme carried over (dark) — T-A05;
   - [ ] the home-screen icon pinned under 2.3.1 still launches the app (Pixel Launcher + One UI/MIUI
         if available) — T-F01;
   - [ ] `e2e/scripts/rustore-entitlement.sh show` equals `before.sha256` — **the entitlement file is
         byte-for-byte unchanged**. Repeat after a week of use in `PremiumComingSoon`.
6. Optional automation of the UI part: `./e2e/scripts/run-compose.sh --flavor rustore --flow flows/compose/rustore/02-pro-user-actions-work.yaml`
   (see `e2e/README.md` → RuStore).

## T-G03 — books from the old database

After step 5: Library → the books that came from the fork's database (source `MIGRATION`) are listed,
and "update" from the catalog replaces them **without duplicates** and **keeps** the user's edits and
favorites (the importer updates songs in place; plan `2026-06-27_global-book-library_plan.md`).

## T-G04 — no RuStore app, no network

Uninstall the RuStore app (`ru.vk.store`) and enable airplane mode; cold-start the new build several
times. The Pay SDK auto-init meta-data must not crash the process (plan risk R4). If it does, move
the `sdk_pay_scheme_value` / `console_app_id_value` meta-data behind a manifest placeholder that is
empty while `pws.rustore.purchasesEnabled=false`.
