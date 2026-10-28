# Purchase flow: final statuses, confirmation, success, errors

## The adapter's one job: `isPaid`

Every store reports purchases through several intermediate states. The adapter collapses them into
one boolean, `ActivePurchase.isPaid`, from an **explicit allow-set of final statuses** — never a
deny-set, so a status that appears in an SDK update does not grant by default. Everything above the
port (UI "owned" list, sync) filters on `isPaid` and nothing else.

Why it is load-bearing: the sync's grants are permanent (no revocation from a list), so counting a
reversible state is an unrevocable free unlock.

### RuStore Pay SDK (evidence: `RuStorePaymentProvider.kt`, Pay SDK 11.1.0 / BOM 2026.08.01)

| Kind | Grants | Does not grant |
|---|---|---|
| product (`ProductPurchase.status`) | `CONFIRMED` | `INVOICE_CREATED`, `PROCESSING`, `EXECUTING`, **`PAID`**, `CANCELLED`, `REJECTED`, `EXPIRED`, `REFUNDING`, `REFUNDED`, `REVERSED` |
| subscription (`SubscriptionPurchase.status`) | `ACTIVE` | `PAUSED`, `TERMINATED`, `CLOSED`, … |

**One-step vs two-step.** `purchase(params, preferredPurchaseType)`:

- `ONE_STEP` (the evidence's choice): funds are captured at once; the final state is `CONFIRMED`; there
  is no separate confirmation call that the app could forget.
- `TWO_STEP`: funds are held (`PAID`) until the app confirms (→ `CONFIRMED`) or cancels (→ `REVERSED`).
  Only worth it when delivery can fail after payment (a server must provision something). If used, the
  confirm call belongs in the adapter, right after delivery succeeds; a `PAID` purchase must never
  grant, and a stale `PAID` found in `purchases()` needs a decision (confirm on next start, or cancel).

The plan's rule for the mapping (T-D02): verify status names against the SDK docs for the exact BOM;
"don't guess — if in doubt, leave a TODO and don't enable sales until verified". Re-check on every SDK
upgrade.

### Google Play Billing (general knowledge — verify against the current Play Billing Library docs)

| Concept | Play | Mapping |
|---|---|---|
| final paid state | `Purchase.purchaseState == PURCHASED` | `isPaid = true` (after acknowledgement, below) |
| not yet paid | `PENDING` (cash, delayed payment methods) | `isPaid = false`; show "pending", grant when it turns `PURCHASED` |
| confirmation | non-consumables and subscriptions must be **acknowledged** (`acknowledgePurchase`, or consumed for consumables) within 3 days, or Google refunds and revokes | adapter acknowledges inside `purchase()` / on the next `purchases()` for any `PURCHASED && !isAcknowledged`; grant only after a successful acknowledge |
| owned list | `queryPurchasesAsync(…INAPP / SUBS)` returns active subscriptions and unconsumed in-app items | same monotonic sync |
| cancel | `BillingResponseCode.USER_CANCELED` | `PurchaseResult.Cancelled` |
| already owned | `ITEM_ALREADY_OWNED` | treat as success → reload purchases |
| subscription end | the client `Purchase` carries no expiry time | without a backend, store a provisional end (e.g. last-seen-active + billing period + grace) extended on each reconcile; with a backend, take `expiryTime` from the Play Developer API |

The last row matters for this skill's model: the monotonic `extendSubscriptionUntil` needs an end.
When the store does not give one on the client, the adapter must produce a conservative one — and the
reconcile trigger (see [`entitlement-sync.md`](entitlement-sync.md#when-to-reconcile)) must run often
enough that a renewing subscriber is not cut off.

## After the store says "success"

Evidence (`PaymentController.makePurchase`, `PurchaseSyncService.onPurchaseSucceeded`, plan T-D03):

1. `PurchaseResult.Success` → `onPurchaseSucceeded(productId)`: **lifetime is granted immediately**;
   a subscription is **not** marked lifetime.
2. Then `loadPurchases()` → `sync(list)` — the subscription's end arrives here.
3. A failure of step 1 is a non-fatal (`purchase_grant_failed`), not a crash; step 2 may still grant.

Why grant before reloading: a hiccup in the follow-up `purchases()` must not lose a purchase the user
just paid for. Why not mark subscriptions as lifetime: the fork's `makePurchase()` set
`purchase_full_access = true` for **any** purchase and relied on the next `loadPurchases()` to set it
back to `false`; when that sync failed, a subscriber kept lifetime forever. The new app honours what
the fork already wrote (golden fixture `sub_and_full` — a subscription date plus
`purchase_full_access = true` — resolves to `Lifetime`, "as the fork did"), but never writes that
combination itself.

## Error surface

`PaymentError` is a sealed, SDK-free type the paywall renders:
`AuthorizationRequired`, `NoStoreApp`, `PurchaseCancelled`, `AuthCheckFailed(cause)`,
`ProductsLoadingFailed(cause)`, `PurchasesLoadingFailed(cause)`, `PurchaseFailed(cause)`.

Rules (evidence `PaymentController.kt`):

- every provider call is in `try/catch (Throwable)` → error state + `recordError(…, stage)`; never a
  crash, never an entitlement write;
- signed out → `isAuthorized = false`, purchases **not loaded and not synced**;
- `Cancelled` is reported as an outcome (`purchase {result: cancelled}`), not as an error non-fatal;
- telemetry carries only the outcome — no product id (it is a build constant anyway), no invoice id;
- `refreshData()` cancels the previous load (`loadingJob?.cancel()`) and runs on every `onResume`
  — the user may have signed in to the store app meanwhile;
- `makePurchase` is ignored while loading (no double purchase from a double tap).

`openAuthorization()` launches the store app (`ru.vk.store` for RuStore). The evidence returns
silently when the store app is missing; the template throws so the controller can show `NoStoreApp`
— choose one and test it.

## Payment-return deeplink

Some stores return to the app through a deeplink after an external payment page. Evidence:
`PaymentActivity` has a `VIEW` + `BROWSABLE` filter on the app's scheme (flavor manifest overlay),
`launchMode="singleTop"`; `onCreate` and `onNewIntent` forward the intent to
`controller.proceedIntent(intent)` → `client.getIntentInteractor().proceedIntent(intent)` (guarded by
`runCatching`). With sales off, the activity forwards to `MainActivity` and finishes **without**
resolving the controller — the filter stays because the SDK will need it later.

## Product catalogue

`ProductIds` is one object of constants equal to the console ids (evidence: `full_access_v1`,
`monthly_subscription_v1`, `yearly_subscription_v1` — the same ids the fork used, otherwise existing
purchases would not be recognised). Product type is decided by membership in `SUBSCRIPTIONS`. Prices
come from the store as a localised label (`amountLabel`); never format prices in the app.
