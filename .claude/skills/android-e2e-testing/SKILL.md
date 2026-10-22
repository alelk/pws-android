---
name: android-e2e-testing
description: Use when deciding whether and how to end-to-end test a native Android app (Compose, offline-first, several store flavors) — what E2E is for (the built release APK: R8, flavors, signing, first run, migration, backup/restore — what unit and ViewModel tests cannot see), why a small black-box Maestro suite on the real release artifact is enough and a big one is a liability, locale-independent selectors (testTag / contentDescription ids shared through one env file, testTagsAsResourceId on the shell root), a selector-parity fitness test, flows split into smoke (read-only) / mutation (clearState) / diagnostic, shared subflows (cold-start, onboarding, keyboard), host-side steps Maestro cannot do (adb run-as state injection, file transfer, airplane mode with an EXIT trap), a per-flavor runner script (applicationId map, install, clean state, retries, JUnit per flow), the Maestro hideKeyboard / occluded-node traps, flake discipline, and the release gate — local pre-publish run vs CI emulator (KVM, macOS runner, Firebase Test Lab), an honest "E2E not run" instead of a disabled step that still says gate.
metadata:
  version: "1.0.0"
  requires: "kotlin-testing-strategy, android-release-build"
  project-types: "android-app, compose-multiplatform-android-shell"
---

# Android E2E Testing

## When to use

- A native Android app ships an APK/AAB to users and the question is "do we need end-to-end tests,
  and how many?" — or the suite exists and keeps growing, flaking or silently not running.
- Adding a flavor/store (second `applicationId`, another signing key, a store SDK) and the new
  artifact has to be proven installable, launchable and upgradeable.
- Someone proposes "a UI test for every screen", or the release workflow has an E2E step nobody
  has seen run.

**Not this skill:** logic of ViewModels, use-cases, repositories → [`kotlin-testing-strategy`](../kotlin-testing-strategy/SKILL.md)
(its "end-to-end (few)" is the *server* scenario over Testcontainers). Release signing, flavors,
R8 → [`android-release-build`](../android-release-build/SKILL.md). Local database upgrade safety →
[`room-local-persistence`](../room-local-persistence/SKILL.md).

## What E2E is for

E2E answers one question: **does the artifact we are about to publish work on a device?** Everything
else is cheaper one layer down. It earns its cost only for risks that exist *only* in the built
artifact:

| Risk                                                     | Why no lower layer sees it                                        |
|----------------------------------------------------------|-------------------------------------------------------------------|
| R8/ProGuard removed or renamed something read by name    | Debug and unit builds are not minified                            |
| A flavor has the wrong `applicationId`, key, SDK, seed    | Unit tests run one variant; the flavor is Gradle wiring           |
| First run: onboarding, seed data, offline start           | Needs the real DB creation, real assets, a real (absent) network  |
| Upgrade over the published version, migrations            | Needs the old APK's data on disk                                  |
| Backup export → wipe → restore through the system picker  | Crosses the app boundary (SAF, files)                             |
| Gated features per store (entitlement, "coming soon")     | Depends on flavor wiring + persisted state read at startup        |

Rule of thumb: **business rules are not E2E's job.** "Favorites toggle adds a row" is a use-case
test; "the release APK of flavor X opens, finds a song and the favorites tab shows it" is E2E.

## Hard rules

- ✅ Test the **built release artifact** (minified, signed as in production or a debuggable twin of
  the same flavor when state injection needs `run-as`), not a debug build of one default flavor.
  *Why:* R8 and flavor wiring are the defects only E2E can catch; a debug-build suite is green
  while the store build crashes on launch.
- ✅ **Few flows.** Aim for a read-only smoke core of ~5–10 and a handful of mutation flows for
  paths that cross the app boundary (first run, backup/restore, offline start, per-flavor gating).
  A behaviour that a ViewModel/use-case test can state moves down. *Why:* every flow is a
  maintenance and flake cost paid at every release; a suite people stop trusting is worse than none.
