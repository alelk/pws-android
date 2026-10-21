# Android Release Build Checklist

## Flavors

- [ ] Flavor table written (flavor → store, applicationId, key, store SDK, prior installed base).
- [ ] One named dimension per independent axis; library modules with flavor code repeat the same
      dimension/flavor names (or the app sets `missingDimensionStrategy`).
- [ ] `applicationId[Suffix]` and provider authorities unique per installed app and unchanged since
      first upload.
- [ ] `versionNameSuffix` per flavor, so reports name the flavor.
- [ ] Every `buildConfigField` read from `src/main` has a default in `defaultConfig` (or every build
      type); flavors only override.
- [ ] No access-granting secret in `BuildConfig`; behavior flags are Gradle properties with a safe
      default and a lifecycle log line.
- [ ] Store-only SDKs are `"<flavor>Implementation"` deps with flavor-scoped `proguardFiles`, and a
      source-scanning test confines their package to `src/<flavor>/`.
- [ ] Flavor source sets expose the same contract (test like `FlavorContractTest`).

## Signing

- [ ] No keystore path, password or alias in any committed file; `.gitignore` covers `*.jks`,
      `*.keystore`, `keystore.properties`, `local.properties`.
- [ ] Credentials read with `findProperty()` from `~/.gradle/gradle.properties` / `-P` /
      `ORG_GRADLE_PROJECT_*` — not expected from `local.properties`.
- [ ] Missing credentials locally → unsigned APK + log line; CI passes `-PrequireReleaseSigning=true`.
- [ ] `signingConfig` set per flavor; the `release` build type has none.
- [ ] Flavors with a pinned certificate have their own keystore and secrets.
- [ ] CI verifies the certificate SHA-256 of **both** APK and AAB for pinned flavors before upload.
- [ ] Release invariants task (applicationId, versionCode floor, minSdk ceiling, authority) wired to
      `pre<Flavor>ReleaseBuild` for flavors with an installed base.

## Version

- [ ] `versionName` derives from `app.version`; nothing else hardcodes the product version.
- [ ] `versionCode` scheme chosen (derived / counter / CI) and written down; floor checked at build
      time; prerelease uploads cannot collide with the final.
- [ ] Hotfix re-upload path defined (new code without editing `app.version`).

## R8 / ProGuard

- [ ] `release`: `isMinifyEnabled = true`, `isShrinkResources = true`,
      `proguard-android-optimize.txt` + `proguard-rules.pro`.
- [ ] No `-keep class <pkg>.** { *; }` / `-keep class ** { *; }` except entries in
      `KNOWN_WIDE_KEEPS` with reasons (typically JNI).
- [ ] Every rule has a "why" comment; file has the header ban and a "deliberately absent" section.
- [ ] `-dontwarn` lines only from `missing_rules.txt`, each with its origin; no `-keep` copied from warnings.
- [ ] `-keepattributes SourceFile,LineNumberTable`; `mapping.txt` archived per variant with
      `versionName` + `versionCode` in the name.
- [ ] `ProguardRulesTest` in the app module, guard + pair tests green, seen red once on a planted
      wildcard rule.
- [ ] `AGENTS.md` carries the hard rule with links to the test and the measurement.

## Verify the artifact

- [ ] Release `assemble`/`bundle` of **every** flavor succeeds.
- [ ] DEX size, class and method counts recorded before/after any rule change; seeds.txt is small.
- [ ] DEX-size gate in the release job, threshold = measured size + ~25% (not a placeholder).
- [ ] Device smoke test of the release build, including **upgrade over the published version**
      (DataStore, encrypted prefs / DB key, serialization, payments, analytics names).
- [ ] Staged rollout and crash-free monitoring after an R8 change.
- [ ] CI pipeline itself (semantic-release, permissions, pinning) checked with
      [`gradle-release-system`](../../gradle-release-system/checklists/release-readiness.md).
