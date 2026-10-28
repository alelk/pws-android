# Cross-context reactions: reverse ports and the effect funnel

Contexts are ranked (`common → identity → planning → motivation → … → delivery`) and an import
may only point to the same or an earlier context. Yet an upstream operation often needs a
**synchronous** reaction from a downstream context, inside the same transaction: a resource
transition must award a balance, creating a member must open a wallet, confirming an address
must create a delivery target. Calling the downstream use-case directly is the defect — it is an
import against the order, and every such call grows a cycle (`resource ↔ balance`,
`member ↔ balance`) that the context test then has to list as an exception.

## The reverse port

The **upstream context declares the port**; the downstream context **implements** it; only
`server:di` (or the domain test wiring) knows both.

```kotlin
// planning (upstream) — declares what it needs to happen, knows nothing of who does it
fun interface TransitionEffect {
    suspend fun apply(transition: Transition, resource: Resource, now: Instant, actor: Actor):
        Either<DomainError, List<DomainEvent>>
}

/** Applies effects in the explicit order supplied by DI — the order IS the lock order. */
class TransitionEffects(private val effects: List<TransitionEffect>) {
    suspend fun apply(transition: Transition, resource: Resource, now: Instant, actor: Actor) =
        either { effects.flatMap { it.apply(transition, resource, now, actor).bind() } }
}

// motivation (downstream) — implements it, imports planning legally
class BalanceTransitionEffect(/* ports of motivation */) : TransitionEffect {
    override suspend fun apply(transition: Transition, resource: Resource, now: Instant, actor: Actor) =
        when (transition) {          // no `else`: a new transition is a compile error here
            Transition.Accepted -> award(resource, now, actor)
            Transition.Reopened -> reverse(resource, now, actor)
            Transition.Started, Transition.Cancelled -> Either.Right(emptyList())
        }
}
```

Typical reverse ports: `TransitionEffect` (resource lifecycle → balances, timers, queues),
`MemberProvisioning` (identity → open a wallet), `TenantProvisioning` (identity → seed
catalogues), `ProofOfAddress` (identity → create a delivery target). For reads, the same shape:
an upstream read port (`AttemptSummaries`) implemented downstream.

## Rules

- **Synchronous and in the caller's transaction.** The effect runs inside the use-case's
  `inRwTransaction`; a `Left` from any effect rolls the whole command back
  ([kotlin-domain-modeling / transaction-runner.md](../../kotlin-domain-modeling/references/transaction-runner.md)).
  Asynchronous reactions go through the outbox instead
  ([`outbox-and-background-jobs`](../../outbox-and-background-jobs/SKILL.md)).
- **Every handler is a `when` without `else`** over the upstream's closed set, so a new
  transition forces a decision in every downstream context.
- **The funnel is an ordered list, and the order is the lock order.** Effects that lock rows
  (a rule application, a promise instance, a wallet) run in the documented lock order; the
  wallet-locking effect goes last. A reversed order is a deadlock that only concurrency finds.
  Pin the order in the **real DI wiring** with a test (reflect the funnel's list, assert the
  indices are sorted and the last one is the wallet effect) — the domain tests use their own
  wiring and cannot see it.
- **Policies carried by many contexts live in the shared kernel**, decisions in the owner: the
  types and pure resolution functions (`AcceptancePolicy`, `resolve()`) are in `common/policy`;
  the decision (close, award) is made by the owning context through the port.
- **Event payloads use kernel types only**, so events never drag a downstream type upstream.

## Tests

- `ContextDependencyTest` (context order + no new cycles) stays green without exceptions for
  these reactions — that is the point of the pattern. Template →
  [`architecture-fitness-tests/templates/domain/ContextDependencyTest.kt`](../../architecture-fitness-tests/templates/domain/ContextDependencyTest.kt).
- An order test for each funnel in the production DI module (`TransitionEffectsOrderTest`).
- A concurrent real-DB test for each chain that has deadlocked once.

Evidence: family-planner ADR 0049 (policies in the kernel, reverse ports), step 67.10 (two
concurrent reopens deadlocked while the points effect ran first; fixed by reordering, pinned by
`TransitionEffectsOrderTest` and `ChallengeJobsTest`).
