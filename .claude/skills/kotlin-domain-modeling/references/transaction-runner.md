# TransactionRunner (explicit tenant, a refusal rolls back)

Decouples the transaction boundary from JVM machinery so use-cases stay in `commonMain`, makes
the tenant a **parameter of the transaction** rather than ambient state, and decides what a
refusal (`Either.Left`) leaves behind.

## Domain (`domain/common/tx`, commonMain)

```kotlin
sealed interface TenantContext {
    data class Tenant(val tenantId: TenantId) : TenantContext     // tenant-scoped work (default)
    data class Account(val accountId: AccountId) : TenantContext  // identity reads before a tenant is chosen
    data object None : TenantContext                              // global tables only; tenant tables fail closed
}

interface TransactionRunner {
    suspend fun <T> inRoTransaction(tenant: TenantContext, block: suspend RoTransactionScope.() -> T): T

    /** Commits what the block returns, unless it is an `Either.Left` — then everything is rolled back. */
    suspend fun <T> inRwTransaction(tenant: TenantContext, block: suspend RwTransactionScope.() -> T): T

    /**
     * Commits whatever the block returns, a `Left` included. Only for a transaction whose write IS the
     * record of the refusal; every caller is named in KNOWN_COMMITS_ON_LEFT with its reason.
     */
    suspend fun <T> inRwTransactionKeepingWrites(tenant: TenantContext, block: suspend RwTransactionScope.() -> T): T
}

interface RoTransactionScope
interface RwTransactionScope
```

`Tenant` is the neutral name used across this catalogue; a project may name the variant after its
tenant noun (`Workspace`, `Organisation`) — once, everywhere. A truly single-tenant project may drop
the parameter (`inRwTransaction { }`), but decide before the first table: retrofitting a tenant
parameter later touches every use-case.

## Test double (commonTest)

```kotlin
class NoopTransactionRunner : TransactionRunner {
    private object Ro : RoTransactionScope
    private object Rw : RwTransactionScope
    override suspend fun <T> inRoTransaction(tenant: TenantContext, block: suspend RoTransactionScope.() -> T) = block(Ro)
    override suspend fun <T> inRwTransaction(tenant: TenantContext, block: suspend RwTransactionScope.() -> T) = block(Rw)
    override suspend fun <T> inRwTransactionKeepingWrites(tenant: TenantContext, block: suspend RwTransactionScope.() -> T) = block(Rw)
}
```

The fake has **no transactions**: it cannot tell the two RW functions apart, and a domain test
cannot see a rollback. Don't build a second transaction engine into the fakes (snapshots per fake
repository) — pin the behaviour on a real database instead (below).

## Real implementation (server:infra)

Opens `suspendTransaction`, arms the tenant as the transaction's first statement
(`SELECT set_config('app.tenant_id', ?, true)` — transaction-local, pool-safe), puts the
`TenantContext` into the coroutine context so repositories can cross-check their `tenantId`
argument, and for `inRwTransaction` rolls back when the block returned a `Left`:

```kotlin
suspendTransaction {
    armTenant(tenant)
    // A block that does not return Either cannot refuse; the check never fires for it.
    block(RwScope).also { if (it is Either<*, *> && it.isLeft()) rollback() }
}
```

The `is Either` check is a runtime check on purpose: overloading on the lambda's result type is
fragile under Kotlin type inference. Template →
[`exposed-postgres/templates/server-infra/ExposedTransactionRunner.kt`](../../exposed-postgres/templates/server-infra/ExposedTransactionRunner.kt);
tenant arming → [`postgres-multitenancy-rls`](../../postgres-multitenancy-rls/SKILL.md).

## A refusal rolls back — and the few writes that must outlive one

**The mine.** A runner that commits whatever the block returns makes `raise(...)` inside
`either { }` a plain short-circuit: everything written before it is committed. A use-case with two
compare-and-set writes cannot put both checks first — the second CAS is lost only *at* the write —
so the database ends up holding half an operation, while every domain test is green (the fake has
no transactions). "Order checks before writes" is good practice; it must not be the only defence.

**The rule.**

