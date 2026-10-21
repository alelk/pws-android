---
name: kotlin-testing-strategy
description: Use when deciding what to test and how in a Kotlin (KMP) + Ktor + PostgreSQL project — the pyramid (domain use-cases on fakes in commonTest, repositories on Testcontainers with real RLS, routes via testApplication, a few end-to-end flows), fakes that honour port contracts, object mothers and TestClock, Kotest FunSpec on the JVM, and the special test forms that catch defects unit tests cannot — N concurrent requests through the route, a counter that triggers through HTTP, refusal before reading an upload, SQL predicate == domain predicate parity, partial-failure retries, real-byte fixtures, adversarial AI inputs, a wall walk sending one principal's token to every route of another over the live routing tree, fault injection by a database trigger (fail-open side effects), "a refusal after a write leaves no row", an erasure census over pg_catalog — plus guard / lock / pair (a stranger refused, one's own not), proving each test red on a broken implementation, and "bug → failing test first".
metadata:
  version: "1.1.0"
  requires: "kotlin-domain-modeling, exposed-postgres, ktor-backend"
  project-types: "any-kotlin-project-with-business-logic"
---

# Kotlin Testing Strategy

## The pyramid

```
        e2e through the whole stack (few)     a tenant's real scenario, Testcontainers
       routes via testApplication (some)      status codes, error codes, auth, tenant from token, concurrency
      repositories on Testcontainers (some)   mapping, JSONB, CAS, RLS, query windows, indexes
     domain use-cases on fakes (many, fast)   rules, state machines, ledgers, idempotency, events
```

- **Domain** — `commonTest`, fakes, `TestClock`, no I/O; the bulk of the tests.
- **Repositories** — real PostgreSQL (same major as prod), migrations applied, app as the
  non-superuser role; never H2.
- **Routes** — the real plugin stack over fakes (or over Testcontainers for e2e).
- **UI** — ViewModels and pure screen logic on fakes ([compose-multiplatform-ui / testing.md](../compose-multiplatform-ui/references/testing.md)).
- **Conventions** — fitness tests ([`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md)).

Everything runs in `./gradlew build`. Docker is a CI requirement, not an option.

## Hard rules

- ✅ **Bug → failing test first**, then the fix; the test names the defect.
- ✅ **A test for a guard is proven on a broken implementation**: remove the lock / move the read
  before the check / drop the comparison, see red, restore. Write the "how to see it fail" recipe in
  the test's KDoc.
- ✅ **Two tests per protection: "a stranger is refused" and "one's own is not"** (lock + pair,
  usually after a guard that pins today's behaviour). A protection tested only from the refusing
  side ships a door that locks out its own users with every test green.
- ✅ Fakes honour the port contract (tenant scoping, CAS on `expectedVersion`, duplicate id →
  `AlreadyExists`) — a lax fake makes every higher test meaningless.
- ✅ Two tenants in every isolation-relevant test; one-tenant tests pass under any bug.
- ✅ Time is pinned (`TestClock`, bound parameters in queries), never `Thread.sleep`/wall clock.
- ❌ Never weaken, skip or delete a test to get green; never loosen an assertion without a documented
  reason.
- ❌ No numeric coverage target — cover key paths and edges; percentages are gamed.
- ❌ No MockK in `commonTest` (JVM-only); mocks at most for third-party SDKs in `jvmTest`.
- ❌ No real provider calls (LLM, STT, SMTP, push) in CI — fakes with deterministic answers; real
  calls in a separate, manual contract suite.

## Special forms (the defects unit tests can't see)

| Situation                                             | Mandatory test                                                            |
|-------------------------------------------------------|---------------------------------------------------------------------------|
| read aggregate → decide → write (balance, quota, slot) | N **concurrent** requests through the route → exactly one wins           |
| count → decide by the number → apply (lockout ladder) | N+1 attempts **through HTTP**; the (N+1)th answers differently; two parallel failures count 2 |
| upload / large body                                   | refusal comes **before** reading: bytes read stays ~0 when the quota is spent or parts are duplicated |
| the same rule in domain and SQL (a board window)       | seed a matrix, compare ids from SQL with ids the pure predicate accepts   |
| batch of independent requests (wizard, import)        | fail request k of n → retry sends only the remaining; a lost response replays as "already exists" |
| byte-exact storage                                    | non-uniform fixture bytes (never `ByteArray(n) { 0 }`) at limit and limit+1 |
| AI verdicts                                            | prompt-injection inputs stay in bounds or escalate; unparsable output → `Failed`, never "accepted" |
| state machines                                        | every transition × every source status, from the table                    |
| a second principal kind (device, service token)       | **wall walk**: its token to every route of the other kind over the live routing tree → all `401`, and back |
| a side effect that must never block the action (audit, metrics) | **fault injection by a DB trigger**: the real adapter fails; same answers, action stood, `ERROR` logged — plus the pair with it working |
| a use-case that writes, then may refuse                | **refusal after a write** on the real DB: nothing it wrote stays          |
| deleting a tenant / a person's data                   | **census over `pg_catalog`**: a fixture fills every tenant table; afterwards no id is in any uuid/text/json column; the neighbour is unchanged |

Details and code → [`references/special-forms.md`](references/special-forms.md); the last four
→ [`references/security-boundaries.md`](references/security-boundaries.md).

## References (load on demand)

- [`references/layers.md`](references/layers.md) — what each layer verifies, harnesses, naming, Kotest setup.
- [`references/special-forms.md`](references/special-forms.md) — the table above with code.
- [`references/security-boundaries.md`](references/security-boundaries.md) — wall walk, DB-trigger fault injection, refusal-after-write, erasure census, guard / lock / pair.
- [`references/red-on-broken.md`](references/red-on-broken.md) — proving a test can fail; the "mine" of a step and its numbered locks.

## Templates

- [`templates/ConcurrentRouteTest.kt`](templates/ConcurrentRouteTest.kt)
- [`templates/PredicateParityTest.kt`](templates/PredicateParityTest.kt)
- [`templates/FaultInjectionTest.kt`](templates/FaultInjectionTest.kt) — a table that refuses inserts for one tenant + captured log lines
- Wall walk: `PrincipalWallTest` in [`ktor-auth-sessions`](../ktor-auth-sessions/SKILL.md) (`templates/test/`)

## Related

- [`architecture-fitness-tests`](../architecture-fitness-tests/SKILL.md) — source/catalog/route-tree locks
  (capability ownership, closed sets, the route surface) that complement these behaviour tests.
- [`tenant-data-lifecycle`](../tenant-data-lifecycle/SKILL.md) — erasure and export; the census test is its lock.
- [`audit-journal-at-the-door`](../audit-journal-at-the-door/SKILL.md) — the fail-open journal the fault-injection form was built for.
- [`the-mine-and-locks`](../the-mine-and-locks/SKILL.md) — naming a step's defect and numbering its locks.

## Apply this skill

Step-by-step → [`USAGE.md`](USAGE.md).

## Verify

Walk through [`checklists/testing-checklist.md`](checklists/testing-checklist.md).
