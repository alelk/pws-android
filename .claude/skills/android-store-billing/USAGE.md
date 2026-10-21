# Usage — Android Store Billing

Prerequisites: a Gradle Android app module with product flavors (or ready to add one), Koin DI,
Kotest on the JVM for unit tests ([`kotlin-project-layout`](../kotlin-project-layout/SKILL.md)); a
shared UI/features module if the app is KMP ([`kotlin-clean-architecture`](../kotlin-clean-architecture/SKILL.md));
the source-scanning conventions of [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md).

## 1. Decide in the plan (before code)

Write these down; an executor must not invent them:

- **Stores and flavors.** Which flavor links which SDK; which flavors sell nothing
  (`Donations` / `None`).
- **Products.** Ids exactly as in the store console (and as any previous app used); lifetime vs
  subscription; what is gated (`PremiumFeature` list) — every entry point of each feature.
- **Final statuses.** For each store, the allow-set of statuses that grant, checked against the SDK
  docs for the exact version/BOM ([`references/purchase-flow.md`](references/purchase-flow.md)).
  One-step or two-step (RuStore); acknowledgement point (Play).
- **Refund policy.** Default: refunds don't revoke automatically. Anything else needs an explicit
  store status and its own plan.
- **Reconcile triggers.** Paywall `onResume` + after purchase (evidence), plus app start or near-expiry
  if subscriptions renew ([`references/entitlement-sync.md`](references/entitlement-sync.md#when-to-reconcile)).
- **Existing paid state on devices?** If yes, adopt its file/keys/encoding verbatim
  ([`references/legacy-entitlement-compat.md`](references/legacy-entitlement-compat.md)) and replace
  the key names in `LocalEntitlementStore`.
- **Launch mode.** Sales off by default (`PremiumComingSoon`) until the store account, console and SDK
  are verified on device.

## 2. Shared, payment-agnostic types

```bash
cp <skill>/templates/core/Entitlement.kt       features/src/commonMain/kotlin/<pkg>/features/premium/
cp <skill>/templates/core/PremiumGate.kt       features/src/commonMain/kotlin/<pkg>/features/premium/
cp <skill>/templates/core/BillingTelemetry.kt  domain/src/commonMain/kotlin/<pkg>/domain/telemetry/   # or use your Telemetry port
```

Bind defaults in the shared Koin module: `EntitlementRepository` → `AlwaysActiveEntitlementRepository`,
`MonetizationMode` → `None`, `PremiumGate` → `DefaultPremiumGate`. One `UpsellHost` at the app root
collects `upsellRequests` (paywall if `purchasesEnabled`, coming-soon sheet otherwise).

## 3. Gradle: flavor, flag, scoped SDK

Merge [`templates/build/app-build.gradle.kts.snippet`](templates/build/app-build.gradle.kts.snippet):
`PURCHASES_ENABLED` `BuildConfig` field (default `false`, store flavor reads
`-Papp.store.purchasesEnabled`), `"<store>Implementation"` for the SDK + BOM, a flavor proguard file.
Add the SDK's manifest meta-data and the payment-return activity to `src/<store>/AndroidManifest.xml`
(scheme == the SDK's scheme value; console app id as a string resource).

## 4. The store flavor

```bash
D=app/src/<store>/kotlin/<pkg>/android
cp <skill>/templates/store/PaymentProvider.kt             $D/payment/
cp <skill>/templates/store/LocalEntitlementStore.kt       $D/payment/
cp <skill>/templates/store/LocalEntitlementRepository.kt  $D/payment/
cp <skill>/templates/store/PurchaseSyncService.kt         $D/payment/
cp <skill>/templates/store/PaymentController.kt           $D/payment/
cp <skill>/templates/flavor/store/FlavorIntegration.kt    $D/flavor/
```

Write the adapter `<Store>PaymentProvider.kt` — for RuStore start from
[`templates/store/examples/RuStorePaymentProvider.kt`](templates/store/examples/RuStorePaymentProvider.kt);
for another store keep its shape (allow-set `isPaid`, cancel → `Cancelled`, async bridge in the same
file). The paywall screen/activity reads `PaymentController.uiState`, calls `refreshData()` in
`onResume`, and — if sales are off — forwards to the main activity without resolving the controller.

## 5. The other flavors

```bash
cp <skill>/templates/flavor/free/FlavorIntegration.kt app/src/<other>/kotlin/<pkg>/android/flavor/
```

Same public names, no-ops; `main` calls `flavorKoinModules()` after the shared module so the store
flavor's `EntitlementRepository` overrides the default.

## 6. Tests

```bash
cp <skill>/templates/test/PaymentSdkIsolationTest.kt  app/src/test/kotlin/<pkg>/android/architecture/
cp <skill>/templates/test/StoreFlavorWiringTest.kt    app/src/test<Store>/kotlin/<pkg>/android/flavor/
cp <skill>/templates/test/PurchaseSyncServiceTest.kt  app/src/test<Store>/kotlin/<pkg>/android/payment/
```

Set `STORE_SDK_PACKAGE_PREFIX` in both guard tests (the placeholder fails the guard on purpose), the
allowed prefixes and `SDK_TOUCH_POINTS`. Add: an entitlement-repository test on virtual time (expiry,
`currentInfo()`), a corrupted-file test, golden-file tests if a legacy file is adopted, a gate test for
`Unknown`, a controller test with a throwing fake provider, and pin the store file name in a
storage-names test ([`references/tests.md`](references/tests.md)).

Prove each red once: restore "write lifetime = present" in the sync; drop the `isPaid` filter; add an
SDK literal in `main`; register the provider unconditionally; add a corruption handler.

```bash
./gradlew :app:test<Store>DebugUnitTest :app:test<Other>DebugUnitTest :app:assemble<Store>Release
./gradlew build
```

## 7. Before switching sales on

- Status allow-sets re-verified against the SDK docs for the shipped version.
- On a device: purchase, cancel, signed-out store app, no store app, airplane mode, reinstall —
  premium never disappears on a failure; a cold start without the store app/network doesn't crash.
- Monitoring: `entitlement_resolved` shares vs console purchase counts, `entitlement_read_failed`,
  `payment_*_failed`, `purchase {result}`.
- Flip with `-Papp.store.purchasesEnabled=true` in CI for the store release only.

Then walk through [`checklists/billing-checklist.md`](checklists/billing-checklist.md).