- ✅ Selectors are **locale-independent ids** — `testTag` (`action:save`, `field:search`) or
  `contentDescription` — defined once in production code and mirrored once in an env file read by
  every flow. ❌ No selector by visible text, except system UI you do not own (file picker "SAVE").
  *Why:* the app has several locales and content languages; a text selector turns a translation
  fix into a red suite.
- ✅ The shell root exposes tags to the automation layer:
  `Modifier.semantics { testTagsAsResourceId = true }` ([template](templates/ShellRootSemantics.kt.snippet)).
  *Why:* without it Compose `testTag`s are invisible to UiAutomator/Maestro and every id selector fails
  with "not found".
- ✅ A **selector-parity fitness test** runs in `./gradlew build`: every id in the env file exists
  as a literal in production source ([template](templates/E2eSelectorParityTest.kt)). *Why:* the
  suite never runs in the everyday gate, so a renamed tag is otherwise found at release time.
- ✅ Every flow declares its state: **smoke** = read-only, may share state; **mutation** = starts
  with a `clearState` cold start subflow; **diagnostic** = not in any suite while stabilising.
  A mutation flow that relies on a previous flow's leftovers is a defect. *Why:* order-dependent
  suites fail only when one flow is rerun alone, which is exactly when you debug.
- ✅ Common sequences are **subflows** (`_helpers/`): cold start, dismiss onboarding, open song,
  dismiss keyboard. A selector or timeout changes in one place. Timeouts are env constants
  (`TIMEOUT_COLD_START`, …), never literals in flows.
- ❌ **Never** `hideKeyboard` (Maestro 2.5.1 sent the app to the launcher; the remaining
  `assertNotVisible` steps then passed against the launcher — a green flow that tested nothing).
  Press Back guarded by "the keyboard is visible", not by "the nav bar is missing".
  → [`references/maestro-flows.md`](references/maestro-flows.md)
- ✅ Every flow ends on a **positive assertion** about the app (`assertVisible` of something only
  the expected screen has). A flow whose last checks are all `assertNotVisible` can pass on the
  wrong screen. *Why:* see the rule above — "nothing is shown" is true on the launcher too.
- ✅ Anything Maestro's sandbox cannot do (adb, `run-as`, file copy between phases, airplane mode)
  lives in the **runner script**, with an `EXIT` trap that restores device state (network on).
  *Why:* a flow that fails after turning airplane mode on leaves the device offline for the next
  flow and the next run → [`references/state-and-flavors.md`](references/state-and-flavors.md).
- ✅ The runner takes `--flavor`, maps flavor → `applicationId`, resolves the APK from
  `app.version`, installs, optionally `pm clear`s, retries per flow, writes **one JUnit file per
  flow** and exits non-zero on any failure ([template](templates/run-e2e.sh)).
- ✅ Retries are for infrastructure flake, default `0` locally. A flow that passes only on retry is
  a bug in the flow or the app — record it, do not hide it.
- ❌ **Never** keep a release step named "E2E" that only prints a notice. Either the gate runs, or
  the workflow and docs say *"E2E is a manual pre-publish step"* and the release checklist records
  the run (flavors, version, date). *Why:* a disabled step that still reads as a quality gate is
  false assurance → [`references/release-gate.md`](references/release-gate.md).
- ✅ Artifacts of a run (JUnit, screenshots, hierarchy dumps) go to git-ignored directories.

## Templates

| Path                                                                       | Purpose                                                                  |
|----------------------------------------------------------------------------|--------------------------------------------------------------------------|
| [`templates/run-e2e.sh`](templates/run-e2e.sh)                             | Per-flavor runner: device check, install, clean, retries, JUnit, network EXIT trap |
| [`templates/compose.env.example`](templates/compose.env.example)           | Selector ids + timeouts + test data shared by all flows                  |
| [`templates/cold-start.yaml`](templates/cold-start.yaml)                   | `clearState` launch + optional onboarding + wait for Home                |
| [`templates/dismiss-keyboard.yaml`](templates/dismiss-keyboard.yaml)       | Back guarded by "keyboard visible"                                       |
| [`templates/flow-template.yaml`](templates/flow-template.yaml)             | A mutation flow: setup, act, positive assertion, screenshot              |
| [`templates/suite.yaml`](templates/suite.yaml)                             | Smoke vs full bundle entrypoints                                         |
| [`templates/ShellRootSemantics.kt.snippet`](templates/ShellRootSemantics.kt.snippet) | `testTagsAsResourceId` on the shell root                       |
| [`templates/E2eSelectorParityTest.kt`](templates/E2eSelectorParityTest.kt) | Kotest fitness test: env ids ⊆ source literals, guard + pair + ratchet   |

