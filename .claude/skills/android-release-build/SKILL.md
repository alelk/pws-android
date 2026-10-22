---
name: android-release-build
description: Use when configuring, auditing or debugging the release build of an Android app (APK / AAB) that ships to more than one store or audience — Gradle productFlavors by dimension (distribution / content level / store), per-flavor signing configs with separate keystores or aliases whose credentials come only from Gradle properties / env (never committed, never local.properties), the buildType-overrides-flavor signingConfig trap, signing-certificate verification of the built APK and AAB, per-flavor applicationId / versionNameSuffix / BuildConfig fields with defaults in defaultConfig, flavor-scoped dependencies and ProGuard files, release invariants checked before the build, versionCode vs semantic app.version, and R8/ProGuard shrinking discipline — why wildcard -keep rules defeat shrink/obfuscate/optimize, targeted keep rules with a why comment each, a DEX-size gate, mapping upload per variant, and a fitness test that forbids wildcard keep rules from creeping back.
metadata:
  version: "1.0.0"
  requires: "kotlin-project-layout"
  project-types: "android-app"
---

# Android Release Build

## When to use

- An Android app gets a second distribution (another store, a content variant, a white label) and
  needs `productFlavors`, separate signing and per-flavor configuration.
- Setting up or auditing release signing: where keystore credentials come from, which flavor is
  signed with which key, how CI proves the artifact carries the right certificate.
- Google Play Console reports "App optimization: Low" (low shrinking / obfuscation / optimization
  percentages, large uncompressed DEX), or someone proposes "just add `-keep class x.** { *; }`" to
  fix a release-only crash.
- Deciding how `versionCode` relates to the semantic `app.version`.

**Not this skill:** the release *pipeline* — Conventional Commits, semantic-release, `app.version`
handoff, GitHub Actions hardening, GitHub Release assets — is
[`gradle-release-system`](../gradle-release-system/SKILL.md). This skill is what that pipeline
calls: the Android module's flavors, signing, shrinking and version code. Apply both; neither
duplicates the other.

## What gets configured

| Area             | Where                                   | What                                                                                                   |
|------------------|-----------------------------------------|--------------------------------------------------------------------------------------------------------|
| Flavors          | app `build.gradle.kts`                  | one named dimension; per flavor `applicationId[Suffix]`, `versionNameSuffix`, `resValue`, `buildConfigField` |
| Routing config   | `defaultConfig` / flavor / build type   | `BuildConfig` fields with a default for every variant (catalog URLs, bundle variant, feature flags)     |
| Signing          | `signingConfigs` + flavor `signingConfig` | one config per key; credentials from Gradle properties; absent → unsigned, CI → hard fail            |
| Flavor isolation | `"<flavor>Implementation"`, flavor `proguardFiles` | a store SDK and its keep rules exist only in the flavor that ships it                        |
| Invariants       | task wired to `pre<Flavor>ReleaseBuild` | applicationId, versionCode floor, minSdk ceiling, provider authority — checked before compiling        |
| Version          | root build                              | `versionName` from `app.version`; `versionCode` monotonic integer (derived or counted, with a guard)   |
| R8               | `proguard-rules.pro` (+ per flavor)     | targeted rules only, each with a "why"; diagnostics read from `build/outputs/mapping/<variant>/`        |
| Locks            | unit test + release CI                  | `ProguardRulesTest` (no wildcard `-keep`) in `./gradlew build`; DEX-size gate + certificate check on the artifact |

## Hard rules

- ❌ **Never** put keystore paths, passwords or aliases in a committed file — not in
  `build.gradle.kts`, not in the repo's `gradle.properties`. They come from `-P…`,
  `~/.gradle/gradle.properties` or `ORG_GRADLE_PROJECT_<name>` env, and in CI from secrets. *Why:*
  a leaked upload key lets anyone ship an "update" to your users; a leaked app-signing key without
  Play App Signing cannot be rotated at all.
- ❌ **Never** expect `findProperty()` to read `local.properties` — Gradle does not expose it as
  project properties; the lookup silently returns `null` and the build quietly ships without the
  value. If a value must live in `local.properties`, load it with `Properties()` explicitly.
- ✅ Missing signing credentials **locally** → unsigned release APK (still fine for R8/DEX
  measurement), with a lifecycle log line. In **CI** a flag (`-PrequireReleaseSigning=true`) turns
  a missing credential into a configuration failure. *Why:* an unsigned or debug-signed artifact
  must never reach an upload step by accident.
