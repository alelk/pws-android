# Usage — Android Release Build

Prerequisite: [`kotlin-project-layout`](../kotlin-project-layout/SKILL.md) is applied (`app.version`,
version catalogue, convention plugins, `./gradlew build` gate). The CI/CD side — semantic-release,
workflow hardening, GitHub Release assets — is [`gradle-release-system`](../gradle-release-system/SKILL.md);
do it before or after, it does not overlap.

## 1. Name the flavors and their identities

Write a table before touching Gradle — it is the contract every later step checks:

| Flavor   | Store / audience | applicationId          | Signing key (keystore / alias) | Store-specific SDK | Prior installed base? |
|----------|------------------|------------------------|--------------------------------|--------------------|-----------------------|
| `free`   | Play             | `com.example.app`      | `play.jks` / `free`            | —                  | no                    |
| `pro`    | Play             | `com.example.app.pro`  | `play.jks` / `pro`             | —                  | no                    |
| `storeB` | Store B          | `com.example.app.b`    | `storeb.jks` / `storeb`        | Store B billing    | yes → pin certificate |

One dimension is usually enough. Add a second only for a genuinely independent axis (and accept
the variant multiplication) → [`references/product-flavors.md`](references/product-flavors.md).

## 2. Version wiring (root build)

Merge [`templates/version-code.gradle.kts.snippet`](templates/version-code.gradle.kts.snippet) into
the root `build.gradle.kts`. Pick the scheme (derived from `app.version`, or an explicit counter)
and set `VERSION_CODE_FLOOR` to the highest code ever uploaded to any listing this app replaces →
[`references/version-code-vs-app-version.md`](references/version-code-vs-app-version.md).

## 3. Flavors, BuildConfig, signing (app module)

Merge [`templates/app-flavors-signing.gradle.kts.snippet`](templates/app-flavors-signing.gradle.kts.snippet)
into the app module's `build.gradle.kts`:

1. `flavorDimensions` + one `create(...)` per row of the table.
2. `buildConfigField` **defaults** in `defaultConfig` (or every build type); overrides only where a
   flavor/build type differs.
3. One `signingConfig` per key, created only when all four properties are present; assigned in
   `productFlavors`, **never** on the `release` build type.
4. Store SDK: `"<flavor>Implementation"(…)` and `proguardFiles("proguard-rules-<flavor>.pro")`
   inside that flavor.
5. If any flavor has a prior installed base: the `verify<Flavor>ReleaseInvariants` task wired to
   `pre<Flavor>ReleaseBuild`.

Library modules that the app depends on with flavor-specific code declare the **same dimension and
flavor names**, or `missingDimensionStrategy` in the app.

## 4. Credentials outside the repository

Locally, in `~/.gradle/gradle.properties` (never the repo's):

```properties
releasePlayKeystorePath=/home/me/keys/play.jks
releasePlayStorePassword=…
releasePlayKeyPassword=…
releaseFreeKeyAlias=free
```

In CI: decode the base64 keystore secret to a temp file, pass the rest as
`ORG_GRADLE_PROJECT_<name>` env from secrets, add `-PrequireReleaseSigning=true` →
[`references/signing-configs.md`](references/signing-configs.md). Check `.gitignore` covers
`*.jks`, `*.keystore`, `keystore.properties`, `local.properties`.

## 5. ProGuard / R8 rules

1. `isMinifyEnabled = true`, `isShrinkResources = true`,
   `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`
   on `release`.
2. Start `proguard-rules.pro` from [`templates/proguard-rules.pro.snippet`](templates/proguard-rules.pro.snippet):
   header ban, then only rules for mechanisms the code really uses (reflection by name, JNI,
   protobuf-lite field lookup, class names as data). Each rule gets a "why" comment.
3. An existing project with broad keeps: **measure first** (step 7), replace the file as a whole,
   fix breakages with the decision tree in [`references/r8-shrinking.md`](references/r8-shrinking.md),
   never by restoring a wildcard.

## 6. Lock it in the build

1. Copy [`templates/ProguardRulesTest.kt`](templates/ProguardRulesTest.kt) into the app module's
   `src/test/kotlin/.../architecture/`, adjust `RULE_FILES` and `KNOWN_WIDE_KEEPS`.
2. See it red: add `-keep class com.example.** { *; }` to `proguard-rules.pro`, run
   `./gradlew :app:test<Flavor>DebugUnitTest`, confirm the failure names the rule, revert.
3. Add a source-scanning isolation test for each store-only SDK package (pattern:
   [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md) capability ownership).
4. Write the hard rule into the project's `AGENTS.md` with a link to the test.

## 7. Measure and verify the artifact

```bash
./gradlew :app:assembleFreeRelease :app:assembleProRelease :app:assembleStoreBRelease   # every flavor
ls app/build/outputs/mapping/freeRelease/        # mapping, seeds, usage, configuration
```

- DEX size / class count / obfuscation estimate from the APK (`unzip -l` on `classes*.dex`, or a
  small dex-report script) — record before/after.
- Certificate of every artifact you will upload:
  `apksigner verify --print-certs <apk>` and `keytool -printcert -jarfile <aab>`.
- On a device: install the **previously published** version, then upgrade to the new release
  build; walk the flows that touch DataStore, encrypted prefs, the DB, serialization, payments.

## 8. CI gates (in the release workflow from `gradle-release-system`)

- Certificate SHA-256 check for flavors with a pinned key; DEX-size gate with threshold = measured
  size + ~25%; archive `mapping.txt` per variant named `mapping-<variant>-<versionName>-<versionCode>.txt`.
- The everyday `./gradlew build` runs `ProguardRulesTest`; the release job runs R8 itself.

## 9. Verify

Walk through [`checklists/release-build-checklist.md`](checklists/release-build-checklist.md).
