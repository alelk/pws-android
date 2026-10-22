# Provider isolation: the store SDK behind a port, in one flavor

The store SDK is the most volatile dependency of the app: it changes with every SDK release, differs
per store, may only ship in some builds, and its failures (no store app, signed out, outage) must
never reach premium checks. So it gets exactly one door.

## Layers

| Layer | Knows | Must not know |
|---|---|---|
| shared UI / features (KMP `commonMain`) | `MonetizationMode`, `EntitlementInfo`, `EntitlementRepository`, `PremiumGate`, `PremiumFeature` | product ids, purchase ids, any store type, how premium was paid |
| store flavor glue (`src/<store>/…/flavor/`) | which repository / provider to bind, `BuildConfig.PURCHASES_ENABLED` | SDK calls |
| store flavor payment (`src/<store>/…/payment/`) | `PaymentProvider` port + generic models, sync, controller, local store | SDK types — except in the adapter |
| adapter (`<Store>PaymentProvider.kt` + its async bridge) | the SDK | anything above the port |
| other flavors (`src/<other>/`) | the same `FlavorIntegration.kt` public names, no-ops | that a store SDK exists |

Evidence: in pws-android the generic port lives in the rustore flavor itself
(`app-compose/src/rustore/.../payment/PaymentProvider.kt`), because only one flavor sells. When a
second store flavor appears, move `PaymentProvider.kt`, the generic models, `PurchaseSyncService`,
`PaymentController` and the local store to a shared Android source set or module (e.g. an
`app-billing` library module both flavors depend on) and keep only the adapter per flavor.

## Scoping everything the SDK brings

- **Dependency**: `"<store>Implementation"(platform(bom))` + `"<store>Implementation"(sdk)`. Never
  `implementation`. (Evidence: `app-compose/build.gradle.kts`, `"rustoreImplementation"`.)
- **Keep / dontwarn rules**: a flavor proguard file (`proguard-rules-rustore.pro`, attached via
  `productFlavors { rustore { proguardFiles(...) } }`) — the other flavors don't pay for the rules.
  The evidence file holds only `-dontwarn ru.rustore.sdk.**` (the SDK ships consumer rules) and a
  commented-out "plan B" wide keep, to be enabled only if minified payments break. A blanket
  `-keep class <pkg>.** { *; }` is forbidden by the project's `ProguardRulesTest` (a wide keep once
  grew the DEX to 36.8 MB).
- **Manifest**: SDK meta-data (RuStore auto-init: `console_app_id_value`, `sdk_pay_scheme_value`) and
  the payment-return deeplink activity go in the flavor manifest overlay
  (`src/rustore/AndroidManifest.xml`). The deeplink scheme must equal `sdk_pay_scheme_value`. The
  console app id is a string resource, not a literal in code.
- **Tests that reference flavor classes** go in `src/test<Store>/` (they don't compile elsewhere);
  **tests that guard the boundary** go in the shared `src/test/` (they must run in every flavor).

## The two isolation tests

1. **Source-set level, shared `src/test`** — `PaymentSdkIsolationTest`
   ([template](../templates/test/PaymentSdkIsolationTest.kt)). Textual scan of every `.kt` under
   the app and data modules; the SDK package regex (`\bru\.rustore\.` in the evidence) may match only
   under `app/src/<store>/` and `app/src/test<Store>/`. Comments are stripped, string literals kept —
   so a literal `"ru.rustore.sdk.Foo"` in `main` is red (that is the evidence's documented red run).
   Guard: the scan finds the SDK where it is allowed, so a wrong cwd or a typo'd prefix cannot pass
   vacuously.
2. **File level, `src/test<Store>`** — in `StoreFlavorWiringTest`
   ([template](../templates/test/StoreFlavorWiringTest.kt)). Inside the store source set, the set of
   files mentioning the SDK equals a pinned set. Evidence (`RustoreKoinModulesTest`):
   `listOf("RuStorePaymentProvider.kt", "RustoreSdkOpts.kt")` — the adapter and the `Task` →
   coroutine bridge `coAwait()`. Exact equality: fails on a new SDK user and on the adapter vanishing.

Why both: (1) protects the other flavors' builds and APKs; (2) protects the port — without it, the
paywall screen or the sync could start using SDK types inside the store flavor and the port becomes
decorative.

A stronger form, when a project already runs Konsist: assert on imports of classes in
`..payment..` instead of text. The regex form is kept because it also sees `main`, which the store
flavor's compiled classes don't include.

## MonetizationMode and the sales flag

`MonetizationMode { Donations, PremiumSales, PremiumComingSoon, None }` is one axis picked once per
build (`val MONETIZATION` in each flavor's `FlavorIntegration.kt`); `donationsEnabled`,
`premiumGatingEnabled`, `purchasesEnabled` derive from it. Decisions recorded in the evidence plan
§4.4:

- **Build-time flag, not remote config.** Store-account status changes rarely and turning sales on
  needs a release anyway (SDK checked on device, console configured); remote config would add a
  network dependency and a new failure class. The mode is bound in Koin, which leaves room for a
  remote override later.
- **`PremiumComingSoon` is an enum value, not a boolean** — no meaningless "donations + paywall".
- **The SDK stays linked but is not called** while sales are off — switching on is one flag. Risk to
  check on device: an SDK that auto-initialises from manifest meta-data must not crash a cold start
  without the store app or network (plan T-G04).

Wiring rule: `storeKoinModules(mode)` always adds the entitlement module (local store + repository),
and adds the payment module (provider, sync, controller) **only** when `mode.purchasesEnabled`. The
payment-return activity checks `MONETIZATION.purchasesEnabled` before resolving the controller and
otherwise forwards to the main activity — the deeplink filter stays in the manifest because the SDK
will need it later. Evidence: `FlavorIntegration.kt` (`rustoreKoinModules`), `PaymentActivity.kt`.

Flavor contract: every flavor declares the same public top-level names in `flavor/FlavorIntegration.kt`;
the evidence pins this with `FlavorContractTest` (`MONETIZATION`, `flavorKoinModules`,
`flavorStartupTasks`, `flavorShowPaywall`), so a rename in one flavor fails on any flavor's build.

## One upsell consumer

Gates emit `UpsellRequest(feature)`; exactly one collector at the app root (`UpsellHost` in the
evidence) shows the paywall when `purchasesEnabled`, otherwise a coming-soon sheet. Two collectors of
a `SharedFlow` show it twice (plan risk R9).