- ✅ Signing is assigned **per flavor**, and the `release` build type has **no** `signingConfig`.
  *Why:* a build type's `signingConfig` overrides the flavor's — one global assignment signs every
  store with the same key, which a store that already has users under another certificate rejects
  as an update.
- ✅ A flavor whose installed base is pinned to an existing certificate (a store you migrated from,
  a fork) gets its **own keystore**, and CI **verifies the certificate SHA-256 of the built APK and
  AAB** against a pinned value before upload. Verify the artifact, not the Gradle config.
- ❌ **Never** change `applicationId`/`applicationIdSuffix`, the signing key or a provider authority
  of a published flavor. Each is an identity: change it and the "update" becomes a different app.
  Pin them in an invariants task (`verify<Flavor>ReleaseInvariants`) wired to the pre-build task.
- ✅ Every `buildConfigField` read from `main` has a value in **every** variant — declare the
  default in `defaultConfig` (or in every build type), override in the one flavor that differs.
  *Why:* a field defined in one flavor only compiles that flavor; the others break late.
- ✅ A store-specific SDK is a flavor-scoped dependency (`"storeBImplementation"(…)`) with its
  keep rules in a flavor-scoped `proguardFiles(…)`, and a source-scanning test forbids its package
  outside `src/<flavor>/`. *Why:* other flavors must not carry its bytes, its rules or its policy
  surface.
- ✅ `versionCode` is a strictly increasing integer across **all** uploads to a store listing and
  never goes back; `versionName` comes from `app.version`. Decide once how the two relate and
  enforce a floor → [`references/version-code-vs-app-version.md`](references/version-code-vs-app-version.md).
- ❌ **Never** add `-keep class <pkg>.** { *; }` (or `-keep class ** { *; }`) as a fix or a default.
  *Why:* it forbids R8 to remove **and** rename **and** optimize the whole tree — the measured
  result on a real app was 18–19% on all three Play metrics and a 36.8 MB DEX. Fix a release-only
  failure with the narrowest rule that names the class/member, preferring `-keepnames` /
  `-keepclassmembers` / `-if` → [`references/r8-shrinking.md`](references/r8-shrinking.md).
- ✅ Every keep rule carries a comment: what reads it reflectively/by name, where in the code, and
  what breaks without it. The file ends with a "deliberately absent" section listing the broad
  rules that were removed and why they are not needed. *Why:* an unexplained rule is never deleted.
- ✅ A wildcard-keep fitness test runs inside `./gradlew build`, with a `KNOWN_WIDE_KEEPS` ratchet
  (`Map<rule, reason>`, exact equality) — the one justified exception is typically JNI.
  Template → [`templates/ProguardRulesTest.kt`](templates/ProguardRulesTest.kt).
- ✅ Only `-dontwarn` lines may be copied from `missing_rules.txt`, each with its origin. Never
  copy a `-keep` from a warning list.
- ✅ After any R8 change: build the release of **every** flavor (not only the default), compare DEX
  size before/after, and smoke-test an **upgrade over the previously published version** on a
  device. *Why:* field renames in protobuf-lite (DataStore, Tink / EncryptedSharedPreferences) do
  not crash on a fresh install — they lose stored settings or the database passphrase on update.
- ✅ `-keepattributes SourceFile,LineNumberTable` and the `mapping.txt` of each released variant
  archived under a name that includes `versionName` and `versionCode`. *Why:* an obfuscated crash
  without its exact mapping is unreadable forever.

## Templates

| Path                                                                                                  | Purpose                                                                          |
|-------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| [`templates/app-flavors-signing.gradle.kts.snippet`](templates/app-flavors-signing.gradle.kts.snippet) | Flavor dimension, per-flavor signing from properties, BuildConfig defaults, flavor-scoped deps / rules, invariants hook |
| [`templates/version-code.gradle.kts.snippet`](templates/version-code.gradle.kts.snippet)               | Root build: `versionName` from `app.version`, `versionCode` derived with a monotonic floor |
| [`templates/proguard-rules.pro.snippet`](templates/proguard-rules.pro.snippet)                         | Header ban, targeted rules with "why", `-dontwarn` discipline, "deliberately absent" |
| [`templates/ProguardRulesTest.kt`](templates/ProguardRulesTest.kt)                                     | Kotest fitness test: no wildcard `-keep`, guard + pair self-test, ratchet        |

