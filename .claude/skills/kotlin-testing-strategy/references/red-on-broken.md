# Prove the test can fail

A test written together with the code it checks is often green for the wrong reason: it exercises a
path that doesn't involve the guard, the fake makes the race impossible, the assertion is on a
state that is also produced by the broken version.

## The discipline

1. Write the test.
2. Break the guard on purpose — the smallest change that reintroduces the defect:
   - remove `forUpdate()` / add `SKIP LOCKED`;
   - move the body read before the quota check;
   - delete the `assigneeId` comparison in a gate;
   - drop a `WITH CHECK` from a policy;
   - make the fake ignore `expectedVersion`.
3. Run: the test must fail **with a message that names the defect**.
4. Restore; run: green.
5. Put the recipe in the test's KDoc: *"To see it fail: drop `forUpdate(...)` from
   `balanceForUpdate` — both requests succeed."*

This is manual mutation testing, targeted at the one guard that matters. (Automated mutation tools
such as PIT exist for the JVM; for a handful of critical guards the manual recipe is cheaper and
documents itself.)

## The step's mine

Each step plan names the defect it is most likely to ship and the test that proves its absence
([`doc-first-agentic-workflow`](../../doc-first-agentic-workflow/SKILL.md), [`the-mine-and-locks`](../../the-mine-and-locks/SKILL.md)).
The mine test is the first one written for the step and is always proven red on a broken
implementation.

A mine usually has several ways to happen, so it gets several **numbered locks**, named in the test
titles so a red build points at the plan: `"12.1 lock 3 guard: …"`, `"12.1 lock 3: …"`,
`"12.1 lock 3 pair: …"` (stage where it was written, lock number, role). Guard / lock / pair are
described in [`security-boundaries.md`](security-boundaries.md).

## Red before the code exists

Locks written in the step's first stage cannot import classes that a later stage creates. They read
them **by name** (`Class.forName(…)`, a file path, a table name in SQL) and are red with
`ClassNotFoundException` / "relation does not exist" until the stage that builds the thing — then
turn green without being edited. A lock that was green before its feature existed proves nothing.

Red **by absence** is not yet red **on the claim**: a missing class or table proves only that the
test runs. Prove each lock red once more against a temporary scaffold of the missing piece that is
present but wrong (the erasure that skips one table, the route without its marker), then delete the
scaffold. Procedure and the notes line:
[`the-mine-and-locks` → red-proof](../../the-mine-and-locks/references/red-proof.md).
