# Entitlement: offline-first truth, monotonic reconciliation

## Trust model

```
                 grant / extend only                      read only
store SDK ──► PurchaseSyncService ──► LocalEntitlementStore ──► LocalEntitlementRepository ──► gates
(list)          (filters isPaid)        (persisted, monotonic)     (Clock, no network)
```

- The **local persisted state is the source of truth** for "is premium active". It is read without
  network, without the store app and without the SDK — in the evidence it is read even in builds that
  never call the SDK (`PremiumComingSoon`). Invariant I7: the unlock works offline.
- The **store is a reconciliation input**, never a per-check oracle. A live call per gate would turn
  every outage, signed-out store app or slow SDK into "you have no premium".
- The **only writer** is the sync, through a store API that has no revoke method.

## Monotonic writes — the central invariant

A purchase list can only **grant or extend**. Rules (evidence: `PurchaseSyncService.kt`, plan
§4.3 / T-D01):

| Input | Write |
|---|---|
| empty list (signed out, nothing bought, SDK returned nothing) | **nothing** |
| `purchases()` threw | `sync` not called — nothing |
| list without the lifetime product (other account, only a subscription) | lifetime untouched — **never `false`** |
| paid lifetime product present | `lifetime = true` (no write if already true) |
| paid subscriptions with ends | `until = max(stored, max(ends))`, write only if it increases |
| subscription without an end | ignored |
| only unpaid purchases (invoice created, on hold, cancelled, refunded) | **nothing** |

**The incident.** The RuStore fork's `syncDataStoreWithPurchases()` set `purchase_full_access` to
whether the product was in the list. The first port into the new app copied that (`setFullAccessPaid(hasFullAccess)`,
unconditional) and did not filter by status. Path to the loss: Settings → "Purchases" →
`PaymentActivity.onResume` → `refreshData()` → `sync()` with an empty list or another account's
list → lifetime erased. Plan finding **C3, critical**; fixed as invariant **I9**, locked by
`PurchaseSyncServiceTest`.

Why "absent from the list" is never evidence of "not owned": the list depends on which store account
is signed in, whether the store app is installed and reachable, SDK paging and caching, and transient
backend errors. None of those are the user's purchase history.

Make the invariant structural, not just tested:

- the local store exposes `grantLifetime()` and `extendSubscriptionUntil(t)` only — no `set(false)`,
  no `clear()`; writes are `internal`;
- `extendSubscriptionUntil` compares inside the `edit {}` transaction (read-compare-write atomic per
  DataStore);
- the test counts writes (`FakePreferencesDataStore.writes`), not only values.

## Refunds and revocation

Consequence of monotonic writes: a refunded or charged-back purchase keeps its unlock. The evidence
records this as a **deliberate** owner decision (plan §1 out-of-scope, §4.3): "a false revocation of
Pro costs more than a rare undeserved access". If revocation is ever needed, it is a separate plan
driven by an **explicit** refunded/revoked status from the store (or a server notification), never by
absence from a list — and it stays out of the generic sync path.

## When to reconcile

Evidence: the sync runs when the paywall opens (`PaymentActivity.onResume → refreshData()`) and after
a successful purchase. Nothing runs in the background, and with sales off nothing runs at all.

Consequence to weigh for subscriptions: an auto-renewed subscription's new end reaches the device
only at the next reconciliation. With sync only on the paywall, a renewing subscriber sees `Expired`
offline until they open it. Options (pick one, write it down):

- also reconcile at app start (when `purchasesEnabled`), fire-and-forget, guarded, with a non-fatal on
  failure — cheap and usually enough;
- reconcile when `currentInfo()` crosses the stored end (the gate turns a blocked tap into a
  reconcile-then-recheck before the upsell);
- a `WorkManager` periodic job with network constraint — only if renewal latency matters more than
  battery and complexity.

Whichever trigger, the write path is the same monotonic sync.

## Reading: resolution and time

`LocalEntitlementRepository` ([template](../templates/store/LocalEntitlementRepository.kt)):

- priority: `lifetime == true` → `Lifetime`; end in the future → `Subscription(end)`; end in the
  past → `Expired(end)`; otherwise or `ReadFailed` → `None`;
- `info` starts at `Unknown` and is shared `Eagerly`, so the first definite value is ready as soon as
  the file is read;
- `currentInfo()` re-resolves the last snapshot against `clock.now()`: a gate asking one second after
  expiry gets `Expired` even if no emission happened yet (evidence plan finding H2: status computed
  only on emission let an expired subscription stay active while the process lived);
- the flow re-emits at the expiry moment (evidence: at each local midnight, because the stored value is
  a date);
- `Clock` and time zone are injected; tests run on virtual time (`runTest`, `advanceTimeBy`).

Day-granularity storage (the evidence's legacy format, `yyyy-MM-dd` in the device zone): the real
expiry moment lies somewhere inside that day, so the **whole day counts** (`today <= until`) — the fork
cut users off at the start of the day, losing up to a day of paid time.

## `Unknown` at the gate

`DefaultPremiumGate` waits up to a bound (3 s in the evidence) for a non-`Unknown` value. On timeout it
drops the action **without** an upsell and records `entitlement_unknown_timeout` — showing "buy Pro"
to someone who may have paid is worse than an ignored tap. `None` and `Expired` do upsell.

## Read errors

A corrupted or unreadable file becomes `ReadFailed` → `None` + `entitlement_read_failed` non-fatal;
non-`IOException` errors propagate (they are bugs). The file is left untouched, so the next start
reads it again. Never install a corruption handler that replaces it (see
[`legacy-entitlement-compat.md`](legacy-entitlement-compat.md)). Evidence plan finding H1: a `stateIn`
without `catch` either crashed the scope or left the status `Unknown` forever, hanging every gate.

## Telemetry that proves it in production

- `entitlement_resolved {kind: lifetime|subscription|expired|none|read_failed, source}` once per
  process — after a release, the shares of `lifetime`/`subscription` must be non-zero and comparable
  to the store console's purchase count (evidence T-B06, T-H02 72-hour monitoring);
- `entitlement_read_failed`, `entitlement_bad_subscription_date`, `entitlement_unknown_timeout`;
- no dates, ids or tokens in attributes; product ids only where they are build constants.
