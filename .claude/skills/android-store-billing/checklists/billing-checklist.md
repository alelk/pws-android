# Store Billing Checklist

## Design (written in the plan)

- [ ] Each flavor's `MonetizationMode` stated; which flavor links which SDK
- [ ] Product ids equal the store console (and any previous app), in one `ProductIds` object
- [ ] Per store: allow-set of final statuses, checked against the SDK docs for the exact version/BOM
- [ ] One-step vs two-step (RuStore) / acknowledgement point (Play) decided
- [ ] Refund policy written down (default: no automatic revocation)
- [ ] Reconcile triggers chosen; renewal latency for subscriptions accepted or addressed
- [ ] Existing paid state on devices? File, keys and encoding adopted verbatim
- [ ] Sales default off; the flag that turns them on is a Gradle property, not remote config

## Isolation

- [ ] SDK dependency is `"<store>Implementation"` (+ BOM), never `implementation`
- [ ] SDK keep/dontwarn rules in a flavor proguard file; no wide `-keep class <sdk>.** { *; }`
- [ ] SDK meta-data and the payment-return deeplink in the flavor manifest; scheme matches the SDK's value
- [ ] `PaymentSdkIsolationTest` in shared `src/test`, real SDK prefix, guard finds the SDK where allowed
- [ ] SDK imported only by the adapter (+ bridge): exact set pinned in a store-flavor test
- [ ] Shared UI/features know only `EntitlementRepository` / `EntitlementInfo` / `MonetizationMode` — no store types, no product ids
- [ ] Every flavor declares the same `FlavorIntegration.kt` public names (pinned)

## Sales off (`PremiumComingSoon`)

- [ ] `PaymentProvider`, `PurchaseSyncService`, `PaymentController` not resolvable (Koin test)
- [ ] Payment-return activity forwards without resolving the controller
- [ ] Paywall entry not offered; gate shows the coming-soon sheet; `flavorShowPaywall` is a no-op
- [ ] Entitlement still read and honoured; the file is not written (SHA-256 unchanged on device)

## Entitlement

- [ ] Local persisted state is the only input to premium checks; no live store call per check
- [ ] One DataStore delegate per file, one owner class; file name pinned by a test
- [ ] No corruption handler; read error → `None` + non-fatal, file untouched
- [ ] Store API has only `grantLifetime` / `extendSubscriptionUntil` (internal); no revoke/clear
- [ ] Repository starts at `Unknown`, shared eagerly; `currentInfo()` re-checks expiry against an injected `Clock`
- [ ] State flips at expiry (or midnight for day-granularity) while the process lives
- [ ] Gate: bounded wait on `Unknown`, drop without upsell on timeout; `None`/`Expired` upsell
- [ ] Exactly one `UpsellRequest` collector

## Sync and purchase

- [ ] `sync` filters `isPaid`; empty/unpaid list → no write; never writes lifetime `false`
- [ ] Subscription end = `max(stored, new)`, written only on increase; no end → ignored
- [ ] `purchases()` failure or signed out → sync not called
- [ ] After `Success`: lifetime granted at once; subscription never marked lifetime; then reload
- [ ] Every provider call guarded → `PaymentError` + non-fatal; never a crash, never a write
- [ ] `refreshData()` on every `onResume`, cancels the previous load; purchase ignored while loading
- [ ] Telemetry: outcomes and kinds only — no tokens, invoice ids, raw dates

## Tests (each proved red once)

- [ ] Sync: empty list keeps lifetime with `writes == 0`; foreign list; unpaid only; earlier end; later end; no end; success paths
- [ ] SDK isolation (source-set and file level)
- [ ] Koin graph: off → no payment beans; on → payment module added; default build does not sell
- [ ] Repository: expiry boundary, flip without new emission, `currentInfo()` fresh, corrupted file
- [ ] Golden files (if a legacy file is adopted), from an independent encoder; SHA-256 unchanged
- [ ] Gate `Unknown` timeout
- [ ] CI builds and unit-tests the store flavor and at least one non-store flavor

## Before enabling sales

- [ ] Status sets re-verified for the shipped SDK version
- [ ] Device matrix: purchase, cancel, signed out, no store app, offline, reinstall, upgrade from the previous release
- [ ] Monitoring in place: `entitlement_resolved` shares vs console, read failures, payment failures
