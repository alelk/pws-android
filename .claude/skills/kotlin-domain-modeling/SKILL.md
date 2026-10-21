---
name: kotlin-domain-modeling
description: Use when designing or modifying entities, use-cases, repository ports, commands, events or error types in the domain layer of a Kotlin (KMP) project. Covers verb-named single-method use-cases with the command in the same file, Arrow Either + Raise DSL (either/ensure/bind), DomainError design, @JvmInline value-class ids with client-generated UUIDv7 and idempotent create, TransactionRunner with an explicit TenantContext where a Left rolls the transaction back (inRwTransactionKeepingWrites + a KNOWN_COMMITS_ON_LEFT ratchet for the few writes that must outlive a refusal), optimistic locking via expectedVersion, Outcome + transactional EventOutbox events stamped by an injected Clock, status machines as pure tables, long operations stored as a state, rules judged by one reader as of a date (derive, don't store), OptionalField PATCH semantics, keyset pagination, and fakes + object-mother fixtures for commonTest. KMP-safe (no java.* in commonMain). Apply after kotlin-clean-architecture.
metadata:
  version: "4.0.0"
  requires: "kotlin-project-layout, kotlin-clean-architecture"
  project-types: "any-kotlin-project-with-business-logic"
---

# Kotlin Domain Modeling

## When to use

- Designing a new context part: entity, port, use-cases, commands, events.
- Adding a PATCH operation (`null` = unchanged or clear?).
- Adding a state transition, an idempotent create, or an optimistic-lock update.
- Auditing `commonMain` for JVM-only imports or wall-clock reads.
- Deciding what a refused command leaves in the database.

## Canonical use-case shape

```kotlin
data class StartTaskCommand(                       // command lives in the use-case's file
    val tenantId: TenantId,
    val taskId: TaskId,
    val actorId: MemberId,
    val expectedVersion: Long,                     // optimistic lock travels in the command
)

class StartTask(                                   // verb + noun; no "UseCase"/"Service" suffix
    private val tasks: TaskRepository,
    private val outbox: EventOutbox,
    private val txRunner: TransactionRunner,
    private val clock: Clock,                      // kotlin.time.Clock, injected
) {
    suspend operator fun invoke(command: StartTaskCommand): Either<DomainError, Outcome<Task>> =
        txRunner.inRwTransaction(TenantContext.Tenant(command.tenantId)) {
            either {
                val task = tasks.findById(command.tenantId, command.taskId).bind()
                ensure(task.assigneeId == command.actorId) { DomainError.Forbidden("only the assignee may start") }
                val now = clock.now()                                  // read once per command
                val saved = tasks.update(task.moveTo(TaskTransition.Started, now).bind(), command.expectedVersion).bind()
                val events = listOf(TaskStarted(saved.tenantId, saved.id, now, Actor.Member(command.actorId)))
                outbox.append(events).bind()                           // same transaction; a Left here rolls back the update too
                Outcome(saved, events)
            }
        }
}
```

## Patterns (read only the reference you need)

| Pattern                           | Reference                                                                      |
|-----------------------------------|--------------------------------------------------------------------------------|
| Use-cases & commands              | [`references/use-cases-and-commands.md`](references/use-cases-and-commands.md) |
| Either + Raise DSL                | [`references/either-error-handling.md`](references/either-error-handling.md)   |
| TransactionRunner, TenantContext, refusal semantics | [`references/transaction-runner.md`](references/transaction-runner.md) |
| Value classes, ids, KMP types     | [`references/value-types.md`](references/value-types.md)                       |
| Idempotent create (client id)     | [`references/idempotent-create.md`](references/idempotent-create.md)           |
| Domain events + outbox            | [`references/domain-events.md`](references/domain-events.md)                   |
| Sealed hierarchies, state machines, process-as-state, one reader | [`references/sealed-hierarchies.md`](references/sealed-hierarchies.md) |
| `OptionalField<T>` (PATCH)        | [`references/optional-field.md`](references/optional-field.md)                 |
| Repositories, Flow, pagination    | [`references/flow-and-pagination.md`](references/flow-and-pagination.md)       |
| Test fakes & fixtures             | [`references/test-fixtures.md`](references/test-fixtures.md)                   |
| Typed results (no-Arrow alternative) | [`references/typed-results.md`](references/typed-results.md)                |

## Hard rules

- **Either everywhere, one error type.** Use-cases and ports return `Either<DomainError, T>`.
  Don't mix with per-operation `*Result` sealed types in the same project (pick once, in an ADR).
- **Raise DSL over `flatMap` chains**: `either { … .bind(); ensure(…) { … } }`.
- All entity ids are `@JvmInline value class`; never raw `String`/`Uuid`/`Long` in signatures.
- **The id of a created entity comes in the command** (client-generated UUIDv7); a repeated
  create with the same id returns the same answer (idempotent), a different body → `Conflict`.
- Every use-case: one `suspend operator fun invoke(command)`, injected `TransactionRunner`,
  explicit `TenantContext`, writes in `inRwTransaction`, reads in `inRoTransaction`.
- **A `Left` rolls the transaction back** (outbox rows included). Defect prevented: half an
  operation committed behind green domain tests (the fake runner has no transactions). A write
  that must outlive a refusal (failed-attempt counter, revoked session chain, audit row of a call
  that happened) uses `inRwTransactionKeepingWrites`, and the file is named with its reason in the
  `KNOWN_COMMITS_ON_LEFT` ratchet ([`templates/domain/CommitsOnLeftTest.kt`](templates/domain/CommitsOnLeftTest.kt));
  both directions are pinned on a real DB. Checks before writes is still good practice.
- **Time is injected.** No `Clock.System.now()` in `commonMain` domain code, no defaulted
  `occurredAt = Clock.System.now()` on events; nor on the database side (no `timestamptz DEFAULT`,
  [`exposed-postgres`](../exposed-postgres/SKILL.md)).
- Events are appended to the outbox **inside** the same transaction as the state change, and
  returned in `Outcome` so callers/tests can see them; a refused `Left` takes them with it.
- **What cannot fit in one transaction is a state** of the aggregate (a sealed state + a
  resumable, idempotent runner), never a flag set at the end or an in-memory job. Defect: an
  operation half-done after a crash with nothing to resume it.
- **One reader per rule; derive, don't store.** A rule that changes over time carries `since`
  and a day is judged by the terms of that day, by exactly one function; an absence (a miss, an
  untouched occurrence) is an outcome of that function, not a row. Defect: a second reader that
  disagrees with the first, or an edit that rewrites the past.
- No `throw` for business errors; `require()` only for programmer errors in value-class `init`.
- `kotlin.Result` is never a domain return type.
- Repositories never open transactions; every tenant-scoped read takes the tenant id explicitly.
- `commonMain` has zero `java.*` imports (substitution table in `value-types.md`).

## Templates

| File | What |
|------|------|
| [`templates/domain/CommitsOnLeftTest.kt`](templates/domain/CommitsOnLeftTest.kt) | `KNOWN_COMMITS_ON_LEFT` ratchet (source scan, exact equality, vacuous-pass guard) |

Real-DB refusal tests → [`exposed-postgres/templates/server-infra-test/RefusalSemanticsTest.kt`](../exposed-postgres/templates/server-infra-test/RefusalSemanticsTest.kt).

## Related

- Runner for long operations (claim → call → settle on a state column) →
  [`outbox-and-background-jobs`](../outbox-and-background-jobs/references/process-as-state.md).
- Synchronous reactions across contexts (reverse ports, ordered effect funnel) →
  [`kotlin-clean-architecture`](../kotlin-clean-architecture/references/cross-context-reactions.md).
- Ledgers and balances spent concurrently → [`ledger-and-wallet`](../ledger-and-wallet/SKILL.md).
- Deletion as a state, erasure order (files outside, rows in one transaction) →
  [`tenant-data-lifecycle`](../tenant-data-lifecycle/SKILL.md).
- Computed recurrence slots / rules counted as of a date → `virtual-recurrence-slots`,
  `rule-as-of-date-counting` (planned skills).

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/domain-checklist.md`](checklists/domain-checklist.md).
