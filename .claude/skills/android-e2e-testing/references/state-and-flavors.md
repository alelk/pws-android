# State, host-side steps and flavors

## Clean state

- Mutation flows start with a `cold-start` subflow: `launchApp: clearState: true`, wait for the
  first-run screen, run the onboarding subflow if it is visible, wait for Home.
- The runner's `--clean` (`adb shell pm clear <appId>`) is for the **whole run**; the per-flow
  `clearState` is what makes a single flow rerunnable.
- Smoke flows do not clear state (they are read-only). Therefore retries are unsafe for them after
  a partial run — keep `--retries 0` locally; in CI use `--clean` plus retries only on mutation
  flows, or accept it knowingly.
- Never let flow B depend on data flow A wrote. If a flow needs data, its setup subflow creates it.

## Flavors

- One runner argument `--flavor` selects: the APK (`<out>/app-release-<version>-<flavor>.apk` from
  `app.version`), the `applicationId` (a `case` map kept next to — and checked against — the Gradle
  flavors), and which flows apply.
- `APP_ID` is forwarded to Maestro explicitly (`--env APP_ID=…`): flows say `appId: ${APP_ID}` and
  fail with "Unable to launch app undefined" if the runner only forwards keys found in the env file.
- Flavor-only behaviour gets its own folder and `suite.yaml` (`flows/<app>/<flavor>/`), not
  `if flavor` branches inside shared flows.
- Smoke each shipped flavor at least once per release: same flows, different `--flavor`. The
  wrong-id / wrong-seed / wrong-key defect lives exactly here.

## Planting persisted state (debuggable build)

For "what does a user with state X see?" (a legacy entitlement file, a premium flag, a corrupted
preference) write the state file into the app's private directory before the flow:

```bash
adb shell am force-stop "$APP_ID"            # DataStore caches the file in memory
adb exec-in run-as "$APP_ID" sh -c 'mkdir -p files/datastore && cat > files/datastore/prefs.preferences_pb' < "$FIXTURE"
```

- Needs `run-as` → a **debuggable** build of the same flavor. Say so in the flow's header; the
  release APK of that flavor is checked by the flows that need no planted state.
- Generate fixtures from a script in the repo (kinds: none / valid / expired / corrupted), dates
  relative to today; print the file's SHA-256 (`show`) so an upgrade test can prove it did not change.
- One script per kind of state, not a branch of shell inside the flow.

## Two-phase flows (host-side steps between phases)

Maestro's JS sandbox cannot call adb, Java or the filesystem. A scenario that needs the host in
the middle (export a backup → wipe the app → import it) is split:

```
phase 1 flow (populate + export)  →  runner: pm clear / adb pull / adb push  →  phase 2 flow (import + verify)
```

The runner owns the special case by flow name; the two flows carry a header comment pointing at
each other. The restore **must** read the file the app wrote, through the real UI — not a
fixture copied from elsewhere ([`backup-restore-drill`](../../backup-restore-drill/SKILL.md)).

## Network and device-state toggles

- `setAirplaneMode` inside a flow is fine for an "offline first run" test. The runner installs
  `trap restore_network EXIT` that re-enables connectivity (two fallbacks: `cmd connectivity
  airplane-mode disable`, then `settings put global airplane_mode_on 0` + broadcast).
- Keep flows that toggle device state **last** in the full suite so a connectivity hiccup cannot
  cascade into the others.
- The same pattern for any other toggle (locale change, font scale, time zone): restore on EXIT.

## Upgrade smoke (the one flow people skip)

1. Install the **previously published** APK (kept per release in the artifact store), launch, create
   user data (or plant the state file).
2. `adb install -r` the new APK over it. Do not `pm clear`.
3. Launch: assert Home, the user's data (a favorite, a tag, settings), no safe-mode screen.

This is where protobuf field renames (DataStore, encrypted prefs), Room migrations and R8 changes
show up; none of them crash on a fresh install. Run it after any R8, schema or storage change
([`android-release-build`](../../android-release-build/SKILL.md),
[`room-local-persistence`](../../room-local-persistence/SKILL.md)).
