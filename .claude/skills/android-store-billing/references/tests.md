# Tests and locks

The mine of store billing is **a paying user losing what they paid for** — through a sync branch, a
"repaired" file, a live-call check during an outage, or the SDK leaking into a build that must not
call it. None of these show up in a happy-path manual test with network and a signed-in store app.
Each lock below is proved red once on a deliberately broken implementation
([`kotlin-testing-strategy`](../../kotlin-testing-strategy/SKILL.md)).

| Lock | Level / source set | Catches | Red run |
|---|---|---|---|
| empty list keeps lifetime, `writes == 0` | unit, `test<Store>` (`PurchaseSyncServiceTest`) | the C3 defect: revoking on an empty list | restore the fork's "write whether present" |
| list without lifetime never writes `false` | same | revoking on another account's list | same |
| only unpaid purchases → no write | same | a grant on `PAID`/`PENDING`/cancelled | drop the `isPaid` filter |
| earlier end never overwrites; later end extends | same | a renewal list shortening the subscription | replace `max` with "last seen" |
| subscription success never sets lifetime | same | the fork's `makePurchase()` bug | grant lifetime for any product |
| SDK package only under `src/<store>/`, `src/test<Store>/` | shared `src/test` (`PaymentSdkIsolationTest`), every flavor | SDK in `main` / other flavors | add `"ru.rustore.sdk.Foo"` literal in `main` |
| SDK only in the adapter files (exact set) | `test<Store>` (`StoreFlavorWiringTest`) | SDK types leaking past the port | import an SDK type in the controller |
| default build is `PremiumComingSoon` | `test<Store>` | sales on by accident | flip the Gradle default |
| no payment beans when sales are off; two modules when on | `test<Store>`, Koin `koinApplication` | SDK reachable in a non-selling build | register the provider unconditionally |
| golden files resolve correctly; SHA-256 unchanged | `test<Store>`, real file DataStore | key/encoding mismatch; a read that rewrites | rename a key; add a corruption handler |
| corrupted file → `Inactive` + one non-fatal, no crash | same | H1: crash or `Unknown` forever | remove the `catch` |
| expiry day counts; next day `Expired`; flips at midnight without new emission; `currentInfo()` fresh | `runTest` virtual time | H2: stale status while the process lives | resolve only on file emission |
| gate: `Unknown` timeout drops without upsell; `Expired`/`None` upsell | shared module test (`DefaultPremiumGateTest` in evidence) | "buy Pro" shown to a payer during first read | treat `Unknown` as inactive |
| store name literal lives in exactly one file | shared `src/test` (`StorageNamesPinnedTest`) | a rename/second DataStore dropping the flag | copy the literal elsewhere |
| flavors declare the same public names | shared `src/test` (`FlavorContractTest`) | drift found only when that flavor builds | rename in one flavor |
| no wide `-keep` in any proguard file | shared `src/test` (`ProguardRulesTest`) | a blanket SDK keep disabling shrinking | add `-keep class ru.rustore.sdk.** { *; }` |
| Maestro: free → coming-soon sheet; Pro → actions work; Settings status | e2e on device (`e2e/flows/compose/rustore/`) | gates missing at secondary entry points | remove a gate |

Template addition, not in the evidence: `StoreFlavorWiringTest` "the entitlement store has no
revoking API" (reflection over method names) — makes the monotonic store structural.

## Fakes

- `FakePreferencesDataStore`: in-memory `DataStore<Preferences>`, synchronous, dispatcher-free (works
  under virtual time), **counts real changes** (`writes`). Counting writes is what turns "value still
  true" into "nothing was touched".
- A fake `PaymentProvider` for controller tests: scripted results per call, plus a "throws" mode for
  each method — the controller's soft-degradation contract is "every method may throw".
- A fixed `Clock` (`object : Clock { override fun now() = at }`) or one driven by
  `testScheduler.currentTime` for midnight/expiry tests.

## Commands

The store flavor's tests run only in its variant: `./gradlew :app:test<Store>DebugUnitTest`. The shared
`src/test` guards run in every variant; CI must build at least one non-store flavor and the store
flavor (evidence DoD: `testRustoreDebugUnitTest`, `assembleRustoreRelease`, `assembleRuDebug`).