## References (load on demand)

- [`references/product-flavors.md`](references/product-flavors.md) — dimensions, identity per
  flavor, `BuildConfig` routing fields, flavor-scoped deps/rules, library modules, flavor contract.
- [`references/signing-configs.md`](references/signing-configs.md) — credential sources, per-flavor
  assignment and override precedence, unsigned-by-default, CI keystore decoding, certificate check.
- [`references/r8-shrinking.md`](references/r8-shrinking.md) — what a wildcard keep costs, the
  replacement rule kit, diagnostic files, the break-fix decision tree, measurement and DEX gate.
- [`references/version-code-vs-app-version.md`](references/version-code-vs-app-version.md) —
  monotonic `versionCode` vs semantic `app.version`, schemes, prerelease collisions, floors.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md). Verify → [`checklists/release-build-checklist.md`](checklists/release-build-checklist.md).

## Related

- [`gradle-release-system`](../gradle-release-system/SKILL.md) — CI/CD pipeline, semantic-release,
  `app.version` handoff, workflow hardening (sibling; this skill is its Android build half).
- [`kotlin-project-layout`](../kotlin-project-layout/SKILL.md) — `app.version`, version catalogue,
  convention plugins, `./gradlew build` as the gate.
- [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md) — ratchet and guard
  conventions the `ProguardRulesTest` follows.

## Evidence

pws-android (verified 2026-10-21):

- `docs/ai/plans/2026-09-10_app-optimization-r8_plan.md` — Play Console baseline **Optimization
  18% · Obfuscation 18% · Shrinking 19%, 36.8 MB uncompressed DEX**, 32 087 classes / 151 693
  methods, caused by **11** `-keep class <pkg>.** { *; }` rules (kotlin, kotlinx, androidx.compose,
  datastore, security.crypto, koin, voyager, sqlcipher, three own packages). `androidx.compose.**`
  alone kept 11 400 material-icon classes (~9.25 MB of code) of which one was used; the uncovered
  Lucide icon library shrank to 39 classes — proof R8 worked and was forbidden. Targets: DEX ≤ 12 MB,
  obfuscation ≥ 85%, shrinking ≥ 60%, optimization ≥ 80%. Post-change Play numbers are not yet
  recorded in the plan.
- `app-compose/proguard-rules.pro` — the rewritten targeted file (header ban, kotlinx.serialization
  `-if` rules, Room, protobuf-lite `<fields>` for DataStore and Tink, one JNI wide keep for
  `net.zetetic.database.**`, `-keepnames` for Voyager screens used as analytics names,
  "deliberately absent" section); `proguard-rules-rustore.pro` attached only to the `rustore` flavor.
- `app-compose/src/test/.../architecture/ProguardRulesTest.kt` — the wildcard-keep ratchet
  (`KNOWN_WIDE_KEEPS` = the sqlcipher rule); `PaymentSdkIsolationTest`, `FlavorContractTest`.
- `app-compose/build.gradle.kts` — dimension `contentLevel` with `ru`/`full`/`uk`/`rustore`;
  signing configs `release-ru` (also used by `full`), `release-uk` (same keystore, other alias),
  `release-rustore` (separate keystore) from `android.release.*` Gradle properties;
  `CATALOG_URLS` / `BUNDLE_VARIANT` per build type, `PURCHASES_ENABLED` default `false` overridden
  by `-Ppws.rustore.purchasesEnabled`; `verifyRustoreReleaseInvariants` on `preRustoreReleaseBuild`
  (applicationId, versionCode > 38, minSdk ≤ 23, db authority).
- Root `build.gradle.kts` — `versionCode` is a hand-bumped `extra(48)`, `versionName` from
  `app.version` plus a build-date suffix and the flavor suffix; `prepare.sh` writes only
  `app.version` (prerelease suffix stripped).
- `.github/workflows/release-build.yml` — base64 keystores decoded to `/tmp`, credentials passed as
  `-P` from secrets, `apksigner` / `keytool` certificate SHA-256 check of the rustore APK **and**
  AAB, DEX-size gate via `tools/dex-report.py --max-dex-mb 15` (threshold still a placeholder to
  recalibrate), R8 mappings uploaded as a workflow artifact.
