---
name: android-store-billing
description: Use when an Android app sells premium features through a store billing SDK — RuStore Pay, Google Play Billing, Huawei/Samsung IAP — as in-app purchases, a lifetime unlock or a subscription. Covers: the SDK behind a store-agnostic PaymentProvider port scoped to one product-flavor source set, with a fitness test failing the build if another source set references the SDK package; a MonetizationMode axis (Donations/PremiumSales/PremiumComingSoon/None) keeping the payment stack out of DI until sales are on; an offline-first entitlement repository where local persisted state is truth and the store only reconciles; a monotonic purchase sync that may grant/extend but never revokes entitlement, counting only confirmed/paid statuses (PAID vs CONFIRMED, PENDING vs PURCHASED); lifetime granted on purchase success; soft-failing SDK calls; Unknown vs None at the premium gate; carrying an older app's paid flag across an update without a corruption handler; refunds as an explicit decision; and tests proving each red.
metadata:
  version: "1.0.0"
  requires: "kotlin-clean-architecture, architecture-fitness-tests"
  project-types: "android-app, compose-mobile, full-stack-kmp-app"
---

# Android Store Billing (IAP behind a store-agnostic port)

## When to use

- An Android app starts selling premium: a lifetime unlock, a subscription, or both, through an
  app-store SDK (RuStore Pay, Google Play Billing, Huawei/Samsung IAP).
- The same codebase ships several store builds (product flavors) and only some of them may link a
  given store SDK.
- A paid status already lives on users' devices (an older app, a fork, a previous SDK) and an update
  must not lose it.
- Any change to code that **writes** the entitlement — the place where one bad branch takes a paid
  feature away from a paying user.

Not this skill: server-side receipt validation and webhooks (Real-Time Developer Notifications), and
money ledgers — see [`ledger-and-wallet`](../ledger-and-wallet/SKILL.md). Paid AI on a backend →
[`ai-spend-and-entitlement`](../ai-spend-and-entitlement/SKILL.md).

## Model

```
shared UI (KMP features, payment-agnostic)
  MonetizationMode { Donations | PremiumSales | PremiumComingSoon | None }   ← one axis per build
  EntitlementRepository.info: StateFlow<EntitlementInfo>                     ← the port the gates read
  EntitlementInfo = Unknown | None | Lifetime | Subscription(until) | Expired(on)
  PremiumGate.requirePremium(feature) { … }  → UpsellRequest → paywall | coming-soon sheet
        ▲ Koin override (store flavor module loads after the shared module)
store flavor source set (src/<store>/)
  LocalEntitlementStore        ← persisted state; the ONLY writer; writes are monotonic
  LocalEntitlementRepository   ← reads the store above; no network, no SDK; never writes
  ── only when mode.purchasesEnabled ───────────────────────────────────────────
  PaymentProvider (port, generic models)  ←  <Store>PaymentProvider (the only SDK user)
  PurchaseSyncService          ← purchase list → grant / extend, never revoke
  PaymentController            ← paywall state; every SDK call guarded, failure = message
other flavors: no SDK dependency, AlwaysActive (or their own) EntitlementRepository
```

