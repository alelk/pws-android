# Where E2E fits

## The layers for an offline-first Compose Android app

```
        E2E on the built release APK (few)      first run, flavors, R8, upgrade, backup/restore, gating
       Compose UI tests / screenshots (some)    one screen's semantics, a11y, layout regressions   [optional, fast]
      ViewModel + screen-model tests (many)     event → state → effect on fakes
     use-cases + repositories (many, fast)      rules, Room on in-memory/instrumented DB, mapping
    fitness tests (cheap, always on)            layering, selector parity, no wildcard keep, i18n parity
```

Everything below the top runs in `./gradlew build`. The top does not, so it needs a rule for *when*
it runs ([`release-gate.md`](release-gate.md)).

## What each layer owns

| Question                                                        | Layer                                    |
|-----------------------------------------------------------------|------------------------------------------|
| Does "toggle favorite" persist and show up in the list?         | use-case + repository test, ViewModel test |
| Does the Save button enable only with a valid title?            | ViewModel / screen-model test            |
| Does this screen read correctly at font scale 2.0 / RTL?        | Compose UI or screenshot test            |
| Does flavor X open, find a song, show the right gated sheet?    | **E2E**                                  |
| Does the minified release keep everything read by name?         | **E2E** (+ `ProguardRulesTest` as a cheap lock) |
| Does a fresh install with no network reach Home?                | **E2E**                                  |
| Does a backup written by the app restore into a wiped app?      | **E2E**                                  |
| Does v(N+1) open the data of v(N)?                              | migration tests (`room-local-persistence`) **and** an E2E upgrade smoke |

If a flow in the suite is "tap, tap, assert a row" and the same thing can be stated against a
ViewModel with a fake repository, delete the flow and write the ViewModel test.

## Tool choice

| Tool                              | Strength                                                         | Cost / limit                                                      |
|-----------------------------------|------------------------------------------------------------------|-------------------------------------------------------------------|
| **Maestro** (YAML, black box)     | Runs against any installed APK, no test APK, no instrumentation; flows readable by non-authors; flavors = another `appId` | Host-side only; no access to app internals; sandboxed JS (no adb); hierarchy-based so overlays/IME hide nodes |
| Espresso / Compose `androidTest`  | In-process, fast, can set up state through app code; runs under Gradle (`connectedAndroidTest`) | Needs a test APK of a *debug*-like build; does not exercise the minified release; setup per flavor |
| UiAutomator (instrumented)        | Black box + system UI from Kotlin                                | Verbose; still needs an instrumentation APK                       |
| Screenshot tests (Paparazzi / Roborazzi / Compose Preview) | Fast layout regression on JVM, no device         | Not behaviour; never sees R8, flavors, first run                  |
| Device farm (Firebase Test Lab, etc.) | Real devices, matrix of API levels, no local emulator        | Cost, upload of the APK to a third party (check the data policy), slower feedback |

Default for a solo or small-team product: **Maestro on the release APK** for the top layer, plus
**Compose UI/screenshot tests only if layout regressions have actually bitten**. Do not buy a
second black-box tool for the same job.

Reach for in-process `androidTest` when the thing under test needs app internals the black box
cannot reach (a fake clock, a stubbed network) *and* a debug-like build is acceptable for it.

## Sizing the suite

- Start with: launch (all root destinations visible), search → open item, one "write then read
  back" path, first run on a clean state, one flow per store flavor that differs in behaviour.
- Add a flow when **a real defect escaped** that only the artifact could show (the offline-onboarding
  Skip button was a regression flow written after the bug). Add a regression flow *per escaped
  artifact-level defect*, not per feature.
- Mutation flows cost the most (clean state, more steps, more flake). Keep one per *boundary*
  (persisted write, file export/import, permission, network toggle), not one per entity.
- Review the suite at every major release: a flow that has never failed and whose behaviour is
  covered one layer down is a deletion candidate; a flow that flakes weekly is either fixed or
  deleted — not retried forever.

## Alternatives that replace E2E (partly)

- **Staged rollout + crash-free monitoring** replaces "did it break for everyone" after publish,
  not "can it launch" before.
- **Pre-launch report (Play Console)** runs a robo crawl on real devices: free, finds launch
  crashes on some devices, finds nothing about your business flows. Use it as an extra net, not as
  the gate.
- **Manual checklist** is a legitimate gate for a solo project *if it is written, short and
  recorded* — an E2E suite is that checklist made executable.
