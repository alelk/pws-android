# Test forms for security boundaries

Route tests create a session and check their own route; repository tests check their own table.
A boundary is invisible to both: it is about **another** principal, **another** tenant, a dependency
that **fails**, a write that **should not** have stayed. Each form below exists because a step
shipped (or nearly shipped) that defect with every ordinary test green.

## 1. Guard / lock / pair

Every protection gets up to three tests with one name stem:

- **guard** — pins today's behaviour without the protection (the baseline the lock compares with)
  and proves the test is not vacuous (floors on the routes/tables/rows it walks);
- **lock** — red on the broken thing: the stranger is refused, the forbidden write is gone;
- **pair** — the protection does not lock out its own: the owner, the right role, the live
  dependency still pass, with the same answers.

The pair is not optional. A security step that only tests refusals ships a door that refuses
everybody. Rule: **two tests per protection — "a stranger is refused" and "one's own is not"**.
Worked cases of a protection that locked out its own:
[`security-hardening-baseline` → protection-locks-out-its-own](../../security-hardening-baseline/references/protection-locks-out-its-own.md).
Refusing an upload before its bytes are read is the same shape at the transport:
[`safe-file-uploads` → locks](../../safe-file-uploads/references/locks.md) (lock 1).

## 2. Wall walk over the live routing tree

A second principal kind (a shared device, a service account) gets its own token audience and its
own provider. If someone mints its token with the person issuer, or widens the person provider to
accept it, the device holds a full session — and every route test stays green.

```kotlin
val routes = mountedRoutes(config)                        // routingRoot.getAllRoutes(), same rendering as ApiSurfaceTest
val personRoutes = routes.filter { it.auth == "jwt" }
val deviceRoutes = routes.filter { it.auth == "device" }
personRoutes.size shouldBeGreaterThanOrEqual MIN_PERSON_ROUTES   // floor: the walk did not lose the tree
deviceRoutes.size shouldBe DEVICE_ROUTES_SO_FAR
val breaches =
    personRoutes.chunkedByCredentialBucket().flatMap { walk(config, it, deviceToken) } +
        walk(config, deviceRoutes, ownerTokenOfTheSameTenant)
breaches shouldBe emptyList()                             // each "METHOD /path with <who> -> <status>"
```

- Every answer must be `401`, **before the handler reads anything**: path params are random ids
  (today's date for `{…date…}`), required query params filled, the body is `{}`.
- Use a token of the **same tenant** — a foreign tenant would be refused for the wrong reason.
- A rate bucket would answer `429` before the wall: walk in chunks smaller than the strictest
  bucket, each on a fresh application (a fresh limiter).
- Generalises to any pair of providers, and to "an unverified account reaches no tenant route".

Template: `PrincipalWallTest` in [`ktor-auth-sessions`](../../ktor-auth-sessions/SKILL.md).

## 3. Fault injection by a database trigger

A side effect that must never block the action (an audit row, a usage counter, a notification
outbox for a best-effort channel) is written in **its own** transaction after the answer is formed.
To prove "the dependency fails, the action stands", make the **real** adapter fail without a code
seam: a trigger that raises for one tenant only.

```sql
CREATE FUNCTION refuse_x() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'down (test)'; END $$;
CREATE TRIGGER refuse_x BEFORE INSERT ON audit_journal FOR EACH ROW
  WHEN (NEW.tenant_id = '<tenant>'::uuid) EXECUTE FUNCTION refuse_x();
```

Assert, through HTTP: the same status codes as without the failure (the guard measured them), the
action's effect is in the database, the journal has no row, and the log has one
`ERROR … write failed kind=… correlationId=…` per door (capture logback with a `ListAppender`).
Then the **pair**: the live journal, same answers, one row per door, no `ERROR`.

It also catches the wrong design for free: a wrapper that writes the row inside the action's
transaction rolls the action back with the row — the effect assertion goes red.

Template: [`../templates/FaultInjectionTest.kt`](../templates/FaultInjectionTest.kt). Drop the
trigger and function in `finally`; name them uniquely per run.

## 4. A refusal after a write leaves no row

If the transaction runner commits on a `Left` (or a use-case writes before its last `ensure`), the
database holds half an operation while every domain test on fakes is green — fakes have no
transactions. On the real database, make the **last** write of the use-case refuse (an outbox fake
that answers `Left`) and assert that nothing the use-case wrote earlier stayed:

```kotlin
start(StartItemCommand(tenant.id, item.id, member.id, item.version)).leftOrNull().shouldBeInstanceOf<DomainError.Conflict>()
adminLong("SELECT count(*) FROM item WHERE id = ?::uuid AND status = 'Pending' AND version = 1", item.id.value) shouldBe 1L
```

Pair it with the source lock "only the named files keep their writes on a refusal"
(`architecture-fitness-tests`, capability ownership) and, for each named file, a test that the kept
write (a failed-attempt counter, a revoked chain) **does** survive.

## 5. Erasure census over the catalog

"After deleting a tenant nothing of it remains" cannot be tested table by table — the table added
next month is the one forgotten. Count against the catalog instead:

- a fixture that fills **every** tenant table (guard: the tables with a `tenant_id` the fixture
  leaves empty == `KNOWN_UNSEEDED`, empty; floors on tables, uuid columns and matches);
- the ids to hunt: the tenant's, its members', its devices', its dependants' accounts;
- run the real erasure, then as **superuser** (RLS would hide leftovers once the tenant row is gone)
  scan every `uuid` column for those ids and every `text`/`json` column by `LIKE`;
- assert the leftovers map (`table.column -> rows`) is empty;
- **neighbour**: a second identical tenant's census and row counts are unchanged.

Details of the erasure itself: [`tenant-data-lifecycle`](../../tenant-data-lifecycle/SKILL.md).

## 6. Hand-run scenarios on a real server

Tests prove the absence of the defect in the process; a step that changes a boundary also runs its
scenario against a locally started server with `curl` (the refusals, the pair, the log line) and
records the transcript in the step's close note. It catches wiring the test harness replaces
(config, proxies, cookies, real providers off).

Evidence: family-planner `KioskWallTest` (step 73, ADR 0082), `AuditNeverBlocksTest` (step 81 lock 5,
ADR 0096), `RollsBackOnLeftTest` + `CommitsOnLeftTest` (step 76, ADR 0087),
`ErasureLeavesNothingTest` (step 80 lock 1, ADR 0093), step 78 rule "two tests per protection".