| What the skill sets up | Where |
|---|---|
| SDK only in `src/<store>/`, dependency `"<store>Implementation"`, flavor keep rules | Gradle + source sets |
| Fitness tests: no SDK package outside the store source sets; SDK only in the adapter files | `src/test/` (runs in every flavor's unit tests) |
| `MonetizationMode` + build flag → payment stack absent from DI when sales are off | shared module + flavor `FlavorIntegration.kt` |
| Offline entitlement read, expiry re-checked at gate time and at the expiry moment | `LocalEntitlementRepository` |
| Monotonic sync + "only confirmed statuses count" | `PurchaseSyncService`, adapter status sets |
| Soft-failing paywall controller with non-fatals | `PaymentController` |

## Hard rules

- ❌ **Never revoke or shorten a stored entitlement from a purchase list.** An empty list, an SDK
  error, a signed-out store app, another store account, an outage or a list without the lifetime
  product must write **nothing**. Lifetime is only ever set `true`; a subscription end only moves
  forward (`max(stored, new)`, write only on increase). *Incident:* the RuStore fork's
  `syncDataStoreWithPurchases()` wrote `purchase_full_access = false` whenever the product was absent
  from the list, and the first port copied it (finding **C3**, critical): opening "Purchases" with an
  empty or another account's list erased a lifetime purchase. *Lock:* `PurchaseSyncServiceTest` ("empty list keeps
  lifetime and writes nothing", `writes shouldBe 0`). → [`references/entitlement-sync.md`](references/entitlement-sync.md)
- ❌ **Never derive "is premium" from a live store call.** The persisted local state is the source of
  truth; the store only reconciles into it. The unlock must work offline, without the store app, and
  in a build whose SDK is never called. *Why:* a network/SDK failure would otherwise lock a paying
  user out of what they bought (invariant I7 in the evidence plan).
- ❌ **Never count a purchase that is not final.** Only statuses the store guarantees as paid and
  irreversible grant: RuStore one-step `CONFIRMED` (not `PAID` — the funds-on-hold state of a
  two-step payment that can still become `REVERSED`), subscription `ACTIVE` (not `PAUSED`,
  `TERMINATED`, …); Play `PURCHASED` (not `PENDING`). The adapter maps this to one generic
  `isPaid` flag; everything above filters on it. *Why:* sync grants are permanent by the rule above,
  so a grant on a reversible status is an unrevocable free unlock.
- ❌ **No store SDK type or package outside the adapter.** Only `src/<store>/` (and its test source
  set) may mention the SDK package, and inside it only the provider (+ its async bridge). *Why:* a
  reference from `main` breaks every other flavor's build or drags the SDK, its keep rules and its
  auto-init into APKs that must not carry it. *Lock:* `PaymentSdkIsolationTest` (scan in the shared
  `src/test`, so it runs in every flavor) + a file-level pin in the store flavor's tests.
  → [`references/provider-isolation.md`](references/provider-isolation.md)
- ❌ **When sales are off, nothing that can call the SDK or write the entitlement is resolvable.**
  The payment module is added to DI only when `mode.purchasesEnabled`; the deeplink activity
  forwards to the main screen without touching the controller. *Why:* a build that "doesn't sell
  yet" must not call the SDK (moderation, crashes without the store app) nor write the file it must
  preserve (invariant I8). *Lock:* Koin graph test — `getOrNull<PaymentProvider>() shouldBe null`.
- ❌ **Never "repair" the entitlement file.** No corruption handler that replaces the file with
  defaults, no second `DataStore` on the same file, no rename of the file or keys. A read error
  becomes `None` + a non-fatal and the file stays untouched for the next start. *Why:* a
  `ReplaceFileCorruptionHandler` turns one bad read into a permanent loss of the paid flag
  (invariant I6). *Lock:* golden-file tests compare the file's SHA-256 before/after.
  → [`references/legacy-entitlement-compat.md`](references/legacy-entitlement-compat.md)
- ❌ **A subscription purchase never sets the lifetime flag.** After `Success`, grant lifetime at once
  only for the lifetime product; a subscription's end arrives with the next purchase list. *Incident:*
  the fork's `makePurchase()` set `purchase_full_access = true` for **any** purchase and relied on a
  later sync to undo it — when that sync failed, a subscriber kept lifetime. (Honour what is already
  written; just don't write it again.) → [`references/purchase-flow.md`](references/purchase-flow.md)
- ✅ **Grant lifetime immediately after the store reports success**, before reloading the list — a
  hiccup in the follow-up `purchases()` call must not lose a purchase the user just paid for.
- ✅ **Every SDK call is guarded and degrades softly**: the failure becomes a paywall error state and
  a non-fatal (`recordError`), never a crash and never an entitlement write. Store failures are
  silent for the user; without telemetry they are invisible to you too.
- ✅ **`Unknown` is not `None`.** The repository starts at `Unknown`; the gate waits (bounded) for a
  definite value and, on timeout, drops the action **without** an upsell — telling a paying user
  "you have no premium" is worse than an ignored tap.
- ✅ **Expiry is re-checked at the moment a gate asks** (`currentInfo()`) and the state flips while
  the process lives (a tick at expiry / at midnight), with an injected `Clock` and time zone.
- ✅ **One monetization axis per build.** `MonetizationMode` is picked once per flavor; the paywall
  entry, the SDK, the donation prompt and gating all derive from it. Sales are switched on by a Gradle
  property → `BuildConfig`, default **off**.
- ✅ **Refunds are an explicit product decision**, written down. The evidence chose "a refund does not
  revoke automatically" (a false revocation costs more than a rare undeserved unlock); revoking needs
  an explicit refunded status from the store and its own plan, never "absent from the list".
- ✅ **Product ids are constants matching the store console** (and any previous app), in one object.

## Templates

| File | What |
|------|------|
| [`templates/core/Entitlement.kt`](templates/core/Entitlement.kt) | `MonetizationMode`, `EntitlementInfo`, `EntitlementRepository` (+ `currentInfo()`), `AlwaysActiveEntitlementRepository` — shared, payment-agnostic |
| [`templates/core/PremiumGate.kt`](templates/core/PremiumGate.kt) | gate with bounded wait on `Unknown`, call-time re-check, single upsell stream |
| [`templates/core/BillingTelemetry.kt`](templates/core/BillingTelemetry.kt) | the two telemetry calls the stack needs (`recordError`, `event`) + no-op |
| [`templates/store/PaymentProvider.kt`](templates/store/PaymentProvider.kt) | the port and generic models (`PaymentProduct`, `ActivePurchase.isPaid`, `PurchaseResult`), `ProductIds` |
| [`templates/store/LocalEntitlementStore.kt`](templates/store/LocalEntitlementStore.kt) | DataStore-backed persisted entitlement; error-safe read, monotonic `grantLifetime` / `extendSubscriptionUntil` |
| [`templates/store/LocalEntitlementRepository.kt`](templates/store/LocalEntitlementRepository.kt) | offline, read-only `EntitlementRepository`; tick at expiry; `currentInfo()` |
| [`templates/store/PurchaseSyncService.kt`](templates/store/PurchaseSyncService.kt) | monotonic reconciliation + `onPurchaseSucceeded` |
| [`templates/store/PaymentController.kt`](templates/store/PaymentController.kt) | paywall state, guarded calls, grant-then-reload |
| [`templates/store/examples/RuStorePaymentProvider.kt`](templates/store/examples/RuStorePaymentProvider.kt) | worked adapter for RuStore Pay SDK 11.1.0 (BOM 2026.08.01): status sets, one-step purchase, `Task` → coroutine |
| [`templates/flavor/store/FlavorIntegration.kt`](templates/flavor/store/FlavorIntegration.kt) | store flavor: `MONETIZATION` from `BuildConfig`, entitlement module always, payment module only when selling |
| [`templates/flavor/free/FlavorIntegration.kt`](templates/flavor/free/FlavorIntegration.kt) | a flavor without a store SDK: same public names, no-ops |
| [`templates/build/app-build.gradle.kts.snippet`](templates/build/app-build.gradle.kts.snippet) | flavor, `purchasesEnabled` property → `BuildConfig`, `"<store>Implementation"`, flavor keep rules |
| [`templates/test/PaymentSdkIsolationTest.kt`](templates/test/PaymentSdkIsolationTest.kt) | source-set-level SDK isolation (shared `src/test`), with guard and comment stripping |
| [`templates/test/StoreFlavorWiringTest.kt`](templates/test/StoreFlavorWiringTest.kt) | store-flavor test: default build doesn't sell, no payment beans when off, SDK in exactly the adapter files |
| [`templates/test/PurchaseSyncServiceTest.kt`](templates/test/PurchaseSyncServiceTest.kt) | the monotonic-sync lock, with an in-memory `DataStore` counting writes |

## References (load on demand)

- [`references/provider-isolation.md`](references/provider-isolation.md) — the port, flavor scoping (dependency, keep rules, manifest), the two isolation tests, `MonetizationMode` and the sales flag.
- [`references/entitlement-sync.md`](references/entitlement-sync.md) — offline-first trust model, monotonic writes, when to reconcile, expiry and clocks, `Unknown`, refunds.
- [`references/purchase-flow.md`](references/purchase-flow.md) — one-step vs two-step, status mapping per store (RuStore, Play), acknowledgement, success handling, error surface, deeplink return.
- [`references/legacy-entitlement-compat.md`](references/legacy-entitlement-compat.md) — keeping an older app's paid flag across an update: same file, same keys, golden files, upgrade test.
- [`references/tests.md`](references/tests.md) — every lock, its level and its red run.

## Variants / open decisions

- **Server-side verification.** With a backend, send the purchase token / invoice id to it and let it
  verify with the store API; the client still keeps a local entitlement for offline use and still
  applies it monotonically. The evidence app has no backend and trusts the SDK's list.
- **Lost local state** (reinstall, new device): the entitlement comes back only through a
  reconciliation with the store (sales on) — or, with sales off, through a manual path (the evidence
  left it out of scope and noted a signed offline unlock code as an option).
- **Backup transfer** of the entitlement (Auto Backup / a backup agent): only if restore is also
  monotonic; the evidence deferred it.
- **Expired subscribers while sales are off** cannot renew through no fault of theirs — block with a
  dated message (evidence default) or extend access until sales open (alternative). Owner decision.

## Related

- [`kotlin-clean-architecture`](../kotlin-clean-architecture/SKILL.md) — ports & adapters, `Unconfigured*`/no-op adapters for optional services.
- [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md) — source scanning, guards, capability ownership.
- [`compose-multiplatform-ui`](../compose-multiplatform-ui/SKILL.md) — where the gate and upsell host live in shared UI.
- [`kotlin-testing-strategy`](../kotlin-testing-strategy/SKILL.md) — red-first, fakes that honour contracts, golden fixtures.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md). Verify → [`checklists/billing-checklist.md`](checklists/billing-checklist.md).

## Evidence

pws-android `rustore` flavor (RuStore Pay SDK `ru.rustore.sdk:pay` via BOM `2026.08.01`), plan
`docs/ai/plans/2026-09-29_rustore-release-compat-pro-coming-soon_plan.md` (findings C3, H1, H2;
invariants I6–I9; decisions §4.3, §4.4, §7):

- `app-compose/src/rustore/kotlin/.../payment/PurchaseSyncService.kt` — monotonic sync ("invariant
  I9"; the fork's `syncDataStoreWithPurchases()` named as the defect); `PaymentProvider.kt` (port,
  `ActivePurchase.isPaid`); `RuStorePaymentProvider.kt` (`PAID_PRODUCT_STATUSES = {CONFIRMED}`,
  `ACTIVE_SUBSCRIPTION_STATUSES = {ACTIVE}`, `PreferredPurchaseType.ONE_STEP`);
  `PaymentController.kt` (guarded calls, grant-then-reload, non-fatals);
  `LegacyRuStoreEntitlementStore.kt` (one DataStore delegate, no corruption handler, monotonic
  writes); `RuStoreCompatEntitlementRepository.kt` (read-only, `Clock`/`TimeZone`, midnight ticks,
  `currentInfo()`).
- `app-compose/src/rustore/kotlin/.../flavor/FlavorIntegration.kt` — `MONETIZATION` from
  `BuildConfig.PURCHASES_ENABLED` (Gradle `pws.rustore.purchasesEnabled`, default false),
  `rustoreKoinModules(mode)` adds the payment module only for `PremiumSales`.
- `app-compose/src/test/kotlin/.../architecture/PaymentSdkIsolationTest.kt` (`\bru\.rustore\.` only
  under `src/rustore/` and `src/testRustore/`); `src/testRustore/.../flavor/RustoreKoinModulesTest.kt`
  (no payment beans in `PremiumComingSoon`; `ru.rustore.sdk` only in `RuStorePaymentProvider.kt` and
  `RustoreSdkOpts.kt`); `PurchaseSyncServiceTest.kt`; `RuStoreCompatEntitlementRepositoryTest.kt`
  (golden `.preferences_pb` fixtures, SHA-256 unchanged).
- pws-core `features/.../premium/EntitlementRepository.kt`, `PremiumGate.kt` (`Unknown` timeout drops
  the action without an upsell), `features/.../monetization/MonetizationMode.kt`.
- e2e: `e2e/flows/compose/rustore/`, `e2e/scripts/rustore-entitlement.sh` (plants a fork file;
  `show` prints its SHA-256 for the before/after check).
