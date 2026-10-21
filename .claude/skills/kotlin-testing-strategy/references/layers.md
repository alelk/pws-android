# What each layer verifies

## Domain (commonTest, fakes)

- Business rules, state machines (iterate the whole transition table), policy resolution, ledgers
  (idempotency per source, reversal), recurrence expansion, time-zone day boundaries.
- Harness: `NoopTransactionRunner`, `Fake*Repository` (contract-faithful), `FakeEventOutbox`,
  `TestClock`, object mothers `aTask(...)`.
- Assert on **returned events** and on the fake store — not on internal calls.
- Kotest `FunSpec`; names read as behaviour: `test("a dependant may only create items for themselves")`.

Limitation to remember: fakes have **no transactions**. Anything whose correctness depends on
locking, isolation or "a `Left` rolls back what was written before it" (and its closed exception,
`inRwTransactionKeepingWrites`) needs a Testcontainers test — see
[`kotlin-domain-modeling` → transaction-runner](../../kotlin-domain-modeling/references/transaction-runner.md) and
[`exposed-postgres` → `RefusalSemanticsTest`](../../exposed-postgres/templates/server-infra-test/RefusalSemanticsTest.kt).

## Repositories (Testcontainers)

- One container per JVM; migrations as the owner; app as the runtime role.
- Round-trips (JSONB included), CAS, duplicate ids, keyset pagination (stable order, `limit+1`),
  RLS isolation (see `postgres-multitenancy-rls`), SQL windows vs domain predicates.
- Schema invariants against `pg_catalog` (see `architecture-fitness-tests`).

## Routes (testApplication)

- The real plugin stack + routes over fakes: wire behaviour (status, `code`, `details`, headers),
  auth, rate-limit keys, tenant from token.
- The pinned API surface test.
- Concurrency and counters **through HTTP** (special forms).

## End-to-end (few)

The product's "done" scenario across all layers with Testcontainers and faked external providers
(e.g. create tasks → submit a report with a fake voice transcript → fake AI review → daily digest).

## UI

ViewModels (event → state → effect), pure screen models, navigation codecs; fitness tests for
conventions; no screenshot diffs until those exist (and there is no stable wasm UI runner).

## Naming & layout

- `XxxTest` next to the code's package; shared `fakes/`, `fixtures/` per module (`commonTest`).
- Test support is excluded from the strictest Detekt rules (object mothers have many parameters).
