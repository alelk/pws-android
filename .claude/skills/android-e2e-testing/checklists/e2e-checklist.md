# Android E2E Checklist

## Scope

- [ ] Written list of artifact-level risks the suite covers (R8, flavors, first run, upgrade,
      backup/restore, gating); each flow maps to one.
- [ ] No flow restates a rule a ViewModel/use-case test can state (those were moved down).
- [ ] Suite size is deliberate: smoke core ~5–10, mutation flows one per boundary, regression flows
      only for escaped artifact-level defects.

## App addressability

- [ ] `testTagsAsResourceId = true` on the shell root (and on sheets/dialogs if their tags are not found).
- [ ] Selectors are `testTag` / `contentDescription` ids; visible-text selectors only for system UI,
      each documented.
- [ ] One env file holds ids, timeouts and test data; flows use `${VAR}` only — no literal ids or timeouts.
- [ ] `E2eSelectorParityTest` in `./gradlew build`: guard (floors), pair (detector), ratchet
      (`KNOWN_UNCHECKED`, `KNOWN_UNLISTED` as `Map<item, reason>`); seen red once.

## Flows

- [ ] Each flow's header states kind (smoke / mutation / diagnostic), precondition and why it is E2E.
- [ ] Mutation flows start with `cold-start` (`clearState: true`); no flow depends on another's data.
- [ ] Every flow ends with a positive `assertVisible` of the expected screen.
- [ ] No `hideKeyboard`; a `dismiss-keyboard` subflow guarded by "keyboard visible" precedes
      bottom-nav taps after text input.
- [ ] Common sequences are `_helpers/` subflows; flavor-only flows live in their own folder/suite.
- [ ] Flows that toggle device state (airplane mode, locale) run last.

## Runner

- [ ] `--flavor` maps to `applicationId` and APK (from `app.version`); the map matches Gradle flavors.
- [ ] `APP_ID` and `SCREENSHOTS_DIR` forwarded to Maestro explicitly.
- [ ] Install `-r`, `--clean` option, `--retries` (default 0), one JUnit file per flow, non-zero exit on failure.
- [ ] `trap … EXIT` restores network/device state.
- [ ] Host-side steps (adb, `run-as`, file transfer) are in the runner/scripts, not in flows.
- [ ] `e2e/artifacts`, `reports`, `screenshots` git-ignored.

## State and flavors

- [ ] Every shipped flavor has had at least the smoke core run on its release APK.
- [ ] Gated/per-store behaviour has its own flows with planted state on a debuggable twin build.
- [ ] Upgrade smoke over the previously published APK was run after any storage / schema / R8 change.
- [ ] Offline first run is covered if the first-run screen depends on the network.

## Gate

- [ ] Where E2E runs is decided and written in AGENTS.md and the release doc.
- [ ] No workflow step called "E2E" that only prints a notice; docs do not call E2E a "gate"
      unless it blocks the release.
- [ ] Release record lists flavors run, version, date, and what was **not** run.
- [ ] If in CI: emulator image/API pinned, Maestro version pinned, reports + failing-flow artifacts
      uploaded, flaky flows reviewed.