## References (load on demand)

- [`references/where-e2e-fits.md`](references/where-e2e-fits.md) — what to test at which layer,
  Maestro vs Espresso/Compose `androidTest` vs screenshot tests vs device farms, how big the suite
  should be, what to delete.
- [`references/maestro-flows.md`](references/maestro-flows.md) — selectors, subflows, timeouts,
  keyboard and occlusion traps, scrolling, system dialogs, writing a flow that can fail.
- [`references/state-and-flavors.md`](references/state-and-flavors.md) — clean state, injecting
  persisted state with `run-as`, two-phase flows with host-side steps, per-flavor ids, offline.
- [`references/release-gate.md`](references/release-gate.md) — local pre-publish run vs CI, the
  emulator constraints, an honest record of what was run.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md). Verify → [`checklists/e2e-checklist.md`](checklists/e2e-checklist.md).

## Related

- [`kotlin-testing-strategy`](../kotlin-testing-strategy/SKILL.md) — the pyramid this sits on top of.
- [`android-release-build`](../android-release-build/SKILL.md) — flavors, signing and R8 that E2E
  proves on the artifact; its upgrade-over-published smoke is an E2E flow.
- [`room-local-persistence`](../room-local-persistence/SKILL.md) — migration/safe-mode checks that
  need a device.
- [`backup-restore-drill`](../backup-restore-drill/SKILL.md) — a restore that was actually run;
  the client-side drill is an E2E flow.
- [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md) — conventions of the
  selector-parity test (guard, pair, ratchet).
- [`gradle-release-system`](../gradle-release-system/SKILL.md) — where the gate sits in the pipeline.
- [`compose-multiplatform-ui`](../compose-multiplatform-ui/SKILL.md) — the shell that carries the tags.

## Evidence

pws-android (verified 2026-10-22):

- `e2e/` — 19 Maestro flows for `app-compose` (`flows/compose/01…19`), 9 `_helpers/` subflows,
  3 `rustore/` flows, `scripts/run-compose.sh`, `config/compose.env`. Smoke list of 10 read-only
  flows, full list adds 9 mutation flows (each starting with `cold-start.yaml`, `clearState: true`).
- `e2e/README.md` — selector table (`NavigationBar.kt`, `SongDetailScreen.kt`, …) and the
  `hideKeyboard` / occluded-node post-mortem: with a full-size soft keyboard the nav tabs are
  *missing* from Maestro's hierarchy (no timeout helps); on an emulator with `hw.keyboard=yes`
  the same flow passes, which made the failure look flaky.
- `e2e/flows/compose/19-onboarding-offline-skip.yaml` — regression guard for a real bug (Skip
  hidden while the catalog loads offline); `run-compose.sh` restores airplane mode on `EXIT`.
- `e2e/flows/compose/17-backup-restore.yaml` + `_run_backup_restore` in `run-compose.sh` — Maestro's
  GraalJS sandbox cannot call adb, so file transfer and `pm clear` between the export and import
  phases are done by the runner.
- `e2e/flows/compose/rustore/` + `scripts/rustore-entitlement.sh` — per-flavor gating proven with a
  state file planted via `adb exec-in run-as` on a debuggable build.
- `.github/workflows/release-build.yml` — the E2E steps are `echo "::notice::E2E tests are
  temporarily disabled."` (ubuntu runner has no KVM; a macOS runner was tried and then reverted),
  while `docs/release-workflow.md` still calls E2E for `ru` a "quality gate". The suite is in
  practice a local pre-publish run. This is the failure mode the "never a notice-only step" rule is
  about.
- No Compose `androidTest` layer exists yet; the suite carries first-run, flavor and R8 risk alone.
