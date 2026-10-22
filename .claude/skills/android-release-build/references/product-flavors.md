# Product flavors

A flavor is a second product built from the same sources: its own identity on the device, its own
store, its own configuration. Most defects here are not compile errors — they are a flavor nobody
built today, a field that exists in one variant only, or an SDK that leaked into an APK that must
not carry it.

*Mine:* a change that compiles and passes for the default flavor and breaks, or silently
misconfigures, another flavor that is built only at release time.

## Dimensions

- One dimension per **independent** axis. pws-android has one: `contentLevel` with
  `ru` / `full` / `uk` / `rustore` (content set and store mixed on one axis — acceptable while the
  combinations are few and fixed).
- Two dimensions (e.g. `store` × `tier`) multiply variants (×2 build types). Use them when the axes
  really combine freely; otherwise list the combinations as flavors of one dimension.
- Library modules with flavor-specific code declare the **same dimension and flavor names**
  (pws-android `:data:db-android` repeats `contentLevel` with per-flavor `DB_AUTHORITY`);
  libraries without flavors are consumed as-is, or the app sets `missingDimensionStrategy`.
- A custom build type (pws-android `localSeed`) must exist in every Android module of the build, or
  variant resolution fails — declare it in the shared convention plugin.

## Identity per flavor

| Property                   | Rule                                                                                      |
|----------------------------|-------------------------------------------------------------------------------------------|
| `applicationId[Suffix]`    | Fixed forever after the first upload. Different values = apps installable side by side.  |
| Content-provider authority | Unique per installed app (two apps with one authority cannot both install) — set via `resValue` per flavor |
| Signing key                | Per flavor → [`signing-configs.md`](signing-configs.md)                                    |
| `versionNameSuffix`        | Per flavor (`-uk`, `-rustore`), so a crash report or support screenshot names the flavor   |
| `versionCode`              | Shared scheme, but each store listing must see a strictly increasing value → [`version-code-vs-app-version.md`](version-code-vs-app-version.md) |

A flavor that replaces an existing published app (pws-android `rustore` replacing a fork at
`io.github.alelk.pws.app`, versionCode 38) uses `applicationId = …` (not a suffix) and pins all of
the above in an invariants task.

## BuildConfig fields as routing config

Use `buildConfigField` / `resValue` for values the code must branch on per variant: remote
endpoints, which signed content variant to fetch, feature flags.

| Scope       | Example (pws-android)                                                                 |
|-------------|---------------------------------------------------------------------------------------|
| Build type  | `CATALOG_URLS` (comma-joined mirror list, split at runtime) and `BUNDLE_VARIANT` = `release` / `debug` — release builds read release-signed content |
| Flavor      | `PURCHASES_ENABLED` default `false` in `defaultConfig`, overridden only in `rustore` from `-Ppws.rustore.purchasesEnabled` |
| defaultConfig | `APPMETRICA_API_KEY` from env / local file; empty → telemetry off, logged at configuration |

Rules:

- **Default in `defaultConfig`** (or in every build type), override where it differs. Code in
  `src/main` reads the field without knowing the flavor; a field missing in one variant fails only
  when that variant is compiled.
- A list travels as one `String` (`joinToString(",")`) — `BuildConfig` has no list type.
- A **secret** in `BuildConfig` is readable by anyone with the APK. Use it only for write keys that
  are public by design (analytics SDK keys), never for credentials that grant access.
- A flag that changes store behavior (sales on/off) is a Gradle property with a safe default and a
  lifecycle log line stating which way the build went — not an edit in the build file.
- Prefer one flavor-specific **implementation file** with the same public names in each flavor
  source set over `if (BuildConfig.FLAVOR == …)` in `main` (pws-android `flavor/FlavorIntegration.kt`,
  pinned by `FlavorContractTest`: every `src/<flavor>` declares the same public top-level names).

## Flavor-scoped dependencies and rules

```kotlin
productFlavors { create("storeB") { proguardFiles("proguard-rules-storeB.pro") } }
dependencies { "storeBImplementation"(libs.storeb.billing) }   // configuration created by AGP, referenced by name
```

- The other flavors carry neither the SDK nor its keep rules (no size, no policy surface).
- A source-scanning test forbids the SDK's package outside `src/storeB/` and `src/testStoreB/`
  (pws-android `PaymentSdkIsolationTest`: `\bru\.rustore\.` only under the `rustore` source sets,
  with a guard that the scan finds the allowed uses).
- Restrict a store-specific Maven repository to its group (`content { includeGroup("…") }`) so it
  is never queried for other dependencies.

## Build-time content per flavor

Flavor-specific generated assets (pws-android downloads seed books for `uk` / `rustore` at build
time) belong in a task registered from `androidComponents.onVariants` and attached with
`variant.sources.assets?.addGeneratedSourceDirectory(task) { it.outputDir }` — AGP owns the output
location and merges it only into that variant.

## Build every flavor

Release `assemble` of every flavor is part of any change to flavors, R8 rules or shared
dependencies. CI release jobs build only the flavors selected for a release; a flavor not released
for months is the one that breaks.