| Function                        | On `Left`        | Use for                                                                   |
|---------------------------------|------------------|---------------------------------------------------------------------------|
| `inRwTransaction`               | rolls back all, outbox rows included | every write (the default)                              |
| `inRwTransactionKeepingWrites`  | commits          | a write that *is* the record of the refusal (narrowly: that transaction, not the whole use-case) |
| `inRoTransaction`               | n/a (no writes)  | reads                                                                     |

Typical keeping-writes cases: a failed-attempt counter and its lockout (or the secret is guessed
by brute force); revoking a whole session chain when a rotated token is replayed (theft signal);
a spent turn of a daily allowance; the audit row of an external (LLM) call that did happen before
a `409`; a lapsed proposal/claim marked `Expired`/`Stopped` before the refusal so a poller does not
pick it again every pass.

The paired example, one feature each way: a **spent turn of a per-user allowance** commits on refusal
(`inRwTransactionKeepingWrites` — the turn was used even if the call is then refused), while the
**money reservation** of the same call rolls back with it (`inRwTransaction`)
([`ai-spend-and-entitlement` → allowance-and-order](../../ai-spend-and-entitlement/references/allowance-and-order.md)).

Work that touches storage outside the database (files, object storage) never sits inside the
transaction: **files outside, one transaction inside** — erase the files first, then the rows in one
transaction, then the files again after commit
([`tenant-data-lifecycle` → erasure-order](../../tenant-data-lifecycle/references/erasure-order.md)).

**Consequences to state in the use-case KDoc:**

- Events of a refused `Left` vanish with the rollback — `Outcome` exists only on `Right`.
- An exception still rolls back (both functions).
- A *batch* transaction (one tenant's pass over N rows) now loses the whole pass on one refusing
  row. Either one row = one transaction, or a savepoint per row (and a retry on `40P01`) —
  [`outbox-and-background-jobs`](../../outbox-and-background-jobs/references/queue-pollers.md).

## Locks (fitness + real-DB tests)

1. **Ratchet `KNOWN_COMMITS_ON_LEFT`** — a source-scanning test in `:domain:jvmTest` collects every
   file calling `.inRwTransactionKeepingWrites(` and compares the set with a `Map<file, reason>` by
   **exact equality** (a new caller is red until named with a reason; a vanished one is red too).
   Template → [`templates/domain/CommitsOnLeftTest.kt`](../templates/domain/CommitsOnLeftTest.kt).
2. **Two tests on a real database** (`server:infra`, Testcontainers): *rolls back on Left* — a
   use-case whose last write (the outbox) is refused leaves the aggregate and the ledger untouched;
   *commits on Left* — each keeping-writes case leaves its row although the caller is refused.
   Template → [`exposed-postgres/templates/server-infra-test/RefusalSemanticsTest.kt`](../../exposed-postgres/templates/server-infra-test/RefusalSemanticsTest.kt).
3. **Inventory before a flip.** Migrating an existing codebase from "Left commits" to "Left rolls
   back": list every `inRwTransaction` call and classify it — **clean** (no write before a possible
   refusal), **atomic** (writes, a later refusal must undo them), **keeps** (the write must survive)
   — write both real-DB tests *before* flipping (the rollback test is red by meaning), then flip
   the runner in one line. Keep the old name for the default so call sites don't change; delete any
   "atomic" variant that the flip made redundant.

Evidence: family-planner ADR 0087 (amends 0015), step 76 (`tx-sites.md` inventory: ~120 calls,
7 keeping files; the seventh found only by a hostile re-check); tests `CommitsOnLeftTest` (ratchet
and real-DB), `RollsBackOnLeftTest`.

## Rules

- Interface in `domain/common/tx`; implementation in `server:infra`.
- Never call `suspendTransaction` in a use-case; never open a transaction in a repository.
- Never write inside `inRoTransaction`.
- One transaction per command. Background jobs open one transaction **per tenant** (or per row)
  they process.
- `inRwTransactionKeepingWrites` is a decision with a reason in the ratchet, never a habit; more
  than ~10 such places → consider a dedicated attempts journal instead.
