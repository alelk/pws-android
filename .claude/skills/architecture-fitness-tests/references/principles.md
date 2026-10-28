# Principles

## 1. Exact equality, both directions

`actual shouldBe KNOWN.keys` — not `actual - KNOWN shouldBe empty`. The one-directional form lets
fixed violations linger in the list forever, and the list stops describing reality. Exact equality
forces the list to shrink in the same change that fixes an entry.

## 1a. A known entry carries its reason

```kotlin
/** File -> what must outlive the refusal. Closed: a new entry is a reviewed decision. */
private val KNOWN_COMMITS_ON_LEFT: Map<String, String> = mapOf(
    "identity/auth/SignInWithPin.kt" to "the wrong-PIN counter and the lock, or a PIN is guessed in an evening",
)
```

`Map<item, reason>`, not `List<item>`: the reason is what the next reviewer (or agent) weighs a
new entry against, and a bare name invites "one more". For a burn-down list the value can be the
stage or ticket that retires the entry (`KNOWN_LATER: Map<String, String>` -> `"step 12.3"`).
An empty ratchet may stay `emptySet()`. Template of this shape:
[`kotlin-domain-modeling` → `CommitsOnLeftTest.kt`](../../kotlin-domain-modeling/templates/domain/CommitsOnLeftTest.kt).

## 2. Guard against the vacuous pass

A source-scanning test that finds zero files passes every check. Every test file starts with:

```kotlin
test("source root is where the test thinks it is") {
    root.isDirectory shouldBe true
    root.ktFiles().size shouldNotBe 0
}
```

For database scans, name a few tables that **must** appear in the scanned set. For file-count rules,
pin the count (`contractSources.size shouldBe 39`) when a silent drop would matter, or a **floor**
(`sources.size > 500`, `memberRoutes.size >= 120`) when the set grows every week.

The guard is the **first test of the spec**, named as one ("guard: the sources are where this test
looks for them"), and it also proves the pattern can match: the allowed owner itself is found
(`builders.any { it.startsWith(GATEWAY) }`). A regex that matches nothing is as green as a clean
codebase. Floors and beacons for erasure-shaped and perimeter-shaped locks:
[`the-mine-and-locks` → beacons-and-floors](../../the-mine-and-locks/references/beacons-and-floors.md).

## 3. A property beats a list

When an exemption list exists, look for the criterion behind it and write the criterion down next to
the list. "Exactly four tables stay outside RLS" is weaker than "only a table whose purpose is to
turn an identifier into a tenant", because the second can be applied to the fifth candidate without
a meeting — and usually rejects it.

## 4. The test is checked on broken input

"Remove the `forUpdate`, see red, restore" (concurrency), "put `MaterialTheme.` in a screen, see red"
(conventions). A guard never observed failing may be guarding nothing.

## 5. Rules with reasons

Each test's KDoc: the rule, the defect it prevents (ideally the incident that motivated it), where
the rule is documented. Short. The failure message should lead a newcomer to the reason in one hop.

## 6. Pin surfaces, not handlers

The API surface comes from the running routing tree; the schema from the database; the screen list
from the declared entries. A list of "things we meant to have" drifts; the artefact doesn't.

## 7. Cheap and inside the gate

Milliseconds each (except schema tests that already share the Testcontainers database). They run in
`./gradlew build` on every change — a fitness test that runs nightly is found out too late.

## 8. What not to pin

Taste ("prefer `when` over `if`"), things a formatter already enforces, rules whose violations are
harmless. Every pinned rule is maintenance; spend it on defects and navigability.

## 9. Guard, lock, pair

A protection is pinned by up to three tests with one name stem:

| Test        | Says                                                                         |
|-------------|------------------------------------------------------------------------------|
| `… guard`   | today's behaviour, before the protection (the baseline the lock compares with; the walk is not vacuous) |
| `… lock`    | red on the broken thing: the stranger is refused, the forbidden path is closed |
| `… pair`    | the protection does not lock out its own: the owner, the right role, the allowed caller still pass |

A protection with only a lock is how a security step ships a door that refuses everybody — all
tests green, the product unusable. **Two tests per protection: "a stranger is refused" and "one's
own is not".** Name them after the step's mine (`<step>.<stage> lock N`, `… lock N pair`) so a red
build points at the plan that explains it.

## 10. Red before the code exists

A lock written in the plan's first stage — before the class it checks — reads that class **by name**
(`Class.forName("…AuditKind")`, a file path, a regex over source) instead of importing it. It is red
with `ClassNotFoundException`/"file not found" until the stage that builds the thing, and turns
green without being edited. This is how the mine's tests are written first and still compile.
Red **by absence** proves only that the test runs; prove it red **on its claim** once, with a
temporary scaffold of the missing class or table
([`the-mine-and-locks` → red-proof](../../the-mine-and-locks/references/red-proof.md)).

Evidence: family-planner step 76 (`CommitsOnLeftTest` with `KNOWN_COMMITS_ON_LEFT` as a map),
step 78 ("two tests per protection"), step 81 (`AuditSurfaceTest` reads `AuditKind` by name;
`81.1 lock N guard / lock N / lock N pair` naming).
